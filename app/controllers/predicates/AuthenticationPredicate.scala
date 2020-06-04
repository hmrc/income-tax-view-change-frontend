/*
 * Copyright 2020 HM Revenue & Customs
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

import auth.{FrontendAuthorisedFunctions, MtdItUserOptionNino}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.BaseController
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticationPredicate @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                        val appConfig: FrontendAppConfig,
                                        override val config: Configuration,
                                        override val env: Environment,
                                        implicit val messagesApi: MessagesApi,
                                        implicit val ec: ExecutionContext,
                                        val itvcErrorHandler: ItvcErrorHandler
                                       )
  extends BaseController with AuthRedirects with ActionBuilder[MtdItUserOptionNino]
    with ActionFunction[Request, MtdItUserOptionNino] {

  override def invokeBlock[A](request: Request[A], f: MtdItUserOptionNino[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    implicit val req: Request[A] = request

    authorisedFunctions.authorised(Enrolment(appConfig.mtdItEnrolmentKey)).retrieve(allEnrolments and name) {
      case enrolments ~ userName => f(buildMtdUserOptionNino(enrolments, userName))
    } recover {
      case _: InsufficientEnrolments =>
        Logger.error("[AuthenticationPredicate][async] No HMRC-MTD-IT Enrolment and/or No NINO.")
        Redirect(controllers.notEnrolled.routes.NotEnrolledController.show())
      case _: BearerTokenExpired =>
        Logger.error("[AuthenticationPredicate][async] Bearer Token Timed Out.")
        Redirect(controllers.timeout.routes.SessionTimeoutController.timeout())
      case _: AuthorisationException =>
        Logger.error("[AuthenticationPredicate][async] Unauthorised request. Redirect to Sign In.")
        Redirect(controllers.routes.SignInController.signIn())
      case s =>
        Logger.error(s"[AuthenticationPredicate][async] Unexpected Error Caught. Show ISE.\n${s.getMessage}\n\n$s\n")
        itvcErrorHandler.showInternalServerError
    }
  }

  private def buildMtdUserOptionNino[A](enrolments: Enrolments, userName: Option[Name])
                                       (implicit request: Request[A]): MtdItUserOptionNino[A] = {
    MtdItUserOptionNino(
      mtditid = enrolments.getEnrolment(appConfig.mtdItEnrolmentKey).flatMap(_.getIdentifier(appConfig.mtdItIdentifierKey)).map(_.value).get,
      nino = enrolments.getEnrolment(appConfig.ninoEnrolmentKey).flatMap(_.getIdentifier(appConfig.ninoIdentifierKey)).map(_.value),
      userName,
      saUtr = enrolments.getEnrolment(appConfig.saEnrolmentKey).flatMap(_.getIdentifier(appConfig.saIdentifierKey)).map(_.value)
    )
  }
}
