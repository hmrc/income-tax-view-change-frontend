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

package views.optOut

import config.FrontendAppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optOut.ConfirmedOptOut

class ConfirmedOptOutViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val confirmedOptOutView: ConfirmedOptOut = app.injector.instanceOf[ConfirmedOptOut]

  class Setup(isAgent: Boolean = true) {
    val pageDocument: Document = Jsoup.parse(contentAsString(confirmedOptOutView(isAgent)))
  }

  object confirmOptOutMessages {
    val heading: String = messages("optout.confirmedOptOut.heading")
    val title: String = messages("htmlTitle", heading)
    val submitTax: String = messages("optout.confirmedOptOut.submitTax")
    val nextUpdatesDue: String = messages("optout.confirmedOptOut.updatesDue")
    val reportQuarterly: String = messages("optout.confirmedOptOut.reportQuarterly")
  }

  "Opt-out confirmed page" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title() shouldBe confirmOptOutMessages.title
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe confirmOptOutMessages.heading
    }

    "have the correct summary heading and page contents" in new Setup(false) {
      pageDocument.getElementById("submitTax").text() shouldBe confirmOptOutMessages.submitTax
      pageDocument.getElementById("updatesDue").text() shouldBe confirmOptOutMessages.nextUpdatesDue
      pageDocument.getElementById("reportQuarterly").text() shouldBe confirmOptOutMessages.reportQuarterly
    }

    "have the correct summary heading and page contents for Agents" in new Setup(true) {
      pageDocument.getElementById("submitTax").text() shouldBe confirmOptOutMessages.submitTax
      pageDocument.getElementById("updatesDue").text() shouldBe confirmOptOutMessages.nextUpdatesDue
      pageDocument.getElementById("reportQuarterly").text() shouldBe confirmOptOutMessages.reportQuarterly
    }

  }
}
