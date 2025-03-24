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

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney.{IncomeSourceType, InitialPage}
import enums.JourneyType.{Cease, IncomeSourceJourneyType}
import forms.incomeSources.cease.DeclareIncomeSourceCeasedForm
import models.incomeSourceDetails.CeaseIncomeSourceData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyChecker
import views.html.incomeSources.cease.DeclarePropertyCeased

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarePropertyCeasedController @Inject()(val authActions: AuthActions,
                                                val view: DeclarePropertyCeased,
                                                val sessionService: SessionService,
                                                val itvcErrorHandler: ItvcErrorHandler,
                                                val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                               (implicit val appConfig: FrontendAppConfig,
                                                mcc: MessagesControllerComponents,
                                                val ec: ExecutionContext
                                               )
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with JourneyChecker {

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Cease, incomeSourceType), journeyState = InitialPage) { _ =>

      val backUrl: String = if (isAgent) controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url else
        controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url
      val postAction: Call = if (isAgent) controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.submitAgent(incomeSourceType) else
        controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.submit(incomeSourceType)

      Future.successful(Ok(view(
        declarePropertyCeasedForm = DeclareIncomeSourceCeasedForm.form(incomeSourceType),
        incomeSourceType = incomeSourceType,
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl
      )))

    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting declare property ceased page: ${ex.getMessage} - ${ex.getCause}")
        val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        errorHandler.showInternalServerError()
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

    DeclareIncomeSourceCeasedForm.form(incomeSourceType).bindFromRequest().fold(
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
        sessionService.setMongoKey(key = CeaseIncomeSourceData.ceaseIncomeSourceDeclare, value = "true", incomeSources = IncomeSourceJourneyType(Cease, incomeSourceType))
          .flatMap {
            case Right(_) => Future.successful(result)
            case Left(exception) => Future.failed(exception)
          }
      }
    )
  }.recover {
    case ex: Exception =>
      Logger("application").error(s"${if (isAgent) "[Agent]"}${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      errorHandler.showInternalServerError()
  }


  def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit request =>
      handleSubmitRequest(isAgent = false, incomeSourceType = incomeSourceType)
  }

  def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleSubmitRequest(isAgent = true, incomeSourceType = incomeSourceType)
  }
}