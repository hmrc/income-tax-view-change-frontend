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

package testOnly.controllers

import play.api.Logger
import play.api.mvc._
import testOnly.utils.FileUtil.getFileFromPath

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestOnlyAssetsController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def at(filePath: String): Action[AnyContent] = Action {
    getFileFromPath(s"/testOnly/$filePath") match {
      case Right(content) =>
        Logger("application").info(s"[TestOnlyAssetsController] - can read content")
        Ok(content).as("text/javascript")
      case Left(ex) =>
        Logger("application").error(s"[TestOnlyAssetsController] - ${filePath} - ${ex}")
        NotFound
    }
  }
}
