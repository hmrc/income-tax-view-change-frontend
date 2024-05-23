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

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.optout.OptOutUpdateRequestModel.OptOutUpdateResponseSuccess
import controllers.agent.predicates.ClientConfirmedController
import models.incomeSourceDetails.TaxYear
import models.optout.{OptOutMultiYearViewModel, OptOutOneYearCheckpointViewModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.optout.OptOutService
import utils.AuthenticatorPredicate
import views.html.optOut.ConfirmOptOut
import views.html.errorPages.CustomNotFoundError
import views.html.optOut.{CheckOptOutAnswers, ConfirmOptOut}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmOptOutController @Inject()(view: ConfirmOptOut,
                                        checkOptOutAnswers: CheckOptOutAnswers,
                                        optOutService: OptOutService,
                                        auth: AuthenticatorPredicate)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val ec: ExecutionContext,
                                        val authorisedFunctions: FrontendAuthorisedFunctions,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                        override val mcc: MessagesControllerComponents)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def withOptOutQualifiedTaxYear(intent: Option[TaxYear], isAgent: Boolean)
                                        (function: OptOutOneYearCheckpointViewModel => Result, multiYearFunction: OptOutMultiYearViewModel => Result)
                                        (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    optOutService.optOutCheckPointPageViewModel(intent).map {
      case Some(viewModel: OptOutOneYearCheckpointViewModel) => function(viewModel)
      case Some(viewModel: OptOutMultiYearViewModel) => multiYearFunction(viewModel)
      case _ =>
        Logger("application").error("No qualified tax year available for opt out")
        errorHandler(isAgent).showInternalServerError()
    }
  }


  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  def show(intentTaxYear: String = TaxYear(2024, 2025).toString, isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      withRecover(isAgent) {
        withOptOutQualifiedTaxYear(TaxYear.getTaxYearModel(intentTaxYear), isAgent)(
          viewModel => Ok(view(viewModel, isAgent)),
          multiYearViewModel => Ok(checkOptOutAnswers(multiYearViewModel, isAgent)),
        )
      }
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent = isAgent) {
    implicit user =>
      optOutService.makeOptOutUpdateRequest().map {
        case OptOutUpdateResponseSuccess(_, _) => Redirect(routes.ConfirmedOptOutController.show(isAgent))
        case _ => itvcErrorHandler.showInternalServerError()
      }
  }
}