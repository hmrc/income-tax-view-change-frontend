/*
 * Copyright 2021 HM Revenue & Customs
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

package auth

import config.ItvcErrorHandler
import controllers.agent.utils.SessionKeys
import controllers.predicates.AuthPredicate._
import controllers.predicates.IncomeTaxUser
import controllers.predicates.agent.AgentAuthenticationPredicate.MissingAgentReferenceNumber
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseFrontendController(implicit val mcc: MessagesControllerComponents, itvcErrorHandler: ItvcErrorHandler)
  extends FrontendController(mcc) with I18nSupport {
  type ActionBody[User <: IncomeTaxUser] = Request[AnyContent] => User => Future[Result]

  implicit val ec: ExecutionContext
  type AuthenticatedAction[User <: IncomeTaxUser] = ActionBody[User] => Action[AnyContent]
  val authorisedFunctions: AuthorisedFunctions

  private def handleExceptions(request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: BearerTokenExpired =>
      Logger("application").info("[BaseFrontendController][handleExceptions] - User's bearer token expired, redirecting to timeout page")
      Redirect(controllers.timeout.routes.SessionTimeoutController.timeout())
    case ex: MissingAgentReferenceNumber =>
      Logger("application").warn(s"[BaseFrontendController][handleExceptions] - ${ex.reason}")
      itvcErrorHandler.showOkTechnicalDifficulties()(request)
    case ex: InsufficientEnrolments =>
      Logger("application").warn(s"[BaseFrontendController][handleExceptions] - ${ex.reason}")
      Redirect(controllers.agent.routes.ClientRelationshipFailureController.show())
    case ex: AuthorisationException =>
      Logger("application").warn(s"[BaseFrontendController][handleExceptions] - AuthorisationException occurred - ${ex.reason}")
      Redirect(controllers.routes.SignInController.signIn())
    case _: NotFoundException =>
      NotFound(itvcErrorHandler.notFoundTemplate(request))
    case ex => throw ex
  }

  protected trait AuthenticatedActions[User <: IncomeTaxUser] {

    def userApply: (Enrolments, Option[AffinityGroup], ConfidenceLevel, Option[Credentials]) => User

    def apply(action: Request[AnyContent] => User => Result): Action[AnyContent] = async(action andThen (_ andThen Future.successful))

    def async: AuthenticatedAction[User]

    protected def asyncInternal(predicate: AuthPredicate[User], requireClientSelected: Boolean)(action: ActionBody[User]): Action[AnyContent] =
      Action.async { implicit request =>
        val clientMtd: Option[String] = if (requireClientSelected) request.session.get(SessionKeys.clientMTDID) else None
        val authPredicate: Predicate = clientMtd match {
          case Some(mtdId) => Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", mtdId).withDelegatedAuthRule("mtd-it-auth")
          case _ => EmptyPredicate
        }

        //Add test headers if found in session
        lazy val updatedHeaders = request.session.get("Gov-Test-Scenario") match {
          case Some(data) => request.headers.add(("Gov-Test-Scenario", data))
          case _ => request.headers
        }

        authorisedFunctions.authorised(authPredicate).retrieve(allEnrolments and affinityGroup and confidenceLevel and credentials) {
          case enrolments ~ affinity ~ confidence ~ credentials =>
            implicit val user: User = userApply(enrolments, affinity, confidence, credentials)
            predicate.apply(request)(user) match {
              case Right(AuthPredicateSuccess) if requireClientSelected && clientMtd.isEmpty =>
                Future.successful(Redirect(controllers.agent.routes.EnterClientsUTRController.show()))
              case Right(AuthPredicateSuccess) =>
                action(request.withHeaders(updatedHeaders))(user)
              case Left(failureResult) => failureResult
            }
        }.recover(handleExceptions(request))
      }

  }

}
