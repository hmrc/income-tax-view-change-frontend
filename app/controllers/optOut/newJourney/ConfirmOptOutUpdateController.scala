/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.optOut.newJourney

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.itsastatus.ITSAStatusUpdateConnectorModel._
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import models.optout.newJourney.CheckOptOutUpdateAnswersViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optout.{ConfirmOptOutUpdateService, OptOutService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.reportingObligations.JourneyCheckerOptOut
import views.html.optOut.newJourney.CheckOptOutUpdateAnswers

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class ConfirmOptOutUpdateController @Inject()(
                                               authActions: AuthActions,
                                               confirmOptOutUpdateService: ConfirmOptOutUpdateService,
                                               view: CheckOptOutUpdateAnswers,
                                               val optOutService: OptOutService,
                                               val itvcErrorHandler: ItvcErrorHandler,
                                               val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                             )(
                                               implicit val appConfig: FrontendAppConfig,
                                               val ec: ExecutionContext,
                                               val mcc: MessagesControllerComponents
                                             )
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with JourneyCheckerOptOut {

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception => handleError(s"request failed :: $ex", isAgent)
    }
  }

  private def handleError(message: String, isAgent: Boolean)(implicit request: Request[_]): Result = {
    val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    Logger("application").error(message)
    errorHandler(isAgent).showInternalServerError()
  }

  def show(isAgent: Boolean = false, taxYear: String): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptOutRFChecks {
        withRecover(isAgent) {
          withSessionData(isStart = false, TaxYear(taxYear.toInt, taxYear.toInt + 1)) {
            for {
              optOutProposition <- optOutService.fetchOptOutProposition()
              quarterlyUpdatesCount <- optOutService.getQuarterlyUpdatesCount(optOutProposition.optOutPropositionType)
            } yield {
              val selectedTaxYear: TaxYear = TaxYear(taxYear.toInt, taxYear.toInt + 1)
              val reportingObligationsURL = controllers.routes.ReportingFrequencyPageController.show(isAgent).url
              Ok(view(CheckOptOutUpdateAnswersViewModel(selectedTaxYear, quarterlyUpdatesCount), isAgent, reportingObligationsURL))
            }
          }
        }
      }
  }

  def submit(isAgent: Boolean, @unused taxYear: String): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user =>
        withOptOutRFChecks {
          for {
            updateTaxYearsITSAStatusRequest: List[ITSAStatusUpdateResponse] <- confirmOptOutUpdateService.updateTaxYearsITSAStatusRequest(itsaStatusToSendUpdatesFor = Voluntary)
            result = updateTaxYearsITSAStatusRequest match {
              case listOfUpdateRequestsMade if !listOfUpdateRequestsMade.exists(_.isInstanceOf[ITSAStatusUpdateResponseFailure]) || listOfUpdateRequestsMade == List.empty  =>
                Redirect(controllers.optOut.routes.ConfirmedOptOutController.show(isAgent))
              case listOfUpdateRequestsMade if listOfUpdateRequestsMade.exists(_.isInstanceOf[ITSAStatusUpdateResponseFailure]) =>
                Redirect(controllers.optOut.oldJourney.routes.OptOutErrorController.show(isAgent))
              case _ =>
                itvcErrorHandler.showInternalServerError()
            }
          } yield {
            result
          }
        }
    }
}