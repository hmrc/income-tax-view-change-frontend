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
import forms.BusinessNameForm
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.AddBusiness
import services.IncomeSourceDetailsService
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessStartDate @Inject()(authenticate: AuthenticationPredicate,
                                      val authorisedFunctions: AuthorisedFunctions,
                                      checkSessionTimeout: SessionTimeoutPredicate,
                                      retrieveNino: NinoPredicate,
                                      val addBusinessView: AddBusiness,
                                      val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                      val retrieveBtaNavBar: NavBarPredicate,
                                      val itvcErrorHandler: ItvcErrorHandler,
                                      incomeSourceDetailsService: IncomeSourceDetailsService)
                                     (implicit val appConfig: FrontendAppConfig,
                                      implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                      implicit override val mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false)
  }

  def showAgent(): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap {
            implicit mtdItUser =>
              handleRequest(isAgent = true)
          }
    }

  def handleRequest(isAgent: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    Future {
      if (isDisabled(IncomeSources)) {
        Redirect(controllers.routes.HomeController.show())
      } else {
        Ok(addBusinessView(BusinessNameForm.form,
          routes.AddBusinessController.show()))
      }
    }
  }


}