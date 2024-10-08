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

package utils

import auth.MtdItUser
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import play.api.Logger
import play.api.mvc.Result
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

trait ErrorRecovery {

  val individualErrorHandler: ItvcErrorHandler
  val agentErrorHandler: AgentItvcErrorHandler

  def logAndRedirect[A](logMsg: String)(implicit user: MtdItUser[A]): Result = {
    logWithUserType(logMsg)
    redirectToErrorPage()
  }

  def logAndRedirect[A](implicit user: MtdItUser[A]): PartialFunction[Throwable, Result] = {
    case throwable: Throwable =>
      logWithUserType(s"[${throwable.getClass.getSimpleName}] ${throwable.getLocalizedMessage}")
      redirectToErrorPage()
  }

  private def logWithUserType[A](msg: String)(implicit user: MtdItUser[A]): Unit = {
    user.userType match {
      case Some(Agent) =>
        Logger(this.getClass).error(s"[Agent] $msg")
      case _ =>
        Logger(this.getClass).error(msg)
    }
  }

  private def redirectToErrorPage[A]()(implicit user: MtdItUser[A]): Result = {
    user.userType match {
      case Some(Agent) =>
        agentErrorHandler.showInternalServerError()
      case _ =>
        individualErrorHandler.showInternalServerError()
    }
  }
}
