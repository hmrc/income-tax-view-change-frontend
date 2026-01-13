/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package auth.authV2.actions

import auth.MtdItUser
import auth.authV2.models.ItsaStatusRetrievalActionError
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.ITSAStatusConnector
import controllers.BaseController
import models.admin.`CY+1YouMustWaitToSignUpPageEnabled`
import play.api.Logger
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import services.DateServiceInterface
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ItsaStatusRetrievalAction @Inject()(
                                           frontendAppConfig: FrontendAppConfig,
                                           itsaStatusConnector: ITSAStatusConnector,
                                           dateService: DateServiceInterface
                                         )(
                                           implicit val executionContext: ExecutionContext,
                                           individualErrorHandler: ItvcErrorHandler,
                                           agentErrorHandler: AgentItvcErrorHandler,
                                           mcc: MessagesControllerComponents
                                         ) extends BaseController with ActionRefiner[MtdItUser, MtdItUser] with FeatureSwitching {

  override val appConfig: FrontendAppConfig = frontendAppConfig

  private def internalServerErrorFor(
                                      request: MtdItUser[_],
                                      context: String,
                                      optError: Option[ItsaStatusRetrievalActionError] = None
                                    ): Result = {

    val logPrefix = s"[ITSAStatusRetrievalAction][$context]"

    (request.authUserDetails.affinityGroup, optError) match {
      case (Some(Agent), Some(error)) =>
        Logger(getClass).error(s"$logPrefix Agent error: ${error.logMessage}")
        agentErrorHandler.showInternalServerError()(request)
      case (Some(Individual), Some(error)) =>
        Logger(getClass).error(s"$logPrefix Individual error: ${error.logMessage}")
        individualErrorHandler.showInternalServerError()(request)
      case _ =>
        Logger(getClass).error(s"$logPrefix Unknown user type or error")
        individualErrorHandler.showInternalServerError()(request)
    }
  }

  //scalastyle:off
  override def refine[A](request: MtdItUser[A]): Future[Either[Result, MtdItUser[A]]] = {

    implicit val req: MtdItUser[A] = request

    lazy val authAction: Future[Either[Result, MtdItUser[A]]] =
      itsaStatusConnector.getITSAStatusDetail(
        nino = req.nino,
        taxYear = dateService.getCurrentTaxYear.formatAsShortYearRange,
        futureYears = true,
        history = false
      ).map {
        case Right(statuses) if statuses.size == 1 && statuses.head.taxYear == dateService.getCurrentTaxYear.nextYear.shortenTaxYearEnd =>
          req.authUserDetails.affinityGroup match {
            case Some(Individual) =>
              Logger(getClass).info(s"[ItsaStatusRetrievalAction][refine] Redirecting user to Non-Agent/Individual's YouMustWaitToSignUp page")
              Left(Redirect(controllers.optIn.routes.YouMustWaitToSignUpController.show(isAgent = false)))
            case Some(Agent) =>
              Logger(getClass).info(s"[ItsaStatusRetrievalAction][refine] Redirecting user to Agent YouMustWaitToSignUp page")
              Left(Redirect(controllers.optIn.routes.YouMustWaitToSignUpController.show(isAgent = true)))
            case Some(Organisation) =>
              Logger(getClass).error(s"[ItsaStatusRetrievalAction][refine] User has passed in as organisation affinity group, redirecting to internal server error page for user")
              Left(internalServerErrorFor(req, "affinity-group", None))
            case None =>
              Logger(getClass).error(s"[ItsaStatusRetrievalAction][refine] Unsuccessful income source and itsa details retrieved or unknown error, redirecting to internal server error page for user")
              Left(internalServerErrorFor(req, "affinity-group", None))
          }
        case Right(_) =>
          Logger(getClass).info(s"[ItsaStatusRetrievalAction][refine] Successful income source and itsa details retrieved, enriching MtdItUser request")
          Right(req)
        case Left(error) =>
          Logger(getClass).error(s"[ItsaStatusRetrievalAction][refine] Unsuccessful income source and itsa details retrieved or unknown error, redirecting to internal server error page for user")
          Left(internalServerErrorFor(req, "itsa-status", Some(ItsaStatusRetrievalActionError.ItsaStatus(error))))
      }

    if (isEnabled(`CY+1YouMustWaitToSignUpPageEnabled`)) {
      authAction
    } else {
      Future(Right(req))
    }
  }
}
//scalastyle:on