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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import views.html.notMigrated.NotMigratedUser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotMigratedUserController @Inject()(val notMigrated: NotMigratedUser,
                                          val authorisedFunctions: AuthorisedFunctions,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          val auth: AuthenticatorPredicate)
                                         (implicit val ec: ExecutionContext,
                                          mcc: MessagesControllerComponents,
                                          val appConfig: FrontendAppConfig) extends ClientConfirmedController with I18nSupport with FeatureSwitching {


  def handleShowRequest(errorHandler: ShowInternalServerError, isAgent: Boolean, backUrl: String)
                       (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    {
      if (user.incomeSources.yearOfMigration.isEmpty) {
        Future {
          Ok(notMigrated(isAgent, backUrl))
        }
      } else {
        Future.failed(new Exception("Migrated user not allowed to access this page"))
      }
    } .recover {
        case ex =>
          Logger("application")
            .error(s"error, ${ex.getMessage} - ${ex.getCause}")
          itvcErrorHandler.showInternalServerError()
      }
  }

  def show(): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleShowRequest(errorHandler = itvcErrorHandler, isAgent = false,
        backUrl = controllers.routes.HomeController.show().url)
  }

  def redirect(): Action[AnyContent] = Action {
    Redirect("https://www.tax.service.gov.uk/contact/self-assessment/ind/%3CUTR%3E/repayment")
  }

  def redirectAgent(): Action[AnyContent] = Action {
    Redirect("https://www.gov.uk/government/collections/hmrc-online-services-for-agents#hmrc-online-services-for-agents-account")
  }

  def showAgent(): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit user =>
      handleShowRequest(errorHandler = itvcErrorHandlerAgent,
        isAgent = true,
        backUrl = controllers.routes.HomeController.showAgent.url)
  }
}
