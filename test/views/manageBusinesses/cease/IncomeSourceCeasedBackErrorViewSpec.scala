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

package views.manageBusinesses.cease

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.manageBusinesses.cease.IncomeSourceCeasedBackErrorView

class IncomeSourceCeasedBackErrorViewSpec extends TestSupport{

  val errorView: IncomeSourceCeasedBackErrorView = app.injector.instanceOf[IncomeSourceCeasedBackErrorView]

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType) {

    lazy val view: HtmlFormat.Appendable = errorView(isAgent, incomeSourceType)
    lazy val document: Document = Jsoup.parse(contentAsString(view))

    val manageLink: String =
      if (isAgent) "/report-quarterly/income-and-expenses/view/agents/manage-your-businesses"
      else "/report-quarterly/income-and-expenses/view/manage-your-businesses"
  }

  "IncomeSourceCeasedBackError - Individual" should {
    "render self employment error page" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("heading").text() shouldBe messages("cannotGoBack.sole-trader-ceased")
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
    "render UK property - error page" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("heading").text() shouldBe messages("cannotGoBack.uk-property-ceased")
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
    "render Foreign property - error page" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("heading").text() shouldBe messages("cannotGoBack.foreign-property-ceased")
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
  }

  "IncomeSourceCeasedBackError - Agent" should {
    "render self employment error page" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("heading").text() shouldBe messages("cannotGoBack.sole-trader-ceased")
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
    "render UK property - error page" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("heading").text() shouldBe messages("cannotGoBack.uk-property-ceased")
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
    "render Foreign property - error page" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("heading").text() shouldBe messages("cannotGoBack.foreign-property-ceased")
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
  }
}