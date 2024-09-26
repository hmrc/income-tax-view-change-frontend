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

package testUtils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Lang, MessagesApi}
import play.twirl.api.Html
import testOnly.views.html.MessageCheckView

import scala.language.reflectiveCalls

class MessageCheckViewSpec extends TestSupport {

  val messageCheckView: MessageCheckView = app.injector.instanceOf[MessageCheckView]
  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val lang: Lang = Lang("cy")

  class LanguageSetupTest(language: String) {

    val messageKeys: List[String] =
      messagesApi.messages.collect {
        case (messageKey, values) if messageKey == language => values.keys
      }.flatten(_.toList).toList

    val messagesMap: Map[String, String] = messagesApi.messages.collect {
      case (messageKey, values) if messageKey == language => values
    }.flatten(_.toMap).toMap

    val view: Html = messageCheckView(messageKeys)

    val document: Document = Jsoup.parse(view.body)
  }

  "Render Message in correct format" when {

    "message key is given" should {

      "return correct English format" in new LanguageSetupTest("default") {

        document.select(".messageRow").forEach { element =>
          val messageKey = element.select(".messageKey").text()
          val messageValue = element.select(".messageValue").text()
          Some(messageValue) shouldBe messagesMap.get(messageKey)
        }
      }

      "return correct Welsh format" in new LanguageSetupTest("cy") {
        messageKeys.foreach { messageKey =>
          Some(msgs(messageKey)) shouldBe messagesMap.get(messageKey)
        }
      }
    }
  }
}
