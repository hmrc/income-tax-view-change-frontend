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
import models.incomeSourceDetails.TaxYear
import models.optout.OneYearOptOutCheckpointViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import services.optout.OneYearOptOutFollowedByAnnual
import testUtils.TestSupport
import views.html.optOut.ConfirmOptOut

class ConfirmOptOutViewWithQuarterlyUpdatesSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val confirmOptOutView: ConfirmOptOut = app.injector.instanceOf[ConfirmOptOut]

  class Setup(isAgent: Boolean = true, infoMessage: Boolean = false) {
    val pageDocument: Document =
      Jsoup.parse(contentAsString(
        confirmOptOutView(
          OneYearOptOutCheckpointViewModel(
            intent = TaxYear.forYearEnd(2022),
            state = Some(OneYearOptOutFollowedByAnnual),
            quarterlyUpdates = Some(4)
          ),
          isAgent = isAgent))
      )
  }

  object confirmOptOutMessages {
    val heading: String = "Confirm and opt out for the 2021 to 2022 tax year"
    val title: String = messages("htmlTitle", heading)
    val summary: String = messages("optout.confirmOptOut.desc")
    val infoMessage: String = "In future, you could be required to report quarterly again if, for example, your income increases or the threshold for reporting quarterly changes. If this happens, we’ll write to you to let you know."
    val confirmButton: String = messages("optout.confirmOptOut.confirm")
    val confirmedOptOutURL: String = controllers.optOut.routes.ConfirmOptOutController.submit(false).url
    val confirmedOptOutURLAgent: String = controllers.optOut.routes.ConfirmOptOutController.submit(true).url
    val cancelButton: String = messages("optout.confirmOptOut.cancel")
  }

  "Opt-out confirm page" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title() shouldBe confirmOptOutMessages.title
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe confirmOptOutMessages.heading
    }

    "have the correct summary heading and page contents" in new Setup(false) {
      pageDocument.getElementById("summary").text() shouldBe confirmOptOutMessages.summary
      pageDocument.getElementById("cancel-button").text() shouldBe confirmOptOutMessages.cancelButton
    }

    "have the correct summary heading and page contents for Agents" in new Setup(true) {
      pageDocument.getElementById("summary").text() shouldBe confirmOptOutMessages.summary
      pageDocument.getElementById("cancel-button").text() shouldBe confirmOptOutMessages.cancelButton
    }


    "have the correct summary heading and page contents with info message" in new Setup(isAgent = false, infoMessage = true) {
      pageDocument.getElementById("summary").text() shouldBe confirmOptOutMessages.summary
      pageDocument.getElementById("info-message").text() shouldBe confirmOptOutMessages.infoMessage
      pageDocument.getElementById("confirm-button").text() shouldBe confirmOptOutMessages.confirmButton
      pageDocument.getElementById("cancel-button").text() shouldBe confirmOptOutMessages.cancelButton
      pageDocument.getElementById("confirm-optout-form").attr("action") shouldBe confirmOptOutMessages.confirmedOptOutURL
    }

    "have the correct summary heading and page contents for Agents with info message" in new Setup(isAgent = true, infoMessage = true) {
      pageDocument.getElementById("summary").text() shouldBe confirmOptOutMessages.summary
      pageDocument.getElementById("info-message").text() shouldBe confirmOptOutMessages.infoMessage
      pageDocument.getElementById("confirm-button").text() shouldBe confirmOptOutMessages.confirmButton
      pageDocument.getElementById("cancel-button").text() shouldBe confirmOptOutMessages.cancelButton
      pageDocument.getElementById("confirm-optout-form").attr("action") shouldBe confirmOptOutMessages.confirmedOptOutURLAgent
    }

    "with quarterly updates as 4 count" in new Setup(false) {
      pageDocument.getElementById("warning-inset")
        .text().startsWith("You have 4 quarterly updates submitted") shouldBe true
    }
  }
}
