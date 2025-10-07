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

package controllers.optIn.newJourney

import auth.authV2.AuthActions
import com.google.inject.Inject
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseSuccess
import forms.optIn.SignUpTaxYearQuestionForm
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.optIn.{OptInService, SignUpUpdateService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.reportingObligations.JourneyCheckerSignUp
import views.html.optIn.newJourney.SignUpTaxYearQuestionView

import scala.concurrent.{ExecutionContext, Future}

class SignUpTaxYearQuestionController @Inject()(
                                                 authActions: AuthActions,
                                                 view: SignUpTaxYearQuestionView,
                                                 optInUpdateService: SignUpUpdateService,
                                                 val optInService: OptInService,
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
        optInService.isSignUpTaxYearValid(taxYear).flatMap {
          case Some(viewModel) =>
            withSessionData(isStart = false, viewModel.signUpTaxYear.taxYear) {
              Future(Ok(
                view(
                  isAgent,
                  viewModel,
                  SignUpTaxYearQuestionForm(viewModel.signUpTaxYear.taxYear, viewModel.signingUpForCY),
                  routes.SignUpTaxYearQuestionController.submit(isAgent, taxYear)
                )
              ))
            }
          case None =>
            Future(Redirect(controllers.routes.ReportingFrequencyPageController.show(isAgent).url))
        }
      }
  }

  def submit(isAgent: Boolean, taxYear: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withSignUpRFChecks {
        optInService.isSignUpTaxYearValid(taxYear).flatMap {
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
                    optInUpdateService.triggerOptInRequest().map {
                      case ITSAStatusUpdateResponseSuccess(_) =>
                        Redirect(routes.SignUpCompletedController.show(isAgent))
                      case _ =>
                        Redirect(controllers.optIn.oldJourney.routes.OptInErrorController.show(isAgent))
                    }
                  case Some(SignUpTaxYearQuestionForm.responseNo) =>
                    Future(Redirect(controllers.routes.ReportingFrequencyPageController.show(isAgent).url))
                  case _ =>
                    Logger("application").error("[SignUpTaxYearQuestionController.submit] Invalid form response")
                    Future(errorHandler(isAgent).showInternalServerError())
                }
              }
            )
          case None =>
            Future(Redirect(controllers.routes.ReportingFrequencyPageController.show(isAgent).url))
        }
      }
  }
}
