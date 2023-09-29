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
import enums.IncomeSourceJourney.ForeignProperty
import forms.CeaseForeignPropertyForm
import forms.utils.SessionKeys.ceaseForeignPropertyDeclare
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.CeaseForeignProperty

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CeaseForeignPropertyController @Inject()(val authenticate: AuthenticationPredicate,
                                               val authorisedFunctions: FrontendAuthorisedFunctions,
                                               val checkSessionTimeout: SessionTimeoutPredicate,
                                               val incomeSourceDetailsService: IncomeSourceDetailsService,
                                               val retrieveBtaNavBar: NavBarPredicate,
                                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                               val retrieveNino: NinoPredicate,
                                               val view: CeaseForeignProperty,
                                               val sessionService: SessionService)
                                              (implicit val appConfig: FrontendAppConfig,
                                               mcc: MessagesControllerComponents,
                                               val ec: ExecutionContext,
                                               val itvcErrorHandler: ItvcErrorHandler,
                                               val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                              )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils {

  def handleRequest(isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = withIncomeSourcesFS {

    val backUrl: String = if (isAgent) controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url else
      controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url
    val postAction: Call = if (isAgent) controllers.incomeSources.cease.routes.CeaseForeignPropertyController.submitAgent else
      controllers.incomeSources.cease.routes.CeaseForeignPropertyController.submit

    Future.successful(Ok(view(
      ceaseForeignPropertyForm = CeaseForeignPropertyForm.form,
      postAction = postAction,
      isAgent = isAgent,
      backUrl = backUrl,
      origin = origin)(user, messages)))

  } recover {
    case ex: Exception =>
      val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"[CeaseForeignPropertyController][handleRequest] Error getting CeaseForeignProperty page: ${ex.getMessage}")
      errorHandler.showInternalServerError()
  }


  def show(origin: Option[String] = None): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          origin
        )
    }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true
            )
        }
  }

  def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    val (postAction, backAction, redirectAction) = {
      if (isAgent)
        (routes.CeaseForeignPropertyController.submitAgent,
          routes.CeaseIncomeSourceController.showAgent(),
          routes.IncomeSourceEndDateController.showAgent(None, ForeignProperty))
      else
        (routes.CeaseForeignPropertyController.submit,
          routes.CeaseIncomeSourceController.show(),
          routes.IncomeSourceEndDateController.show(None, ForeignProperty))
    }

    CeaseForeignPropertyForm.form.bindFromRequest().fold(
      hasErrors => {
        val result = BadRequest(view(
          ceaseForeignPropertyForm = hasErrors,
          postAction = postAction,
          backUrl = backAction.url,
          isAgent = isAgent
        ))
        sessionService.set(key = ceaseForeignPropertyDeclare, value = "false", result = result).flatMap {
          case Right(result) => Future.successful(result)
          case Left(exception) => Future.failed(exception)
        }
      },
      _ => {
        val result = Redirect(redirectAction)
        sessionService.set(key = ceaseForeignPropertyDeclare, value = "true", result = result).flatMap {
          case Right(result) => Future.successful(result)
          case Left(exception) => Future.failed(exception)
        }
      }
    )
  }.recover {
    case exception: Exception =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${if (isAgent) "[Agent]"}[CeaseForeignPropertyController][handleSubmitRequest]: $exception")
      errorHandler.showInternalServerError()
  }


  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(isAgent = false)
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true)
        }
  }
}