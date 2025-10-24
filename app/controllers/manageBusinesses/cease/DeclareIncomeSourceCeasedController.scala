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

package controllers.manageBusinesses.cease

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment}
import enums.InitialPage
import enums.JourneyType.{Cease, IncomeSourceJourneyType}
import forms.manageBusinesses.cease.DeclareIncomeSourceCeasedForm
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.{Mode, NormalMode}
import models.incomeSourceDetails.CeaseIncomeSourceData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.cease.DeclareIncomeSourceCeased

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclareIncomeSourceCeasedController @Inject()(val authActions: AuthActions,
                                                    val view: DeclareIncomeSourceCeased,
                                                    val sessionService: SessionService,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                   (implicit val appConfig: FrontendAppConfig,
                                                    mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext
                                                   )
  extends FrontendController(mcc) with I18nSupport with JourneyCheckerManageBusinesses {

  private def getBackUrl(isAgent: Boolean): String = if(isAgent) {
    controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
  } else {
    controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
  }

  def show(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDIndividual.async {
      implicit user =>
        handleRequest(id, isAgent = false, incomeSourceType)
  }

  def showAgent(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDAgentWithConfirmedClient.async {
      implicit mtdItUser =>
        handleRequest(id, isAgent = true, incomeSourceType)
  }

  def submit(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDIndividual.async {
      implicit request =>
        handleSubmitRequest(id, isAgent = false, mode = NormalMode, incomeSourceType)
  }

  def submitAgent(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDAgentWithConfirmedClient.async {
      implicit mtdItUser =>
        handleSubmitRequest(id, isAgent = true, mode = NormalMode, incomeSourceType)
  }

  def handleRequest(id: Option[String], isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    withSessionData(IncomeSourceJourneyType(Cease, incomeSourceType), journeyState = InitialPage) { _ =>

      (incomeSourceType, id, getBusinessName(user, id)) match {
        case (SelfEmployment, None, _) =>
          Logger("application").error("IncomeSourceId not found for SelfEmployment")
          Future.successful { showInternalServerError() }
        case (_, _, maybeBusinessName) =>
          Future.successful(
            Ok(
              view(
                form = DeclareIncomeSourceCeasedForm.form(incomeSourceType),
                incomeSourceType = incomeSourceType,
                soleTraderBusinessName = maybeBusinessName,
                isAgent = isAgent,
                backUrl = getBackUrl(isAgent),
                postAction = postAction(id, isAgent, incomeSourceType)
              )
            )
          )
      }
    } recover {
      case ex: Exception =>
        Logger("application")
          .error(s"Error getting declare income source ceased page: ${ex.getMessage} - ${ex.getCause}")
        showInternalServerError()
    }

  def handleSubmitRequest(id: Option[String], isAgent: Boolean, mode: Mode, incomeSourceType: IncomeSourceType)
                         (implicit user: MtdItUser[_]): Future[Result] = {
    sessionService.setMongoKey(
        key = CeaseIncomeSourceData.ceaseIncomeSourceDeclare,
        value = "true",
        incomeSources = IncomeSourceJourneyType(Cease, incomeSourceType)
      )
      .flatMap {
        case Right(_) => Future.successful(Redirect(redirectAction(id, isAgent, mode, incomeSourceType)))
        case Left(exception) => Future.failed(exception)
      }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      showInternalServerError()
  }

  private val postAction: (Option[String], Boolean, IncomeSourceType) => Call = (id, isAgent, incomeSourceType) =>
    if (isAgent) routes.DeclareIncomeSourceCeasedController.submitAgent(id, incomeSourceType)
    else         routes.DeclareIncomeSourceCeasedController.submit(id, incomeSourceType)

  private val redirectAction: (Option[String], Boolean, Mode, IncomeSourceType) => Call = (id, isAgent, mode, incomeSourceType) =>
    routes.IncomeSourceEndDateController.show(id, incomeSourceType, isAgent, mode)

  private def showInternalServerError()(implicit mtdItUser: MtdItUser[_]): Result = {
    if (mtdItUser.userType.contains(Agent)) itvcErrorHandlerAgent
    else itvcErrorHandler
  }.showInternalServerError()

  private def getBusinessName(user: MtdItUser[_], id: Option[String]): Option[String] = {
    user.incomeSources.businesses
      .find(x => id.contains(mkIncomeSourceId(x.incomeSourceId).toHash.hash))
      .flatMap(_.tradingName)
  }
}
