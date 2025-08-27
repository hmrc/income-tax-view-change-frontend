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
import forms.manageBusinesses.add.IncomeSourceReportingMethodForm
import models.incomeSourceDetails.LatencyYear
import models.incomeSourceDetails.viewmodels.IncomeSourceReportingMethodViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.data.Form
import play.api.mvc.Call
import play.test.Helpers.contentAsString
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.manageBusinesses.add.IncomeSourceReportingMethod

class IncomeSourceReportingMethodViewSpec extends TestSupport {

  val IncomeSourceReportingMethodView: IncomeSourceReportingMethod = app.injector.instanceOf[IncomeSourceReportingMethod]

  def getMessage(incomeSourceType: IncomeSourceType, key:String): String = {
    incomeSourceType match {
      case SelfEmployment => messages(s"incomeSources.add.incomeSourceReportingMethod.se.$key")
      case UkProperty => messages(s"incomeSources.add.incomeSourceReportingMethod.uk.$key")
      case ForeignProperty =>messages(s"incomeSources.add.incomeSourceReportingMethod.fp.$key")
    }
  }

  class Setup(isAgent: Boolean, error: Boolean = false, incomeSourceType: IncomeSourceType) {
    val taxYear1 = "tax_year_1_reporting_method_tax_year"
    val taxYear2 = "tax_year_2_reporting_method_tax_year"
    val taxYearReporting1 = "new_tax_year_1_reporting_method"
    val taxYearReporting2 = "new_tax_year_2_reporting_method"
    val radioMustBeSelectedMessageKey = "incomeSources.add.incomeSourceReportingMethod.error"

    val form: Form[_] = IncomeSourceReportingMethodForm.form

    val formWithErrorScenario1: Form[_] = IncomeSourceReportingMethodForm.form
      .withError(taxYearReporting1, radioMustBeSelectedMessageKey, "2021", "2022")
      .withError(taxYearReporting2, radioMustBeSelectedMessageKey, "2022", "2023")

    val formWithErrorScenario2: Form[_] = IncomeSourceReportingMethodForm.form
      .withError(taxYearReporting2, radioMustBeSelectedMessageKey, "2022", "2023")

    val latencyYear1: LatencyYear = LatencyYear("2022", "A")
    val latencyYear2: LatencyYear = LatencyYear("2023", "Q")
    val viewModelTwoLatencyYears: IncomeSourceReportingMethodViewModel = IncomeSourceReportingMethodViewModel(Some(latencyYear1), Some(latencyYear2))
    val viewModelOneLatencyYear: IncomeSourceReportingMethodViewModel = IncomeSourceReportingMethodViewModel(None, Some(latencyYear2))

    //TODO: Defaulting to false but need to add tests for isChange true
    val postAction: Call = controllers.manageBusinesses.add.routes.IncomeSourceReportingFrequencyController.submit(isAgent, false, incomeSourceType)

    lazy val viewTwoLatencyYears: HtmlFormat.Appendable = IncomeSourceReportingMethodView(incomeSourceType ,form, viewModelTwoLatencyYears, postAction, isAgent)
    lazy val viewOneLatencyYear: HtmlFormat.Appendable = IncomeSourceReportingMethodView(incomeSourceType, form, viewModelOneLatencyYear, postAction, isAgent)
    lazy val viewTwoLatencyYearsWithError: HtmlFormat.Appendable = IncomeSourceReportingMethodView(incomeSourceType, formWithErrorScenario1, viewModelTwoLatencyYears, postAction, isAgent)
    lazy val viewOneLatencyYearWithError: HtmlFormat.Appendable = IncomeSourceReportingMethodView(incomeSourceType, formWithErrorScenario2, viewModelOneLatencyYear, postAction, isAgent)

    lazy val documentWithTwoLatencyYears: Document = if (error) Jsoup.parse(contentAsString(viewTwoLatencyYearsWithError)) else Jsoup.parse(contentAsString(viewTwoLatencyYears))
    lazy val documentWithOneLatencyYear: Document = if (error) Jsoup.parse(contentAsString(viewOneLatencyYearWithError)) else Jsoup.parse(contentAsString(viewOneLatencyYear))

    def checkHeading(): Assertion = {
      documentWithTwoLatencyYears.getElementsByClass("govuk-caption-l").text().contains(messages(getMessage(incomeSourceType, "caption")))
      documentWithTwoLatencyYears.getElementsByClass("govuk-heading-xl").text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.heading")
      documentWithTwoLatencyYears.getElementsByClass("govuk-heading-m").text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseReport")
    }

