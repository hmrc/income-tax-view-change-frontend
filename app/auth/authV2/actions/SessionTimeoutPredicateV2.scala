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

package auth.authV2.actions

import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.http.SessionKeys

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionTimeoutPredicateV2 @Inject()(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[Request, Request] with ActionBuilder[Request, AnyContent] {

  override protected def refine[A](request: Request[A]): Future[Either[Result, Request[A]]] = {

    val updatedHeaders = request.session.get("Gov-Test-Scenario") match {
      case Some(data) => request.headers.add(("Gov-Test-Scenario", data))
      case _ => request.headers
    }

    (request.session.get(SessionKeys.lastRequestTimestamp), request.session.get(SessionKeys.authToken)) match {
      case (Some(_), None) =>
        // Auth session has been wiped by Frontend Bootstrap Filter, hence timed out.
        Logger("application").warn("Session Time Out.")
        Future.successful(Left(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout)))
      case (_, _) =>
        val mtdItUserWithUpdatedHeaders = request.withHeaders(updatedHeaders)
        Future.successful(Right(mtdItUserWithUpdatedHeaders))
    }
  }

}
