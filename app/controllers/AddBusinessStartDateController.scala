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
import views.html.incomeSources.add.AddBusinessStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessStartDateController @Inject()(authenticate: AuthenticationPredicate,
                                               val authorisedFunctions: AuthorisedFunctions,
                                               checkSessionTimeout: SessionTimeoutPredicate,
                                               retrieveNino: NinoPredicate,
                                               val addBusinessStartDate: AddBusinessStartDate,
                                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                               val retrieveBtaNavBar: NavBarPredicate,
                                               val itvcErrorHandler: ItvcErrorHandler,
                                               incomeSourceDetailsService: IncomeSourceDetailsService)
                                              (implicit val appConfig: FrontendAppConfig,
                                      implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                      implicit override val mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  lazy val backUrl: String = controllers.routes.AddBusinessNameController.show().url
  lazy val backUrlAgent: String = controllers.routes.AddBusinessNameController.showAgent().url
  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        isAgent = false,
        backUrl = backUrlAgent
      )
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              backUrl = backUrl
            )
        }
    }

  def handleRequest(isAgent: Boolean, backUrl: String)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    Future {
      if (isDisabled(IncomeSources)) {
        Redirect(controllers.routes.HomeController.show())
      } else {
        Ok("PAGE IN DEVELOPMENT")
      }
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      BusinessNameForm.form.bindFromRequest().fold(
        formWithErrors => {
          Future {
            Ok(addBusinessStartDate(
              formWithErrors,
              routes.AddBusinessStartDateController.submit(),
              backUrl
            ))
          }
        },
        formData => {
          Future {
            Redirect(routes.AddBusinessStartDate.show())
              .withSession(request.session + (SessionKeys.businessName -> formData.name))
          }
        }
      )
  }

}
