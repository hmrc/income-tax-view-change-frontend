/*
 * Copyright 2026 HM Revenue & Customs
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
import connectors.IncomeTaxCalculationConnector
import controllers.BaseController
import enums.TaxYearSummary.CalculationRecord.LATEST
import enums.TriggeredMigration.Channel
import enums.TriggeredMigration.Channel.confirmedUsers
import models.admin.TriggeredMigration
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import services.{DateServiceInterface, ITSAStatusService}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TriggeredMigrationRetrievalAction @Inject()(
                                                   frontendAppConfig: FrontendAppConfig,
                                                   ITSAStatusService: ITSAStatusService,
                                                   incomeTaxConnector: IncomeTaxCalculationConnector,
                                                   dateService: DateServiceInterface
                                                 )
                                                 (implicit val executionContext: ExecutionContext,
                                                  individualErrorHandler: ItvcErrorHandler,
                                                  agentErrorHandler: AgentItvcErrorHandler,
                                                  mcc: MessagesControllerComponents)
  extends BaseController with FeatureSwitching {

  override val appConfig: FrontendAppConfig = frontendAppConfig

  def apply(isTriggeredMigrationPage: Boolean): ActionRefiner[MtdItUser, MtdItUser] =
    new ActionRefiner[MtdItUser, MtdItUser] {

      implicit val executionContext: ExecutionContext = mcc.executionContext

      override protected def refine[A](request: MtdItUser[A]): Future[Either[Result, MtdItUser[A]]] = {
        implicit val req: MtdItUser[A] = request

        lazy val authAction = {
          (confirmedUsers.contains(request.incomeSources.channel), isTriggeredMigrationPage) match {
            case (true, false) => Future(Right(req))
            case (true, true) => Future(Left(Redirect(controllers.routes.HomeController.show())))
            case (false, _) =>
              isItsaStatusVoluntaryOrMandated().flatMap {
                case false => Future(Right(req))
                case true => isCalculationCrystallised(req, req.incomeSources.startingTaxYear.toString).map {
                  case Right(true) => Right(req)
                  case Right(false) => Left(Redirect(controllers.triggeredMigration.routes.CheckHmrcRecordsController.show(req.isAgent())))
                  case Left(errorResult) => Left(errorResult)
                }
              }
          }
        }

        if (isEnabled(TriggeredMigration)) {
          authAction
        } else {
          Future(Right(req))
        }
      }
    }

  private def isCalculationCrystallised(req: MtdItUser[_], taxYear: String)(implicit hc: HeaderCarrier) = {
    incomeTaxConnector.getCalculationResponse(
      req.mtditid,
      req.nino,
      taxYear,
      Some(LATEST)
    ).map {
      case calcError: LiabilityCalculationError if calcError.status == NO_CONTENT => Right(false)
      case _: LiabilityCalculationError => Left(internalServerErrorFor(req, "Error retrieving liability calculation during triggered migration retrieval", None))
      case calcResponse: LiabilityCalculationResponse => Right(calcResponse.metadata.isCalculationCrystallised)
    }
  }

  private def isItsaStatusVoluntaryOrMandated()(implicit hc: HeaderCarrier, user: MtdItUser[_]) = {
    ITSAStatusService.getITSAStatusDetail(
      taxYear = dateService.getCurrentTaxYear,
      futureYears = false,
      history = false
    ).map {
      case statusDetails if statusDetails.exists(_.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary))) => true
      case _ => false
    }
  }

  private def internalServerErrorFor(
                                      request: MtdItUser[_],
                                      context: String,
                                      optError: Option[ItsaStatusRetrievalActionError] = None
                                    ): Result = {

    val logPrefix = s"[TriggeredMigrationRetrievalAction][$context]"

    (request.authUserDetails.affinityGroup, optError) match {
      case (Some(Agent), Some(error)) =>
        Logger(getClass).error(s"$logPrefix Agent error: ${error.logMessage}")
        agentErrorHandler.showInternalServerError()(request)
      case (Some(Individual), Some(error)) =>
        Logger(getClass).error(s"$logPrefix Individual error: ${error.logMessage}")
        individualErrorHandler.showInternalServerError()(request)
      case (Some(Organisation), Some(error)) =>
        Logger(getClass).error(s"$logPrefix Organisation error: ${error.logMessage}")
        individualErrorHandler.showInternalServerError()(request)
      case _ =>
        Logger(getClass).error(s"$logPrefix Unknown user type or error")
        individualErrorHandler.showInternalServerError()(request)
    }
  }
}
