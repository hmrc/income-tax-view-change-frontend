/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.predicates

import javax.inject.Inject

import auth.MtdItUser
import config.FrontendAppConfig
import controllers.BaseController
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.Retrievals.authorisedEnrolments
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.frontend.Redirects
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

class AuthenticationPredicate @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                        val appConfig: FrontendAppConfig,
                                        override val config: Configuration,
                                        override val env: Environment,
                                        implicit val messagesApi: MessagesApi
                                       ) extends BaseController with Redirects {

  private type AsyncUserRequest = Request[AnyContent] => MtdItUser => Future[Result]

  lazy val mtdItEnrolmentKey: String = appConfig.mtdItEnrolmentKey
  lazy val mtdItIdentifierKey: String = appConfig.mtdItIdentifierKey

  lazy val ninoEnrolmentKey: String = appConfig.ninoEnrolmentKey
  lazy val ninoIdentifierKey: String = appConfig.ninoIdentifierKey

  def async(action: AsyncUserRequest): Action[AnyContent] = Action.async { implicit request =>
    checkSessionTimeout(request) {
      authorisedFunctions.authorised(Enrolment(mtdItEnrolmentKey) and Enrolment(ninoEnrolmentKey)).retrieve(authorisedEnrolments) {
        authorisedEnrolments => {
          action(request)(
            MtdItUser(
              mtditid = getEnrolmentIdentifierValue(mtdItEnrolmentKey, mtdItIdentifierKey)(authorisedEnrolments),
              nino = getEnrolmentIdentifierValue(ninoEnrolmentKey, ninoIdentifierKey)(authorisedEnrolments)
            )
          )
        }
      }.recoverWith {
        case _: InsufficientEnrolments =>
          Logger.debug("[AuthenticationPredicate][async] No HMRC-MTD-IT Enrolment and/or No NINO.")
          Future.successful(showInternalServerError)
        case _: BearerTokenExpired =>
          Logger.debug("[AuthenticationPredicate][async] Bearer Token Timed Out.")
          Future.successful(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout()))
        case _ =>
          Logger.debug("[AuthenticationPredicate][async] Unauthorised request. Redirect to Sign In.")
          Future.successful(Redirect(controllers.routes.SignInController.signIn()))
      }
    }
  }

  private[AuthenticationPredicate] def checkSessionTimeout(request: Request[AnyContent])(f: => Future[Result]) = {
    (request.session.get(SessionKeys.lastRequestTimestamp), request.session.get(SessionKeys.authToken)) match {
      case (Some(x), None) =>
        // Auth session has been wiped by Frontend Bootstrap Filter, hence timed out.
        Logger.debug("[AuthenticationPredicate][handleSessionTimeout] Session Time Out.")
        Future.successful(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout()))
      case (_, _) =>
        f
    }
  }

  // The following private function uses get on the value as the auth predicate should have already established the enrolments exists.
  private[AuthenticationPredicate] def getEnrolmentIdentifierValue(enrolment: String, identifier: String)(enrolments: Enrolments)  =
    enrolments.getEnrolment(enrolment).flatMap(_.getIdentifier(identifier)).map(_.value).get

}