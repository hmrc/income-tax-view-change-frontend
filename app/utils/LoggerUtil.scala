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

package utils

import auth.MtdItUser
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import play.api.i18n.Lang.logger
import play.api.mvc.{Call, Result}

trait LoggerUtil {
  self =>

  val fileName: String = self.getClass.getSimpleName

  private val formattedMessage = (isAgent: Boolean, methodName: String, message: String) =>
    s"[TEST]${if (isAgent) "[Agent]" else ""}[$fileName][$methodName] - $message"

  def logAndShowError(methodName: String)(message: String, redirect: Option[Result] = None)
                     (implicit user: MtdItUser[_],
                      itvcErrorHandler: ItvcErrorHandler,
                      agentItvcErrorHandler: AgentItvcErrorHandler): Result = {

    logger.error(formattedMessage(user.isAgent, methodName, message))
    redirect
      .getOrElse(
        (if (user.isAgent) itvcErrorHandler else agentItvcErrorHandler).showInternalServerError()
      )
  }

  def logWithDebug(message: String)(methodName: String)
                  (implicit user: MtdItUser[_]): Unit =
    logger.debug(formattedMessage(user.isAgent, methodName, message))

  def logWithInfo(message: String)(methodName: String)
                 (implicit user: MtdItUser[_]): Unit =
    logger.info(formattedMessage(user.isAgent, methodName, message))
}