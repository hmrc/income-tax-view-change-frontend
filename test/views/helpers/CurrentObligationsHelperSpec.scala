/*
 * Copyright 2020 HM Revenue & Customs
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

package views.helpers

import java.time.LocalDate

import assets.BaseTestConstants.testMtdItUser
import assets.BusinessDetailsTestConstants.{business1, testTradeName}
import assets.Messages.{CurrentObligationsHelper => messages}
import assets.PropertyDetailsTestConstants.propertyDetails
import assets.ReportDeadlinesTestConstants.{twoObligationsSuccessModel, _}
import implicits.ImplicitDateFormatter
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class CurrentObligationsHelperSpec extends TestSupport with ImplicitDateFormatter {

  class Setup(model: ObligationsModel) {
    val pageDocument: Document = Jsoup.parse(contentAsString(views.html.helpers.currentObligationsHelper(model)))
  }

  "The Current Obligations Helper" should {

    lazy val businessIncomeSource = ObligationsModel(Seq(ReportDeadlinesModel(
      business1.incomeSourceId,
      twoObligationsSuccessModel.obligations
    )))

    lazy val piQuarterlyReturnSource = ObligationsModel(Seq(ReportDeadlinesModel(
      propertyDetails.incomeSourceId,
      reportDeadlinesDataSelfEmploymentSuccessModel.obligations
    )))

    lazy val twoPiQuarterlyReturnSource = ObligationsModel(Seq(ReportDeadlinesModel(
      propertyDetails.incomeSourceId,
      quarterlyObligationsDataSuccessModel.obligations
    )))


    lazy val quarterlyBusinessIncomeSource = ObligationsModel(Seq(ReportDeadlinesModel(
      business1.incomeSourceId,
      List(quarterlyBusinessObligation)
    )))

    lazy val eopsPropertyIncomeSource = ObligationsModel(Seq(ReportDeadlinesModel(
      propertyDetails.incomeSourceId,
      List(
        ReportDeadlineModel(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 31), LocalDate.of(2020, 1, 1), "EOPS", None, "EOPS")
      )
    )))

    lazy val crystallisedIncomeSource = ObligationsModel(Seq(
      ReportDeadlinesModel(
        business1.incomeSourceId,
        List(crystallisedObligation)),
      ReportDeadlinesModel(
        testMtdItUser.mtditid,
        List(crystallisedObligation))
    ))


    lazy val multiCrystallisedIncomeSource = ObligationsModel(Seq(
      ReportDeadlinesModel(
        business1.incomeSourceId,
        List(crystallisedObligation)),
      ReportDeadlinesModel(
        testMtdItUser.mtditid,
        List(crystallisedObligationTwo, crystallisedObligation))
    ))


    lazy val eopsSEIncomeSource = ObligationsModel(Seq(ReportDeadlinesModel(
      business1.incomeSourceId,
      List(openEOPSObligation)
    )))

    lazy val noIncomeSource = ObligationsModel(Seq())

    "display all of the correct information for the main elements/sections" when {

      s"show the sub heading para about record keeping software" in new Setup(businessIncomeSource) {
        pageDocument.select("#sub-heading-section p").text shouldBe messages.subHeadingPara
      }


      "showing the heading for the quarterly updates section" in new Setup(businessIncomeSource) {
        pageDocument.getElementById("quarterlyReturns-heading").text shouldBe messages.quarterlyHeading
      }

      "showing the heading for the annual updates section" in new Setup(eopsPropertyIncomeSource) {
        pageDocument.getElementById("annualUpdates-heading").text shouldBe messages.annualHeading
      }

      "showing the heading for the final declaration section" in new Setup(eopsPropertyIncomeSource) {
        pageDocument.getElementById("declarations-heading").text shouldBe messages.declarationsHeading
      }

      "showing the Quarterly update heading and drop down section on the page" in new Setup(businessIncomeSource) {
        pageDocument.getElementById("quarterly-dropdown-title").text shouldBe messages.quarterlyDropDown
        pageDocument.getElementById("quarterly-dropdown-line1").text shouldBe messages.quarterlyDropdownLine1
        pageDocument.getElementById("quarterly-dropdown-line2").text shouldBe messages.quarterlyDropdownLine2
      }

      "showing the Annual update heading and drop down section on the page" in new Setup(businessIncomeSource) {
        pageDocument.getElementById("annual-dropdown-title").text shouldBe messages.annualDropDown
        pageDocument.getElementById("annual-dropdown-line1").text shouldBe messages.annualDropdownListOne
        pageDocument.getElementById("annual-dropdown-line2").text shouldBe messages.annualDropdownListTwo
      }

      "showing the Final declaration heading and drop down section on the page" in new Setup(businessIncomeSource) {
        pageDocument.getElementById("declaration-dropdown-title").text shouldBe messages.finalDeclarationDropDown
        pageDocument.getElementById("details-content-2").text shouldBe messages.finalDeclerationDetails
      }

    }
    "display all of the correct information for the EOPS property section" when {
      "showing the eops property income section" in new Setup(eopsPropertyIncomeSource) {
        pageDocument.select("#eops-return-section-0 div div:nth-child(1) div:nth-child(1)").text shouldBe messages.propertyIncome
        pageDocument.select("#eops-return-section-0 div div:nth-child(1) div:nth-child(2)").text shouldBe messages.fromToDates("1 January 2019", "31 January 2020")
        pageDocument.select("#eops-return-section-0 div div:nth-child(2) div:nth-child(1)").text shouldBe messages.dueOn
        pageDocument.select("#eops-return-section-0 div div:nth-child(2) div:nth-child(2)").text shouldBe "1 January 2020"
      }

      "not showing the eops property section when there is no property income report" in new Setup(noIncomeSource) {
        Option(pageDocument.getElementById("eopsPropertyTableRow")) shouldBe None
      }

      "display all of the correct information for the EOPS business section" when {

        "showing the eops business income section" in new Setup(eopsSEIncomeSource) {
          pageDocument.select("#eops-return-section-0 div div:nth-child(1) div:nth-child(1)").text shouldBe testTradeName
          pageDocument.select("#eops-return-section-0 div div:nth-child(1) div:nth-child(2)").text shouldBe messages.fromToDates("6 April 2017", "5 April 2018")
          pageDocument.select("#eops-return-section-0 div div:nth-child(2) div:nth-child(1)").text shouldBe messages.dueOn
          pageDocument.select("#eops-return-section-0 div div:nth-child(2) div:nth-child(2)").text shouldBe "31 October 2017"
        }
      }

      "display all of the correct information for the quarterly property section" when {

        "showing the quarterly property income section" in new Setup(piQuarterlyReturnSource) {
          pageDocument.select("#quarterly-return-section-0 div div:nth-child(1) div:nth-child(1)").text shouldBe messages.propertyIncome
          pageDocument.select("#quarterly-return-section-0 div div:nth-child(1) div:nth-child(2)").text shouldBe messages.fromToDates("1 July 2017", "30 September 2017")
          pageDocument.select("#quarterly-return-section-0 div div:nth-child(2) div:nth-child(1)").text shouldBe messages.dueOn
          pageDocument.select("#quarterly-return-section-0 div div:nth-child(2) div:nth-child(2)").text shouldBe "30 October 2017"
        }

        "showing the property income quarterly return due date most recent when there are more then one" in new Setup(twoPiQuarterlyReturnSource) {
          pageDocument.select("#quarterly-return-section-0 div div:nth-child(2) div:nth-child(2)").text shouldBe "31 October 2017"
        }
      }

      "display all of the correct information for the quarterly business section" when {

        "showing the quarterly business income section" in new Setup(quarterlyBusinessIncomeSource) {
          pageDocument.select("#quarterly-return-section-0 div div:nth-child(1) div:nth-child(1)").text() shouldBe testTradeName
          pageDocument.select("#quarterly-return-section-0 div div:nth-child(1) div:nth-child(2)").text() shouldBe messages.fromToDates("1 July 2017", "30 September 2017")
          pageDocument.select("#quarterly-return-section-0 div div:nth-child(2) div:nth-child(1)").text shouldBe messages.dueOn
          pageDocument.select("#quarterly-return-section-0 div div:nth-child(2) div:nth-child(2)").text() shouldBe "30 October 2019"
        }
      }


      "display all of the correct information for the crystallised section" when {

        "showing the crystallised section" in new Setup(crystallisedIncomeSource) {
          pageDocument.select("#crystallised-section-0 div div:nth-child(1) div:nth-child(1)").text shouldBe messages.crystallisedHeading
          pageDocument.select("#crystallised-section-0 div div:nth-child(1) div:nth-child(2)").text shouldBe messages.fromToDates("1 October 2017", "30 October 2018")
          pageDocument.select("#crystallised-section-0 div div:nth-child(2) div:nth-child(1)").text shouldBe messages.dueOn
          pageDocument.select("#crystallised-section-0 div div:nth-child(2) div:nth-child(2)").text shouldBe "31 October 2017"
        }
      }

      "display all of the correct information for the crystallised section for multiple crystallised obligations" when {

        "showing the crystallised section for the first obligation" in new Setup(multiCrystallisedIncomeSource) {
          pageDocument.select("#crystallised-section-0 div div:nth-child(1) div:nth-child(1)").text() shouldBe messages.crystallisedHeading
          pageDocument.select("#crystallised-section-0 div div:nth-child(1) div:nth-child(2)").text shouldBe messages.fromToDates("1 October 2017", "30 October 2018")
          pageDocument.select("#crystallised-section-0 div div:nth-child(2) div:nth-child(1)").text shouldBe messages.dueOn
          pageDocument.select("#crystallised-section-0 div div:nth-child(2) div:nth-child(2)").text shouldBe "31 October 2017"
        }

        "showing the crystallised section for the second obligation" in new Setup(multiCrystallisedIncomeSource) {
          pageDocument.select("#crystallised-section-1 div div:nth-child(1) div:nth-child(1)").text() shouldBe messages.crystallisedHeading
          pageDocument.select("#crystallised-section-1 div div:nth-child(1) div:nth-child(2)").text shouldBe messages.fromToDates("1 October 2017", "30 October 2018")
          pageDocument.select("#crystallised-section-1 div div:nth-child(2) div:nth-child(1)").text shouldBe messages.dueOn
          pageDocument.select("#crystallised-section-1 div div:nth-child(2) div:nth-child(2)").text shouldBe "31 October 2017"
        }
      }

    }
  }
}
