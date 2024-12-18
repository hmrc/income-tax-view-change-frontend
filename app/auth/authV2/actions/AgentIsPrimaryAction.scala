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

package auth.authV2.actions

import auth.MtdItUserOptionNino
import com.google.inject.Singleton
import config.AgentItvcErrorHandler
import play.api.mvc.{ActionRefiner, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentIsPrimaryAction @Inject()(agentItvcErrorHandler: AgentItvcErrorHandler)(implicit val executionContext: ExecutionContext)
extends ActionRefiner[MtdItUserOptionNino, MtdItUserOptionNino] {

  override protected def refine[A](request: MtdItUserOptionNino[A]): Future[Either[Result, MtdItUserOptionNino[A]]] = {
    if(request.isSupportingAgent) {
      Future.successful(Left(agentItvcErrorHandler.supportingAgentUnauthorised()(request)))
    } else {
      Future.successful(Right(request))
    }
  }
}
