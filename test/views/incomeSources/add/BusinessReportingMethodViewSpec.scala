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


import forms.incomeSources.add.AddBusinessReportingMethodForm
import models.incomeSourceDetails.viewmodels.BusinessReportingMethodViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.Call
import play.test.Helpers.contentAsString
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.incomeSources.add.BusinessReportingMethod

class BusinessReportingMethodViewSpec extends TestSupport {

  val businessReportingMethodView: BusinessReportingMethod = app.injector.instanceOf[BusinessReportingMethod]

  class Setup(isAgent: Boolean, error: Boolean = false) {
    val taxYear1 = "tax_year_1_reporting_method_tax_year"
    val taxYear2 = "tax_year_2_reporting_method_tax_year"
    val taxYearReporting1 = "new_tax_year_1_reporting_method"
    val taxYearReporting2 = "new_tax_year_2_reporting_method"
    val radioMustBeSelectedMessageKey = "incomeSources.add.businessReportingMethod.error"

    val form: Form[_] = AddBusinessReportingMethodForm.form

    val formWithErrorScenario1: Form[_] = AddBusinessReportingMethodForm.form
      .withError(taxYearReporting1, radioMustBeSelectedMessageKey, "2021", "2022")
      .withError(taxYearReporting2, radioMustBeSelectedMessageKey, "2022", "2023")

    val formWithErrorScenario2: Form[_] = AddBusinessReportingMethodForm.form
      .withError(taxYearReporting2, radioMustBeSelectedMessageKey, "2022", "2023")

    val viewModelScenario1 = BusinessReportingMethodViewModel(Some("2022"), Some("A"), Some("2023"), Some("A"))
    val viewModelScenario2 = BusinessReportingMethodViewModel(None, None, Some("2023"))

    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.BusinessReportingMethodController.submitAgent("") else
      controllers.incomeSources.add.routes.BusinessReportingMethodController.submit("")


    lazy val viewScenario1: HtmlFormat.Appendable = businessReportingMethodView(form, viewModelScenario1, postAction, isAgent)
    lazy val viewScenario2: HtmlFormat.Appendable = businessReportingMethodView(form, viewModelScenario2, postAction, isAgent)
    lazy val viewScenario1Error: HtmlFormat.Appendable = businessReportingMethodView(formWithErrorScenario1, viewModelScenario1, postAction, isAgent)
    lazy val viewScenario2Error: HtmlFormat.Appendable = businessReportingMethodView(formWithErrorScenario2, viewModelScenario2, postAction, isAgent)

    lazy val documentScenario1: Document = if (error) Jsoup.parse(contentAsString(viewScenario1Error)) else Jsoup.parse(contentAsString(viewScenario1))
    lazy val documentScenario2: Document = if (error) Jsoup.parse(contentAsString(viewScenario2Error)) else Jsoup.parse(contentAsString(viewScenario2))

  }

  "BusinessReportingMethodView - Individual" when {
    "with static content" should {
      "render the heading" in new Setup(false) {
        documentScenario1.getElementsByClass("govuk-heading-xl").text() shouldBe messages("incomeSources.add.businessReportingMethod.heading")
        documentScenario1.getElementsByClass("govuk-heading-m").text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseReport")
      }
      "render the content" in new Setup(false) {
        val doc = documentScenario1.getElementById("main-content")
        doc.getElementsByTag("p").get(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.description1", viewModelScenario1.taxYear2.get)
        doc.getElementsByTag("p").get(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.description2")
        doc.getElementsByTag("p").get(2).text() shouldBe messages("incomeSources.add.businessReportingMethod.description3")
        doc.getElementsByTag("ul").get(0).text() shouldBe Jsoup.parse(messages("incomeSources.add.businessReportingMethod.description4")).text()
      }

    }
    "no back url" in new Setup(true) {
      documentScenario1.getElementById("back") shouldBe null
    }
    "render the continue button" in new Setup(false) {
      documentScenario1.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "taxYear1 and taxYear2 are present in Latency" should {
      "render the radio form" in new Setup(false) {
        val doc = documentScenario1.getElementById("add-business-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.taxYear", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
        doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 2
      }

      "render the radio form with input error" in new Setup(false, error = true) {
        val doc = documentScenario1.getElementById("add-business-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.taxYear", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
        doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 2
        doc.getElementById("new_tax_year_1_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.businessReportingMethod.error", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
        doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.businessReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        documentScenario1.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        documentScenario1.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
          messages("incomeSources.add.businessReportingMethod.error", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get) + " " +
            messages("incomeSources.add.businessReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        }
      }
    }
    "taxYear1 is crystallised and taxYear2 are present in Latency" should {
      "render the radio form" in new Setup(false) {
        val doc = documentScenario2.getElementById("add-business-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.taxYear", (viewModelScenario2.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 1
      }

      "render the radio form with input error" in new Setup(false, error = true) {
        val doc = documentScenario2.getElementById("add-business-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 1
        doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.businessReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        documentScenario2.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        documentScenario2.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
          messages("incomeSources.add.businessReportingMethod.error", (viewModelScenario2.taxYear2.get.toInt - 1).toString, viewModelScenario2.taxYear2.get)
        }
      }
    }
  }

  "BusinessReportingMethodView - Agent" when {
    "with static content" should {
      "render the heading" in new Setup(true) {
        documentScenario1.getElementsByClass("govuk-heading-xl").text() shouldBe messages("incomeSources.add.businessReportingMethod.heading")
        documentScenario1.getElementsByClass("govuk-heading-m").text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseReport")
      }
      "render the content" in new Setup(true) {
        val doc = documentScenario1.getElementById("main-content")
        doc.getElementsByTag("p").get(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.description1", viewModelScenario1.taxYear2.get)
        doc.getElementsByTag("p").get(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.description2")
        doc.getElementsByTag("p").get(2).text() shouldBe messages("incomeSources.add.businessReportingMethod.description3")
        doc.getElementsByTag("ul").get(0).text() shouldBe Jsoup.parse(messages("incomeSources.add.businessReportingMethod.description4")).text()
      }

    }
    "no back url" in new Setup(true) {
      documentScenario1.getElementById("back") shouldBe null
    }
    "render the continue button" in new Setup(true) {
      documentScenario1.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "taxYear1 and taxYear2 are present in Latency" should {
      "render the radio form" in new Setup(true) {
        val doc = documentScenario1.getElementById("add-business-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.taxYear", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
        doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 2
      }

      "render the radio form with input error" in new Setup(true, error = true) {
        val doc = documentScenario1.getElementById("add-business-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.taxYear", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
        doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 2
        doc.getElementById("new_tax_year_1_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.businessReportingMethod.error", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
        doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.businessReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        documentScenario1.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        documentScenario1.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
          messages("incomeSources.add.businessReportingMethod.error", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get) + " " +
            messages("incomeSources.add.businessReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        }
      }
    }
    "taxYear1 is crystallised and taxYear2 are present in Latency" should {
      "render the radio form" in new Setup(true) {
        val doc = documentScenario2.getElementById("add-business-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.taxYear", (viewModelScenario2.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 1
      }

      "render the radio form with input error" in new Setup(true, error = true) {
        val doc = documentScenario2.getElementById("add-business-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 1
        doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.businessReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        documentScenario2.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        documentScenario2.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
          messages("incomeSources.add.businessReportingMethod.error", (viewModelScenario2.taxYear2.get.toInt - 1).toString, viewModelScenario2.taxYear2.get)
        }
      }
    }
  }
}
