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
          (request.incomeSources.isConfirmedUser, isTriggeredMigrationPage) match {
            case (true, false) =>
              println(Console.CYAN_B + "Confirmed user " + request.incomeSources.channel + " accessing non-triggered migration page" + Console.RESET)
              Future(Right(req))
            case (true, true) =>
              println(Console.MAGENTA_B + "Confirmed user " + request.incomeSources.channel + " accessing triggered migration page" + Console.RESET)
              if (req.isAgent()) {
              Future(Left(Redirect(controllers.routes.HomeController.showAgent())))
            } else {
              Future(Left(Redirect(controllers.routes.HomeController.show())))
            }
            case (false, _) =>
              println(Console.YELLOW_B + "Unconfirmed user " + request.incomeSources.channel + " accessing page " + (if(isTriggeredMigrationPage) "triggered migration" else "non-triggered migration") + Console.RESET)
              isItsaStatusVoluntaryOrMandated().flatMap {
                case Right(false) =>
                  println(Console.RED_B + "User is not mandated or voluntary" + Console.RESET)
                  Future(Right(req))
                case Left(errorResult) =>
                  println(Console.RED_B + "Error retrieving ITSA status during triggered migration retrieval" + Console.RESET)
                  Future(Left(errorResult))
                case Right(true) =>
                  println(Console.GREEN_B + "User is mandated or voluntary, checking crystallisation status" + Console.RESET)
                  isCalculationCrystallised(req, req.incomeSources.startingTaxYear.toString).map {
                  case Right(true) =>
                    println(Console.GREEN_B + "Calculation is crystallised" + Console.RESET)
                    Right(req)
                  case Right(false) => if(isTriggeredMigrationPage) {
                    println(Console.YELLOW_B + "Calculation is not crystallised" + Console.RESET)
                    Right(req)
                  } else {
                    println(Console.CYAN_B + "Redirecting to check HMRC records page as calculation is not crystallised" + Console.RESET)
                    Left(Redirect(controllers.triggeredMigration.routes.CheckHmrcRecordsController.show(req.isAgent())))
                  }
                  case Left(errorResult) =>
                    println(Console.RED_B + "Error retrieving calculation crystallisation status during triggered migration retrieval" + Console.RESET)
                    Left(errorResult)
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

  private def isItsaStatusVoluntaryOrMandated()(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Result, Boolean]] = {
    def redirectBasedOnUser: Future[Either[Result, Boolean]] =
      Future(Left(if (user.isAgent()) Redirect(controllers.routes.HomeController.showAgent()) else Redirect(controllers.routes.HomeController.show())))

    ITSAStatusService.getITSAStatusDetail(dateService.getCurrentTaxYear, futureYears = false, history = false).flatMap {
      case statusDetail if statusDetail.exists(_.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary))) =>
        Future(Right(true))
      case statusDetail if statusDetail.exists(_.itsaStatusDetails.exists(_.exists(!_.isMandatedOrVoluntary))) =>
        Future(Right(false))
      case _ =>
        ITSAStatusService.getITSAStatusDetail(dateService.getCurrentTaxYear.nextYear, futureYears = false, history = false).flatMap {
          case statusDetail if statusDetail.nonEmpty => redirectBasedOnUser
          case _ => Future(Left(internalServerErrorFor(user, "Error retrieving ITSA status during triggered migration retrieval")))
        }
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
