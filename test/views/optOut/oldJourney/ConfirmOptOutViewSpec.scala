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

package views.optOut.oldJourney

import config.FrontendAppConfig
import models.incomeSourceDetails.TaxYear
import models.optout.OneYearOptOutCheckpointViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import services.optout.OneYearOptOutFollowedByAnnual
import testUtils.TestSupport
import views.html.optOut.oldJourney.ConfirmOptOut
import views.messages.ConfirmOptOutMessages

class ConfirmOptOutViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val confirmOptOutView: ConfirmOptOut = app.injector.instanceOf[ConfirmOptOut]

  val expectedQuarterlyUpdates = 4

  val confirmedOptOutUrl: Boolean => String = (isAgent: Boolean) => controllers.optOut.oldJourney.routes.ConfirmOptOutController.submit(isAgent).url
  val cancelLinkUrl: String = controllers.optOut.oldJourney.routes.OptOutCancelledController.show().url

  class Setup(isAgent: Boolean = true, infoMessage: Boolean = false, quarterlyUpdates: Int = 0) {

    val pageDocument: Document = {
      Jsoup.parse(
        contentAsString(
          confirmOptOutView(
            viewModel =
              OneYearOptOutCheckpointViewModel(
                intent = TaxYear.forYearEnd(2022),
                state = Some(OneYearOptOutFollowedByAnnual),
                quarterlyUpdates = Some(quarterlyUpdates)
              ),
            isAgent = isAgent,
            cancelURL = cancelLinkUrl
          )
        )
      )
    }
  }

  "Opt-out confirm view" when {

    "infoMessage == false, not show the info message when " should {

      "have the correct title" in new Setup(false) {
        pageDocument.title() shouldBe ConfirmOptOutMessages.title
      }

      "have the correct heading" in new Setup(false) {
        pageDocument.select("h1").text() shouldBe ConfirmOptOutMessages.heading
      }

      "have the correct summary heading and page contents" in new Setup(false) {
        pageDocument.getElementById("summary").text() shouldBe ConfirmOptOutMessages.summary
        pageDocument.getElementById("cancel-button").text() shouldBe ConfirmOptOutMessages.cancelLink
      }

      "have the correct summary heading and page contents for Agents" in new Setup(true) {
        pageDocument.getElementById("summary").text() shouldBe ConfirmOptOutMessages.summary
        pageDocument.getElementById("cancel-button").text() shouldBe ConfirmOptOutMessages.cancelLink
      }

      "with quarterly updates as zero count" in new Setup(false) {
        pageDocument.select("#warning-inset").size() shouldBe 0
      }

      "with quarterly updates as 4 count" in new Setup(isAgent = false, quarterlyUpdates = expectedQuarterlyUpdates) {
        pageDocument.getElementById("warning-inset")
          .text().startsWith("You have 4 quarterly updates submitted") shouldBe true
      }
    }

    "infoMessage == true, show the info message" should {

      "have the correct summary heading and page contents with info message" in new Setup(isAgent = false, infoMessage = true) {
        pageDocument.getElementById("summary").text() shouldBe ConfirmOptOutMessages.summary
        pageDocument.getElementById("info-message").text() shouldBe ConfirmOptOutMessages.infoMessage
        pageDocument.getElementById("confirm-button").text() shouldBe ConfirmOptOutMessages.confirmButton
        pageDocument.getElementById("cancel-button").text() shouldBe ConfirmOptOutMessages.cancelLink
        pageDocument.getElementById("cancel-button").attr("href") shouldBe cancelLinkUrl
        pageDocument.getElementById("confirm-optout-form").attr("action") shouldBe confirmedOptOutUrl(false)
      }

      "have the correct summary heading and page contents for Agents with info message" in new Setup(isAgent = true, infoMessage = true) {
        pageDocument.getElementById("summary").text() shouldBe ConfirmOptOutMessages.summary
        pageDocument.getElementById("info-message").text() shouldBe ConfirmOptOutMessages.infoMessage
        pageDocument.getElementById("confirm-button").text() shouldBe ConfirmOptOutMessages.confirmButton
        pageDocument.getElementById("cancel-button").text() shouldBe ConfirmOptOutMessages.cancelLink
        pageDocument.getElementById("cancel-button").attr("href") shouldBe cancelLinkUrl
        pageDocument.getElementById("confirm-optout-form").attr("action") shouldBe confirmedOptOutUrl(true)
      }
    }
  }
}
