/*
 * Copyright 2025 HM Revenue & Customs
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

package views.reportingObligations.signUp

import models.incomeSourceDetails.TaxYear
import models.reportingObligations.signUp.{SignUpCompletedViewModel, YouMustWaitToSignUpViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.reportingObligations.signUp.{YouMustWaitToSignUpView, SignUpCompletedView}

class YouMustWaitToSignUpViewSpec extends TestSupport {

  val view: YouMustWaitToSignUpView = app.injector.instanceOf[YouMustWaitToSignUpView]

  class Setup(isAgent: Boolean, testDate: TaxYear = TaxYear(2024,2025)) {
    val model: YouMustWaitToSignUpViewModel = YouMustWaitToSignUpViewModel(testDate)

    val pageDocument: Document = Jsoup.parse(contentAsString(view(model, isAgent)))
  }

  object SignUpCompletedViewMessages {
    def individualHeading(endYear: String = "2025"): String = s"You can access this service from 6 April $endYear"
    def individualTextOne(endYear: String = "2025"): String = s"This is because you are signed up to use Making Tax Digital for Income Tax from the next tax year, which starts 6 April $endYear."
    val individualTextTwo: String = "Find more about Making Tax Digital for Income Tax (opens in new tab)."

    def agentHeading(endYear: String = "2025"): String = s"You can view this client from 6 April $endYear"
    def agentText(endYear: String = "2025"): String = s"This is because your client is signed up to use Making Tax digital for Income Tax from the next tax year, which starts 6 April $endYear."
    val agentButtonText: String = "Enter another UTR"
  }

  "YouMustWaitToSignUpView" when {
    "user is an agent" should {
      "don't render the back link" in new Setup(isAgent = true) {
        pageDocument.getElementsByClass("govuk-back-link").text() shouldBe ""
      }
      "render the agent heading" in new Setup(isAgent = true) {
        pageDocument.select("h1").text() shouldBe SignUpCompletedViewMessages.agentHeading()
      }
      "render the agent text" in new Setup(isAgent = true) {
        pageDocument.getElementById("you-must-wait-agent-text").text() shouldBe SignUpCompletedViewMessages.agentText()
      }
      "render the agent button text" in new Setup(isAgent = true) {
        pageDocument.getElementById("you-must-wait-agent-button").text() shouldBe SignUpCompletedViewMessages.agentButtonText
      }
      "render agent heading and text with alternative year" in new Setup(isAgent = true, testDate = TaxYear(2026,2027)) {
        pageDocument.select("h1").text() shouldBe SignUpCompletedViewMessages.agentHeading("2027")
        pageDocument.getElementById("you-must-wait-agent-text").text() shouldBe SignUpCompletedViewMessages.agentText("2027")
      }
    }

    "user is an individual" should {
      "render the back link" in new Setup(isAgent = false) {
        pageDocument.getElementsByClass("govuk-back-link").text() shouldBe "Back"
      }
      "render the individual heading" in new Setup(isAgent = false) {
        pageDocument.select("h1").text() shouldBe SignUpCompletedViewMessages.individualHeading()
      }
      "render the individual text 1" in new Setup(isAgent = false) {
        pageDocument.getElementById("you-must-wait-text").text() shouldBe SignUpCompletedViewMessages.individualTextOne()
      }
      "render the individual text 2 (text + link text)" in new Setup(isAgent = false) {
        pageDocument.getElementById("you-must-wait-text-2").text() shouldBe SignUpCompletedViewMessages.individualTextTwo
      }
      "render individual heading and text with alternative year" in new Setup(isAgent = false, testDate = TaxYear(2022,2023)) {
        pageDocument.select("h1").text() shouldBe SignUpCompletedViewMessages.individualHeading("2023")
        pageDocument.getElementById("you-must-wait-text").text() shouldBe SignUpCompletedViewMessages.individualTextOne("2023")
      }
    }

  }
}
