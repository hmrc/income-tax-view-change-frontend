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

package views.incomeSources.cease

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.cease.CeaseIncomeSourceEndDateFormProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import services.DateService
import testConstants.BaseTestConstants.{testMtditid, testNino, testSelfEmploymentId}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.ukPlusForeignPropertyWithSoleTraderIncomeSource
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.incomeSources.cease.IncomeSourceEndDate

import java.time.LocalDate

class IncomeSourceEndDateViewSpec extends TestSupport {

  val IncomeSourceEndDateView: IncomeSourceEndDate = app.injector.instanceOf[IncomeSourceEndDate]
  val incomeSourceEndDateForm: CeaseIncomeSourceEndDateFormProvider = app.injector.instanceOf[CeaseIncomeSourceEndDateFormProvider]

  val testUser: MtdItUser[_] = defaultMTDITUser(Some(Individual),
    ukPlusForeignPropertyWithSoleTraderIncomeSource, fakeRequestWithNinoAndOrigin("pta"))


  class Setup(isAgent: Boolean, error: Boolean = false, incomeSourceType: IncomeSourceType) {
    val mockDateService: DateService = app.injector.instanceOf[DateService]
    val testPostActionCall: Call = Call("GET", "/test/path")
    val testBackUrl: String = "/test/back/path"
    val view = incomeSourceType match {
      case SelfEmployment =>
        val form: Form[LocalDate] = incomeSourceEndDateForm.apply(SelfEmployment, Some(testSelfEmploymentId), false)
        IncomeSourceEndDateView(SelfEmployment, form, testPostActionCall, isAgent, testBackUrl)
      case _ =>
        val form: Form[LocalDate] = incomeSourceEndDateForm.apply(incomeSourceType, None, false)
        IncomeSourceEndDateView(incomeSourceType, form, testPostActionCall, isAgent, testBackUrl)
    }

    val viewError = incomeSourceType match {
      case SelfEmployment =>
        val form: Form[LocalDate] = incomeSourceEndDateForm.apply(SelfEmployment, Some(testSelfEmploymentId), false)
        val errorFormSE = form.withError(FormError("income-source-end-date", "dateForm.error.monthAndYear.required"))
        IncomeSourceEndDateView(SelfEmployment, errorFormSE, testPostActionCall, isAgent, testBackUrl)
      case _ =>
        val form: Form[LocalDate] = incomeSourceEndDateForm.apply(SelfEmployment, Some(testSelfEmploymentId), false)
        val errorFormSE = form.withError(FormError("income-source-end-date", "dateForm.error.monthAndYear.required"))
        IncomeSourceEndDateView(SelfEmployment, errorFormSE, testPostActionCall, isAgent, testBackUrl)
    }


    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewError)) else Jsoup.parse(contentAsString(view))
  }

  "BusinessEndDateView - Individual" should {
    "render the heading - Self employment" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.cease.endDate.selfEmployment.heading")
    }
    "render the heading - Foreign property" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.cease.endDate.foreignProperty.heading")
    }
    "render the heading - Uk Property " in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.cease.endDate.ukProperty.heading")
    }
    "render the hint" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementById("income-source-end-date-hint-example").text() shouldBe messages("dateForm.hint")
    }
    "render the date form" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe testBackUrl
    }
    "render the continue button" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error summary" in new Setup(isAgent = false, error = true, incomeSourceType = SelfEmployment) {
      document.getElementById("error-summary").text() shouldBe messages("base.error_summary.heading") + " " + messages("dateForm.error.monthAndYear.required")
    }
  }

  "BusinessEndDateView - Agent" should {
    "render the heading - Self employment" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.cease.endDate.selfEmployment.heading")
    }
    "render the heading - Foreign property" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.cease.endDate.foreignProperty.heading")
    }
    "render the heading - Uk Property " in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.cease.endDate.ukProperty.heading")
    }
    "render the hint" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementById("income-source-end-date-hint-example").text() shouldBe messages("dateForm.hint")
    }
    "render the date form" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe testBackUrl
    }
    "render the continue button" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error summary" in new Setup(isAgent = true, error = true, incomeSourceType = SelfEmployment) {
      document.getElementById("error-summary").text() shouldBe messages("base.error_summary.heading") + " " + messages("dateForm.error.monthAndYear.required")
    }
  }

}
