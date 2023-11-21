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

///*
// * Copyright 2023 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package views.incomeSources.add
//
//import forms.incomeSources.add.AddForeignPropertyReportingMethodForm
//import models.incomeSourceDetails.viewmodels.ForeignPropertyReportingMethodViewModel
//import org.jsoup.Jsoup
//import org.jsoup.nodes.Document
//import play.api.data.Form
//import play.api.mvc.Call
//import play.test.Helpers.contentAsString
//import play.twirl.api.HtmlFormat
//import testUtils.TestSupport
//import views.html.incomeSources.add.ForeignPropertyReportingMethod
//
//class ForeignPropertyReportingMethodViewSpec extends TestSupport {
//
//  val foreignPropertyReportingMethodView: ForeignPropertyReportingMethod = app.injector.instanceOf[ForeignPropertyReportingMethod]
//
//  class Setup(isAgent: Boolean, error: Boolean = false) {
//    val taxYear1 = "tax_year_1_reporting_method_tax_year"
//    val taxYear2 = "tax_year_2_reporting_method_tax_year"
//    val taxYearReporting1 = "new_tax_year_1_reporting_method"
//    val taxYearReporting2 = "new_tax_year_2_reporting_method"
//    val radioMustBeSelectedMessageKey = "incomeSources.add.foreignPropertyReportingMethod.error"
//
//    val form: Form[_] = AddForeignPropertyReportingMethodForm.form
//
//    val formWithErrorScenario1: Form[_] = AddForeignPropertyReportingMethodForm.form
//      .withError(taxYearReporting1, radioMustBeSelectedMessageKey, "2021", "2022")
//      .withError(taxYearReporting2, radioMustBeSelectedMessageKey, "2022", "2023")
//
//    val formWithErrorScenario2: Form[_] = AddForeignPropertyReportingMethodForm.form
//      .withError(taxYearReporting2, radioMustBeSelectedMessageKey, "2022", "2023")
//
//    val viewModelScenario1 = ForeignPropertyReportingMethodViewModel(Some("2022"), Some("A"), Some("2023"), Some("A"))
//    val viewModelScenario2 = ForeignPropertyReportingMethodViewModel(None, None, Some("2023"))
//
//    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submitAgent("") else
//      controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submit("")
//
//
//    lazy val viewScenario1: HtmlFormat.Appendable = foreignPropertyReportingMethodView(form, viewModelScenario1, postAction, isAgent)
//    lazy val viewScenario2: HtmlFormat.Appendable = foreignPropertyReportingMethodView(form, viewModelScenario2, postAction, isAgent)
//    lazy val viewScenario1Error: HtmlFormat.Appendable = foreignPropertyReportingMethodView(formWithErrorScenario1, viewModelScenario1, postAction, isAgent)
//    lazy val viewScenario2Error: HtmlFormat.Appendable = foreignPropertyReportingMethodView(formWithErrorScenario2, viewModelScenario2, postAction, isAgent)
//
//    lazy val documentScenario1: Document = if (error) Jsoup.parse(contentAsString(viewScenario1Error)) else Jsoup.parse(contentAsString(viewScenario1))
//    lazy val documentScenario2: Document = if (error) Jsoup.parse(contentAsString(viewScenario2Error)) else Jsoup.parse(contentAsString(viewScenario2))
//
//  }
//
//  "ForeignPropertyReportingMethodView - Individual" when {
//    "with static content" should {
//      "render the heading" in new Setup(false) {
//        documentScenario1.getElementsByClass("govuk-heading-xl").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.heading")
//        documentScenario1.getElementsByClass("govuk-heading-m").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseReport")
//      }
//      "render the content" in new Setup(false) {
//        val doc = documentScenario1.getElementById("main-content")
//        doc.getElementsByTag("p").get(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description1", viewModelScenario1.taxYear2.get)
//        doc.getElementsByTag("p").get(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description2")
//        doc.getElementsByTag("p").get(2).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description3")
//        doc.getElementsByTag("li").get(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet1")
//        doc.getElementsByTag("li").get(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet2")
//        doc.getElementsByTag("li").get(2).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet3")
//      }
//
//    }
//    "no back url" in new Setup(true) {
//      documentScenario1.getElementById("back") shouldBe null
//    }
//    "render the continue button" in new Setup(false) {
//      documentScenario1.getElementById("continue-button").text() shouldBe messages("base.continue")
//    }
//    "taxYear1 and taxYear2 are present in Latency" should {
//      "render the radio form" in new Setup(false) {
//        val doc = documentScenario1.getElementById("add-foreign-property-reporting-method-form")
//        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
//        doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
//        doc.getElementsByClass("govuk-radios").size() shouldBe 2
//      }
//
//      "render the radio form with input error" in new Setup(false, error = true) {
//        val doc = documentScenario1.getElementById("add-foreign-property-reporting-method-form")
//        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
//        doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
//        doc.getElementsByClass("govuk-radios").size() shouldBe 2
//        doc.getElementById("new_tax_year_1_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.foreignPropertyReportingMethod.error", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
//        doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.foreignPropertyReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        documentScenario1.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
//        documentScenario1.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
//          messages("incomeSources.add.foreignPropertyReportingMethod.error", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get) + " " +
//            messages("incomeSources.add.foreignPropertyReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        }
//      }
//    }
//    "taxYear1 is crystallised and taxYear2 are present in Latency" should {
//      "render the radio form" in new Setup(false) {
//        val doc = documentScenario2.getElementById("add-foreign-property-reporting-method-form")
//        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", (viewModelScenario2.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
//        doc.getElementsByClass("govuk-radios").size() shouldBe 1
//      }
//
//      "render the radio form with input error" in new Setup(false, error = true) {
//        val doc = documentScenario2.getElementById("add-foreign-property-reporting-method-form")
//        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
//        doc.getElementsByClass("govuk-radios").size() shouldBe 1
//        doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.foreignPropertyReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        documentScenario2.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
//        documentScenario2.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
//          messages("incomeSources.add.foreignPropertyReportingMethod.error", (viewModelScenario2.taxYear2.get.toInt - 1).toString, viewModelScenario2.taxYear2.get)
//        }
//      }
//    }
//  }
//
//  "ForeignPropertyReportingMethodView - Agent" when {
//    "with static content" should {
//      "render the heading" in new Setup(true) {
//        documentScenario1.getElementsByClass("govuk-heading-xl").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.heading")
//        documentScenario1.getElementsByClass("govuk-heading-m").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseReport")
//      }
//      "render the content" in new Setup(true) {
//        val doc = documentScenario1.getElementById("main-content")
//        doc.getElementsByTag("p").get(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description1", viewModelScenario1.taxYear2.get)
//        doc.getElementsByTag("p").get(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description2")
//        doc.getElementsByTag("p").get(2).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description3")
//        doc.getElementsByTag("li").get(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet1")
//        doc.getElementsByTag("li").get(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet2")
//        doc.getElementsByTag("li").get(2).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet3")      }
//
//    }
//    "no back url" in new Setup(true) {
//      documentScenario1.getElementById("back") shouldBe null
//    }
//    "render the continue button" in new Setup(true) {
//      documentScenario1.getElementById("continue-button").text() shouldBe messages("base.continue")
//    }
//    "taxYear1 and taxYear2 are present in Latency" should {
//      "render the radio form" in new Setup(true) {
//        val doc = documentScenario1.getElementById("add-foreign-property-reporting-method-form")
//        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
//        doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
//        doc.getElementsByClass("govuk-radios").size() shouldBe 2
//      }
//
//      "render the radio form with input error" in new Setup(true, error = true) {
//        val doc = documentScenario1.getElementById("add-foreign-property-reporting-method-form")
//        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
//        doc.getElementsByTag("legend").get(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(2).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(3).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
//        doc.getElementsByClass("govuk-radios").size() shouldBe 2
//        doc.getElementById("new_tax_year_1_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.foreignPropertyReportingMethod.error", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get)
//        doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.foreignPropertyReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        documentScenario1.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
//        documentScenario1.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
//          messages("incomeSources.add.foreignPropertyReportingMethod.error", (viewModelScenario1.taxYear1.get.toInt - 1).toString, viewModelScenario1.taxYear1.get) + " " +
//            messages("incomeSources.add.foreignPropertyReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        }
//      }
//    }
//    "taxYear1 is crystallised and taxYear2 are present in Latency" should {
//      "render the radio form" in new Setup(true) {
//        val doc = documentScenario2.getElementById("add-foreign-property-reporting-method-form")
//        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", (viewModelScenario2.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
//        doc.getElementsByClass("govuk-radios").size() shouldBe 1
//      }
//
//      "render the radio form with input error" in new Setup(true, error = true) {
//        val doc = documentScenario2.getElementById("add-foreign-property-reporting-method-form")
//        doc.getElementsByTag("legend").get(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
//        doc.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
//        doc.getElementsByClass("govuk-radios").size() shouldBe 1
//        doc.getElementById("new_tax_year_2_reporting_method-error").text() shouldBe messages("base.error-prefix") + " " + messages("incomeSources.add.foreignPropertyReportingMethod.error", (viewModelScenario1.taxYear2.get.toInt - 1).toString, viewModelScenario1.taxYear2.get)
//        documentScenario2.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
//        documentScenario2.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe {
//          messages("incomeSources.add.foreignPropertyReportingMethod.error", (viewModelScenario2.taxYear2.get.toInt - 1).toString, viewModelScenario2.taxYear2.get)
//        }
//      }
//    }
//  }
//}
