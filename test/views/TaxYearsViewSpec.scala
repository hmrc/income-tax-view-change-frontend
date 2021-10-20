/*
 * Copyright 2021 HM Revenue & Customs
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

package views

import testConstants.EstimatesTestConstants._
import testConstants.MessagesLookUp.{TaxYears => taxYears}
import config.FrontendAppConfig
import models.calculation.CalculationResponseModelWithYear
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.ViewSpec
import views.html.TaxYears

class TaxYearsViewSpec extends ViewSpec {

  val taxYearsView: TaxYears = app.injector.instanceOf[TaxYears]
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  class Setup(calcs: List[CalculationResponseModelWithYear],
                        itsaSubmissionFeatureSwitch: Boolean = false,
                        utr: Option[String] = None)  {
    lazy val page: HtmlFormat.Appendable =
      taxYearsView(calcs, "testBackURL", utr, itsaSubmissionFeatureSwitch)(FakeRequest(),implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }


  "The TaxYears view with itsaSubmissionFeatureSwitch FS disabled" when {
    "the view is displayed" should {
      s"have the title '${taxYears.title}'" in new Setup(lastTaxCalcWithYearList) {
        document.title() shouldBe taxYears.title
      }

      "have a header" in new Setup(lastTaxCalcWithYearList) {
        layoutContent.selectHead("h1").text shouldBe taxYears.heading
      }

      "have a table header" in new Setup(lastTaxCalcWithYearList) {
        document.selectNth("th", 1).text shouldBe taxYears.tableHeadingTaxYear
        document.selectNth("th", 2).text shouldBe taxYears.tableHeadingOptions
      }
    }

    "the user has two tax years" should {
      "display two tax years" in new Setup(lastTaxCalcWithYearList){
        document.selectHead("tbody").selectNth("tr", 1)
          .selectNth("td", 1).selectNth("li", 1).text() shouldBe taxYears.taxYear(testYear.toString, testYearPlusOne.toString)
        document.selectHead("tbody").selectNth("tr", 2)
          .selectNth("td", 1).selectNth("li", 1).text() shouldBe taxYears.taxYear((testYear-1).toString, testYear.toString)
      }

      "display two view return links for the correct tax year" in new Setup(lastTaxCalcWithYearList){
        document.getElementById("viewReturn-link-2018").text() shouldBe
          s"${taxYears.viewReturn} ${taxYears.taxYear((testYear-1).toString, testYear.toString)}"
        document.getElementById("viewReturn-link-2019").text() shouldBe
          s"${taxYears.viewReturn} ${taxYears.taxYear(testYear.toString, testYearPlusOne.toString)}"
      }

      "not display any update return link" in new Setup(lastTaxCalcWithYearList){
        Option(document.getElementById("updateReturn-link-2018")) shouldBe None
        Option(document.getElementById("updateReturn-link-2019")) shouldBe None
      }
    }

    "the user has three tax years records" should {
      "display three tax years" in new Setup(lastThreeTaxCalcWithYear){
        document.selectHead("tbody").selectNth("tr", 1)
          .selectNth("td", 1).selectNth("li", 1).text() shouldBe taxYears.taxYear(testYearPlusOne.toString, testYearPlusTwo.toString)
        document.selectHead("tbody").selectNth("tr", 2)
          .selectNth("td", 1).selectNth("li", 1).text() shouldBe taxYears.taxYear(testYear.toString, testYearPlusOne.toString)
        document.selectHead("tbody").selectNth("tr", 3)
          .selectNth("td", 1).selectNth("li", 1).text() shouldBe taxYears.taxYear((testYear-1).toString, testYear.toString)
      }

       "display three view return links for the correct tax year" in new Setup(lastThreeTaxCalcWithYear){
        document.getElementById("viewReturn-link-2018").text() shouldBe
          s"${taxYears.viewReturn} ${taxYears.taxYear((testYear-1).toString, testYear.toString)}"
        document.getElementById("viewReturn-link-2019").text() shouldBe
          s"${taxYears.viewReturn} ${taxYears.taxYear(testYear.toString, testYearPlusOne.toString)}"
        document.getElementById("viewReturn-link-2020").text() shouldBe
          s"${taxYears.viewReturn} ${taxYears.taxYear(testYearPlusOne.toString, testYearPlusTwo.toString)}"
      }

      "not display any update return link" in new Setup(lastThreeTaxCalcWithYear){
        Option(document.getElementById("updateReturn-link-2018")) shouldBe None
        Option(document.getElementById("updateReturn-link-2019")) shouldBe None
        Option(document.getElementById("updateReturn-link-2020")) shouldBe None
      }
    }

    "the user has no taxYears" should {
      s"have the paragraph '${taxYears.noEstimates}'" in new Setup(List()){
        document.getElementById("no-taxYears").text shouldBe taxYears.noEstimates
      }
    }

    "the paragraph explaining about previous Self Assessments" should {
      "appear if the user has a UTR" in new Setup(lastTaxCalcWithYearList, utr = Some("1234567890")){
        layoutContent.select(Selectors.p).text shouldBe taxYears.saNote
        layoutContent.selectFirst(Selectors.p).hasCorrectLinkWithNewTab(taxYears.saLink, "http://localhost:8930/self-assessment/ind/1234567890/account")
      }

      "not appear if the user does not have a UTR" in new Setup(lastTaxCalcWithYearList){
        Option(document.selectFirst("#content p")) shouldBe None
      }
    }
  }

   "The TaxYears view with itsaSubmissionFeatureSwitch FS enabled" when {
     "the user has two tax years" should {
       "display two tax years" in new Setup(lastTaxCalcWithYearList, true){
         document.selectHead("tbody").selectNth("tr", 1)
           .selectNth("td", 1).selectNth("li", 1).text() shouldBe taxYears.taxYear(testYear.toString, testYearPlusOne.toString)
         document.selectHead("tbody").selectNth("tr", 2)
           .selectNth("td", 1).selectNth("li", 1).text() shouldBe taxYears.taxYear((testYear - 1).toString, testYear.toString)
       }

       "display two view return links for the correct tax year" in new Setup(lastTaxCalcWithYearList, true){
         document.getElementById("viewReturn-link-2018").text() shouldBe
           s"${taxYears.viewReturn} ${taxYears.taxYear((testYear - 1).toString, testYear.toString)}"
         document.getElementById("viewReturn-link-2019").text() shouldBe
           s"${taxYears.viewReturn} ${taxYears.taxYear(testYear.toString, testYearPlusOne.toString)}"
       }

       "display two update return links for the correct tax year" in new Setup(lastTaxCalcWithYearList, true){
         document.getElementById("updateReturn-link-2018") .text() shouldBe
           s"${taxYears.updateReturn} ${taxYears.taxYear((testYear - 1).toString, testYear.toString)}"
         document.getElementById("updateReturn-link-2019") .text() shouldBe
           s"${taxYears.updateReturn} ${taxYears.taxYear(testYear.toString, testYearPlusOne.toString)}"
       }

     }
   }

}
