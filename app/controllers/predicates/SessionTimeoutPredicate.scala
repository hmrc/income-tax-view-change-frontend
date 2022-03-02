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

package controllers.predicates

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.http.SessionKeys
import controllers.BaseController


import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionTimeoutPredicate @Inject()(implicit mcc: MessagesControllerComponents, val ec: ExecutionContext)
  extends BaseController with ActionBuilder[Request, AnyContent] with ActionFunction[Request, Request] {

  override val parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser
  override val executionContext: ExecutionContext = mcc.executionContext


  override def invokeBlock[A](request: Request[A], f: Request[A] => Future[Result]): Future[Result] = {
    //Add test headers if found in session
    val updatedHeaders = request.session.get("Gov-Test-Scenario") match {
      case Some(data) => request.headers.add(("Gov-Test-Scenario", data))
      case _ => request.headers
    }


    (request.session.get(SessionKeys.lastRequestTimestamp), request.session.get(SessionKeys.authToken)) match {
      case (Some(_), None) =>
        // Auth session has been wiped by Frontend Bootstrap Filter, hence timed out.
        Logger("application").warn("[AuthenticationPredicate][handleSessionTimeout] Session Time Out.")
        Future.successful(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout()))
      case (_, _) => f(Request(request.withHeaders(updatedHeaders), request.body))
    }
  }
}
