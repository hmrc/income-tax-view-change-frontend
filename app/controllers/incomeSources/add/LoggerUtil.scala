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

package controllers.incomeSources.add

import auth.MtdItUser
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import play.api.i18n.Lang.logger
import play.api.mvc.Result

trait LoggerUtil { self =>

  val fileName: String = self.getClass.getSimpleName

  lazy val methodName: String =
    Thread.currentThread.getStackTrace()(14).getMethodName.split('$').toList.reverse match {
      case List(_, methodName, _*) => methodName
      case _ => throw new Exception("could not get method name")
    }

  private val formattedMessage = (isAgent: Boolean, message: String) =>
    s"${if (isAgent) "[Agent]" else ""}[$fileName][$methodName] - $message"

  def logWithError(message: String)
                  (implicit user: MtdItUser[_],
                   itvcErrorHandler: ItvcErrorHandler,
                   agentItvcErrorHandler: AgentItvcErrorHandler): Result = {

    logger.error(formattedMessage(user.isAgent, message))
    (if (user.isAgent) itvcErrorHandler else agentItvcErrorHandler).showInternalServerError()
  }

  def logWithDebug(message: String)
                  (implicit user: MtdItUser[_]): Unit =
    logger.debug(formattedMessage(user.isAgent, message))

  def logWithInfo(message: String)
                 (implicit user: MtdItUser[_]): Unit =
    logger.info(formattedMessage(user.isAgent, message))
}
