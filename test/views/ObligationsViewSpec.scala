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

package views

import java.time.LocalDate

import assets.BaseTestConstants.testMtdItUser
import assets.BusinessDetailsTestConstants.{business1, testTradeName}
import assets.Messages.{Breadcrumbs => breadcrumbMessages, Obligations => messages}
import assets.PropertyDetailsTestConstants.propertyDetails
import assets.ReportDeadlinesTestConstants.{twoObligationsSuccessModel, _}
import config.FrontendAppConfig
import implicits.ImplicitDateFormatter
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class ObligationsViewSpec extends TestSupport with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  class Setup(model: ObligationsModel) {
    val html: HtmlFormat.Appendable = views.html.obligations(model)(FakeRequest(), implicitly, mockAppConfig, testMtdItUser)
    val pageDocument: Document = Jsoup.parse(contentAsString(views.html.obligations(model)))
  }

  "The Deadline Reports Page" should {

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

    "have a link to the previous obligations" in new Setup(businessIncomeSource) {
      pageDocument.select(s"a[href='${controllers.routes.PreviousObligationsController.getPreviousObligations().url}']").text shouldBe messages.previousObligations
    }

    "display all of the correct information for the main elements/sections on the page" when {

      "showing the breadcrumb trail on the page" in new Setup(businessIncomeSource) {
        pageDocument.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
        pageDocument.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
        pageDocument.getElementById("breadcrumb-updates").text shouldBe breadcrumbMessages.updates
      }

      s"showing the title ${messages.title} on the page" in new Setup(businessIncomeSource) {
        pageDocument.title() shouldBe messages.title
      }

      s"showing the heading ${messages.heading} on the page" in new Setup(businessIncomeSource) {
        pageDocument.getElementById("page-heading").text shouldBe messages.heading
      }

      s"showing the Sub heading ${messages.subTitle} on page" in new Setup(businessIncomeSource) {
        pageDocument.getElementById("page-sub-heading").text shouldBe messages.subTitle
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
        pageDocument.getElementById("quarterly-dropdown-line1").text == messages.quarterlyDropdownLine1
        pageDocument.getElementById("quarterly-dropdown-line2").text == messages.quarterlyDropdownLine2
      }

      "showing the Annual update heading and drop down section on the page" in new Setup(businessIncomeSource) {
        pageDocument.getElementById("annual-dropdown-title").text shouldBe messages.annualDropDown
        pageDocument.getElementById("annual-dropdown-line1").text == messages.annualDropdownListOne
        pageDocument.getElementById("annual-dropdown-line2").text == messages.annualDropdownListTwo
      }

      "showing the Final declaration heading and drop down section on the page" in new Setup(businessIncomeSource) {
        pageDocument.getElementById("declaration-dropdown-title").text shouldBe messages.finalDeclarationDropDown
        pageDocument.getElementById("details-content-2").text == messages.finalDeclerationDetails
      }

    }
    "display all of the correct information for the EOPS property section" when {
      "showing the eops property income section" in new Setup(eopsPropertyIncomeSource) {
        pageDocument.select("#eops-return-section-0 #eops-return-title").text() shouldBe messages.propertyIncome
        pageDocument.select("#eops-return-section-0 #eops-return-period").text shouldBe messages.fromToDates("1 January 2019", "31 January 2020")
        pageDocument.select("#eops-return-section-0 #eops-due-on-title").text shouldBe messages.dueOn
        pageDocument.select("#eops-return-section-0 #eops-return-due-date").text shouldBe s"1 January 2020"
      }

      "not showing the eops property section when there is no property income report" in new Setup(noIncomeSource) {
        Option(pageDocument.getElementById("eopsPropertyTableRow")) shouldBe None
      }

      "display all of the correct information for the EOPS business section" when {

        "showing heading for a business income source" in new Setup(eopsSEIncomeSource) {
          pageDocument.select("#eops-return-section-0 #eops-return-title").text() shouldBe "business"
        }

        "showing tax year dates" in new Setup(eopsSEIncomeSource) {
          pageDocument.select("#eops-return-section-0 #eops-return-period").text shouldBe messages.fromToDates("6 April 2017", "5 April 2018")
        }

        "showing text due on" in new Setup(eopsSEIncomeSource) {
          pageDocument.select("#eops-return-section-0 #eops-due-on-title").text shouldBe messages.dueOn
        }

        "showing EOPS due date 31 October 2017 for SE income source" in new Setup(eopsSEIncomeSource) {
          pageDocument.select("#eops-return-section-0 #eops-return-due-date").text shouldBe "31 October 2017"
        }

      }

      "display all of the correct information for the quarterly property section" when {

        "showing the property income quarterly return title" in new Setup(piQuarterlyReturnSource) {
          pageDocument.select("#quarterly-return-section-0 #quarterly-return-title").text shouldBe messages.propertyIncome
        }

        "showing the property income quarterly return Due title on the page" in new Setup(piQuarterlyReturnSource) {
          pageDocument.select("#quarterly-return-section-0 #quarterly-due-on-title").text shouldBe messages.dueOn
        }

        "showing the property income quarterly return period on the page" in new Setup(piQuarterlyReturnSource) {
          val result = pageDocument.select("#quarterly-return-section-0 #quarterly-return-period").text
          val expectedResult = "1 July 2017 to 30 September 2017"
          result shouldBe expectedResult
        }

        "showing the property income quarterly return due date" in new Setup(piQuarterlyReturnSource) {
          val result = pageDocument.select("#quarterly-return-section-0 #quarterly-return-due-date").text
          val expectedResult = "30 October 2017"
          result shouldBe expectedResult
        }

        "showing the property income quarterly return due date most recent when there are more then one" in new Setup(twoPiQuarterlyReturnSource) {
          val result = pageDocument.select("#quarterly-return-section-0 #quarterly-return-due-date").text
          val expectedResult = "31 October 2017"
          result shouldBe expectedResult
        }
      }

      "display all of the correct information for the quarterly business section" when {

        "showing the name of the income sources" in new Setup(quarterlyBusinessIncomeSource) {
          pageDocument.select("#quarterly-return-section-0 #quarterly-return-title").text() shouldBe testTradeName
        }

        "showing the period of the income source" in new Setup(quarterlyBusinessIncomeSource) {
          pageDocument.select("#quarterly-return-section-0 #quarterly-return-period").text() shouldBe messages.fromToDates("1 July 2017", "30 September 2017")
        }

        "showing the due date of the income source" in new Setup(quarterlyBusinessIncomeSource) {
          pageDocument.select("#quarterly-return-section-0 #quarterly-return-due-date").text() shouldBe "30 October 2019"
        }
      }


      "display all of the correct information for the crystallised section" when {

        "showing the title of the deadline" in new Setup(crystallisedIncomeSource) {
          pageDocument.select("#crystallised-section-0 #crystallised-title").text shouldBe messages.crystallisedHeading
        }

        "showing the period of the deadline" in new Setup(crystallisedIncomeSource) {
          pageDocument.select("#crystallised-section-0 #crystallised-period").text shouldBe messages.fromToDates("1 October 2017", "30 October 2018")
        }

        "showing the due date of the deadline" in new Setup(crystallisedIncomeSource) {
          pageDocument.select("#crystallised-section-0 #crystallised-due-date").text shouldBe "31 October 2017"
        }
      }

      "display all of the correct information for the crystallised section for multiple crystallised obligations" when {

        "showing the title of the deadline" in new Setup(multiCrystallisedIncomeSource) {
          pageDocument.select("#crystallised-section-0 #crystallised-title").text() shouldBe messages.crystallisedHeading
        }

        "showing the period of the deadline" in new Setup(multiCrystallisedIncomeSource) {
          pageDocument.select("#crystallised-section-0 #crystallised-period").text shouldBe messages.fromToDates("1 October 2017", "30 October 2018")
        }


        "showing the due date of the deadline" in new Setup(multiCrystallisedIncomeSource) {
          pageDocument.select("#crystallised-section-0 #crystallised-due-date").text shouldBe "31 October 2017"
        }

      }


    }
  }
}

