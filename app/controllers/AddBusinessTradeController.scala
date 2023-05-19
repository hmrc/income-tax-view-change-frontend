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
import forms.utils.SessionKeys
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.AddBusinessTrade
import models.incomeSourceDetails.BusinessTradeForm
import services.IncomeSourceDetailsService

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
  extends ClientConfirmedController with I18nSupport with FeatureSwitching{

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
      if (isDisabled(IncomeSources)) {
        Future.successful(Redirect(controllers.routes.HomeController.show()))
      } else {
        Future {
          if (!isAgent) Ok(addBusinessTradeView(BusinessTradeForm.form, routes.AddBusinessTradeController.submit(), isAgent, backURL, agentBackURL, false))
          else Ok(addBusinessTradeView(BusinessTradeForm.form, routes.AddBusinessTradeController.agentSubmit(), isAgent, backURL, agentBackURL, false))
        }
      }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      BusinessTradeForm.form.bindFromRequest().fold(
        formWithErrors => {
          Future {
            Ok(addBusinessTradeView(formWithErrors, routes.AddBusinessTradeController.submit(), false, backURL, agentBackURL, false))
          }
        },
        formData => {
          if (formData.trade == request.session.get("addBusinessName").get){
            Future {
              Ok(addBusinessTradeView(BusinessTradeForm.form, routes.AddBusinessTradeController.submit(), false, backURL, agentBackURL, true))
            }
          }
          else {
            Future.successful {
              Redirect(routes.AddBusinessAddressController.show().url).withSession(request.session + (SessionKeys.businessTrade -> formData.trade))
            }
          }
        }
      )
    }

  def agentSubmit: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            BusinessTradeForm.form.bindFromRequest().fold(
              formWithErrors => {
                Future {
                  Ok(addBusinessTradeView(formWithErrors, routes.AddBusinessTradeController.agentSubmit(), true, backURL, agentBackURL, false))
                }
              },
              formData => {
                if (formData.trade == request.session.get("businessName").get) {
                  Future {
                    Ok(addBusinessTradeView(BusinessTradeForm.form, routes.AddBusinessTradeController.agentSubmit(), true, backURL, agentBackURL, true))
                  }
                }
                else {
                  Future.successful {
                    Redirect(routes.AddBusinessAddressController.showAgent().url).withSession(request.session + (SessionKeys.businessTrade -> formData.trade))
                  }
                }
              }
            )
        }
  }
}
