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
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.manageBusinesses.add.IncomeSourceNotAddedError

class IncomeSourceNotAddedErrorViewSpec extends TestSupport {

  val incomeSourceNotAddedErrorView: IncomeSourceNotAddedError = app.injector.instanceOf[IncomeSourceNotAddedError]
  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType) {

    val continueAction = if(isAgent) controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent() else
      controllers.manageBusinesses.routes.ManageYourBusinessesController.show()

    lazy val view: HtmlFormat.Appendable = incomeSourceNotAddedErrorView(isAgent = isAgent, incomeSourceType = incomeSourceType, continueAction = continueAction)
    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  "IncomeSourceNotAddedError - Individual" should {
    "render self employment error page" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.incomeSourceNotSaved.p1", "sole trader")
      document.getElementById("error-income-source-not-saved-form").attr("action") shouldBe continueAction.url
      document.getElementById("continue-button").text() shouldBe messages("incomeSources.add.error.incomeSourceNotSaved.incomeSources")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
    "render UK property - error page" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.incomeSourceNotSaved.p1", "UK property")
      document.getElementById("error-income-source-not-saved-form").attr("action") shouldBe continueAction.url
      document.getElementById("continue-button").text() shouldBe messages("incomeSources.add.error.incomeSourceNotSaved.incomeSources")
      Option(document.getElementById("back")).isDefined shouldBe false
    }

    "render Foreign property - error page" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.incomeSourceNotSaved.p1", "foreign property")
      document.getElementById("error-income-source-not-saved-form").attr("action") shouldBe continueAction.url
      document.getElementById("continue-button").text() shouldBe messages("incomeSources.add.error.incomeSourceNotSaved.incomeSources")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
  }
  "IncomeSourceNotAddedError - Agent" should {
    "render self employment error page" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.incomeSourceNotSaved.p1", "sole trader")
      document.getElementById("error-income-source-not-saved-form").attr("action") shouldBe continueAction.url
      document.getElementById("continue-button").text() shouldBe messages("incomeSources.add.error.incomeSourceNotSaved.incomeSources")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
    "render UK property error page" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.incomeSourceNotSaved.p1", "UK property")
      document.getElementById("error-income-source-not-saved-form").attr("action") shouldBe continueAction.url
      document.getElementById("continue-button").text() shouldBe messages("incomeSources.add.error.incomeSourceNotSaved.incomeSources")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
    "render Foreign property error page" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementsByTag("h1").first().text() shouldBe messages("incomeSources.add.error.standardError")
      document.getElementById("paragraph-1").text() shouldBe messages("incomeSources.add.error.incomeSourceNotSaved.p1", "foreign property")
      document.getElementById("error-income-source-not-saved-form").attr("action") shouldBe continueAction.url
      document.getElementById("continue-button").text() shouldBe messages("incomeSources.add.error.incomeSourceNotSaved.incomeSources")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
  }
}