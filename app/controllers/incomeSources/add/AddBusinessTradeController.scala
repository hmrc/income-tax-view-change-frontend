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

package controllers.incomeSources.add

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.incomeSources.add.BusinessTradeForm
import forms.utils.SessionKeys
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.AddBusinessTrade

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AddBusinessTradeController @Inject()(authenticate: AuthenticationPredicate,
                                           val authorisedFunctions: AuthorisedFunctions,
                                           checkSessionTimeout: SessionTimeoutPredicate,
                                           retrieveNino: NinoPredicate,
                                           val addBusinessTradeView: AddBusinessTrade,
                                           val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                           val retrieveBtaNavBar: NavBarPredicate,
                                           val itvcErrorHandler: ItvcErrorHandler,
                                           incomeSourceDetailsService: IncomeSourceDetailsService)
                                          (implicit val appConfig: FrontendAppConfig,
                                           implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                           implicit override val mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils {

  lazy val backURL: String = controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.show().url
  lazy val agentBackURL: String = controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.showAgent().url
  lazy val postAction: Call = controllers.incomeSources.add.routes.AddBusinessTradeController.submit()
  lazy val postActionAgent: Call = controllers.incomeSources.add.routes.AddBusinessTradeController.agentSubmit()
  lazy val redirect: String = controllers.incomeSources.add.routes.AddBusinessAddressController.show().url
  lazy val redirectAgent: String = controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent().url

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

  def change(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      val businessTradeFromSession = getBusinessTradeFromSession
      handleRequest(isAgent = false, businessTradeFromSession)
  }

  def changeAgent(): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              val businessTradeFromSession = getBusinessTradeFromSession
              handleRequest(isAgent = true, businessTradeFromSession)
          }
    }

  def getBusinessTradeFromSession(implicit user: MtdItUser[_]): Option[String] = {
    user.session.get(SessionKeys.businessTrade)
  }

  def handleRequest(isAgent: Boolean, businessTrade: Option[String] = None)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      Future {
        if (!isAgent) Ok(addBusinessTradeView(BusinessTradeForm.form, controllers.incomeSources.add.routes.AddBusinessTradeController.submit(), isAgent, backURL, sameNameError = false, businessTrade))
        else Ok(addBusinessTradeView(BusinessTradeForm.form, controllers.incomeSources.add.routes.AddBusinessTradeController.agentSubmit(), isAgent, agentBackURL, sameNameError = false, businessTrade))
      }
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(isAgent = false)
  }

  def agentSubmit: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true)
        }
  }

  def changeSubmit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(isAgent = false)
  }

  def changeSubmitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true)
        }
  }

  def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      val (postActionLocal, backUrlLocal, redirectLocal) = {
        if (isAgent) (postActionAgent, agentBackURL, redirectAgent)
        else (postAction, backURL, redirect)
      }
      BusinessTradeForm.form.bindFromRequest().fold(
        formWithErrors => {
          Future {
            Ok(addBusinessTradeView(formWithErrors, postActionLocal, isAgent = true, backUrlLocal, sameNameError = false))
          }
        },
        formData => {
          if (formData.trade == user.session.get(SessionKeys.businessName).get) {
            Future {
              Ok(addBusinessTradeView(BusinessTradeForm.form, postActionLocal, isAgent = true, backUrlLocal, sameNameError = true))
            }
          }
          else {
            Future.successful {
              Redirect(redirectLocal)
                .addingToSession(SessionKeys.businessTrade -> formData.trade)
            }
          }
        }
      )
    }
  }
}
