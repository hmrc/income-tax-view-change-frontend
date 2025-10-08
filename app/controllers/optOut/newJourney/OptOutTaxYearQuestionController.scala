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

package controllers.optOut.newJourney

import auth.MtdItUser
import auth.authV2.AuthActions
import com.google.inject.Inject
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseSuccess
import forms.optOut.OptOutTaxYearQuestionForm
import models.itsaStatus.ITSAStatus
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optout.{OptOutService, OptOutSubmissionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.reportingObligations.JourneyCheckerOptOut
import views.html.optOut.newJourney.OptOutTaxYearQuestionView

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
    controllers.routes.ReportingFrequencyPageController.show(isAgent).url

  def show(isAgent: Boolean, taxYear: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptOutRFChecks {
        optOutService.isOptOutTaxYearValid(taxYear).flatMap {
          case Some(viewModel) =>
            withSessionData(true, viewModel.taxYear.taxYear) {
              Future(Ok(
                view(
                  isAgent,
                  viewModel,
                  OptOutTaxYearQuestionForm(viewModel.taxYear.taxYear),
                  controllers.optOut.newJourney.routes.OptOutTaxYearQuestionController.submit(isAgent, taxYear)
                )
              ))
            }
          case None => Future(Redirect(reportingObligationsRedirectUrl(isAgent)))
        }
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
          Future(Redirect(controllers.optOut.newJourney.routes.ConfirmOptOutUpdateController.show(isAgent, taxYear.getOrElse(""))))
        } else {
          optOutSubmissionService.updateTaxYearsITSAStatusRequest(ITSAStatus.Voluntary).map {
            case List(ITSAStatusUpdateResponseSuccess(_)) =>
              Redirect(controllers.optOut.routes.ConfirmedOptOutController.show(isAgent))
            case _ =>
              Redirect(controllers.optOut.oldJourney.routes.OptOutErrorController.show(isAgent))
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
                      controllers.optOut.newJourney.routes.OptOutTaxYearQuestionController.submit(isAgent, taxYear)
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
