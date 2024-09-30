/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.optIn.routes.OptInErrorController
import models.itsaStatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseSuccess
import models.optin.ConfirmTaxYearViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.DateService
import services.optIn.OptInService
import utils.AuthenticatorPredicate
import views.html.optIn.ConfirmTaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmTaxYearController @Inject()(val view: ConfirmTaxYear,
                                         val optInService: OptInService,
                                         val authorisedFunctions: FrontendAuthorisedFunctions,
                                         val auth: AuthenticatorPredicate)
                                        (implicit val dateService: DateService,
                                         val appConfig: FrontendAppConfig,
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
        optInService.getConfirmTaxYearViewModel(isAgent) map {
          case Some(model) => Ok(view(ConfirmTaxYearViewModel(
            model.availableOptInTaxYear,
            model.cancelURL,
            model.isNextTaxYear,
            model.isAgent)))
          case None => errorHandler(isAgent).showInternalServerError()
        }

      }
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      optInService.makeOptInCall() map {
        case ITSAStatusUpdateResponseSuccess(_) => redirectToCheckpointPage(isAgent)
        case _ => Redirect(OptInErrorController.show(isAgent))
      }
  }

  private def redirectToCheckpointPage(isAgent: Boolean): Result = {
    val nextPage = controllers.optIn.routes.OptInCompletedController.show(isAgent)
    Logger("application").info(s"redirecting to : $nextPage")
    Redirect(nextPage)
  }
}
