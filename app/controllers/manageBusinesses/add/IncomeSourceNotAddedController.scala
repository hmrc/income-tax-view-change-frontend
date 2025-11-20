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

package controllers.manageBusinesses.add

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.IncomeSourceType
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.CreateBusinessDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.IncomeSourcesUtils
import views.html.manageBusinesses.add.IncomeSourceNotAddedError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceNotAddedController @Inject()(val authActions: AuthActions,
                                               val businessDetailsService: CreateBusinessDetailsService,
                                               val incomeSourceNotAddedError: IncomeSourceNotAddedError,
                                               val itvcErrorHandler: ItvcErrorHandler,
                                               val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                              (implicit val appConfig: FrontendAppConfig,
                                               mcc: MessagesControllerComponents,
                                               val ec: ExecutionContext) extends FrontendController(mcc)
  with IncomeSourcesUtils with I18nSupport{


  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType, isTriggeredMigration: Boolean)
                   (implicit user: MtdItUser[_]): Future[Result] = {
    val incomeSourceRedirect: Call = {
      (isAgent, isTriggeredMigration) match {
        case (false, false)  => controllers.manageBusinesses.routes.ManageYourBusinessesController.show()
        case (true, false)   => controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent()
        case (isAgent, true) => controllers.triggeredMigration.routes.CheckHmrcRecordsController.show(isAgent)
      }
    }

    Future.successful(Ok(incomeSourceNotAddedError(
      isAgent,
      incomeSourceType = incomeSourceType,
      continueAction = incomeSourceRedirect
    )))
  }

  def show(incomeSourceType: IncomeSourceType, isTriggeredMigration: Boolean = false): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType,
        isTriggeredMigration
      )
  }

  def showAgent(incomeSourceType: IncomeSourceType, isTriggeredMigration: Boolean = false): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleRequest(
        isAgent = true,
        incomeSourceType = incomeSourceType,
        isTriggeredMigration
      )
  }
}

