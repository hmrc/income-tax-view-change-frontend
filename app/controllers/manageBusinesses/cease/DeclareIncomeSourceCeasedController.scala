/*
 * Copyright 2024 HM Revenue & Customs
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

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney.{IncomeSourceType, InitialPage, SelfEmployment}
import enums.JourneyType.{Cease, JourneyType}
import forms.incomeSources.cease.DeclareIncomeSourceCeasedForm
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.CeaseIncomeSourceData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.cease.DeclareIncomeSourceCeased

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclareIncomeSourceCeasedController @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                                    val view: DeclareIncomeSourceCeased,
                                                    val sessionService: SessionService,
                                                    val auth: AuthenticatorPredicate)
                                                   (implicit val appConfig: FrontendAppConfig,
                                                    mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                                   )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  def show(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent = false) {
      implicit user =>
        handleRequest(id, isAgent = false, incomeSourceType)
  }

  def showAgent(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent = true) {
      implicit mtdItUser =>
        handleRequest(id, isAgent = true, incomeSourceType)
  }

  def submit(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent = false) {
      implicit request =>
        handleSubmitRequest(id, isAgent = false, isChange = false, incomeSourceType)
  }

  def submitAgent(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent = true) {
      implicit mtdItUser =>
        handleSubmitRequest(id, isAgent = true, isChange = false, incomeSourceType)
  }

  def handleRequest(id: Option[String], isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    withSessionData(JourneyType(Cease, incomeSourceType), journeyState = InitialPage) { _ =>

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
                backUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent).url,
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

  def handleSubmitRequest(id: Option[String], isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType)
                         (implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {

    DeclareIncomeSourceCeasedForm.form(incomeSourceType).bindFromRequest().fold(
      hasErrors =>
        Future.successful {
          BadRequest(view(
            form = hasErrors,
            incomeSourceType = incomeSourceType,
            soleTraderBusinessName = getBusinessName(user, id),
            postAction = postAction(id, isAgent, incomeSourceType),
            backUrl = backCall(isAgent).url,
            isAgent = isAgent
          ))
        },
      _ => {
        sessionService.setMongoKey(key = CeaseIncomeSourceData.ceaseIncomeSourceDeclare, value = "true", journeyType = JourneyType(Cease, incomeSourceType))
          .flatMap {
            case Right(_) => Future.successful(Redirect(redirectAction(id, isAgent, isChange, incomeSourceType)))
            case Left(exception) => Future.failed(exception)
          }
      }
    )
  }.recover {
    case ex: Exception =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      showInternalServerError()
  }

  private val postAction: (Option[String], Boolean, IncomeSourceType) => Call = (id, isAgent, incomeSourceType) =>
    if (isAgent) routes.DeclareIncomeSourceCeasedController.submitAgent(id, incomeSourceType)
    else         routes.DeclareIncomeSourceCeasedController.submit(id, incomeSourceType)

  private val backCall: Boolean => Call = isAgent =>
    if (isAgent) routes.CeaseIncomeSourceController.showAgent()
    else         routes.CeaseIncomeSourceController.show()

  private val redirectAction: (Option[String], Boolean, Boolean, IncomeSourceType) => Call = (id, isAgent, isChange, incomeSourceType) =>
    routes.IncomeSourceEndDateController.show(id, incomeSourceType, isAgent, isChange)

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
