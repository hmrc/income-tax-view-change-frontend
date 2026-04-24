/*
 * Copyright 2025 HM Revenue & Customs
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

package obligations.controllers.reportingObligations.optOut

import auth.MtdItUser
import auth.authV2.AuthActions
import com.google.inject.Inject
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import forms.reportingObligations.optOut.OptOutTaxYearQuestionForm
import obligations.connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseFailure
import obligations.controllers.errors.routes as errorRoutes
import obligations.controllers.reportingObligations.optOut.routes
import obligations.controllers.reportingObligations.routes as reportingObligationsRoutes
import obligations.services.reportingObligations.optOut.{OptOutService, OptOutSubmissionService}
import obligations.utils.reportingObligations.JourneyCheckerOptOut
import obligations.views.html.reportingObligations.optOut.OptOutTaxYearQuestionView
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OptOutTaxYearQuestionController @Inject()(
                                                 authActions: AuthActions,
                                                 optOutSubmissionService: OptOutSubmissionService,
                                                 view: OptOutTaxYearQuestionView,
                                                 val optOutService: OptOutService,
                                                 val itvcErrorHandler: ItvcErrorHandler,
                                                 val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                               )(implicit val appConfig: FrontendAppConfig,
                                                 val mcc: MessagesControllerComponents,
                                                 val ec: ExecutionContext)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with JourneyCheckerOptOut {

  lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) =>
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler

  private def reportingObligationsRedirectUrl(isAgent: Boolean): String =
    reportingObligationsRoutes.ReportingFrequencyPageController.show(isAgent).url

  def show(isAgent: Boolean, taxYear: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptOutRFChecks {
        optOutService.fetchJourneyCompleteStatus().flatMap(journeyIsComplete => {
          if(!journeyIsComplete){
            optOutService.isOptOutTaxYearValid(taxYear).flatMap {
              case Some(viewModel) =>
                withSessionData(isStart = true, viewModel.taxYear.taxYear) {
                  Future(Ok(
                    view(
                      isAgent,
                      viewModel,
                      OptOutTaxYearQuestionForm(viewModel.taxYear.taxYear),
                      routes.OptOutTaxYearQuestionController.submit(isAgent, taxYear)
                    )
                  ))
                }
              case None => Future(Redirect(reportingObligationsRedirectUrl(isAgent)))
            }
          }
          else Future.successful(Redirect(errorRoutes.SignUpOptOutCannotGoBackController.show(isAgent, isSignUpJourney = Some(false))))
        })
      }
  }


  private def handleValidForm(
                               validForm: OptOutTaxYearQuestionForm,
                               isAgent: Boolean,
                               taxYear: Option[String],
                               redirectToConfirmUpdatesPage: Boolean
                             )(implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val formResponse = validForm.toFormMap(OptOutTaxYearQuestionForm.response).headOption

    formResponse match {
      case Some(OptOutTaxYearQuestionForm.responseYes) =>
        if (redirectToConfirmUpdatesPage) {
          Future(Redirect(routes.ConfirmOptOutUpdateController.show(isAgent, taxYear.getOrElse(""))))
        } else {
          optOutSubmissionService.updateTaxYearsITSAStatusRequest().map {
            case List() =>
              Redirect(errorRoutes.CannotUpdateReportingObligationsController.show(isAgent))
            case listOfUpdateRequestsMade if !listOfUpdateRequestsMade.exists(_.isInstanceOf[ITSAStatusUpdateResponseFailure]) =>
              Redirect(routes.ConfirmedOptOutController.show(isAgent))
            case _ =>
              Redirect(errorRoutes.CannotUpdateReportingObligationsController.show(isAgent))
          }
        }
      case Some(OptOutTaxYearQuestionForm.responseNo) =>
        Future(Redirect(reportingObligationsRedirectUrl(isAgent)))
      case _ =>
        Logger("application").error("[OptOutTaxYearQuestionController.submit] Invalid form response")
        Future(errorHandler(isAgent).showInternalServerError())
    }
  }

  def submit(isAgent: Boolean, taxYear: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptOutRFChecks {
        optOutService.isOptOutTaxYearValid(taxYear).flatMap {
          case Some(viewModel) =>
            OptOutTaxYearQuestionForm(viewModel.taxYear.taxYear).bindFromRequest().fold(
              formWithErrors => {
                Future {
                  BadRequest(
                    view(
                      isAgent,
                      viewModel,
                      formWithErrors,
                      routes.OptOutTaxYearQuestionController.submit(isAgent, taxYear)
                    )
                  )
                }
              },
              form =>
                handleValidForm(form, isAgent, taxYear, viewModel.redirectToConfirmUpdatesPage)
            )
          case None =>
            Logger("application").warn(s"[OptOutTaxYearQuestionController.submit] Invalid tax year provided: $taxYear, redirecting to Reporting Frequency Page")
            Future(Redirect(reportingObligationsRedirectUrl(isAgent)))
        }
      }
  }
}
