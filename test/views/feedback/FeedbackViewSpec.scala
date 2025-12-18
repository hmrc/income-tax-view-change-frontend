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

package views.feedback

import forms.FeedbackForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.i18n.{Lang, MessagesApi}
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.feedback.FeedbackView

class FeedbackViewSpec extends ViewSpec {

  val feedbackView: FeedbackView = app.injector.instanceOf[FeedbackView]
  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val lang: Lang = Lang("GB")

  class TestSetup(isAgent: Boolean = false) {
    val view: Html = feedbackView(FeedbackForm.form, testCall, isAgent)
    val document: Document = Jsoup.parse(view.toString())
  }

  "The Feedback page" when {
    "an individual loads the page" should {
      "have the correct title" in new TestSetup() {
        document.title shouldBe msgs("htmlTitle", msgs("feedback.heading"))
      }

      "have the correct heading" in new TestSetup() {
        document.select("h1").text shouldBe msgs("feedback.heading")
      }

      "have the correct description about the feedback page" in new TestSetup() {
        document.select("p.govuk-body").text shouldBe msgs("feedback.description")
      }

      "have a set of radio buttons" in new TestSetup() {
        val radios: Elements = document.select("fieldset")
        def radioButton(nthchild: Int): String = radios.select(s".govuk-radios__item:nth-child($nthchild)").text

        radios.select("fieldset legend").text shouldBe msgs("feedback.radiosDescription")
        radioButton(1) shouldBe msgs("feedback.veryGood")
        radioButton(2) shouldBe msgs("feedback.good")
        radioButton(3) shouldBe msgs("feedback.neutral")
        radioButton(4) shouldBe msgs("feedback.bad")
        radioButton(5) shouldBe msgs("feedback.veryBad")
      }

      "have an input for a Full name" in new TestSetup() {
        document.select(".govuk-form-group:nth-of-type(2) .govuk-label").text shouldBe msgs("feedback.fullName")
      }

      "have an input for an Email" in new TestSetup() {
        document.select(".govuk-form-group:nth-of-type(3) .govuk-label").text shouldBe msgs("feedback.email")
      }

      "have an input for Comments" in new TestSetup() {
        document.select(".govuk-character-count label").text shouldBe msgs("feedback.comments")
        document.select("#feedback-comments-hint").text shouldBe msgs("feedback.comments.hint")
      }

      "have a send button" in new TestSetup() {
        document.select("button.govuk-button").text.contains(msgs("feedback.send")) shouldBe true
      }
    }

    "an agent loads the page" should {
      "have the correct title" in new TestSetup(isAgent = true) {
        document.title shouldBe msgs("htmlTitle.agent", msgs("feedback.heading"))
      }
    }
  }

}