    def checkContent(): Assertion = {
      val doc = documentWithTwoLatencyYears.getElementById("main-content")
      doc.getElementsByTag("p").get(1).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.description1", viewModelTwoLatencyYears.latencyYear2.get.taxYear)
      doc.getElementsByTag("p").get(2).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.description2")
      doc.getElementsByTag("p").get(3).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.description3")
      doc.getElementsByTag("ul").get(0).text() shouldBe Jsoup.parse(messages("incomeSources.add.incomeSourceReportingMethod.description4.bullet1") + " " +
        messages("incomeSources.add.incomeSourceReportingMethod.description4.bullet2") + " " + messages("incomeSources.add.incomeSourceReportingMethod.description4.bullet3")).text()
    }

    def checkNoBackButton(): Assertion = {
      documentWithTwoLatencyYears.getElementById("back") shouldBe null
    }

    def checkContinueButton(): Assertion = {
      documentWithTwoLatencyYears.getElementById("continue-button").text() shouldBe messages("base.continue")
    }

    def checkBothTaxYears(): Assertion = {
      val doc = documentWithTwoLatencyYears.getElementById("add-uk-property-reporting-method-form")
      doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.taxYear", (viewModelTwoLatencyYears.latencyYear1.get.taxYear.toInt - 1).toString, viewModelTwoLatencyYears.latencyYear1.get.taxYear)
      doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.taxYear", (viewModelTwoLatencyYears.latencyYear2.get.taxYear.toInt - 1).toString, viewModelTwoLatencyYears.latencyYear2.get.taxYear)
      doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseAnnualReport")
      doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseQuarterlyReport")
      doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseAnnualReport")
      doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseQuarterlyReport")
      doc.getElementsByClass("govuk-radios").size() shouldBe 2
    }

    def checkSecondTaxYear(): Assertion = {
      val doc = documentWithOneLatencyYear.getElementById("add-uk-property-reporting-method-form")
      doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.taxYear", (viewModelOneLatencyYear.latencyYear2.get.taxYear.toInt - 1).toString, viewModelTwoLatencyYears.latencyYear2.get.taxYear)
      doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseAnnualReport")
      doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseQuarterlyReport")
      doc.getElementsByClass("govuk-radios").size() shouldBe 1
    }

    def checkInputErrorBothTaxYears(): Assertion = {
      val doc = documentWithOneLatencyYear.getElementById("add-uk-property-reporting-method-form")
      doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.taxYear", (viewModelTwoLatencyYears.latencyYear2.get.taxYear.toInt - 1).toString, viewModelTwoLatencyYears.latencyYear2.get.taxYear)
      doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseAnnualReport")
      doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseQuarterlyReport")
      doc.getElementsByClass("govuk-radios").size() shouldBe 1
      doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.incomeSourceReportingMethod.error", (viewModelTwoLatencyYears.latencyYear2.get.taxYear.toInt - 1).toString, viewModelTwoLatencyYears.latencyYear2.get.taxYear)
      documentWithOneLatencyYear.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      documentWithOneLatencyYear.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
        messages("incomeSources.add.incomeSourceReportingMethod.error", (viewModelOneLatencyYear.latencyYear2.get.taxYear.toInt - 1).toString, viewModelOneLatencyYear.latencyYear2.get.taxYear)
      }
    }

