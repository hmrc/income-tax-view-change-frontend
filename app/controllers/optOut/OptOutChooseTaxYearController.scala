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

import auth.FrontendAuthorisedFunctions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import forms.optOut.ConfirmOptOutMultiTaxYearChoiceForm
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optout.OptOutService
import utils.AuthenticatorPredicate
import views.html.optOut.OptOutChooseTaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptOutChooseTaxYearController @Inject()(val optOutChooseTaxYear: OptOutChooseTaxYear,
                                              val optOutService: OptOutService)
                                             (implicit val appConfig: FrontendAppConfig,
                                              val ec: ExecutionContext,
                                              val auth: AuthenticatorPredicate,
                                              val authorisedFunctions: FrontendAuthorisedFunctions,
                                              val itvcErrorHandler: ItvcErrorHandler,
                                              val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                              override val mcc: MessagesControllerComponents
                                             )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(isAgent: Boolean = false): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      for {
        optOutProposition <- optOutService.fetchOptOutProposition()
        submissionCountForTaxYear <- optOutService.getQuarterlyUpdatesCountForOfferedYears(optOutProposition)
        intent <- optOutService.fetchSavedIntent()
      } yield {
        val taxYearsList = optOutProposition.availableTaxYearsForOptOut.map(_.toString).toList
        val cancelURL = if (isAgent) controllers.routes.NextUpdatesController.showAgent.url else controllers.routes.NextUpdatesController.show().url
        val form = intent match {
          case Some(savedIntent) =>
            ConfirmOptOutMultiTaxYearChoiceForm(taxYearsList).fill(ConfirmOptOutMultiTaxYearChoiceForm(Some(savedIntent.toString)))
          case None =>
            ConfirmOptOutMultiTaxYearChoiceForm(taxYearsList)
        }
        Ok(optOutChooseTaxYear(form, optOutProposition.availableTaxYearsForOptOut, submissionCountForTaxYear, isAgent, cancelURL))
      }
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      for {
        optOutProposition <- optOutService.fetchOptOutProposition()
        quarterlyUpdatesCountModel <- optOutService.getQuarterlyUpdatesCountForOfferedYears(optOutProposition)
        cancelURL = if (isAgent) controllers.routes.NextUpdatesController.showAgent.url else controllers.routes.NextUpdatesController.show().url
        formResult <- ConfirmOptOutMultiTaxYearChoiceForm(optOutProposition.availableTaxYearsForOptOut.map(_.toString).toList).bindFromRequest().fold(
          formWithError => Future.successful(BadRequest(optOutChooseTaxYear(formWithError, optOutProposition.availableTaxYearsForOptOut, quarterlyUpdatesCountModel, isAgent, cancelURL))),
          form => saveTaxYearChoice(form).map {
            case true => redirectToCheckpointPage(isAgent)
            case false => itvcErrorHandler.showInternalServerError()
          }
        )
      } yield formResult
  }

  private def saveTaxYearChoice(form: ConfirmOptOutMultiTaxYearChoiceForm)(implicit request: RequestHeader): Future[Boolean] = {
    form.choice.flatMap(strFormat => TaxYear.getTaxYearModel(strFormat)).map { intent =>
      optOutService.saveIntent(intent)
    } getOrElse Future.failed(new RuntimeException("no tax-year choice available"))
  }

  private def redirectToCheckpointPage(isAgent: Boolean): Result = {
    val nextPage = controllers.optOut.routes.ConfirmOptOutController.show(isAgent)
    Logger("application").info(s"redirecting to : $nextPage")
    Redirect(nextPage)
  }
}