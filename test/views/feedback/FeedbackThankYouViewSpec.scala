/*
 * Copyright 2022 HM Revenue & Customs
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

package views.feedback

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.i18n.{Lang, MessagesApi}
import play.twirl.api.Html
import testUtils.TestSupport
import views.html.feedback.FeedbackThankYou

class FeedbackThankYouViewSpec extends TestSupport {

  val feedbackThankYouView: FeedbackThankYou = app.injector.instanceOf[FeedbackThankYou]
  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val lang: Lang = Lang("GB")

  val testBackUrl: String = "/testBackUrl"

  class Setup(isAgent: Boolean = false) {
    val view: Html = feedbackThankYouView(backUrl = testBackUrl, isAgent = isAgent)
    val document: Document = Jsoup.parse(view.toString())
  }

  "The Feedback Thank You page" when {
    "an individual loads the page" should {
      "have the correct title" in new Setup() {
        document.title shouldBe msgs("titlePattern.serviceName.govUk", msgs("feedback.thankYou"))
      }

      "have the correct para informing that the feedback has been received" in new Setup() {
        document.select("p.govuk-body").text shouldBe msgs("feedback.received")
      }

      "have a back button" in new Setup {
        val backButton: Elements = document.select("a.govuk-button")
        backButton.text shouldBe msgs("base.back")
        backButton.attr("href") shouldBe testBackUrl
      }
    }

    "an agent loads the page" should {
      "have the correct title" in new Setup(isAgent = true) {
        document.title shouldBe msgs("agent.titlePattern.serviceName.govUk", msgs("feedback.thankYou"))
      }
    }

    }

}
