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

import audit.AuditingService
import audit.models.AccessDeniedForSupportingAgentAuditModel
import auth.authV2.models.AuthorisedAndEnrolledRequest
import com.google.inject.Singleton
import config.AgentItvcErrorHandler
import enums.MTDSupportingAgent
import play.api.mvc.{ActionRefiner, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentIsPrimaryAction @Inject()(agentItvcErrorHandler: AgentItvcErrorHandler,
                                     auditingService: AuditingService)(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[AuthorisedAndEnrolledRequest, AuthorisedAndEnrolledRequest] {

  override protected def refine[A](request: AuthorisedAndEnrolledRequest[A]): Future[Either[Result, AuthorisedAndEnrolledRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)
    implicit val req: Request[A] = request
    if (request.mtdUserRole == MTDSupportingAgent) {
      auditingService.extendedAudit(AccessDeniedForSupportingAgentAuditModel(request))
      Future.successful(Left(agentItvcErrorHandler.supportingAgentUnauthorised()(request)))
    } else {
      Future.successful(Right(request))
    }
  }
}
