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

package controllers.feedback

import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.FeedbackConnector
import forms.FeedbackForm
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.feedback.{FeedbackView, FeedbackThankYouView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeedbackController @Inject()(val authActions: AuthActions,
                                   val feedbackView: FeedbackView,
                                   val feedbackThankYouView: FeedbackThankYouView,
                                   val itvcErrorHandler: ItvcErrorHandler,
                                   val agentItvcErrorHandler: AgentItvcErrorHandler,
                                   val feedbackConnector : FeedbackConnector)
                                  (implicit val config: FrontendAppConfig,
                                    mcc: MessagesControllerComponents,
                                    ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {


  def show: Action[AnyContent] = authActions.asAuthorisedUser.async {
    implicit request =>
      val feedback = feedbackView(FeedbackForm.form, postAction = routes.FeedbackController.submit())
      request.headers.get(REFERER) match {
        case Some(ref) => Future.successful(Ok(feedback).withSession(request.session + (REFERER -> ref)))
        case _ => Future.successful(Ok(feedback))
      }
  }

  def showAgent: Action[AnyContent] = authActions.asAuthorisedUser.async { implicit request =>
        val feedback = feedbackView(FeedbackForm.form, postAction = routes.FeedbackController.submitAgent(), isAgent = true)
        request.headers.get(REFERER) match {
          case Some(ref) => Future.successful(Ok(feedback).withSession(request.session + (REFERER -> ref)))
          case _ => Future.successful(Ok(feedback))
        }
  }

  def submit: Action[AnyContent] = authActions.asAuthorisedUser.async {
    implicit request =>
      FeedbackForm.form.bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(feedbackView(feedbackForm = hasErrors,
          postAction = routes.FeedbackController.submit()))),
        formData =>
          feedbackConnector.submit(formData).flatMap {
            case Right(_) =>
              Future.successful(Redirect(routes.FeedbackController.thankYou()))
            case Left(status) =>
              Logger("application").error(s"Unexpected status code from feedback form: $status")
              throw new Error(s"Failed to on post request: ${status}")
          }).recover {
          case ex: Exception =>
            Logger("application")
              .error(s"Unexpected error code from feedback form: - ${ex.getMessage} - ${ex.getCause}")
            itvcErrorHandler.showInternalServerError()
        }
  }

  def submitAgent: Action[AnyContent] = authActions.asAuthorisedUser.async {
    implicit request =>
      FeedbackForm.form.bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(feedbackView(feedbackForm = hasErrors,
          postAction = routes.FeedbackController.submitAgent()))),
        formData =>
          feedbackConnector.submit(formData).flatMap {
            case Right(_) =>
              Future.successful(Redirect(routes.FeedbackController.thankYouAgent()))
            case Left(status) =>
              Logger("application").error(s"[Agent] Unexpected status code from feedback form: $status")
              throw new Error(s"Failed to on post request: ${status}")
          }).recover {
          case ex: Exception =>
            Logger("application")
              .error(s"[Agent] Unexpected error code from feedback form: ${ex.getMessage} - ${ex.getCause}")
            agentItvcErrorHandler.showInternalServerError()
        }
  }

  def thankYou: Action[AnyContent] = authActions.asAuthorisedUser {
    implicit request =>
      val referer = request.session.get(REFERER).getOrElse(config.baseUrl)
      Ok(feedbackThankYouView(referer)).withSession(request.session - REFERER)
  }

  def thankYouAgent: Action[AnyContent] = authActions.asAuthorisedUser.async {
    implicit request =>
      val referer = request.session.get(REFERER).getOrElse(config.agentBaseUrl)
      Future.successful(Ok(feedbackThankYouView(referer, isAgent = true)).withSession(request.session - REFERER))
  }

}
