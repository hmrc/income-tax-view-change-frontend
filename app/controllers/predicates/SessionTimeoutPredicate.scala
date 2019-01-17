/*
 * Copyright 2019 HM Revenue & Customs
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

import controllers.BaseController
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future

@Singleton
class SessionTimeoutPredicate @Inject()(implicit val messagesApi: MessagesApi)
  extends BaseController with ActionBuilder[Request] with ActionFunction[Request, Request] {

  override def invokeBlock[A](request: Request[A], f: (Request[A]) => Future[Result]): Future[Result] = {

    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    (request.session.get(SessionKeys.lastRequestTimestamp), request.session.get(SessionKeys.authToken)) match {
      case (Some(_), None) =>
        // Auth session has been wiped by Frontend Bootstrap Filter, hence timed out.
        Logger.debug("[AuthenticationPredicate][handleSessionTimeout] Session Time Out.")
        Future.successful(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout()))
      case (_, _) => f(request)
    }
  }
}
