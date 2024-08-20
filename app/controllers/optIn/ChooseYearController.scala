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

package controllers.optIn

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import forms.optIn.ChooseTaxYearForm
import models.incomeSourceDetails.TaxYear
import models.optin.ChooseTaxYearViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optIn.OptInService
import utils.AuthenticatorPredicate
import views.html.optIn.ChooseTaxYearView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChooseYearController @Inject()(val optInService: OptInService,
                                     val view: ChooseTaxYearView,
                                     val authorisedFunctions: FrontendAuthorisedFunctions,
                                     val auth: AuthenticatorPredicate)
                                    (implicit val appConfig: FrontendAppConfig,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  def show(isAgent: Boolean = false): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      withRecover(isAgent) {

        optInService.availableOptInTaxYear().map { taxYears =>
          Ok(view(ChooseTaxYearForm(taxYears.map(_.toString)), viewModel(taxYears, isAgent)))
        }
      }
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
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

  private def saveTaxYearChoice(form: ChooseTaxYearForm)(implicit mtdItUser: MtdItUser[_]): Future[Boolean] = {
    form.choice.flatMap(strFormat => TaxYear.getTaxYearModel(strFormat)).map { intent =>
      optInService.saveIntent(intent)
    } getOrElse Future.failed(new RuntimeException("no tax-year choice available"))
  }

  private def redirectToCheckpointPage(isAgent: Boolean): Result = {
    val nextPage = controllers.optIn.routes.CheckYourAnswersController.show(isAgent)
    Logger("application").info(s"redirecting to : $nextPage")
    Redirect(nextPage)
  }

  private def cancelUrl(isAgent: Boolean): String = {
    routes.ChooseYearController.show(isAgent).url //todo change this to the correct url
  }

  private def viewModel(availableOptInTaxYear: Seq[TaxYear], isAgent: Boolean): ChooseTaxYearViewModel = {
    ChooseTaxYearViewModel(availableOptInTaxYear = availableOptInTaxYear, cancelURL = cancelUrl(isAgent), isAgent = isAgent)
  }
}