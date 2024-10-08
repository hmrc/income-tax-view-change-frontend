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

package controllers.predicates

import auth.{MtdItUser, MtdItUserOptionNino}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.BaseController
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}
import play.api.Logger
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceDetailsPredicate @Inject()(val incomeSourceDetailsService: IncomeSourceDetailsService)
                                            (implicit val executionContext: ExecutionContext,
                                             val individualErrorHandler: ItvcErrorHandler,
                                             val agentErrorHandler: AgentItvcErrorHandler,
                                             mcc: MessagesControllerComponents) extends BaseController with
  ActionRefiner[MtdItUserOptionNino, MtdItUser] {

  override def refine[A](request: MtdItUserOptionNino[A]): Future[Either[Result, MtdItUser[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val req: MtdItUserOptionNino[A] = request

    // no caching for now
    incomeSourceDetailsService.getIncomeSourceDetails() map {
      case response: IncomeSourceDetailsModel =>
        Right(MtdItUser(request.mtditid, response.nino, request.userName, response, None, request.saUtr, request.credId, request.userType, request.arn))
      case error: IncomeSourceDetailsError => Left(logWithUserType(s"[${error.status}] ${error.reason}"))
    } recover logAndRedirect()

  }

  def logAndRedirect[A]()(implicit req: MtdItUserOptionNino[A]): PartialFunction[Throwable, Either[Result, MtdItUser[A]]] = {
    case throwable: Throwable =>
      Left(logWithUserType(s"[${throwable.getClass.getSimpleName}] ${throwable.getLocalizedMessage}"))
  }

  def logWithUserType[A](msg: String)(implicit req: MtdItUserOptionNino[A]): Result = {
    req.userType match {
      case Some(Agent) =>
        Logger(this.getClass).error(s"[Agent] $msg")
        agentErrorHandler.showInternalServerError()
      case _ =>
        Logger(this.getClass).error(msg)
        individualErrorHandler.showInternalServerError()
    }
  }
}