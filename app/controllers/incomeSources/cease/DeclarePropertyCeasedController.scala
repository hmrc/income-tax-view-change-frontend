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

package controllers.incomeSources.cease

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.IncomeSourceType
import enums.JourneyType.{Cease, JourneyType}
import forms.incomeSources.cease.DeclarePropertyCeasedForm
import models.incomeSourceDetails.CeaseIncomeSourceData
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.cease.DeclarePropertyCeased

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarePropertyCeasedController @Inject()(val authenticate: AuthenticationPredicate,
                                                val authorisedFunctions: FrontendAuthorisedFunctions,
                                                val checkSessionTimeout: SessionTimeoutPredicate,
                                                val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                val retrieveBtaNavBar: NavBarPredicate,
                                                val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                                val view: DeclarePropertyCeased,
                                                val sessionService: SessionService)
                                               (implicit val appConfig: FrontendAppConfig,
                                                mcc: MessagesControllerComponents,
                                                val ec: ExecutionContext,
                                                val itvcErrorHandler: ItvcErrorHandler,
                                                val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                               )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils with JourneyChecker {

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = withSessionData(JourneyType(Cease, incomeSourceType)) { _ =>

    val backUrl: String = if (isAgent) controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url else
      controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url
    val postAction: Call = if (isAgent) controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.submitAgent(incomeSourceType) else
      controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.submit(incomeSourceType)

    sessionService.createSession(JourneyType(Cease, incomeSourceType).toString).flatMap { _ =>
      Future.successful(Ok(view(
        declarePropertyCeasedForm = DeclarePropertyCeasedForm.form(incomeSourceType),
        incomeSourceType = incomeSourceType,
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl)(user, messages)))
    }
  } recover {
    case ex: Exception =>
      val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"[DeclarePropertyCeasedController][handleRequest] Error getting declare property ceased page: ${ex.getMessage}")
      errorHandler.showInternalServerError()
  }


  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate
      andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType
        )
    }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType
            )
        }
  }

  def handleSubmitRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    val (postAction, backAction, redirectAction) = {
      if (isAgent)
        (routes.DeclarePropertyCeasedController.submitAgent(incomeSourceType),
          routes.CeaseIncomeSourceController.showAgent(),
          routes.IncomeSourceEndDateController.showAgent(None, incomeSourceType))
      else
        (routes.DeclarePropertyCeasedController.submit(incomeSourceType),
          routes.CeaseIncomeSourceController.show(),
          routes.IncomeSourceEndDateController.show(None, incomeSourceType))
    }

    DeclarePropertyCeasedForm.form(incomeSourceType).bindFromRequest().fold(
      hasErrors =>
        Future.successful {
          BadRequest(view(
            declarePropertyCeasedForm = hasErrors,
            incomeSourceType = incomeSourceType,
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
    case exception: Exception =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${if (isAgent) "[Agent]"}[DeclarePropertyCeasedController][handleSubmitRequest]: $exception")
      errorHandler.showInternalServerError()
  }


  def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(isAgent = false, incomeSourceType = incomeSourceType)
  }

  def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true, incomeSourceType = incomeSourceType)
        }
  }
}