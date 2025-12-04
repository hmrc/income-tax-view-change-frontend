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

package views.optIn.newJourney

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.optIn.newJourney.SignUpStartView

class SignUpStartViewSpec extends TestSupport {

  val signUpStartView: SignUpStartView = app.injector.instanceOf[SignUpStartView]
  val startButtonUrl = "/some/signUp/url"

  class Setup(isAgent: Boolean = true, isCY: Boolean) {
    val pageDocument: Document = Jsoup.parse(contentAsString(signUpStartView(isAgent, isCY, startButtonUrl)))
  }

  object signUpStart {
    val title = "Signing up to Making Tax Digital for Income Tax - Manage your Self Assessment - GOV.UK"
    val heading = "Signing up to Making Tax Digital for Income Tax"
    val description = "This allows HMRC to give you a more precise forecast of how much tax you owe to help you budget more accurately."
    val inset = "If you voluntarily sign up, you will need software compatible with Making Tax Digital for Income Tax. There are both paid and free options for you or your agent to choose from."
    val reportingQuarterlyHeading ="Reporting quarterly"
    val reportingQuarterlyText = "Voluntarily signing up will mean you need to:"
    val bullet1 = "keep digital records of your sole trader and property income and expenses"
    val bullet2 = "submit an update every 3 months for each of these income sources"
    val bullet3 = "still file a tax return"
    val cyOnlyDesc = "If for this tax year you have already used software to submit income and expenses to HMRC, you will need to resubmit this information in your next quarterly update."
    val ifYouChangeYourMindHeading = "If you change your mind"
    val ifYouChangeYourMindText = "As you would be voluntarily signed up, you could decide at any time to opt out."
    val button = "Sign up"
  }

  "sign up start page" should {
    "have the correct title" in new Setup(false, isCY = false) {
      pageDocument.title() shouldBe signUpStart.title
    }

    "have the correct heading" in new Setup(false, isCY = false) {
      pageDocument.select("h1").text() shouldBe signUpStart.heading
    }

    "have the correct description" in new Setup(false, isCY = false) {
      pageDocument.getElementById("sign-up-start-description").text() shouldBe signUpStart.description
    }

    "have the correct inset text" in new Setup(false, isCY = false) {
      pageDocument.getElementById("sign-up-inset").text() shouldBe signUpStart.inset
    }

    "have the correct reporting quarterly heading and text" in new Setup(false, isCY = false) {
      pageDocument.getElementById("sign-up-reporting-quarterly-heading").text() shouldBe signUpStart.reportingQuarterlyHeading
      pageDocument.getElementById("sign-up-reporting-quarterly-text-1").text() shouldBe signUpStart.reportingQuarterlyText
      pageDocument.select("#sign-up-bullet").get(0).text() shouldBe signUpStart.bullet1
      pageDocument.select("#sign-up-bullet").get(1).text() shouldBe signUpStart.bullet2
      pageDocument.select("#sign-up-bullet").get(2).text() shouldBe signUpStart.bullet3
    }

    "have the correct CY only description" in new Setup(false, isCY = true) {
      pageDocument.getElementById("sign-up-reporting-quarterly-text-2").text() shouldBe signUpStart.cyOnlyDesc
    }

    "have the correct 'if you change your mind' heading and text" in new Setup(false, isCY = false) {
      pageDocument.getElementById("sign-up-if-you-change-your-mind-heading").text() shouldBe signUpStart.ifYouChangeYourMindHeading
      pageDocument.getElementById("sign-up-if-you-change-your-mind-text").text() shouldBe signUpStart.ifYouChangeYourMindText
    }

    "have the correct button text" in new Setup(false, isCY = false) {
      pageDocument.getElementById("sign-up-continue-button").text() shouldBe signUpStart.button
    }
  }
}
