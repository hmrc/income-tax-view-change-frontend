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

package views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.SignUpOptOutCannotGoBackView

class SignUpOptOutCannotGoBackViewSpec extends TestSupport {

  object SignUpOptOutCannotGoBackViewMessages {
    val title: String = "You cannot go back - Manage your Self Assessment - GOV.UK"
    val heading: String = "You cannot go back"
    val signUpContextualText: String = "You have signed up to Making Tax Digital for Income Tax"
    val optOutContextualText: String = "You have opted out of Making Tax Digital for Income Tax"
    val ulHeading: String = "You can:"
    val upcomingDeadlinesLinkText: String = "view your upcoming deadlines"
    val homePageLinkText: String = "go back to the home page"
  }

  val view: SignUpOptOutCannotGoBackView = app.injector.instanceOf[SignUpOptOutCannotGoBackView]

  class Setup(isAgent: Boolean = false, isSignUp: Boolean) {
    val pageDocument: Document = Jsoup.parse(contentAsString(view(isAgent = isAgent, isSignUp = isSignUp)))
  }

  "SignUpOptOutCannotGoBackView" when {
    "Accessing through the sign-up journey" should {
      "Render the page correctly" in new Setup(isSignUp = true) {
        pageDocument.title shouldBe SignUpOptOutCannotGoBackViewMessages.title
        pageDocument.getElementById("page-heading").text() shouldBe SignUpOptOutCannotGoBackViewMessages.heading
        pageDocument.getElementById("contextual-section-text").text() shouldBe SignUpOptOutCannotGoBackViewMessages.signUpContextualText
        pageDocument.getElementById("links-section-heading").text() shouldBe SignUpOptOutCannotGoBackViewMessages.ulHeading
        pageDocument.getElementById("links-section-upcoming-deadlines-link").text() shouldBe SignUpOptOutCannotGoBackViewMessages.upcomingDeadlinesLinkText
        pageDocument.getElementById("links-section-home-page-link").text() shouldBe SignUpOptOutCannotGoBackViewMessages.homePageLinkText
      }
    }
    "Accessing through the opt-out journey" should {
      "Render the page correctly" in new Setup(isSignUp = false) {
        pageDocument.title shouldBe SignUpOptOutCannotGoBackViewMessages.title
        pageDocument.getElementById("page-heading").text() shouldBe SignUpOptOutCannotGoBackViewMessages.heading
        pageDocument.getElementById("contextual-section-text").text() shouldBe SignUpOptOutCannotGoBackViewMessages.optOutContextualText
        pageDocument.getElementById("links-section-heading").text() shouldBe SignUpOptOutCannotGoBackViewMessages.ulHeading
        pageDocument.getElementById("links-section-upcoming-deadlines-link").text() shouldBe SignUpOptOutCannotGoBackViewMessages.upcomingDeadlinesLinkText
        pageDocument.getElementById("links-section-home-page-link").text() shouldBe SignUpOptOutCannotGoBackViewMessages.homePageLinkText
      }
    }
  }
}
