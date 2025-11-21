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
import testUtils.ViewSpec
import views.html.manageBusinesses.add.IncomeSourceReportingMethodNotSaved

class IncomeSourceReportingMethodNotSavedViewSpec extends ViewSpec {
  val incomeSourceReportingMethodNotSaved: IncomeSourceReportingMethodNotSaved = app.injector.instanceOf[IncomeSourceReportingMethodNotSaved]

  val getManageBusinessUrl: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
  val getManageBusinessAgentUrl: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url

  def getMessage(incomeSourceType: IncomeSourceType, key:String): String = {
    incomeSourceType match {
      case SelfEmployment => messages(s"incomeSources.add.error.reportingMethodNotSaved.se.$key")
      case UkProperty => messages(s"incomeSources.add.error.reportingMethodNotSaved.uk.$key")
      case ForeignProperty =>messages(s"incomeSources.add.error.reportingMethodNotSaved.fp.$key")
    }
  }

  class TestSetup(isAgent: Boolean, incomeSourceType: IncomeSourceType) {
    val id = "testId"

    val selfEmploymentText: String = messages("incomeSources.add.error.reportingMethodNotSaved.se.incomeSource")
    val ukPropertyText: String = messages("incomeSources.add.error.reportingMethodNotSaved.uk.incomeSource")
    val foreignPropertyText: String = messages("incomeSources.add.error.reportingMethodNotSaved.fp.incomeSource")


    val action: Call = incomeSourceType match {
      case UkProperty =>
        val controller = controllers.manageBusinesses.add.routes.IncomeSourceAddedController
        if (isAgent) controller.show(UkProperty) else controller.showAgent(UkProperty)
      case ForeignProperty =>
        val controller = controllers.manageBusinesses.add.routes.IncomeSourceAddedController
        if (isAgent) controller.show(ForeignProperty) else controller.showAgent(ForeignProperty)
      case SelfEmployment =>
        val controller = controllers.manageBusinesses.add.routes.IncomeSourceAddedController
        if (isAgent) controller.show(SelfEmployment) else controller.showAgent(SelfEmployment)
    }
    lazy val view: HtmlFormat.Appendable = incomeSourceReportingMethodNotSaved(isAgent = isAgent, incomeSourceType = incomeSourceType, continueAction = action)
    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  "IncomeSourceReportingMethodNotSave - Individual" should {
    "render self employment - error page" in new TestSetup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-caption-xl").text().contains (getMessage(SelfEmployment, "caption"))
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p1", selfEmploymentText)
      document.getElementById("paragraph-2").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p2")
      document.getElementById("paragraph-3").text() shouldBe "You can change this at any time in the your businesses section."
      document.getElementById("your-businesses-link").attr("href") shouldBe getManageBusinessUrl
      document.getElementById("continue-button").attr("href") shouldBe action.url
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render UK property - error page" in new TestSetup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-caption-xl").text().contains (getMessage(UkProperty, "caption"))
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p1", ukPropertyText)
      document.getElementById("paragraph-2").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p2")
      document.getElementById("paragraph-3").text() shouldBe "You can change this at any time in the your businesses section."
      document.getElementById("your-businesses-link").attr("href") shouldBe getManageBusinessUrl
      document.getElementById("continue-button").attr("href") shouldBe action.url
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render Foreign property - error page" in new TestSetup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-caption-xl").text().contains (getMessage(ForeignProperty, "caption"))
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p1", foreignPropertyText)
      document.getElementById("paragraph-2").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p2")
      document.getElementById("paragraph-3").text() shouldBe "You can change this at any time in the your businesses section."
      document.getElementById("your-businesses-link").attr("href") shouldBe getManageBusinessUrl
      document.getElementById("continue-button").attr("href") shouldBe action.url
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
  }
  "IncomeSourceReportingMethodNotSave - Agent" should {
    "render self employment - error page" in new TestSetup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-caption-xl").text().contains (getMessage(SelfEmployment, "caption"))
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p1", selfEmploymentText)
      document.getElementById("paragraph-2").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p2")
      document.getElementById("paragraph-3").text() shouldBe "You can change this at any time in the your businesses section."
      document.getElementById("your-businesses-link").attr("href") shouldBe getManageBusinessAgentUrl
      document.getElementById("continue-button").attr("href") shouldBe action.url
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render UK property - error page" in new TestSetup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-caption-xl").text().contains (getMessage(UkProperty, "caption"))
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p1", ukPropertyText)
      document.getElementById("paragraph-2").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p2")
      document.getElementById("paragraph-3").text() shouldBe "You can change this at any time in the your businesses section."
      document.getElementById("your-businesses-link").attr("href") shouldBe getManageBusinessAgentUrl
      document.getElementById("continue-button").attr("href") shouldBe action.url
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render Foreign property - error page" in new TestSetup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-caption-xl").text().contains (getMessage(ForeignProperty, "caption"))
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p1", foreignPropertyText)
      document.getElementById("paragraph-2").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p2")
      document.getElementById("paragraph-3").text() shouldBe "You can change this at any time in the your businesses section."
      document.getElementById("your-businesses-link").attr("href") shouldBe getManageBusinessAgentUrl
      document.getElementById("continue-button").attr("href") shouldBe action.url
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
  }
}
