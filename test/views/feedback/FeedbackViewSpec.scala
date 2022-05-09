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
import views.html.feedback.Feedback

class FeedbackViewSpec extends TestSupport {

  val feedbackView: Feedback = app.injector.instanceOf[Feedback]
  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val lang: Lang = Lang("GB")

  class Setup(isAgent: Boolean = false) {
    val view: Html = feedbackView(isAgent = isAgent)
    val document: Document = Jsoup.parse(view.toString())
  }

  "The Feedback page" when {
    "an individual loads the page" should {
      "have the correct title" in new Setup() {
        document.title shouldBe msgs("titlePattern.serviceName.govUk", msgs("feedback.heading"))
      }

      "have the correct heading" in new Setup() {
        document.select("h1").text shouldBe msgs("feedback.heading")
      }

      "have the correct description about the feedback page" in new Setup() {
        document.select("p.govuk-body").text shouldBe msgs("feedback.description")
      }

      "have a set of radio buttons" in new Setup() {
        val radios: Elements = document.select("fieldset")
        def radioButton(nthchild: Int): String = radios.select(s".govuk-radios__item:nth-child($nthchild)").text

        radios.select("fieldset legend").text shouldBe msgs("feedback.radiosDescription")
        radioButton(1) shouldBe msgs("feedback.veryGood")
        radioButton(2) shouldBe msgs("feedback.good")
        radioButton(3) shouldBe msgs("feedback.neutral")
        radioButton(4) shouldBe msgs("feedback.bad")
        radioButton(5) shouldBe msgs("feedback.veryBad")
      }

      "have an input for a Full name" in new Setup() {
        document.select(".govuk-form-group:nth-of-type(2) .govuk-label").text shouldBe msgs("feedback.fullName")
      }

      "have an input for an Email" in new Setup() {
        document.select(".govuk-form-group:nth-of-type(3) .govuk-label").text shouldBe msgs("feedback.email")
      }

      "have an input for Comments" in new Setup() {
        document.select(".govuk-character-count label").text shouldBe msgs("feedback.comments")
        document.select("#comments-hint").text shouldBe msgs("feedback.comments.hint")
      }

      "have a send button" in new Setup() {
        document.select("button.govuk-button").text shouldBe msgs("feedback.send")
      }
    }

    "an agent loads the page" should {
      "have the correct title" in new Setup(isAgent = true) {
        document.title shouldBe msgs("agent.titlePattern.serviceName.govUk", msgs("feedback.heading"))
      }
    }
  }

}
