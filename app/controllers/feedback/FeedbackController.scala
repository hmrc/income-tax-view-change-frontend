/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.MtdItUser
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import controllers.predicates.agent.AgentAuthenticationPredicate.defaultAgentPredicates
import forms.FeedbackForm
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import views.html.feedback.{Feedback, FeedbackThankYou}

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeedbackController @Inject()(implicit val config: FrontendAppConfig,
                                   implicit val ec: ExecutionContext,
                                   val checkSessionTimeout: SessionTimeoutPredicate,
                                   val authenticate: AuthenticationPredicate,
                                   val authorisedFunctions: AuthorisedFunctions,
                                   val retrieveNino: NinoPredicate,
                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                   val retrieveBtaNavBar: NavBarPredicate,
                                   val feedbackView: Feedback,
                                   val feedbackThankYouView: FeedbackThankYou,
                                   val itvcHeaderCarrierForPartialsConverter: HeaderCarrierForPartialsConverter,
                                   val httpClient: HttpClient,
                                   val incomeSourceDetailsService: IncomeSourceDetailsService,
                                   mcc: MessagesControllerComponents,
                                   val itvcErrorHandler: ItvcErrorHandler,
                                   val agentItvcErrorHandler: AgentItvcErrorHandler
                                  ) extends ClientConfirmedController with I18nSupport {


  def show: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      val feedback = feedbackView(FeedbackForm.form, postAction = routes.FeedbackController.submit)
      request.headers.get(REFERER) match {
        case Some(ref) => Future.successful(Ok(feedback).withSession(request.session + (REFERER -> ref)))
        case _ => Future.successful(Ok(feedback))
      }
  }

  lazy val notAnAgentPredicate = {
    val redirectNotAnAgent = Future.successful(Redirect(controllers.agent.errors.routes.AgentErrorController.show()))
    defaultAgentPredicates(onMissingARN = redirectNotAnAgent)
  }

  def showAgent: Action[AnyContent] = Authenticated.asyncWithoutClientAuth(notAnAgentPredicate) {
    implicit request =>
      implicit user =>
        val feedback = feedbackView(FeedbackForm.form, postAction = routes.FeedbackController.submitAgent(), isAgent = true)
        request.headers.get(REFERER) match {
          case Some(ref) => Future.successful(Ok(feedback).withSession(request.session + (REFERER -> ref)))
          case _ => Future.successful(Ok(feedback))
        }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      FeedbackForm.form.bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(feedbackView(feedbackForm = hasErrors,
          postAction = routes.FeedbackController.submit))),
        formData => {
          httpClient.POSTForm[HttpResponse](feedbackServiceSubmitUrl,
            formData.toFormMap(request.headers.get(REFERER).getOrElse("N/A")))(readForm, partialsReadyHeaderCarrier, ec).map {
            resp =>
              resp.status match {
                case OK => Redirect(routes.FeedbackController.thankYou)
                case status =>
                  Logger("application").error(s"Unexpected status code from feedback form: $status")
                  itvcErrorHandler.showInternalServerError()
              }
          }
        }.recover {
          case ex: Exception => {
            Logger("application").error(s"Unexpected error code from feedback form: ${ex.toString}")
            itvcErrorHandler.showInternalServerError()
          }
        }
      )
  }

  def submitAgent: Action[AnyContent] = Authenticated.asyncWithoutClientAuth(notAnAgentPredicate) {
    implicit request =>
      implicit agent =>
        FeedbackForm.form.bindFromRequest().fold(
          hasErrors => Future.successful(BadRequest(feedbackView(feedbackForm = hasErrors,
            postAction = routes.FeedbackController.submitAgent))),
          formData => {
            httpClient.POSTForm[HttpResponse](feedbackServiceSubmitUrl,
              formData.toFormMap(request.headers.get(REFERER).getOrElse("N/A")))(readForm, partialsReadyHeaderCarrier, ec).map {
              resp =>
                resp.status match {
                  case OK => Redirect(routes.FeedbackController.thankYouAgent)
                  case status =>
                    Logger("application").error(s"[Agent] Unexpected status code from feedback form: $status")
                    agentItvcErrorHandler.showInternalServerError()
                }
            }
          }.recover {
            case ex: Exception => {
              Logger("application").error(s"Unexpected error code from feedback form: ${ex.toString}")
              itvcErrorHandler.showInternalServerError()
            }
          }
        )
  }

  def thankYou: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar) {
    implicit request =>
      val referer = request.session.get(REFERER).getOrElse(config.baseUrl)
      Ok(feedbackThankYouView(referer)).withSession(request.session - REFERER)
  }

  def thankYouAgent: Action[AnyContent] = Authenticated.asyncWithoutClientAuth(notAnAgentPredicate) {
    implicit request =>
      implicit user =>
        val referer = request.session.get(REFERER).getOrElse(config.agentBaseUrl)
        Future.successful(Ok(feedbackThankYouView(referer, isAgent = true)).withSession(request.session - REFERER))
  }

  private def urlEncode(value: String) = URLEncoder.encode(value, "UTF-8")

  private def partialsReadyHeaderCarrier(implicit request: Request[_]): HeaderCarrier = {
    val hc = itvcHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
    itvcHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc)
  }

  private def handleSubmit(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    FeedbackForm.form.bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(feedbackView(feedbackForm = hasErrors,
        postAction = if (isAgent) routes.FeedbackController.submitAgent else routes.FeedbackController.submit))),
      formData => {
        httpClient.POSTForm[HttpResponse](feedbackServiceSubmitUrl,
          formData.toFormMap(user.headers.get(REFERER).getOrElse("N/A")))(readForm, partialsReadyHeaderCarrier, ec).map {
          resp =>
            resp.status match {
              case OK if isAgent =>
                Redirect(routes.FeedbackController.thankYouAgent)
              case OK => Redirect(routes.FeedbackController.thankYou)
              case status if isAgent =>
                Logger("application").error(s"[Agent] Unexpected status code from feedback form: $status")
                agentItvcErrorHandler.showInternalServerError()
              case status =>
                Logger("application").error(s"Unexpected status code from feedback form: $status")
                itvcErrorHandler.showInternalServerError()
            }
        }
      }.recover {
        case ex: Exception => {
          Logger("application").error(s"Unexpected error code from feedback form: ${ex.toString}")
          itvcErrorHandler.showInternalServerError()
        }
      }
    )
  }

  lazy val feedbackServiceSubmitUrl: String =
    s"${
      config.contactFrontendBaseUrl
    }/contact/beta-feedback/submit?" +
      s"service=${
        urlEncode(config.contactFormServiceIdentifier)
      }"

  // The default HTTPReads will wrap the response in an exception and make the body inaccessible
  implicit val readForm: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse) = response
  }
}
