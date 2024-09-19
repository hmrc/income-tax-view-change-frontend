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

package views.optIn

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.optIn.CyPlusOneConfirmation

class CyPlusOneConfirmationViewSpec extends TestSupport {

  val cyPlusOneConfirmationView: CyPlusOneConfirmation = app.injector.instanceOf[CyPlusOneConfirmation]

  class Setup(isAgent: Boolean) {
    val pageDocument: Document = Jsoup.parse(contentAsString(cyPlusOneConfirmationView(isAgent)))
    val cancelButtonHref: String = controllers.routes.ReportingFrequencyPageController.show(isAgent).url
  }

  object cyPlusOneConfirmationMessages {
    val heading: String = messages("optin.cyPlusOneConfirmation.heading")
    val title: String = messages("htmlTitle", heading)
    val text: String = messages("optin.cyPlusOneConfirmation.text")
    val continueButton: String = messages("optin.cyPlusOneConfirmation.confirm")
    val cancelButton: String = messages("optin.cyPlusOneConfirmation.cancel")
  }

  "Opt-out confirm page" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title() shouldBe cyPlusOneConfirmationMessages.title
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe cyPlusOneConfirmationMessages.heading
    }

    "have the correct summary heading and page contents" in new Setup(false) {
      pageDocument.getElementById("confirmation-text").text() shouldBe cyPlusOneConfirmationMessages.text
      pageDocument.getElementById("confirm-button").text() shouldBe cyPlusOneConfirmationMessages.continueButton
      pageDocument.getElementById("cancel-button").text() shouldBe cyPlusOneConfirmationMessages.cancelButton
      pageDocument.getElementById("cancel-button").attr("href") shouldBe cancelButtonHref
    }

    "have the correct summary heading and page contents for Agents" in new Setup(true) {
      pageDocument.getElementById("confirmation-text").text() shouldBe cyPlusOneConfirmationMessages.text
      pageDocument.getElementById("confirm-button").text() shouldBe cyPlusOneConfirmationMessages.continueButton
      pageDocument.getElementById("cancel-button").text() shouldBe cyPlusOneConfirmationMessages.cancelButton
      pageDocument.getElementById("cancel-button").attr("href") shouldBe cancelButtonHref
    }

  }
}
