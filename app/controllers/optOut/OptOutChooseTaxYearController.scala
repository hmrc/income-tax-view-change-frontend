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

package controllers.optOut

import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import forms.optOut.ConfirmOptOutMultiTaxYearChoiceForm
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import repositories.OptOutSessionDataRepository
import services.optout.OptOutService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.optOut.OptOutChooseTaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptOutChooseTaxYearController @Inject()(val optOutChooseTaxYear: OptOutChooseTaxYear,
                                              val optOutService: OptOutService,
                                              val repository: OptOutSessionDataRepository,
                                              val authActions: AuthActions,
                                              val itvcErrorHandler: ItvcErrorHandler,
                                              val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                             (implicit val appConfig: FrontendAppConfig,
                                              val ec: ExecutionContext,
                                              val mcc: MessagesControllerComponents
                                             )
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def show(isAgent: Boolean = false): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      for {
        (optOutProposition, intentTaxYear) <- optOutService.recallOptOutPropositionWithIntent()
        submissionCountForTaxYear <- optOutService.getQuarterlyUpdatesCountForOfferedYears(optOutProposition)
      } yield {
        val taxYearsList = optOutProposition.availableTaxYearsForOptOut.map(_.toString).toList
        val cancelUrl =
          if (isAgent) controllers.optOut.routes.OptOutCancelledController.showAgent().url
          else controllers.optOut.routes.OptOutCancelledController.show().url
        val form = intentTaxYear match {
          case Some(savedIntent) =>
            ConfirmOptOutMultiTaxYearChoiceForm(taxYearsList).fill(ConfirmOptOutMultiTaxYearChoiceForm(Some(savedIntent.toString)))
          case None =>
            ConfirmOptOutMultiTaxYearChoiceForm(taxYearsList)
        }
        Ok(optOutChooseTaxYear(form, optOutProposition.availableTaxYearsForOptOut, submissionCountForTaxYear, isAgent, cancelUrl))
      }
  }

  def submit(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      for {
        (optOutProposition, _) <- optOutService.recallOptOutPropositionWithIntent()
        quarterlyUpdatesCountModel <- optOutService.getQuarterlyUpdatesCountForOfferedYears(optOutProposition)
        cancelUrl =
          if (isAgent) controllers.optOut.routes.OptOutCancelledController.showAgent().url
          else controllers.optOut.routes.OptOutCancelledController.show().url
        formResult <- ConfirmOptOutMultiTaxYearChoiceForm(optOutProposition.availableTaxYearsForOptOut.map(_.toString).toList).bindFromRequest().fold(
          formWithError => Future.successful(
            BadRequest(
              optOutChooseTaxYear(
                formWithError,
                optOutProposition.availableTaxYearsForOptOut,
                quarterlyUpdatesCountModel,
                isAgent,
                cancelUrl
              ))
          ),
          form => saveTaxYearChoice(form).map {
            case true => redirectToCheckpointPage(isAgent)
            case false => itvcErrorHandler.showInternalServerError()
          }
        )
      } yield formResult
  }

  private def saveTaxYearChoice(form: ConfirmOptOutMultiTaxYearChoiceForm)(implicit request: RequestHeader): Future[Boolean] = {
    form.choice.flatMap(strFormat => TaxYear.getTaxYearModel(strFormat)).map { intent =>
      repository.saveIntent(intent)
    } getOrElse Future.failed(new RuntimeException("no tax-year choice available"))
  }

  private def redirectToCheckpointPage(isAgent: Boolean): Result = {
    val nextPage = controllers.optOut.routes.ConfirmOptOutController.show(isAgent)
    Logger("application").info(s"redirecting to : $nextPage")
    Redirect(nextPage)
  }
}