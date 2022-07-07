/*
 * Copyright 2022 HM Revenue & Customs
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

import audit.AuditingService
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.notMigrated.NotMigratedUser
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger

@Singleton
class NotMigratedUserController @Inject()(val notMigrated: NotMigratedUser,
                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                          val authenticate: AuthenticationPredicate,
                                          val authorisedFunctions: AuthorisedFunctions,
                                          val retrieveNino: NinoPredicate,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService,
                                          val retrieveBtaNavBar: NavBarPredicate)
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
          Logger("application").error(s"[NotMigratedUserController][NotMigrated] error, ${ex.getMessage}")
          itvcErrorHandler.showInternalServerError()
      }
  }

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
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

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit agent =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap {
          implicit user =>
            handleShowRequest(errorHandler = itvcErrorHandlerAgent,
              isAgent = true,
              backUrl = controllers.routes.HomeController.showAgent().url)
        }
  }

}
