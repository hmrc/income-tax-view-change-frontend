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

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import views.html.errorPages.CustomNotFoundError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceReportingMethodNotSavedController @Inject()(val authenticate: AuthenticationPredicate,
                                                              val authorisedFunctions: FrontendAuthorisedFunctions,
                                                              val checkSessionTimeout: SessionTimeoutPredicate,
                                                              val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                              val retrieveBtaNavBar: NavBarPredicate,
                                                              val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                              val retrieveNino: NinoPredicate,
                                                              val customNotFoundErrorView: CustomNotFoundError)
                                                             (implicit val appConfig: FrontendAppConfig,
                                                              mcc: MessagesControllerComponents,
                                                              val ec: ExecutionContext,
                                                              val itvcErrorHandler: ItvcErrorHandler,
                                                              val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                                             )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def showUKProperty(): Action[AnyContent] = show()

  def showUKPropertyAgent(): Action[AnyContent] = showAgent()

  def showForeignProperty(): Action[AnyContent] = show()

  def showForeignPropertyAgent(): Action[AnyContent] = showAgent()

  def showBusiness(): Action[AnyContent] = show()

  def showBusinessAgent(): Action[AnyContent] = showAgent()

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false)
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).map {
          implicit mtdItUser =>
            errorHandler(isAgent = true).showInternalServerError()
        }
  }

  def handleRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    Future.successful {
      errorHandler(isAgent).showInternalServerError()
    }
  }

  def errorHandler(isAgent: Boolean): ShowInternalServerError = {
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler
  }

}