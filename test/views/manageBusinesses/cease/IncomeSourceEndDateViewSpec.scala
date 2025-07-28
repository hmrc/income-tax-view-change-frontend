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
import forms.manageBusinesses.cease.CeaseIncomeSourceEndDateFormProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.Html
import testConstants.BaseTestConstants.testSelfEmploymentId
import testUtils.TestSupport
import views.html.manageBusinesses.cease.IncomeSourceEndDate

import java.time.LocalDate

class IncomeSourceEndDateViewSpec extends TestSupport {

  val IncomeSourceEndDateView: IncomeSourceEndDate = app.injector.instanceOf[IncomeSourceEndDate]
  val incomeSourceEndDateForm: CeaseIncomeSourceEndDateFormProvider = app.injector.instanceOf[CeaseIncomeSourceEndDateFormProvider]
  val nextTaxYear: Int = dateService.getCurrentTaxYearEnd + 1

  val prefixSoleTrader: String = SelfEmployment.endDateMessagePrefix
  val prefixUKProperty: String = UkProperty.endDateMessagePrefix
  val prefixForeignProperty: String = ForeignProperty.endDateMessagePrefix

  val ceasePrefix: String = "incomeSources.cease"

  class Setup(isAgent: Boolean, error: Boolean = false, incomeSourceType: IncomeSourceType) {
    val testPostActionCall: Call = Call("GET", "/test/path")
    val testBackUrl: String = "/test/back/path"
    val view: Html = incomeSourceType match {
      case SelfEmployment =>
        val form: Form[LocalDate] = incomeSourceEndDateForm.apply(SelfEmployment, Some(testSelfEmploymentId), newIncomeSourceJourney = true)
        IncomeSourceEndDateView(SelfEmployment, form, testPostActionCall, isAgent, testBackUrl)
      case _ =>
        val form: Form[LocalDate] = incomeSourceEndDateForm.apply(incomeSourceType, None, newIncomeSourceJourney = true)
        IncomeSourceEndDateView(incomeSourceType, form, testPostActionCall, isAgent, testBackUrl)
    }

    val viewError: Html = incomeSourceType match {
      case SelfEmployment =>
        val form: Form[LocalDate] = incomeSourceEndDateForm.apply(SelfEmployment, Some(testSelfEmploymentId), newIncomeSourceJourney = true)
        val errorFormSE = form.withError(FormError("income-source-end-date", "dateForm.error.monthAndYear.required"))
        IncomeSourceEndDateView(SelfEmployment, errorFormSE, testPostActionCall, isAgent, testBackUrl)
      case _ =>
        val form: Form[LocalDate] = incomeSourceEndDateForm.apply(incomeSourceType, None, newIncomeSourceJourney = true)
        val errorForm = form.withError(FormError("income-source-end-date", "dateForm.error.monthAndYear.required"))
        IncomeSourceEndDateView(incomeSourceType, errorForm, testPostActionCall, isAgent, testBackUrl)
    }

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewError)) else Jsoup.parse(contentAsString(view))
  }

  "BusinessEndDateView - Individual" should {
    "render the heading - Self employment" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe "Date your self-employed business stopped"
      document.getElementById(s"$prefixSoleTrader-caption").text() shouldBe "This section is: Sole trader"
    }
    "render the heading - Foreign property" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe "Date your foreign property business stopped"
      document.getElementById(s"$prefixForeignProperty-caption").text() shouldBe "This section is: Foreign property"
    }
    "render the heading - Uk Property " in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe "Date your UK property business stopped"
      document.getElementById(s"$prefixUKProperty-caption").text() shouldBe "This section is: UK property"
    }
    "render the hint - Self Employment" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementById("income-source-end-date-hint-example").text() shouldBe "For example, 27 3 2020"
    }
    "render the hint - Uk Property" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementById("income-source-end-date-hint").text() shouldBe "This is the date you last received rental income or made an expense related to your UK property business."
      document.getElementById("income-source-end-date-hint-example").text() shouldBe "For example, 27 3 2020"
    }
    "render the hint - Foreign Property" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementById("income-source-end-date-hint").text() shouldBe "This is the date you last received rental income or made an expense related to your foreign property business."
      document.getElementById("income-source-end-date-hint-example").text() shouldBe "For example, 27 3 2020"
    }
    "render the date form" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementById("back-fallback").text() shouldBe "Back"
      document.getElementById("back-fallback").attr("href") shouldBe testBackUrl
    }
    "render the continue button" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementById("continue-button").text() shouldBe "Continue"
    }
    "render the error summary" in new Setup(isAgent = false, error = true, incomeSourceType = SelfEmployment) {
      document.getElementById("error-summary").text() shouldBe "There is a problem The date must include a month and a year"
    }
  }

  "BusinessEndDateView - Agent" should {
    "render the heading - Self employment" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe "Date your self-employed business stopped"
      document.getElementById(s"$prefixSoleTrader-caption").text() shouldBe "This section is: Sole trader"
    }
    "render the heading - Foreign property" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe "Date your foreign property business stopped"
      document.getElementById(s"$prefixForeignProperty-caption").text() shouldBe "This section is: Foreign property"
    }
    "render the heading - Uk Property " in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe "Date your UK property business stopped"
      document.getElementById(s"$prefixUKProperty-caption").text() shouldBe "This section is: UK property"
    }
    "render the hint - Self Employment" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementById("income-source-end-date-hint-example").text() shouldBe "For example, 27 3 2020"
    }
    "render the hint - Uk Property" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementById("income-source-end-date-hint").text() shouldBe "This is the date you last received rental income or made an expense related to your UK property business."
      document.getElementById("income-source-end-date-hint-example").text() shouldBe "For example, 27 3 2020"
    }
    "render the hint - Foreign Property" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementById("income-source-end-date-hint").text() shouldBe "This is the date you last received rental income or made an expense related to your foreign property business."
      document.getElementById("income-source-end-date-hint-example").text() shouldBe "For example, 27 3 2020"
    }
    "render the date form" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementById("back-fallback").text() shouldBe "Back"
      document.getElementById("back-fallback").attr("href") shouldBe testBackUrl
    }
    "render the continue button" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementById("continue-button").text() shouldBe "Continue"
    }
    "render the error summary" in new Setup(isAgent = true, error = true, incomeSourceType = SelfEmployment) {
      document.getElementById("error-summary").text() shouldBe "There is a problem The date must include a month and a year"
    }
  }

}