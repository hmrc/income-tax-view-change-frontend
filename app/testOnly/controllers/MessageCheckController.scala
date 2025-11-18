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

import javax.inject.Inject


class MessageCheckController @Inject()(messageCheckView: MessageCheckView,
                                       mcc: MessagesControllerComponents,
                                       languageUtils: LanguageUtils) extends ItvcLanguageController(mcc, languageUtils) {


  def show(): Action[AnyContent] = Action { implicit req =>
    val keys = readMessageFileKeys("default")
    Ok(messageCheckView(keys))
  }

  def showWelsh(): Action[AnyContent] = Action { implicit req =>
    val keys = readMessageFileKeys("cy")
    Ok(messageCheckView(keys))
  }

  private def readMessageFileKeys(language: String): List[String] = {
    mcc.messagesApi.messages.filter(_._1 == language).flatMap(_._2).keys.toList
  }

}
