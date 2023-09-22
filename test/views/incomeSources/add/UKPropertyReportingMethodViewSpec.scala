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

import forms.incomeSources.add.AddUKPropertyReportingMethodForm
import models.incomeSourceDetails.viewmodels.UKPropertyReportingMethodViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.Call
import play.test.Helpers.contentAsString
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.incomeSources.add.UKPropertyReportingMethod

class UKPropertyReportingMethodViewSpec extends TestSupport {

  val ukPropertyReportingMethodView: UKPropertyReportingMethod = app.injector.instanceOf[UKPropertyReportingMethod]

  class Setup(isAgent: Boolean, error: Boolean = false) {
    val taxYear1 = "tax_year_1_reporting_method_tax_year"
    val taxYear2 = "tax_year_2_reporting_method_tax_year"
    val taxYearReporting1 = "new_tax_year_1_reporting_method"
    val taxYearReporting2 = "new_tax_year_2_reporting_method"
    val radioMustBeSelectedMessageKey = "incomeSources.add.ukPropertyReportingMethod.error"

    val form: Form[_] = AddUKPropertyReportingMethodForm.form

    val formWithErrorScenario1: Form[_] = AddUKPropertyReportingMethodForm.form
      .withError(taxYearReporting1, radioMustBeSelectedMessageKey, "2021", "2022")
      .withError(taxYearReporting2, radioMustBeSelectedMessageKey, "2022", "2023")

    val formWithErrorScenario2: Form[_] = AddUKPropertyReportingMethodForm.form
      .withError(taxYearReporting2, radioMustBeSelectedMessageKey, "2022", "2023")

    val viewModelScenario1 = UKPropertyReportingMethodViewModel(Some("2022"), Some("A"), Some("2023"), Some("A"))
    val viewModelScenario2 = UKPropertyReportingMethodViewModel(None, None, Some("2023"))

    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.UKPropertyReportingMethodController.submitAgent() else
      controllers.incomeSources.add.routes.UKPropertyReportingMethodController.submit()


    lazy val viewScenario1: HtmlFormat.Appendable = ukPropertyReportingMethodView(form, viewModelScenario1, postAction, isAgent)
    lazy val viewScenario2: HtmlFormat.Appendable = ukPropertyReportingMethodView(form, viewModelScenario2, postAction, isAgent)
    lazy val viewScenario1Error: HtmlFormat.Appendable = ukPropertyReportingMethodView(formWithErrorScenario1, viewModelScenario1, postAction, isAgent)
    lazy val viewScenario2Error: HtmlFormat.Appendable = ukPropertyReportingMethodView(formWithErrorScenario2, viewModelScenario2, postAction, isAgent)

    lazy val documentScenario1: Document = if (error) Jsoup.parse(contentAsString(viewScenario1Error)) else Jsoup.parse(contentAsString(viewScenario1))
    lazy val documentScenario2: Document = if (error) Jsoup.parse(contentAsString(viewScenario2Error)) else Jsoup.parse(contentAsString(viewScenario2))

  }

  "UKPropertyReportingMethodView - Individual" when {
    "with static content" should {
      "render the heading" in new Setup(false) {
        documentScenario1.getElementsByClass("govuk-heading-xl").text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.heading")
        documentScenario1.getElementsByClass("govuk-heading-m").text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseReport")
      }
      "render the content" in new Setup(false) {
        val doc = documentScenario1.getElementById("main-content")
        doc.getElementsByTag("p").get(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.description1", viewModelScenario1.taxYear2.get)
        doc.getElementsByTag("p").get(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.description2")
        doc.getElementsByTag("p").get(2).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.description3")
        doc.getElementsByTag("ul").get(0).text() shouldBe Jsoup.parse(messages("incomeSources.add.ukPropertyReportingMethod.description4.bullet1") + " " +
          messages("incomeSources.add.ukPropertyReportingMethod.description4.bullet2") + " " + messages("incomeSources.add.ukPropertyReportingMethod.description4.bullet3")).text()
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
        val doc = documentScenario1.getElementById("add-uk-property-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
        doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 2
      }

      "render the radio form with input error" in new Setup(false, error = true) {
        val doc = documentScenario1.getElementById("add-uk-property-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
        doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 2
        doc.getElementById("new_tax_year_1_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.ukPropertyReportingMethod.error", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
        doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.ukPropertyReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        documentScenario1.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        documentScenario1.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
          messages("incomeSources.add.ukPropertyReportingMethod.error", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get) + " " +
            messages("incomeSources.add.ukPropertyReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        }
      }
    }
    "taxYear1 is crystallised and taxYear2 are present in Latency" should {
      "render the radio form" in new Setup(false) {
        val doc = documentScenario2.getElementById("add-uk-property-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.taxYear", (viewModelScenario2.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 1
      }

      "render the radio form with input error" in new Setup(false, error = true) {
        val doc = documentScenario2.getElementById("add-uk-property-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 1
        doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.ukPropertyReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        documentScenario2.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        documentScenario2.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
          messages("incomeSources.add.ukPropertyReportingMethod.error", (viewModelScenario2.taxYear2.get.toInt - 1).toString, viewModelScenario2.taxYear2.get)
        }
      }
    }
  }

  "UKPropertyReportingMethodView - Agent" when {
    "with static content" should {
      "render the heading" in new Setup(true) {
        documentScenario1.getElementsByClass("govuk-heading-xl").text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.heading")
        documentScenario1.getElementsByClass("govuk-heading-m").text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseReport")
      }
      "render the content" in new Setup(true) {
        val doc = documentScenario1.getElementById("main-content")
        doc.getElementsByTag("p").get(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.description1", viewModelScenario1.taxYear2.get)
        doc.getElementsByTag("p").get(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.description2")
        doc.getElementsByTag("p").get(2).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.description3")
        doc.getElementsByTag("ul").get(0).text() shouldBe Jsoup.parse(messages("incomeSources.add.ukPropertyReportingMethod.description4.bullet1") + " " +
          messages("incomeSources.add.ukPropertyReportingMethod.description4.bullet2") + " " + messages("incomeSources.add.ukPropertyReportingMethod.description4.bullet3")).text()
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
        val doc = documentScenario1.getElementById("add-uk-property-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
        doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 2
      }

      "render the radio form with input error" in new Setup(true, error = true) {
        val doc = documentScenario1.getElementById("add-uk-property-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
        doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 2
        doc.getElementById("new_tax_year_1_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.ukPropertyReportingMethod.error", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
        doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.ukPropertyReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        documentScenario1.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        documentScenario1.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
          messages("incomeSources.add.ukPropertyReportingMethod.error", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get) + " " +
            messages("incomeSources.add.ukPropertyReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        }
      }
    }
    "taxYear1 is crystallised and taxYear2 are present in Latency" should {
      "render the radio form" in new Setup(true) {
        val doc = documentScenario2.getElementById("add-uk-property-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.taxYear", (viewModelScenario2.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 1
      }

      "render the radio form with input error" in new Setup(true, error = true) {
        val doc = documentScenario2.getElementById("add-uk-property-reporting-method-form")
        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseAnnualReport")
        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.ukPropertyReportingMethod.chooseQuarterlyReport")
        doc.getElementsByClass("govuk-radios").size() shouldBe 1
        doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.ukPropertyReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
        documentScenario2.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        documentScenario2.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
          messages("incomeSources.add.ukPropertyReportingMethod.error", (viewModelScenario2.taxYear2.get.toInt - 1).toString, viewModelScenario2.taxYear2.get)
        }
      }
    }
  }
}