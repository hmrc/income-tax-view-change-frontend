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

package controllers.optIn.oldJourney

import auth.MtdItUser
import auth.authV2.AuthActions
import cats.data.OptionT
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import forms.optIn.ChooseTaxYearForm
import models.incomeSourceDetails.TaxYear
import models.optin.ChooseTaxYearViewModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.optIn.OptInService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.reportingObligations.ReportingObligationsUtils
import views.html.optIn.oldJourney.ChooseTaxYearView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseYearController @Inject()(val optInService: OptInService,
                                     val view: ChooseTaxYearView,
                                     val authActions: AuthActions,
                                     val itvcErrorHandler: ItvcErrorHandler,
                                     val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                    (implicit val appConfig: FrontendAppConfig,
                                     mcc: MessagesControllerComponents,
                                     val ec: ExecutionContext)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with ReportingObligationsUtils {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  def show(isAgent: Boolean = false): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptInRFChecks {
        withRecover(isAgent) {

          optInService.availableOptInTaxYear().flatMap { taxYears =>
            (for {
              savedTaxYear <- OptionT(optInService.fetchSavedChosenTaxYear())
            } yield {
              createResult(Some(savedTaxYear), taxYears, isAgent)
            }).getOrElse(createResult(None, taxYears, isAgent))
          }
        }
      }
  }

  private def createResult(savedTaxYear: Option[TaxYear], taxYears: Seq[TaxYear], isAgent: Boolean)
                          (implicit messages: Messages, user: MtdItUser[_]): Result = {
    val form = ChooseTaxYearForm(taxYears.map(_.toString))(messages)
    val filledForm = savedTaxYear.map(ty => form.fill(ChooseTaxYearForm(Some(ty.toString)))).getOrElse(form)
    Ok(view(filledForm, viewModel(taxYears, isAgent))(messages, user))
  }

  def submit(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptInRFChecks {
        optInService.availableOptInTaxYear().flatMap { taxYears =>
          ChooseTaxYearForm(taxYears.map(_.toString)).bindFromRequest().fold(
            formWithError => Future.successful(BadRequest(view(formWithError, viewModel(taxYears, isAgent)))),
            form => saveTaxYearChoice(form).map {
              case true => redirectToCheckpointPage(isAgent)
              case false => itvcErrorHandler.showInternalServerError()
            }
          )
        }
      }
  }

  private def saveTaxYearChoice(form: ChooseTaxYearForm)(implicit mtdItUser: MtdItUser[_]): Future[Boolean] = {
    form.choice.flatMap(strFormat => TaxYear.getTaxYearModel(strFormat)).map { intent =>
      optInService.saveIntent(intent)
    } getOrElse Future.failed(new RuntimeException("no tax-year choice available"))
  }

  private def redirectToCheckpointPage(isAgent: Boolean): Result = {
    val nextPage = controllers.optIn.oldJourney.routes.OptInCheckYourAnswersController.show(isAgent)
    Logger("application").info(s"redirecting to : $nextPage")
    Redirect(nextPage)
  }

  private def cancelUrl(isAgent: Boolean): String = {
    controllers.routes.ReportingFrequencyPageController.show(isAgent).url
  }

  private def viewModel(availableOptInTaxYear: Seq[TaxYear], isAgent: Boolean): ChooseTaxYearViewModel = {
    ChooseTaxYearViewModel(availableOptInTaxYear = availableOptInTaxYear, cancelURL = cancelUrl(isAgent), isAgent = isAgent)
  }
}