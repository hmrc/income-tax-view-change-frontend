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

package testOnly.controllers

import play.api.Logging
import play.api.mvc.*
import testOnly.utils.FileUtil.getFileFromPath

import javax.inject.Inject

class TestOnlyAssetsController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Logging {

  def at(filePath: String, isNewContextRoot: Boolean): Action[AnyContent] = Action {
    getFileFromPath(s"/testOnly/$filePath") match {
      case Right(content) =>
        logger.info(s"can read content")
        Ok(content).as("text/javascript")
      case Left(ex) =>
        logger.error(s"$filePath - $ex")
        NotFound
    }
  }
}
