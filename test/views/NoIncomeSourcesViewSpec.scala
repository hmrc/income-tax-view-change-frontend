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

package views

import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.NoIncomeSourcesView

class NoIncomeSourcesViewSpec extends ViewSpec {

  lazy val view: NoIncomeSourcesView = app.injector.instanceOf[NoIncomeSourcesView]

  val testUrl = "/contact-hmrc"

  def render(isAgent: Boolean = false): Html =
    view(isAgent, testUrl)

  "No Income Sources page" should {

    "have the correct title" in new Setup(render()) {
      document.title() shouldBe messages("htmlTitle", messages("noIncomeSources.error.title"))
    }

    "have the correct heading" in new Setup(render()) {
      document hasPageHeading messages("noIncomeSources.error.title")
    }

    "display the main description" in new Setup(render()) {
      document.getElementById("no-income-sources-description").text() shouldBe
        messages("noIncomeSources.error.p1")
    }

    "display the contact heading" in new Setup(render()) {
      document.getElementById("no-income-sources-contact-heading").text() shouldBe
        messages("noIncomeSources.error.contact.heading")
    }

    "display the steps list" in new Setup(render()) {
      document.getElementById("numbered-element-0").text() shouldBe messages("noIncomeSources.error.step1")
      document.getElementById("numbered-element-1").text() shouldBe messages("noIncomeSources.error.step2")
      document.getElementById("numbered-element-2").text() shouldBe messages("noIncomeSources.error.step3")
    }

    "display the additional info text" in new Setup(render()) {
      document.getElementById("no-income-sources-updates-info").text() shouldBe
        messages("noIncomeSources.error.p2")
    }

    "have the contact button with correct link" in new Setup(render()) {
      val button = document.getElementById("no-income-sources-contact-button")
      button.text() shouldBe messages("noIncomeSources.error.button")
      button.attr("href") shouldBe testUrl
    }
  }
}