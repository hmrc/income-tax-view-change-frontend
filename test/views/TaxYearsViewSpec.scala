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

import assets.EstimatesTestConstants._
import assets.MessagesLookUp
import config.FrontendAppConfig
import models.calculation.CalculationResponseModelWithYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.ViewSpec
import views.html.TaxYears

class TaxYearsViewSpec extends ViewSpec {

  val taxYearsView = app.injector.instanceOf[TaxYears]
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  private def pageSetup(calcs: List[CalculationResponseModelWithYear],
                        itsaSubmissionFeatureSwitch: Boolean = false,


                       ) = new {
    lazy val page: HtmlFormat.Appendable =
      taxYearsView(calcs, "testBackURL",itsaSubmissionFeatureSwitch)(FakeRequest(),implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The TaxYears view with itsaSubmissionFeatureSwitch FS disabled" when {
    "the user has two tax years" should {
      val setup = pageSetup(lastTaxCalcWithYearList)
      import setup._
      val messages = MessagesLookUp.TaxYears

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "have a header" in {
        document.getElementById("heading").text shouldBe messages.heading
      }

      "have a table header" in {
        document.selectNth("th", 1).text shouldBe messages.tableHeadingTaxYear
        document.selectNth("th", 2).text shouldBe messages.tableHeadingOptions
      }

      "display two tax years" in {
        document.selectHead("tbody").selectNth("tr", 1)
          .selectNth("td", 1).selectNth("li", 1).text() shouldBe messages.taxYear(testYear.toString, testYearPlusOne.toString)
        document.selectHead("tbody").selectNth("tr", 2)
          .selectNth("td", 1).selectNth("li", 1).text() shouldBe messages.taxYear((testYear-1).toString, testYear.toString)
      }

      "display two view return links for the correct tax year" in {
        document.getElementById("viewReturn-link-2018").text() shouldBe
          s"${messages.viewReturn} ${messages.taxYear((testYear-1).toString, testYear.toString)}"
        document.getElementById("viewReturn-link-2019").text() shouldBe
          s"${messages.viewReturn} ${messages.taxYear(testYear.toString, testYearPlusOne.toString)}"
      }

      "not display any update return link" in {
        document.getElementById("updateReturn-link-2018") shouldBe null
        document.getElementById("updateReturn-link-2019") shouldBe null
      }
    }

    "the user has three tax years records" should {
      val setup = pageSetup(lastThreeTaxCalcWithYear)
      import setup._
      val messages = MessagesLookUp.TaxYears

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "have a header" in {
        document.getElementById("heading").text shouldBe messages.heading
      }

      "have a table header" in {
        document.selectNth("th", 1).text shouldBe messages.tableHeadingTaxYear
        document.selectNth("th", 2).text shouldBe messages.tableHeadingOptions
      }

      "display three tax years" in {
        document.selectHead("tbody").selectNth("tr", 1)
          .selectNth("td", 1).selectNth("li", 1).text() shouldBe messages.taxYear(testYearPlusOne.toString, testYearPlusTwo.toString)
        document.selectHead("tbody").selectNth("tr", 2)
          .selectNth("td", 1).selectNth("li", 1).text() shouldBe messages.taxYear(testYear.toString, testYearPlusOne.toString)
        document.selectHead("tbody").selectNth("tr", 3)
          .selectNth("td", 1).selectNth("li", 1).text() shouldBe messages.taxYear((testYear-1).toString, testYear.toString)
      }

       "display three view return links for the correct tax year" in {
        document.getElementById("viewReturn-link-2018").text() shouldBe
          s"${messages.viewReturn} ${messages.taxYear((testYear-1).toString, testYear.toString)}"
        document.getElementById("viewReturn-link-2019").text() shouldBe
          s"${messages.viewReturn} ${messages.taxYear(testYear.toString, testYearPlusOne.toString)}"
        document.getElementById("viewReturn-link-2020").text() shouldBe
          s"${messages.viewReturn} ${messages.taxYear(testYearPlusOne.toString, testYearPlusTwo.toString)}"
      }

      "not display any update return link" in {
        document.getElementById("updateReturn-link-2018") shouldBe null
        document.getElementById("updateReturn-link-2019") shouldBe null
        document.getElementById("updateReturn-link-2020") shouldBe null
      }
    }

    "the user has no taxYears" should {
      val setup = pageSetup(List())
      import setup._
      val messages = MessagesLookUp.TaxYears

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "have a header" in {
        document.getElementById("heading").text shouldBe messages.heading
      }

      s"have the paragraph '${messages.noEstimates}'" in {
        document.getElementById("no-taxYears").text shouldBe messages.noEstimates
      }
    }
  }

   "The TaxYears view with itsaSubmissionFeatureSwitch FS enabled" when {
     "the user has two tax years" should {
       val setup = pageSetup(lastTaxCalcWithYearList, true)
       import setup._
       val messages = MessagesLookUp.TaxYears

       s"have the title '${messages.title}'" in {
         document.title() shouldBe messages.title
       }

       "have a header" in {
         document.getElementById("heading").text shouldBe messages.heading
       }

       "have a table header" in {
         document.selectNth("th", 1).text shouldBe messages.tableHeadingTaxYear
         document.selectNth("th", 2).text shouldBe messages.tableHeadingOptions
       }

       "display two tax years" in {
         document.selectHead("tbody").selectNth("tr", 1)
           .selectNth("td", 1).selectNth("li", 1).text() shouldBe messages.taxYear(testYear.toString, testYearPlusOne.toString)
         document.selectHead("tbody").selectNth("tr", 2)
           .selectNth("td", 1).selectNth("li", 1).text() shouldBe messages.taxYear((testYear - 1).toString, testYear.toString)
       }

       "display two view return links for the correct tax year" in {
         document.getElementById("viewReturn-link-2018").text() shouldBe
           s"${messages.viewReturn} ${messages.taxYear((testYear - 1).toString, testYear.toString)}"
         document.getElementById("viewReturn-link-2019").text() shouldBe
           s"${messages.viewReturn} ${messages.taxYear(testYear.toString, testYearPlusOne.toString)}"
       }

       "display two update return links for the correct tax year" in {
         document.getElementById("updateReturn-link-2018") .text() shouldBe
           s"${messages.updateReturn} ${messages.taxYear((testYear - 1).toString, testYear.toString)}"
         document.getElementById("updateReturn-link-2019") .text() shouldBe
           s"${messages.updateReturn} ${messages.taxYear(testYear.toString, testYearPlusOne.toString)}"
       }

     }
   }

}
