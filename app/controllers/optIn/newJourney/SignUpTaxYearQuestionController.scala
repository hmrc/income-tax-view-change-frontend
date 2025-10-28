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

import auth.MtdItUser
import auth.authV2.AuthActions
import com.google.inject.Inject
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseSuccess
import forms.optIn.SignUpTaxYearQuestionForm
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.optIn.OptInService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.reportingObligations.JourneyCheckerSignUp
import views.html.optIn.newJourney.SignUpTaxYearQuestionView

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignUpTaxYearQuestionController @Inject()(val optInService: OptInService,
                                                authActions: AuthActions,
                                                view: SignUpTaxYearQuestionView,
                                                val itvcErrorHandler: ItvcErrorHandler,
                                                val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                               )(implicit val appConfig: FrontendAppConfig,
                                                 val mcc: MessagesControllerComponents,
                                                 val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with JourneyCheckerSignUp {

  lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) =>
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler

  private def reportingObligationsRedirectUrl(isAgent: Boolean): String = {
    controllers.routes.ReportingFrequencyPageController.show(isAgent).url
  }

  def show(isAgent: Boolean, taxYear: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withSignUpRFChecks {
        optInService.isSignUpTaxYearValid(taxYear).flatMap {
          case Some(viewModel) =>
            retrieveIsJourneyComplete.flatMap { journeyIsComplete =>
              if (!journeyIsComplete) {
                withSessionData(isStart = false, viewModel.signUpTaxYear.taxYear, None) {
                  Future.successful(Ok(
                    view(
                      isAgent,
                      viewModel,
                      SignUpTaxYearQuestionForm(viewModel.signUpTaxYear.taxYear, viewModel.signingUpForCY),
                      routes.SignUpTaxYearQuestionController.submit(isAgent, taxYear)
                    )
                  ))
                }
              } else {
                Future.successful(Redirect(controllers.routes.SignUpOptOutCannotGoBackController.show(isAgent, isSignUpJourney = Some(true))))
              }
            }
          case None =>
            Future.successful(Redirect(reportingObligationsRedirectUrl(isAgent)))
        }
      }
  }

  def submit(isAgent: Boolean, taxYear: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withSignUpRFChecks {
        optInService.isSignUpTaxYearValid(taxYear).flatMap {
          case Some(viewModel) =>
            SignUpTaxYearQuestionForm(viewModel.signUpTaxYear.taxYear, viewModel.signingUpForCY).bindFromRequest().fold(
              formWithErrors => Future.successful(BadRequest(
                view(
                  isAgent,
                  viewModel,
                  formWithErrors,
                  routes.SignUpTaxYearQuestionController.submit(isAgent, taxYear)
                )
              )),
              form => {
                handleValidForm(form, isAgent)
              }
            )
          case None =>
            Future.successful(Redirect(reportingObligationsRedirectUrl(isAgent)))
        }
      }
  }

  private def handleValidForm(validForm: SignUpTaxYearQuestionForm, isAgent: Boolean)(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    val formResponse = validForm.toFormMap(SignUpTaxYearQuestionForm.response).headOption

    formResponse match {
      case Some(SignUpTaxYearQuestionForm.responseYes) =>
        optInService.makeOptInCall() map {
          case ITSAStatusUpdateResponseSuccess(_) => Redirect(routes.SignUpCompletedController.show(isAgent))
          case _ => Redirect(controllers.optIn.oldJourney.routes.OptInErrorController.show(isAgent))
        }
      case Some(SignUpTaxYearQuestionForm.responseNo) => Future.successful(Redirect(reportingObligationsRedirectUrl(isAgent)))
      case _ =>
        Logger("application").error("[SignUpTaxYearQuestionController.submit] Invalid form response")
        Future.successful(errorHandler(isAgent).showInternalServerError())
    }
  }
}
