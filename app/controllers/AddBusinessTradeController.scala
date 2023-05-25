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

package controllers

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.incomeSources.add.BusinessTradeForm
import forms.utils.SessionKeys
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.AddBusinessTrade
import services.IncomeSourceDetailsService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AddBusinessTradeController @Inject()(authenticate: AuthenticationPredicate,
                                           val authorisedFunctions: AuthorisedFunctions,
                                           checkSessionTimeout: SessionTimeoutPredicate,
                                           retrieveNino: NinoPredicate,
                                           val addBusinessTradeView: AddBusinessTrade,
                                           val form: BusinessTradeForm,
                                           val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                           val retrieveBtaNavBar: NavBarPredicate,
                                           val itvcErrorHandler: ItvcErrorHandler,
                                           incomeSourceDetailsService: IncomeSourceDetailsService)
                                          (implicit val appConfig: FrontendAppConfig,
                                           implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                           implicit override val mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext)

  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  val backURL: String = controllers.routes.AddBusinessStartDateCheckController.show().url
  val agentBackURL: String = controllers.routes.AddBusinessStartDateCheckController.showAgent().url

  def show: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false)
  }

  def showAgent: Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              handleRequest(isAgent = true)
          }
    }

  def handleRequest(isAgent: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    val submitAction = Future {
      if (isAgent) Ok(addBusinessTradeView(form.apply(user), routes.AddBusinessTradeController.agentSubmit(), isAgent, agentBackURL))
      else Ok(addBusinessTradeView(form.apply(user), routes.AddBusinessTradeController.submit(), isAgent, backURL))
    }

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      submitAction
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      form.apply.bindFromRequest().fold(
        formWithErrors => {
          Future {
            BadRequest(addBusinessTradeView(formWithErrors, routes.AddBusinessTradeController.submit(), false, backURL))
          }
        },
        formData => {
          Future.successful {
            Redirect(routes.AddBusinessAddressController.show().url).addingToSession(SessionKeys.businessTrade -> formData)
          }
        }
      )
  }

  def agentSubmit: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            form.apply.bindFromRequest().fold(
              formWithErrors =>
                Future {
                  BadRequest(addBusinessTradeView(formWithErrors, routes.AddBusinessTradeController.agentSubmit(), true, agentBackURL))
                },
              formData =>
                Future.successful {
                  Redirect(routes.AddBusinessAddressController.showAgent().url).addingToSession(SessionKeys.businessTrade -> formData)                }
            )
        }
  }
}
