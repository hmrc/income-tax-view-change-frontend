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
import testUtils.{TestSupport, ViewSpec}
import views.html.incomeSources.add.IncomeSourceReportingMethodNotSaved

class IncomeSourceReportingMethodNotSavedViewSpec extends ViewSpec {
  val incomeSourceReportingMethodNotSaved: IncomeSourceReportingMethodNotSaved = app.injector.instanceOf[IncomeSourceReportingMethodNotSaved]

  class TestSetup(isAgent: Boolean, incomeSourceType: IncomeSourceType) extends TestSupport {
    val id = "testId"

    val selfEmploymentText: String = messages("incomeSources.add.error.reportingMethodNotSaved.se")
    val foreignPropertyText: String = messages("incomeSources.add.error.reportingMethodNotSaved.fp")
    val ukPropertyText: String = messages("incomeSources.add.error.reportingMethodNotSaved.uk")

    val action: Call = incomeSourceType match {
      case UkProperty =>
        val controller = controllers.incomeSources.add.routes.UKPropertyAddedController
        if (isAgent) controller.show(id) else controller.showAgent(id)
      case ForeignProperty =>
        val controller = controllers.incomeSources.add.routes.ForeignPropertyAddedController
        if (isAgent) controller.show(id) else controller.showAgent(id)
      case SelfEmployment =>
        val controller = controllers.incomeSources.add.routes.BusinessAddedObligationsController
        if (isAgent) controller.show(id) else controller.showAgent(id)
    }
    lazy val view: HtmlFormat.Appendable = incomeSourceReportingMethodNotSaved(isAgent = isAgent, incomeSourceType = incomeSourceType, continueAction = action)
    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  "IncomeSourceReportingMethodNotSave - Individual" should {
    "render self employment - error page" in new TestSetup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p1", selfEmploymentText)
      document.getElementById("paragraph-2").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p2")
      document.getElementById("error-reporting-method-not-saved-form").attr("action") shouldBe action.url
      document.getElementById("error-reporting-method-not-saved-form").attr("method") shouldBe action.method
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render UK property - error page" in new TestSetup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p1", ukPropertyText)
      document.getElementById("paragraph-2").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p2")
      document.getElementById("error-reporting-method-not-saved-form").attr("action") shouldBe action.url
      document.getElementById("error-reporting-method-not-saved-form").attr("method") shouldBe action.method
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render Foreign property - error page" in new TestSetup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p1", foreignPropertyText)
      document.getElementById("paragraph-2").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p2")
      document.getElementById("error-reporting-method-not-saved-form").attr("action") shouldBe action.url
      document.getElementById("error-reporting-method-not-saved-form").attr("method") shouldBe action.method
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
  }
  "IncomeSourceReportingMethodNotSave - Agent" should {
    "render self employment - error page" in new TestSetup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p1", selfEmploymentText)
      document.getElementById("paragraph-2").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p2")
      document.getElementById("error-reporting-method-not-saved-form").attr("action") shouldBe action.url
      document.getElementById("error-reporting-method-not-saved-form").attr("method") shouldBe action.method
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render UK property - error page" in new TestSetup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p1", ukPropertyText)
      document.getElementById("paragraph-2").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p2")
      document.getElementById("error-reporting-method-not-saved-form").attr("action") shouldBe action.url
      document.getElementById("error-reporting-method-not-saved-form").attr("method") shouldBe action.method
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render Foreign property - error page" in new TestSetup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p1", foreignPropertyText)
      document.getElementById("paragraph-2").text() shouldBe messages("incomeSources.add.error.reportingMethodNotSaved.p2")
      document.getElementById("error-reporting-method-not-saved-form").attr("action") shouldBe action.url
      document.getElementById("error-reporting-method-not-saved-form").attr("method") shouldBe action.method
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
  }
}
