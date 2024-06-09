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
import cats.data.OptionT
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.optout.OptOutUpdateRequestModel.OptOutUpdateResponseSuccess
import controllers.agent.predicates.ClientConfirmedController
import models.optout.OptOutCheckpointViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.optout.OptOutService
import utils.AuthenticatorPredicate
import views.html.optOut.{ConfirmOptOut, ConfirmOptOutMultiYear}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmOptOutController @Inject()(view: ConfirmOptOut,
                                        multiyearCheckpointView: ConfirmOptOutMultiYear,
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

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      withRecover(isAgent) {

        val resultToReturn = for {
          viewModel <- OptionT(optOutService.optOutCheckPointPageViewModel())
          result <- OptionT(Future.successful(Option(toPropositionView(isAgent, viewModel))))
        } yield result

        resultToReturn.getOrElse {
          Logger("application").error("No qualified tax year available for opt out")
          errorHandler(isAgent).showInternalServerError()
        }
      }
  }

  private def toPropositionView(isAgent: Boolean, viewModel: OptOutCheckpointViewModel)(implicit mtdItUser: MtdItUser[_]) = viewModel match {
    case m: OptOutCheckpointViewModel if m.isOneYear => Ok(view(viewModel, isAgent = isAgent))
    case _ => Ok(multiyearCheckpointView(viewModel, isAgent = isAgent))
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent = isAgent) {
    implicit user =>
      optOutService.makeOptOutUpdateRequest().map {
        case OptOutUpdateResponseSuccess(_, _) => Redirect(routes.ConfirmedOptOutController.show(isAgent))
        case _ => itvcErrorHandler.showInternalServerError()
      }
  }

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

}