    def checkInputErrorSecondTaxYear(): Assertion = {
      val doc = documentWithTwoLatencyYears.getElementById("add-uk-property-reporting-method-form")
      doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.taxYear", (viewModelTwoLatencyYears.latencyYear1.get.taxYear.toInt - 1).toString, viewModelTwoLatencyYears.latencyYear1.get.taxYear)
      doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.taxYear", (viewModelTwoLatencyYears.latencyYear2.get.taxYear.toInt - 1).toString, viewModelTwoLatencyYears.latencyYear2.get.taxYear)
      doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseAnnualReport")
      doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseQuarterlyReport")
      doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseAnnualReport")
      doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.incomeSourceReportingMethod.chooseQuarterlyReport")
      doc.getElementsByClass("govuk-radios").size() shouldBe 2
      doc.getElementById("new_tax_year_1_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.incomeSourceReportingMethod.error", (viewModelTwoLatencyYears.latencyYear1.get.taxYear.toInt - 1).toString, viewModelTwoLatencyYears.latencyYear1.get.taxYear)
      doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.incomeSourceReportingMethod.error", (viewModelTwoLatencyYears.latencyYear2.get.taxYear.toInt - 1).toString, viewModelTwoLatencyYears.latencyYear2.get.taxYear)
      documentWithTwoLatencyYears.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      documentWithTwoLatencyYears.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
        messages("incomeSources.add.incomeSourceReportingMethod.error", (viewModelTwoLatencyYears.latencyYear1.get.taxYear.toInt - 1).toString, viewModelTwoLatencyYears.latencyYear1.get.taxYear) + " " +
          messages("incomeSources.add.incomeSourceReportingMethod.error", (viewModelTwoLatencyYears.latencyYear2.get.taxYear.toInt - 1).toString, viewModelTwoLatencyYears.latencyYear2.get.taxYear)
      }
    }
  }

  "IncomeSourceReportingMethodView" should {
    "render the heading" when {
      "UK Property - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = UkProperty) {
        checkHeading()
      }
      "UK Property - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = UkProperty) {
        checkHeading()
      }
      "Foreign Property - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
        checkHeading()
      }
      "Foreign Property - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
        checkHeading()
      }
      "Self Employment - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = SelfEmployment) {
        checkHeading()
      }
      "Self Employment - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
        checkHeading()
      }
    }

    "render text content" when {
      "UK Property - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = UkProperty) {
        checkContent()
      }
      "UK Property - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = UkProperty) {
        checkContent()
      }
      "Foreign Property - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
        checkContent()
      }
      "Foreign Property - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
        checkContent()
      }
      "Self Employment - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = SelfEmployment) {
        checkContent()
      }
      "Self Employment - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
        checkContent()
      }
    }

    "not have a back button" when {
      "UK Property - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = UkProperty) {
        checkNoBackButton()
      }
      "UK Property - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = UkProperty) {
        checkNoBackButton()
      }
      "Foreign Property - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
        checkNoBackButton()
      }
      "Foreign Property - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
        checkNoBackButton()
      }
      "Self Employment - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = SelfEmployment) {
        checkNoBackButton()
      }
      "Self Employment - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
        checkNoBackButton()
      }
    }

    "render a continue button" when {
      "UK Property - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = UkProperty) {
        checkContinueButton()
      }
      "UK Property - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = UkProperty) {
        checkContinueButton()
      }
      "Foreign Property - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
        checkContinueButton()
      }
      "Foreign Property - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
        checkContinueButton()
      }
      "Self Employment - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = SelfEmployment) {
        checkContinueButton()
      }
      "Self Employment - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
        checkContinueButton()
      }
    }

    "render both tax years when present in latency details" when {
      "UK Property - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = UkProperty) {
        checkBothTaxYears()
      }
      "UK Property - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = UkProperty) {
        checkBothTaxYears()
      }
      "Foreign Property - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
        checkBothTaxYears()
      }
      "Foreign Property - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
        checkBothTaxYears()
      }
      "Self Employment - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = SelfEmployment) {
        checkBothTaxYears()
      }
      "Self Employment - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
        checkBothTaxYears()
      }
    }

    "render second tax year when first year is crystallised" when {
      "UK Property - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = UkProperty) {
        checkSecondTaxYear()
      }
      "UK Property - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = UkProperty) {
        checkSecondTaxYear()
      }
      "Foreign Property - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
        checkSecondTaxYear()
      }
      "Foreign Property - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
        checkSecondTaxYear()
      }
      "Self Employment - Individual" in new Setup(isAgent = false, error = false, incomeSourceType = SelfEmployment) {
        checkSecondTaxYear()
      }
      "Self Employment - Agent" in new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
        checkSecondTaxYear()
      }
    }

    "render input error on form error - both tax years when present in latency details" when {
      "UK Property - Individual" in new Setup(isAgent = false, error = true, incomeSourceType = UkProperty) {
        checkInputErrorBothTaxYears()
      }
      "UK Property - Agent" in new Setup(isAgent = true, error = true, incomeSourceType = UkProperty) {
        checkInputErrorBothTaxYears()
      }
      "Foreign Property - Individual" in new Setup(isAgent = false, error = true, incomeSourceType = ForeignProperty) {
        checkInputErrorBothTaxYears()
      }
      "Foreign Property - Agent" in new Setup(isAgent = true, error = true, incomeSourceType = ForeignProperty) {
        checkInputErrorBothTaxYears()
      }
      "Self Employment - Individual" in new Setup(isAgent = false, error = true, incomeSourceType = SelfEmployment) {
        checkInputErrorBothTaxYears()
      }
      "Self Employment - Agent" in new Setup(isAgent = true, error = true, incomeSourceType = SelfEmployment) {
        checkInputErrorBothTaxYears()
      }
    }

    "render input error on form error - second tax year" when {
      "UK Property - Individual" in new Setup(isAgent = false, error = true, incomeSourceType = UkProperty) {
        checkInputErrorSecondTaxYear()
      }
      "UK Property - Agent" in new Setup(isAgent = true, error = true, incomeSourceType = UkProperty) {
        checkInputErrorSecondTaxYear()
      }
      "Foreign Property - Individual" in new Setup(isAgent = false, error = true, incomeSourceType = ForeignProperty) {
        checkInputErrorSecondTaxYear()
      }
      "Foreign Property - Agent" in new Setup(isAgent = true, error = true, incomeSourceType = ForeignProperty) {
        checkInputErrorSecondTaxYear()
      }
      "Self Employment - Individual" in new Setup(isAgent = false, error = true, incomeSourceType = SelfEmployment) {
        checkInputErrorSecondTaxYear()
      }
      "Self Employment - Agent" in new Setup(isAgent = true, error = true, incomeSourceType = SelfEmployment) {
        checkInputErrorSecondTaxYear()
      }
    }
  }
}
