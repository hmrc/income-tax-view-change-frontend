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

package controllers.optOut

import auth.MtdItUser
import auth.authV2.AuthActions
import com.google.inject.Inject
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import forms.optOut.OptOutTaxYearQuestionForm
import models.admin.{OptOutFs, ReportingFrequencyPage}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optout.OptOutService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.optOut.OptOutTaxYearQuestionView

import scala.concurrent.{ExecutionContext, Future}

class OptOutTaxYearQuestionController @Inject()(optOutService: OptOutService,
                                                authActions: AuthActions,
                                                view: OptOutTaxYearQuestionView,
                                                itvcErrorHandler: ItvcErrorHandler,
                                                itvcErrorHandlerAgent: AgentItvcErrorHandler
                                               )(implicit val appConfig: FrontendAppConfig,
                                                 val mcc: MessagesControllerComponents,
                                                 val ec: ExecutionContext)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport {

  lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) =>
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler

  def show(isAgent: Boolean, taxYear: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptOutRFChecks(isAgent) {
        optOutService.isOptOutTaxYearValid(taxYear).flatMap {
          case Some(viewModel) =>
            Future.successful(Ok(
              view(
                isAgent,
                viewModel,
                OptOutTaxYearQuestionForm(viewModel.taxYear.taxYear),
                routes.OptOutTaxYearQuestionController.submit(isAgent, taxYear)
              )
            ))
          case None =>
            Future.successful(Redirect(reportingObligationsRedirectUrl(isAgent)))
        }
      }
  }

  def submit(isAgent: Boolean, taxYear: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptOutRFChecks(isAgent) {
        optOutService.isOptOutTaxYearValid(taxYear).flatMap {
          case Some(viewModel) =>
            OptOutTaxYearQuestionForm(viewModel.taxYear.taxYear).bindFromRequest().fold(
              formWithErrors => {
                Future.successful {
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
              form => handleValidForm(form, isAgent, taxYear)
            )
          case None =>
            Logger("application").warn(s"[OptOutTaxYearQuestionController.submit] Invalid tax year provided: $taxYear, redirecting to Reporting Frequency Page")
            Future.successful(Redirect(reportingObligationsRedirectUrl(isAgent)))
        }
      }

  }

  private def handleValidForm(validForm: OptOutTaxYearQuestionForm, isAgent: Boolean, taxYear: Option[String])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    val formResponse = validForm.toFormMap(OptOutTaxYearQuestionForm.response).headOption

    formResponse match {
      case Some(OptOutTaxYearQuestionForm.responseYes) => Future.successful(Redirect(routes.OptOutTaxYearQuestionController.show(isAgent, taxYear)))
      case Some(OptOutTaxYearQuestionForm.responseNo) => Future.successful(Redirect(reportingObligationsRedirectUrl(isAgent)))
      case _ =>
        Logger("application").error("[OptOutTaxYearQuestionController.submit] Invalid form response")
        Future.successful(errorHandler(isAgent).showInternalServerError())
    }
  }

  private def withOptOutRFChecks(isAgent: Boolean)(codeBlock: => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    (isEnabled(OptOutFs), isEnabled(ReportingFrequencyPage)) match {
      case (true, true) => codeBlock
      case _ =>
        Logger("application").warn("[OptOutTaxYearQuestionController] Feature Switches not enabled, redirecting to Home Page")
        Future.successful(Redirect(homePageRedirectUrl(isAgent)))
    }
  }

  private def reportingObligationsRedirectUrl(isAgent: Boolean): String = {
    controllers.routes.ReportingFrequencyPageController.show(isAgent).url
  }

  private def homePageRedirectUrl(isAgent: Boolean): Call = {
    if (isAgent) controllers.routes.HomeController.showAgent()
    else controllers.routes.HomeController.show()
  }
}
