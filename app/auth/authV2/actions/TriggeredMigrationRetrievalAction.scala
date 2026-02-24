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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.IncomeTaxCalculationConnector
import controllers.BaseController
import enums.JourneyType.TriggeredMigrationJourney
import enums.TaxYearSummary.CalculationRecord.LATEST
import models.admin.TriggeredMigration
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import services.{CustomerFactsUpdateService, DateServiceInterface, ITSAStatusService, SessionService}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TriggeredMigrationRetrievalAction @Inject()(
                                                   frontendAppConfig: FrontendAppConfig,
                                                   ITSAStatusService: ITSAStatusService,
                                                   incomeTaxConnector: IncomeTaxCalculationConnector,
                                                   dateService: DateServiceInterface,
                                                   customerFactsUpdateService: CustomerFactsUpdateService,
                                                   sessionService: SessionService
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

        lazy val authAction: Future[Either[Result, MtdItUser[A]]] = {
          (request.incomeSources.isConfirmedUser, isTriggeredMigrationPage) match {
            case (true, false) => Future(Right(req))
            case (true, true) =>
              checkIfRecentlyConfirmed().map {
                case true => Right(req)
                case false => Left(redirectToHome(req.isAgent()))
              }
            case (false, _) =>
              isItsaStatusVoluntaryOrMandated().flatMap {
                case Right(false) => confirmIneligibleUser(req, isTriggeredMigrationPage)
                case Left(errorResult) => Future(Left(errorResult))
                case Right(true) => isCalculationCrystallised(req, req.incomeSources.startingTaxYear.toString).flatMap {
                  case Right(true) => confirmIneligibleUser(req, isTriggeredMigrationPage)
                  case Right(false) => if (isTriggeredMigrationPage) {
                    Future.successful(Right(req))
                  } else {
                    Future.successful(
                      Left(Redirect(controllers.triggeredMigration.routes.CheckHmrcRecordsController.show(req.isAgent())))
                    )
                  }
                  case Left(errorResult) => Future.successful(Left(errorResult))
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
      case calcResponse: LiabilityCalculationResponse => Right(calcResponse.metadata.isCalculationCrystallised)
      case _: LiabilityCalculationError => Left(internalServerErrorFor(req, "Error retrieving liability calculation during triggered migration retrieval"))
    }
  }

  private def isItsaStatusVoluntaryOrMandated()(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Result, Boolean]] = {
    def redirectBasedOnUser: Future[Either[Result, Boolean]] =
      Future(Left(if (user.isAgent()) Redirect(controllers.routes.HomeController.showAgent()) else Redirect(controllers.routes.HomeController.show())))

    ITSAStatusService.getITSAStatusDetail(dateService.getCurrentTaxYear, futureYears = true, history = false).flatMap {
      itsaStatusList => itsaStatusList.find(_.taxYear == dateService.getCurrentTaxYear.shortenTaxYearEnd) match {
          case Some(status) if status.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary)) => Future(Right(true))
          case Some(status) if status.itsaStatusDetails.exists(_.exists(!_.isMandatedOrVoluntary)) => Future(Right(false))
          case _ => itsaStatusList.find(_.taxYear == dateService.getCurrentTaxYear.nextYear.shortenTaxYearEnd) match {
              case Some(_) => redirectBasedOnUser
              case _ => Future(Left(internalServerErrorFor(user, "Error retrieving ITSA status during triggered migration retrieval - no current or next year status found")))
          }
      }
    }
  }

  private def internalServerErrorFor(
                                      request: MtdItUser[_],
                                      context: String
                                    ): Result = {

    Logger(getClass).error(s"[TriggeredMigrationRetrievalAction][$context]")
    request.authUserDetails.affinityGroup match {
      case Some(Agent) => agentErrorHandler.showInternalServerError()(request)
      case _ => individualErrorHandler.showInternalServerError()(request)
    }
  }

  private def checkIfRecentlyConfirmed()(implicit hc: HeaderCarrier): Future[Boolean] = {
    sessionService.getMongo(TriggeredMigrationJourney).map {
      case Right(Some(data)) =>
        data.triggeredMigrationData match {
          case Some(trigMigData) =>
            Logger(getClass).info(s"[TriggeredMigrationRetrievalAction][checkIfRecentlyConfirmed] triggeredMigrationData found in session data")
            trigMigData.recentlyConfirmed
          case None =>
            Logger(getClass).info(s"[TriggeredMigrationRetrievalAction][checkIfRecentlyConfirmed] triggeredMigrationData missing from session data")
            false
        }
      case _ =>
        Logger(getClass).info(s"[TriggeredMigrationRetrievalAction][checkIfRecentlyConfirmed] No session data found for TriggeredMigrationJourney")
        false
    }
  }

  private def redirectToHome(isAgent: Boolean): Result = {
    if (isAgent) {
      Redirect(controllers.routes.HomeController.showAgent())
    } else {
      Redirect(controllers.routes.HomeController.show())
    }
  }

  private def confirmIneligibleUser[A](req: MtdItUser[A], isTriggeredMigrationPage: Boolean)(implicit hc: HeaderCarrier) = {
    customerFactsUpdateService.updateCustomerFacts(req.mtditid).map {
      _ =>
        if (isTriggeredMigrationPage) {
          Left(redirectToHome(req.isAgent()))
        } else {
          Right(req)
        }
    }
  }
}
