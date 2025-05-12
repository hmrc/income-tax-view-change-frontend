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
import enums.IncomeSourceJourney._
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.YouCannotGoBackError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingMethodSetBackErrorController @Inject()(val authActions: AuthActions,
                                                      val sessionService: SessionService,
                                                      val cannotGoBackError: YouCannotGoBackError,
                                                      val itvcErrorHandler: ItvcErrorHandler,
                                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                     (implicit val appConfig: FrontendAppConfig,
                                                      mcc: MessagesControllerComponents,
                                                      val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with JourneyCheckerManageBusinesses {


  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] =
    withSessionDataAndNewIncomeSourcesFS(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = CannotGoBackPage) { _ =>
      val subheadingContent = getSubheadingContent(incomeSourceType)
      Future.successful(Ok(cannotGoBackError(isAgent, subheadingContent)))
    }

  def getSubheadingContent(incomeSourceType: IncomeSourceType)(implicit request: Request[_]): String = {
    incomeSourceType match {
      case SelfEmployment => messagesApi.preferred(request)("cannotGoBack.soleTraderAdded")
      case UkProperty => messagesApi.preferred(request)("cannotGoBack.ukPropertyAdded")
      case ForeignProperty => messagesApi.preferred(request)("cannotGoBack.foreignPropertyAdded")
    }
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
