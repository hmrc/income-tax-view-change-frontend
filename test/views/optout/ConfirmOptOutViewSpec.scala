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

package views.optout

import config.FrontendAppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optout.ConfirmOptOut

class ConfirmOptOutViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val confirmOptOutView: ConfirmOptOut = app.injector.instanceOf[ConfirmOptOut]

  class Setup(isAgent: Boolean = true) {
    val pageDocument: Document = Jsoup.parse(contentAsString(confirmOptOutView(isAgent)))
  }

  object confirmOptOutMessages {
    val heading: String = messages("optout.confirmOptOut.heading")
    val title: String = messages("htmlTitle", heading)
    val summary: String = messages("optout.confirmOptOut.desc")
    val confirmButton: String = messages("optout.confirmOptOut.confirm")
    val cancelButton: String = messages("optout.confirmOptOut.cancel")
  }

  "Next Updates page" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title() shouldBe confirmOptOutMessages.title
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe confirmOptOutMessages.heading
    }

    "have the correct summary heading and page contents" in new Setup(false) {
      pageDocument.getElementById("summary").text() shouldBe confirmOptOutMessages.summary
      pageDocument.getElementById("confirm-button").text() shouldBe confirmOptOutMessages.confirmButton
      pageDocument.getElementById("cancel-button").text() shouldBe confirmOptOutMessages.cancelButton
    }

  }
}
