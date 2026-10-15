/*
 * Copyright 2024 HM Revenue & Customs
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

package common.auth.actions

import com.google.inject.Singleton
import common.auth.AuthorisedAndEnrolledRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import common.config.FrontendAppConfig

@Singleton
class AgentHasConfirmedClientAction @Inject()(appConfig: FrontendAppConfig)(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[AuthorisedAndEnrolledRequest, AuthorisedAndEnrolledRequest] {

  override protected def refine[A](request: AuthorisedAndEnrolledRequest[A]): Future[Either[Result, AuthorisedAndEnrolledRequest[A]]] = {
    if(request.clientDetails.exists(_.confirmed)) {
      Future.successful(Right(request))
    } else {
      Future.successful(Left(Redirect(appConfig.confirmClientUTRUrl)))
    }
  }
}
