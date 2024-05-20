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
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import models.optOut.OptOutOneYearCheckpointViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import services.optout.OptOutService
import utils.AuthenticatorPredicate
import views.html.errorPages.CustomNotFoundError
import views.html.optOut.ConfirmOptOut

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmOptOutController @Inject()(val authenticate: AuthenticationPredicate,
                                        val authorisedFunctions: FrontendAuthorisedFunctions,
                                        val confirmOptOut: ConfirmOptOut,
                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                        val incomeSourceDetailsService: IncomeSourceDetailsService,
                                        val retrieveBtaNavBar: NavBarPredicate,
                                        val retrieveNino: NinoPredicate,
                                        val customNotFoundErrorView: CustomNotFoundError,
                                        val auth: AuthenticatorPredicate,
                                        val optOutService: OptOutService)
                                       (implicit val appConfig: FrontendAppConfig,
                                        mcc: MessagesControllerComponents,
                                        val ec: ExecutionContext,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                       )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def withOptOutQualifiedTaxYear(isAgent: Boolean)(function: OptOutOneYearCheckpointViewModel => Result)
                                        (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    optOutService.optOutCheckPointPageViewModel().map {
      case Some(optOutOneYearCheckpointViewModel) => function(optOutOneYearCheckpointViewModel)
      case None =>
        Logger("application").error("No qualified tax year available for opt out")
        errorHandler(isAgent).showInternalServerError()
    }

  }

  def show(): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      withOptOutQualifiedTaxYear(isAgent = false)(
        viewModel => Ok(confirmOptOut(viewModel, isAgent = false))
      )
  }

  def showAgent(): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      withOptOutQualifiedTaxYear(isAgent = false)(
        viewModel => Ok(confirmOptOut(viewModel, isAgent = true))
      )
  }

}