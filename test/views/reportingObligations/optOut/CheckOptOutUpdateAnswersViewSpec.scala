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

package views.reportingObligations.optOut

import config.FrontendAppConfig
import models.incomeSourceDetails.TaxYear
import models.reportingObligations.optOut.CheckOptOutUpdateAnswersViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.*
import testUtils.TestSupport
import views.html.reportingObligations.optOut.CheckOptOutUpdateAnswersView
import views.messages.CheckOptOutUpdateAnswersMessages as viewMessages

class CheckOptOutUpdateAnswersViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val checkOptOutUpdateAnswersView: CheckOptOutUpdateAnswersView = app.injector.instanceOf[CheckOptOutUpdateAnswersView]

  val confirmOptOutUpdateSubmitURL: (Boolean, String) => String = (isAgent: Boolean, taxYear: String) => controllers.reportingObligations.optOut.routes.ConfirmOptOutUpdateController.submit(isAgent, taxYear).url

  class Setup(isAgent: Boolean = true) {
    val reportingObligationsURL: String = controllers.reportingObligations.routes.ReportingFrequencyPageController.show(isAgent).url

    val pageDocument: Document = {
      Jsoup.parse(
        contentAsString(
          checkOptOutUpdateAnswersView(
            viewModel = CheckOptOutUpdateAnswersViewModel(TaxYear(2025, 2026), 2),
            isAgent = isAgent,
            reportingObligationsURL = reportingObligationsURL
          )
        )
      )
    }
  }

  "check opt-out update answers view" when {

    "render the page" should {

      "user is agent" in new Setup(isAgent = true) {
        pageDocument.title() shouldBe viewMessages.title

        pageDocument.getElementById("check-opt-out-update-heading").text() shouldBe viewMessages.heading

        pageDocument.getElementById("check-opt-out-update-p1").text() shouldBe viewMessages.p1

        pageDocument.getElementById("check-opt-out-update-inset").text() shouldBe viewMessages.inset

        pageDocument.getElementById("confirm-button").text() shouldBe viewMessages.confirmButton
        pageDocument.getElementById("confirm-button").attr("href") shouldBe ""

        pageDocument.getElementById("cancel-button").text() shouldBe viewMessages.cancelButton
        pageDocument.getElementById("cancel-button").attr("href") shouldBe reportingObligationsURL

      }

      "user is not agent" in new Setup(isAgent = false) {
        pageDocument.title() shouldBe viewMessages.title

        pageDocument.getElementById("check-opt-out-update-heading").text() shouldBe viewMessages.heading

        pageDocument.getElementById("check-opt-out-update-p1").text() shouldBe viewMessages.p1

        pageDocument.getElementById("check-opt-out-update-inset").text() shouldBe viewMessages.inset

        pageDocument.getElementById("confirm-button").text() shouldBe viewMessages.confirmButton
        pageDocument.getElementById("confirm-button").attr("href") shouldBe ""

        pageDocument.getElementById("cancel-button").text() shouldBe viewMessages.cancelButton
        pageDocument.getElementById("cancel-button").attr("href") shouldBe reportingObligationsURL

      }
    }

    "render the page with different quarterly updates counts for opt-out scenarios followed by mandated" should {

      "display page with 0 quarterly updates" in {
        val reportingObligationsURL: String =
          controllers.reportingObligations.routes.ReportingFrequencyPageController.show(isAgent = false).url

        val pageDocument: Document = Jsoup.parse(
          contentAsString(
            checkOptOutUpdateAnswersView(
              viewModel = CheckOptOutUpdateAnswersViewModel(TaxYear(2023, 2024), 0),
              isAgent = false,
              reportingObligationsURL = reportingObligationsURL
            )
          )
        )

        pageDocument.getElementById("check-opt-out-update-inset") shouldBe null
      }

      "display 2 quarterly updates" in {
        val reportingObligationsURL: String = controllers.reportingObligations.routes.ReportingFrequencyPageController.show(isAgent = false).url
        val pageDocument: Document = Jsoup.parse(
          contentAsString(
            checkOptOutUpdateAnswersView(
              viewModel = CheckOptOutUpdateAnswersViewModel(TaxYear(2023, 2024), 2),
              isAgent = false,
              reportingObligationsURL = reportingObligationsURL
            )
          )
        )

        val insetText = pageDocument.getElementById("check-opt-out-update-inset").text()
        insetText should include("2")
        insetText should include("quarterly updates")
        insetText should include("If you continue, these updates will be deleted")
      }

      "display 3 quarterly updates correctly for V-M-M scenario" in {
        val reportingObligationsURL: String = controllers.reportingObligations.routes.ReportingFrequencyPageController.show(isAgent = false).url
        val pageDocument: Document = Jsoup.parse(
          contentAsString(
            checkOptOutUpdateAnswersView(
              viewModel = CheckOptOutUpdateAnswersViewModel(TaxYear(2023, 2024), 3),
              isAgent = false,
              reportingObligationsURL = reportingObligationsURL
            )
          )
        )

        val insetText = pageDocument.getElementById("check-opt-out-update-inset").text()
        insetText should include("3")
        insetText should include("quarterly updates")
        insetText should include("If you continue, these updates will be deleted")
      }
    }
  }
}
