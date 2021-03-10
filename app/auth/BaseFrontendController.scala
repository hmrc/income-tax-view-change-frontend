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
import controllers.predicates.AuthPredicate._
import controllers.predicates.IncomeTaxUser
import controllers.predicates.agent.AgentAuthenticationPredicate.MissingAgentReferenceNumber
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseFrontendController(implicit val mcc: MessagesControllerComponents, itvcErrorHandler: ItvcErrorHandler)
  extends FrontendController(mcc) with I18nSupport {

  val authorisedFunctions: AuthorisedFunctions

  implicit val ec: ExecutionContext

  type ActionBody[User <: IncomeTaxUser] = Request[AnyContent] => User => Future[Result]
  type AuthenticatedAction[User <: IncomeTaxUser] = ActionBody[User] => Action[AnyContent]

  protected trait AuthenticatedActions[User <: IncomeTaxUser] {

    def userApply: (Enrolments, Option[AffinityGroup], ConfidenceLevel) => User

    def apply(action: Request[AnyContent] => User => Result): Action[AnyContent] = async(action andThen (_ andThen Future.successful))

    protected def asyncInternal(predicate: AuthPredicate[User])(action: ActionBody[User]): Action[AnyContent] =
      Action.async { implicit request =>
        authorisedFunctions.authorised().retrieve(allEnrolments and affinityGroup and confidenceLevel) {
          case enrolments ~ affinity ~ confidence =>
            implicit val user: User = userApply(enrolments, affinity, confidence)

            predicate.apply(request)(user) match {
              case Right(AuthPredicateSuccess) => action(request)(user)
              case Left(failureResult) => failureResult
            }
        }.recover(handleExceptions(request))
      }

    def async: AuthenticatedAction[User]

  }

  private def handleExceptions(request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: BearerTokenExpired =>
      Logger.info("[BaseFrontendController][handleExceptions] - User's bearer token expired, redirecting to timeout page")
      Redirect(controllers.timeout.routes.SessionTimeoutController.timeout())
    case ex: MissingAgentReferenceNumber =>
      Logger.warn(s"[BaseFrontendController][handleExceptions] - ${ex.reason}")
      itvcErrorHandler.showOkTechnicalDifficulties()(request)
    case ex: AuthorisationException =>
      Logger.warn(s"[BaseFrontendController][handleExceptions] - AuthorisationException occured - ${ex.reason}")
      Redirect(controllers.routes.SignInController.signIn())
    case _: NotFoundException =>
      NotFound(itvcErrorHandler.notFoundTemplate(request))
    case ex => throw ex
  }

}
