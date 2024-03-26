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

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney.{IncomeSourceType, InitialPage, SelfEmployment}
import enums.JourneyType.{Cease, JourneyType}
import forms.incomeSources.cease.DeclarePropertyCeasedForm
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.CeaseIncomeSourceData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.cease.DeclarePropertyCeased

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarePropertyCeasedController @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                                val view: DeclarePropertyCeased,
                                                val sessionService: SessionService,
                                                val auth: AuthenticatorPredicate)
                                               (implicit val appConfig: FrontendAppConfig,
                                                mcc: MessagesControllerComponents,
                                                val ec: ExecutionContext,
                                                val itvcErrorHandler: ItvcErrorHandler,
                                                val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                               )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  def handleRequest(id: Option[String], isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    withSessionData(JourneyType(Cease, incomeSourceType), journeyState = InitialPage) { _ =>

      val backUrl: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent).url

      val maybeBusinessName = getBusinessName(user, id)

      (incomeSourceType, id, maybeBusinessName) match {
        case (SelfEmployment, None, _) =>
          Logger("application").error(s"${if (isAgent) "[Agent]"}" +
            s"[DeclarePropertyCeasedController][handleRequest]: IncomeSourceId not found for IncomeSourceType: SelfEmployment")
          Future.successful(
            (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler).showInternalServerError()
          )
        case (_, _, maybeBusinessName) =>
          Future.successful(Ok(view(
            form = DeclarePropertyCeasedForm.form(incomeSourceType),
            incomeSourceType = incomeSourceType,
            soleTraderBusinessName = maybeBusinessName,
            isAgent = isAgent,
            backUrl = backUrl,
            postAction = {
              if (isAgent) routes.DeclarePropertyCeasedController.submitAgent(id.map(mkIncomeSourceId).map(_.toHash.hash), incomeSourceType)
              else         routes.DeclarePropertyCeasedController.submit(id.map(mkIncomeSourceId).map(_.toHash.hash), incomeSourceType)

            }
          )))
      }
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"[DeclarePropertyCeasedController][handleRequest] Error getting declare property ceased page: ${ex.getMessage} - ${ex.getCause}")
        val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        errorHandler.showInternalServerError()
    }

  def show(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
      implicit user =>
        handleRequest(
          id = id,
          isAgent = false,
          incomeSourceType = incomeSourceType
        )
    }

  def showAgent(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleRequest(
        id = id,
        isAgent = true,
        incomeSourceType = incomeSourceType
      )
  }

  def handleSubmitRequest(id: Option[String], isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    val (postAction, backAction, redirectAction) = {
      if (isAgent)
        (routes.DeclarePropertyCeasedController.submitAgent(id, incomeSourceType),
          routes.CeaseIncomeSourceController.showAgent(),
          routes.IncomeSourceEndDateController.showAgent(id, incomeSourceType))
      else
        (routes.DeclarePropertyCeasedController.submit(id, incomeSourceType),
          routes.CeaseIncomeSourceController.show(),
          routes.IncomeSourceEndDateController.show(id, incomeSourceType))
    }

    DeclarePropertyCeasedForm.form(incomeSourceType).bindFromRequest().fold(
      hasErrors =>
        Future.successful {
          BadRequest(view(
            form = hasErrors,
            incomeSourceType = incomeSourceType,
            soleTraderBusinessName = getBusinessName(user, id),
            postAction = postAction,
            backUrl = backAction.url,
            isAgent = isAgent
          ))
        },
      _ => {
        val result = Redirect(redirectAction)
        sessionService.setMongoKey(key = CeaseIncomeSourceData.ceasePropertyDeclare, value = "true", journeyType = JourneyType(Cease, incomeSourceType))
          .flatMap {
            case Right(_) => Future.successful(result)
            case Left(exception) => Future.failed(exception)
          }
      }
    )
  }.recover {
    case ex: Exception =>
      Logger("application").error(s"${if (isAgent) "[Agent]"}[DeclarePropertyCeasedController][handleSubmitRequest]: - ${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      errorHandler.showInternalServerError()
  }

  private def getBusinessName(user: MtdItUser[_], id: Option[String]): Option[String] = {
    user.incomeSources.businesses
      .find(x => id.contains(mkIncomeSourceId(x.incomeSourceId).toHash.hash))
      .flatMap(_.tradingName)
  }

  def submit(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit request =>
      val incomeSourceIdMaybe = id.map(mkIncomeSourceId)
      handleSubmitRequest(id = incomeSourceIdMaybe.map(_.toHash.hash), isAgent = false, incomeSourceType = incomeSourceType)
  }

  def submitAgent(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      val incomeSourceIdMaybe = id.map(mkIncomeSourceId)
      handleSubmitRequest(id = incomeSourceIdMaybe.map(_.toHash.hash), isAgent = true, incomeSourceType = incomeSourceType)
  }
}