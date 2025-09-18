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

package views.optOut.newJourney

import config.FrontendAppConfig
import models.incomeSourceDetails.TaxYear
import models.optout.newJourney.CheckOptOutUpdateAnswersViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optOut.newJourney.CheckOptOutUpdateAnswers
import views.messages.{CheckOptOutUpdateAnswersMessages => viewMessages}

class CheckOptOutUpdateAnswersViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val checkOptOutUpdateAnswersView: CheckOptOutUpdateAnswers = app.injector.instanceOf[CheckOptOutUpdateAnswers]

  val confirmOptOutUpdateSubmitURL: (Boolean, String) => String = (isAgent: Boolean, taxYear: String) => controllers.optOut.newJourney.routes.ConfirmOptOutUpdateController.submit(isAgent, taxYear).url
  val cancelLinkUrl: String = controllers.optOut.oldJourney.routes.OptOutCancelledController.show().url

  class Setup(isAgent: Boolean = true) {

    val pageDocument: Document = {
      Jsoup.parse(
        contentAsString(
          checkOptOutUpdateAnswersView(
            viewModel = CheckOptOutUpdateAnswersViewModel(TaxYear(2025, 2026), 2),
            isAgent = isAgent,
            cancelURL = cancelLinkUrl
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
        pageDocument.getElementById("cancel-button").attr("href") shouldBe cancelLinkUrl

      }

      "user is not agent" in new Setup(isAgent = false) {
        pageDocument.title() shouldBe viewMessages.title

        pageDocument.getElementById("check-opt-out-update-heading").text() shouldBe viewMessages.heading

        pageDocument.getElementById("check-opt-out-update-p1").text() shouldBe viewMessages.p1

        pageDocument.getElementById("check-opt-out-update-inset").text() shouldBe viewMessages.inset

        pageDocument.getElementById("confirm-button").text() shouldBe viewMessages.confirmButton
        pageDocument.getElementById("confirm-button").attr("href") shouldBe ""

        pageDocument.getElementById("cancel-button").text() shouldBe viewMessages.cancelButton
        pageDocument.getElementById("cancel-button").attr("href") shouldBe cancelLinkUrl

      }
    }
  }
}
