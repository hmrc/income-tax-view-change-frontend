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

package views.incomeSources.add

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.incomeSources.add.IncomeSourceAddedBackError

class IncomeSourceAddedBackErrorViewSpec extends TestSupport{

  val errorView: IncomeSourceAddedBackError = app.injector.instanceOf[IncomeSourceAddedBackError]

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType) extends TestSupport {

    lazy val postCall: Call = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.submitAgent(incomeSourceType)
    else controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.submit(incomeSourceType)
    lazy val view: HtmlFormat.Appendable = errorView(isAgent, incomeSourceType, postCall)
    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  "ReportingMethodSetBackError - Individual" should {
    "render self employment error page" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementById("title").text() shouldBe messages("cannot-go-back.heading")
      document.getElementById("warning-message").text() shouldBe s"! Warning ${messages("cannot-go-back.warning-message")}"
      document.getElementById("manage-message").text() shouldBe
        s"${messages("cannot-go-back.sole-trader")}. ${messages("cannot-go-back.has-been-added-1")} ${messages("cannot-go-back.manage-link")} ${messages("cannot-go-back.has-been-added-2")}"
      document.getElementById("manage-link").text() shouldBe messages("cannot-go-back.manage-link")
      document.getElementById("choose-message").text() shouldBe messages("cannot-go-back.need-to-choose")
      "not render the back button" in {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
    "render UK property - error page" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementById("title").text() shouldBe messages("cannot-go-back.heading")
      document.getElementById("warning-message").text() shouldBe s"! Warning ${messages("cannot-go-back.warning-message")}"
      document.getElementById("manage-message").text() shouldBe
        s"${messages("cannot-go-back.uk-property")}. ${messages("cannot-go-back.has-been-added-1")} ${messages("cannot-go-back.manage-link")} ${messages("cannot-go-back.has-been-added-2")}"
      document.getElementById("manage-link").text() shouldBe messages("cannot-go-back.manage-link")
      document.getElementById("choose-message").text() shouldBe messages("cannot-go-back.need-to-choose")
      "not render the back button" in {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
    "render Foreign property - error page" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementById("title").text() shouldBe messages("cannot-go-back.heading")
      document.getElementById("warning-message").text() shouldBe s"! Warning ${messages("cannot-go-back.warning-message")}"
      document.getElementById("manage-message").text() shouldBe
        s"${messages("cannot-go-back.foreign-property")}. ${messages("cannot-go-back.has-been-added-1")} ${messages("cannot-go-back.manage-link")} ${messages("cannot-go-back.has-been-added-2")}"
      document.getElementById("manage-link").text() shouldBe messages("cannot-go-back.manage-link")
      document.getElementById("choose-message").text() shouldBe messages("cannot-go-back.need-to-choose")
      "not render the back button" in {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
  }

  "ReportingMethodSetBackError - Agent" should {
    "render self employment error page" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementById("title").text() shouldBe messages("cannot-go-back.heading")
      document.getElementById("warning-message").text() shouldBe s"! Warning ${messages("cannot-go-back.warning-message")}"
      document.getElementById("manage-message").text() shouldBe
        s"${messages("cannot-go-back.sole-trader")}. ${messages("cannot-go-back.has-been-added-1")} ${messages("cannot-go-back.manage-link")} ${messages("cannot-go-back.has-been-added-2")}"
      document.getElementById("manage-link").text() shouldBe messages("cannot-go-back.manage-link")
      document.getElementById("choose-message").text() shouldBe messages("cannot-go-back.need-to-choose")
      "not render the back button" in {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
    "render UK property - error page" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementById("title").text() shouldBe messages("cannot-go-back.heading")
      document.getElementById("warning-message").text() shouldBe s"! Warning ${messages("cannot-go-back.warning-message")}"
      document.getElementById("manage-message").text() shouldBe
        s"${messages("cannot-go-back.uk-property")}. ${messages("cannot-go-back.has-been-added-1")} ${messages("cannot-go-back.manage-link")} ${messages("cannot-go-back.has-been-added-2")}"
      document.getElementById("manage-link").text() shouldBe messages("cannot-go-back.manage-link")
      document.getElementById("choose-message").text() shouldBe messages("cannot-go-back.need-to-choose")
      "not render the back button" in {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
    "render Foreign property - error page" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementById("title").text() shouldBe messages("cannot-go-back.heading")
      document.getElementById("warning-message").text() shouldBe s"! Warning ${messages("cannot-go-back.warning-message")}"
      document.getElementById("manage-message").text() shouldBe
        s"${messages("cannot-go-back.foreign-property")}. ${messages("cannot-go-back.has-been-added-1")} ${messages("cannot-go-back.manage-link")} ${messages("cannot-go-back.has-been-added-2")}"
      document.getElementById("manage-link").text() shouldBe messages("cannot-go-back.manage-link")
      document.getElementById("choose-message").text() shouldBe messages("cannot-go-back.need-to-choose")
      "not render the back button" in {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
  }
}
