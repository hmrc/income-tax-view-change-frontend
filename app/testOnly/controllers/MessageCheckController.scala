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

import config.FrontendAppConfig
import controllers.ItvcLanguageController
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import testOnly.views.html.MessageCheckView
import uk.gov.hmrc.play.language.LanguageUtils
import java.io.{FileInputStream, IOException}
import java.util.Properties
import javax.inject.Inject


class MessageCheckController @Inject()(messageCheckView: MessageCheckView,
                                       mcc: MessagesControllerComponents,
                                       appConfig: FrontendAppConfig,
                                       languageUtils: LanguageUtils) extends ItvcLanguageController(mcc, appConfig, languageUtils) {


  def show(): Action[AnyContent] = Action { implicit req =>
    val filePath = "conf/messages"
    val keys = readMessageFileKeys(filePath)
    Ok(messageCheckView(keys))
  }

  def showWelsh(): Action[AnyContent] = Action { implicit req =>
    val filePath = "conf/messages.cy"
    val keys = readMessageFileKeys(filePath)
    Ok(messageCheckView(keys))
  }

  private def readMessageFileKeys(filePath: String): List[String] = {
    val properties = new Properties()
    try {
      val fileInput = new FileInputStream(filePath)
      properties.load(fileInput)
      fileInput.close()
    } catch {
      case e: IOException => e.printStackTrace()
    }
    properties.stringPropertyNames().toArray.map(_.toString).toList
  }

}
