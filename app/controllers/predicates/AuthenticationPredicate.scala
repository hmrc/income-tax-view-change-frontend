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

import config.FrontendAppConfig
import controllers.BaseController
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.Retrievals.authorisedEnrolments
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.frontend.Redirects

import scala.concurrent.Future

class AuthenticationPredicate @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                        val appConfig: FrontendAppConfig,
                                        override val config: Configuration,
                                        override val env: Environment,
                                        implicit val messagesApi: MessagesApi
                                       ) extends BaseController with Redirects {

  private type PlayRequest  = Request[AnyContent] => Result
  private type AsyncRequest = Request[AnyContent] => String => Future[Result]

  lazy val ggSignInRedirect: Result =  toGGLogin(appConfig.ggSignInContinueUrl)

  lazy val mtdItEnrolmentKey: String = appConfig.mtdItEnrolmentKey
  lazy val mtdItIdentifierKey: String = appConfig.mtdItIdentifierKey

  def async(action: AsyncRequest): Action[AnyContent] = {
    Action.async { implicit request =>
      authorisedFunctions.authorised(Enrolment(mtdItEnrolmentKey)).retrieve(authorisedEnrolments) { authorisedEnrolments =>
        action(request)(getMtdItID(authorisedEnrolments))
      }.recoverWith {
        case _ :InsufficientEnrolments =>
          Logger.debug("[AuthenticationPredicate][async] No HMRC-MTD-IT Enrolment.")
          Future.successful(showInternalServerError)
        case _ :BearerTokenExpired =>
          Logger.debug("[AuthenticationPredicate][async] Session Time Out.")
          Future.successful(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout()))
        case _ =>
          Logger.debug("[AuthenticationPredicate][async] Unauthorised request. Redirect to GG Sign In")
          Future.successful(ggSignInRedirect)
      }
    }
  }

  private[AuthenticationPredicate] def getMtdItID(activeEnrolments: Enrolments) =
    activeEnrolments.getEnrolment(mtdItEnrolmentKey).flatMap(_.getIdentifier(mtdItIdentifierKey)).map(_.value).get
}