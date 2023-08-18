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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.SelfEmployment
import forms.incomeSources.add.BusinessTradeForm
import forms.utils.SessionKeys
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
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
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def backURL(isAgent: Boolean): String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.handleRequest(SelfEmployment.key, isAgent = isAgent, isUpdate = false).url
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

  def handleRequest(isAgent: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      Future {
        if (!isAgent) Ok(addBusinessTradeView(BusinessTradeForm.form, controllers.incomeSources.add.routes.AddBusinessTradeController.submit(), isAgent, backURL(isAgent), sameNameError = false))
        else Ok(addBusinessTradeView(BusinessTradeForm.form, controllers.incomeSources.add.routes.AddBusinessTradeController.agentSubmit(), isAgent, backURL(isAgent), sameNameError = false))
      }
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handelSubmitRequest(isAgent = false)
  }

  def agentSubmit: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handelSubmitRequest(isAgent = true)
        }
  }

  def handelSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    val (postActionLocal, backUrlLocal, redirectLocal) = {
      if (isAgent) (postActionAgent, backURL(isAgent), redirectAgent)
      else (postAction, backURL(isAgent), redirect)
    }
    BusinessTradeForm.form.bindFromRequest().fold(
      formWithErrors => {
        Future {
          Ok(addBusinessTradeView(formWithErrors, postActionLocal, isUserAgent = true, backURL(isAgent), sameNameError = false))
        }
      },
      formData => {
        if (formData.trade == user.session.get(SessionKeys.businessName).get) {
          Future {
            Ok(addBusinessTradeView(BusinessTradeForm.form, postActionLocal, isUserAgent = true, backUrlLocal, sameNameError = true))
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

  def changeBusinessTrade(): Action[AnyContent] = Action {
    Ok("Change Business Trade  WIP")
  }

  def changeBusinessTradeAgent(): Action[AnyContent] = Action {
    Ok("Agent Change Business Trade  WIP")
  }
}
