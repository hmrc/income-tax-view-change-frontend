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

package testOnly.utils

import play.api.Logging
import play.api.mvc.Results.Ok

import scala.concurrent.Future

trait OptOutCustomDataUploadHelper extends Logging {

  def handleDefaultValues(status: String)(codeBlock: => Future[Unit]): Future[Unit] = {
    if (status == "Default") {
      logger.info(s"[handleDefaultValues(status: String)] Default was chosen by the user. There is nothing to overwrite. < Status: $status >")
      Future.successful(Ok(s"Default was chosen by the user. There is nothing to overwrite. < Status: $status >"))
    } else {
      codeBlock
    }
  }

  def handleDefaultValues(status: Option[String])(codeBlock: => Future[Unit]): Future[Unit] = {
    status match {
      case Some("Default") =>
        logger.info(s"[handleDefaultValues(status: Option[String])] Default was chosen by the user. There is nothing to overwrite. < Status: $status >")
        Future.successful(Ok(s"Default was chosen by the user. There is nothing to overwrite. < Status: $status >"))
      case Some(value) =>
        logger.info(s"Status value provided by the user: $value. Proceeding with the code block execution.")
        codeBlock
      case None =>
        logger.info("No status value provided by the user. Proceeding with the code block execution.")
        codeBlock
    }
  }
}
