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
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.incomeSources.add.ReportingMethodSetBackError

class ReportingMethodSetBackErrorViewSpec extends TestSupport{

  val errorView: ReportingMethodSetBackError = app.injector.instanceOf[ReportingMethodSetBackError]

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType) extends TestSupport {

    lazy val view: HtmlFormat.Appendable = errorView(isAgent, incomeSourceType)
    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  "ReportingMethodSetBackError - Individual" should {
    "render self employment error page" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementById("title").text() shouldBe messages("cannot-go-back.heading")
      document.getElementById("heading").text() shouldBe messages("cannot-go-back.sole-trader")
      document.getElementById("manage-link").text() shouldBe messages("cannot-go-back.manage-link")
      document.getElementById("home-link").text() shouldBe messages("cannot-go-back.home-link")
      "not render the back button" in {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
    "render UK property - error page" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementById("title").text() shouldBe messages("cannot-go-back.heading")
      document.getElementById("heading").text() shouldBe messages("cannot-go-back.uk-property")
      document.getElementById("manage-link").text() shouldBe messages("cannot-go-back.manage-link")
      document.getElementById("home-link").text() shouldBe messages("cannot-go-back.home-link")
      "not render the back button" in {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
    "render Foreign property - error page" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementById("title").text() shouldBe messages("cannot-go-back.heading")
      document.getElementById("heading").text() shouldBe messages("cannot-go-back.foreign-property")
      document.getElementById("manage-link").text() shouldBe messages("cannot-go-back.manage-link")
      document.getElementById("home-link").text() shouldBe messages("cannot-go-back.home-link")
      "not render the back button" in {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
  }

  "ReportingMethodSetBackError - Agent" should {
    "render self employment error page" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementById("title").text() shouldBe messages("cannot-go-back.heading")
      document.getElementById("heading").text() shouldBe messages("cannot-go-back.sole-trader")
      document.getElementById("manage-link").text() shouldBe messages("cannot-go-back.manage-link")
      document.getElementById("home-link").text() shouldBe messages("cannot-go-back.home-link")
      "not render the back button" in {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
    "render UK property - error page" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementById("title").text() shouldBe messages("cannot-go-back.heading")
      document.getElementById("heading").text() shouldBe messages("cannot-go-back.uk-property")
      document.getElementById("manage-link").text() shouldBe messages("cannot-go-back.manage-link")
      document.getElementById("home-link").text() shouldBe messages("cannot-go-back.home-link")
      "not render the back button" in {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
    "render Foreign property - error page" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementById("title").text() shouldBe messages("cannot-go-back.heading")
      document.getElementById("heading").text() shouldBe messages("cannot-go-back.foreign-property")
      document.getElementById("manage-link").text() shouldBe messages("cannot-go-back.manage-link")
      document.getElementById("home-link").text() shouldBe messages("cannot-go-back.home-link")
      "not render the back button" in {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
  }


}
