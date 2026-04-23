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

package obligations.controllers.reportingObligations.signUp

import auth.authV2.AuthActions
import com.google.inject.Inject
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import obligations.connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseFailure
import obligations.controllers.errors.routes as errorRoutes
import obligations.controllers.reportingObligations.routes as reportingObligationsRoutes
import obligations.controllers.reportingObligations.signUp.routes
import forms.reportingObligations.signUp.SignUpTaxYearQuestionForm
import obligations.services.reportingObligations.signUp.{SignUpService, SignUpSubmissionService}
import obligations.utils.reportingObligations.JourneyCheckerSignUp
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import obligations.views.html.reportingObligations.signUp.SignUpTaxYearQuestionView

import scala.concurrent.{ExecutionContext, Future}

class SignUpTaxYearQuestionController @Inject()(
                                                 authActions: AuthActions,
                                                 view: SignUpTaxYearQuestionView,
                                                 signUpSubmissionService: SignUpSubmissionService,
                                                 val signUpService: SignUpService,
                                                 val itvcErrorHandler: ItvcErrorHandler,
                                                 val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                               )(implicit val appConfig: FrontendAppConfig,
                                                 val mcc: MessagesControllerComponents,
                                                 val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with JourneyCheckerSignUp {

  private def errorHandler(isAgent: Boolean): ShowInternalServerError =
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler

  def show(isAgent: Boolean, taxYear: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withSignUpRFChecks {
        signUpService.isSignUpTaxYearValid(taxYear).flatMap {
          case Some(viewModel) =>
            retrieveIsJourneyComplete.flatMap { journeyIsComplete =>
              if (!journeyIsComplete) {
                withSessionData(isStart = false, viewModel.signUpTaxYear.taxYear, None) {
                  Future(Ok(
                    view(
                      isAgent,
                      viewModel,
                      SignUpTaxYearQuestionForm(viewModel.signUpTaxYear.taxYear, viewModel.signingUpForCY),
                      routes.SignUpTaxYearQuestionController.submit(isAgent, taxYear)
                    )
                  ))
                }
              } else {
                Future.successful(Redirect(errorRoutes.SignUpOptOutCannotGoBackController.show(isAgent, isSignUpJourney = Some(true))))
              }
            }
          case None =>
            Future(Redirect(reportingObligationsRoutes.ReportingFrequencyPageController.show(isAgent).url))
        }
      }
  }

  def submit(isAgent: Boolean, taxYear: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withSignUpRFChecks {
        signUpService.isSignUpTaxYearValid(taxYear).flatMap {
          case Some(viewModel) =>
            SignUpTaxYearQuestionForm(viewModel.signUpTaxYear.taxYear, viewModel.signingUpForCY).bindFromRequest().fold(
              formWithErrors => Future(BadRequest(
                view(
                  isAgent = isAgent,
                  viewModel = viewModel,
                  form = formWithErrors,
                  postAction = routes.SignUpTaxYearQuestionController.submit(isAgent, taxYear)
                )
              )),
              form => {
                val formResponse = form.toFormMap(SignUpTaxYearQuestionForm.response).headOption
                formResponse match {
                  case Some(SignUpTaxYearQuestionForm.responseYes) =>
                    signUpSubmissionService.triggerSignUpRequest().map {
                      case response if response.isInstanceOf[ITSAStatusUpdateResponseFailure] =>
                        Redirect(errorRoutes.CannotUpdateReportingObligationsController.show(isAgent))
                      case _ =>
                        Redirect(routes.SignUpCompletedController.show(isAgent))
                    }
                  case Some(SignUpTaxYearQuestionForm.responseNo) =>
                    Future(Redirect(reportingObligationsRoutes.ReportingFrequencyPageController.show(isAgent).url))
                  case _ =>
                    Logger("application").error("[SignUpTaxYearQuestionController.submit] Invalid form response")
                    Future(errorHandler(isAgent).showInternalServerError())
                }
              }
            )
          case None =>
            Future(Redirect(reportingObligationsRoutes.ReportingFrequencyPageController.show(isAgent).url))
        }
      }
  }
}
