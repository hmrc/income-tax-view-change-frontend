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

import javax.inject.{Inject, Singleton}

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import connectors.{ServiceInfoPartialConnector, UserDetailsConnector}
import controllers.BaseController
import models.{UserDetailsError, UserDetailsModel}
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Request, Result}
import play.api.{Configuration, Environment, Logger}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.Retrievals._
import uk.gov.hmrc.auth.core._

import scala.concurrent.Future
import uk.gov.hmrc.play.frontend.config.AuthRedirects

@Singleton
class AuthenticationPredicate @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                        val appConfig: FrontendAppConfig,
                                        override val config: Configuration,
                                        override val env: Environment,
                                        implicit val messagesApi: MessagesApi,
                                        val userDetailsConnector: UserDetailsConnector
                                       ) extends BaseController with AuthRedirects {

  lazy val mtdItEnrolmentKey: String = appConfig.mtdItEnrolmentKey
  lazy val mtdItIdentifierKey: String = appConfig.mtdItIdentifierKey
  lazy val ninoEnrolmentKey: String = appConfig.ninoEnrolmentKey
  lazy val ninoIdentifierKey: String = appConfig.ninoIdentifierKey

  def authorisedUser(f: MtdItUser => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    authorisedFunctions.authorised(Enrolment(mtdItEnrolmentKey) and Enrolment(ninoEnrolmentKey)).retrieve(authorisedEnrolments and userDetailsUri) {
      case enrolments ~ userDetailsUrl => {
        userDetailsConnector.getUserDetails(userDetailsUrl.get).flatMap {
          case userDetails: UserDetailsModel =>
            f(buildMtdUser(enrolments, Some(userDetails)))
          case UserDetailsError =>
            f(buildMtdUser(enrolments))
        }
      }
    }.recoverWith {
      case _: InsufficientEnrolments =>
        Logger.debug("[AuthenticationPredicate][async] No HMRC-MTD-IT Enrolment and/or No NINO.")
        Future.successful(Redirect(controllers.notEnrolled.routes.NotEnrolledController.show()))
      case _: BearerTokenExpired =>
        Logger.debug("[AuthenticationPredicate][async] Bearer Token Timed Out.")
        Future.successful(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout()))
      case _: AuthorisationException =>
        Logger.debug("[AuthenticationPredicate][async] Unauthorised request. Redirect to Sign In.")
        Future.successful(Redirect(controllers.routes.SignInController.signIn()))
      case _ =>
        Logger.debug("[AuthenticationPredicate][async] Unexpected Error Caught. Show ISE.")
        Future.successful(showInternalServerError)
    }
  }

  private def buildMtdUser(enrolments: Enrolments, userDetails: Option[UserDetailsModel] = None): MtdItUser = {
    MtdItUser(
      mtditid = enrolments.getEnrolment(mtdItEnrolmentKey).flatMap(_.getIdentifier(mtdItIdentifierKey)).map(_.value).get,
      nino = enrolments.getEnrolment(ninoEnrolmentKey).flatMap(_.getIdentifier(ninoIdentifierKey)).map(_.value).get,
      userDetails
    )
  }
}