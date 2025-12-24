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

import auth.MtdItUser
import auth.authV2.models.AuthorisedAndEnrolledRequest
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.BaseController
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}
import play.api.Logger
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceRetrievalAction @Inject()(val incomeSourceDetailsService: IncomeSourceDetailsService)
                                           (
                                             implicit val executionContext: ExecutionContext,
                                             val individualErrorHandler: ItvcErrorHandler,
                                             val agentErrorHandler: AgentItvcErrorHandler,
                                             mcc: MessagesControllerComponents
                                           ) extends BaseController with ActionRefiner[AuthorisedAndEnrolledRequest, MtdItUser] {

  private def internalServerErrorFor(request: AuthorisedAndEnrolledRequest[_]): Result = {

    request.authUserDetails.affinityGroup match {
      case Some(Agent) =>
        Logger(getClass).error(s"[IncomeSourceRetrievalAction][internalServerErrorFor] Showing Agent error page")
        agentErrorHandler.showInternalServerError()(request)
      case Some(Individual) =>
        Logger(getClass).error(s"[IncomeSourceRetrievalAction][internalServerErrorFor] Showing Individual error page")
        individualErrorHandler.showInternalServerError()(request)
      case _ =>
        Logger(getClass).error(s"[IncomeSourceRetrievalAction][internalServerErrorFor] Unknown user type or unknown error")
        individualErrorHandler.showInternalServerError()(request)
    }
  }

  override def refine[A](request: AuthorisedAndEnrolledRequest[A]): Future[Either[Result, MtdItUser[A]]] = {

    implicit val req: AuthorisedAndEnrolledRequest[A] = request

    incomeSourceDetailsService.getIncomeSourceDetails().map {
      case response: IncomeSourceDetailsModel =>
        Right(MtdItUser(req.mtditId, response.nino, req.mtdUserRole, req.authUserDetails, req.clientDetails, response))
      case error: IncomeSourceDetailsError =>
        Left(internalServerErrorFor(request))
    }
  }

  def logAndRedirect[A]()(implicit req: AuthorisedAndEnrolledRequest[A]): PartialFunction[Throwable, Either[Result, MtdItUser[A]]] = {
    case throwable: Throwable =>
      Left(logWithUserType(s"[${throwable.getClass.getSimpleName}] ${throwable.getLocalizedMessage}"))
  }

  def logWithUserType[A](msg: String)(implicit req: AuthorisedAndEnrolledRequest[A]): Result = {
    req.authUserDetails.affinityGroup match {
      case Some(Agent) =>
        Logger(this.getClass).error(s"[Agent] $msg")
        agentErrorHandler.showInternalServerError()
      case _ =>
        Logger(this.getClass).error(msg)
        individualErrorHandler.showInternalServerError()
    }
  }
}