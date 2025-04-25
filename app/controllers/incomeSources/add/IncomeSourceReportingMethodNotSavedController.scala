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
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.IncomeSourceType
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.IncomeSourceReportingMethodNotSaved

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceReportingMethodNotSavedController @Inject()(val authActions: AuthActions,
                                                              val view: IncomeSourceReportingMethodNotSaved,
                                                              val itvcAgentErrorHandler: AgentItvcErrorHandler,
                                                              val itvcErrorHandler: ItvcErrorHandler)
                                                             (implicit val ec: ExecutionContext,
                                                              val mcc: MessagesControllerComponents,
                                                              val appConfig: FrontendAppConfig) extends FrontendController(mcc)
  with I18nSupport with IncomeSourcesUtils {

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {

    val action: Call =
      if (isAgent)
        controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(incomeSourceType)
      else
        controllers.incomeSources.add.routes.IncomeSourceAddedController.show(incomeSourceType)

    Future.successful(Ok(view(incomeSourceType = incomeSourceType, continueAction = action, isAgent = isAgent)))
  }


  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType
      )
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleRequest(
        isAgent = true,
        incomeSourceType = incomeSourceType
      )
  }
}
