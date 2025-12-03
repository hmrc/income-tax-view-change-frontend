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

package views.manageBusinesses.add

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.manageBusinesses.add.IncomeSourceAddedBackErrorView

class IncomeSourceAddedBackErrorViewSpec extends TestSupport{

  val errorView: IncomeSourceAddedBackErrorView = app.injector.instanceOf[IncomeSourceAddedBackErrorView]

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType) {

    lazy val postCall: Call = if (isAgent) controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.submitAgent(incomeSourceType)
    else controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.submit(incomeSourceType)
    lazy val view: HtmlFormat.Appendable = errorView(isAgent, incomeSourceType, postCall)
    lazy val document: Document = Jsoup.parse(contentAsString(view))

    val manageLink: String =
      if (isAgent) "/report-quarterly/income-and-expenses/view/agents/manage-your-businesses"
      else "/report-quarterly/income-and-expenses/view/manage-your-businesses"
  }

  "ReportingMethodSetBackError - Individual" should {
    "render self employment error page" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("warning-message").text() shouldBe s"! Warning ${messages("cannotGoBack.warningMessage")}"
      document.getElementById("manage-message").text() shouldBe
        s"${messages("cannotGoBack.soleTraderAdded")}. ${messages("cannotGoBack.hasBeenAdded1")} ${messages("cannotGoBack.manageLink")} ${messages("cannotGoBack.hasBeenAdded2")}"
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("choose-message").text() shouldBe messages("cannotGoBack.needToChoose")
      Option(document.getElementById("back")).isDefined shouldBe false
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render UK property - error page" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("warning-message").text() shouldBe s"! Warning ${messages("cannotGoBack.warningMessage")}"
      document.getElementById("manage-message").text() shouldBe
        s"${messages("cannotGoBack.ukPropertyAdded")}. ${messages("cannotGoBack.hasBeenAdded1")} ${messages("cannotGoBack.manageLink")} ${messages("cannotGoBack.hasBeenAdded2")}"
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("choose-message").text() shouldBe messages("cannotGoBack.needToChoose")
      Option(document.getElementById("back")).isDefined shouldBe false
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render Foreign property - error page" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("warning-message").text() shouldBe s"! Warning ${messages("cannotGoBack.warningMessage")}"
      document.getElementById("manage-message").text() shouldBe
        s"${messages("cannotGoBack.foreignPropertyAdded")}. ${messages("cannotGoBack.hasBeenAdded1")} ${messages("cannotGoBack.manageLink")} ${messages("cannotGoBack.hasBeenAdded2")}"
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("choose-message").text() shouldBe messages("cannotGoBack.needToChoose")
      Option(document.getElementById("back")).isDefined shouldBe false
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
  }

  "ReportingMethodSetBackError - Agent" should {
    "render self employment error page" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("warning-message").text() shouldBe s"! Warning ${messages("cannotGoBack.warningMessage")}"
      document.getElementById("manage-message").text() shouldBe
        s"${messages("cannotGoBack.soleTraderAdded")}. ${messages("cannotGoBack.hasBeenAdded1")} ${messages("cannotGoBack.manageLink")} ${messages("cannotGoBack.hasBeenAdded2")}"
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("choose-message").text() shouldBe messages("cannotGoBack.needToChoose")
      Option(document.getElementById("back")).isDefined shouldBe false
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render UK property - error page" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("warning-message").text() shouldBe s"! Warning ${messages("cannotGoBack.warningMessage")}"
      document.getElementById("manage-message").text() shouldBe
        s"${messages("cannotGoBack.ukPropertyAdded")}. ${messages("cannotGoBack.hasBeenAdded1")} ${messages("cannotGoBack.manageLink")} ${messages("cannotGoBack.hasBeenAdded2")}"
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("choose-message").text() shouldBe messages("cannotGoBack.needToChoose")
      Option(document.getElementById("back")).isDefined shouldBe false
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render Foreign property - error page" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("warning-message").text() shouldBe s"! Warning ${messages("cannotGoBack.warningMessage")}"
      document.getElementById("manage-message").text() shouldBe
        s"${messages("cannotGoBack.foreignPropertyAdded")}. ${messages("cannotGoBack.hasBeenAdded1")} ${messages("cannotGoBack.manageLink")} ${messages("cannotGoBack.hasBeenAdded2")}"
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("choose-message").text() shouldBe messages("cannotGoBack.needToChoose")
      Option(document.getElementById("back")).isDefined shouldBe false
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
  }
}
