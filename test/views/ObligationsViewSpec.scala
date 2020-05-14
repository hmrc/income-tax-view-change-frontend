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
import assets.BusinessDetailsTestConstants.business1
import assets.Messages.{Breadcrumbs => breadcrumbMessages, Obligations => messages}
import assets.PropertyDetailsTestConstants.propertyDetails
import assets.ReportDeadlinesTestConstants.{twoObligationsSuccessModel, _}
import config.FrontendAppConfig
import implicits.ImplicitDateFormatter
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.PropertyDetailsModel
import models.incomeSourcesWithDeadlines.{BusinessIncomeWithDeadlinesModel, CrystallisedDeadlinesModel, IncomeSourcesWithDeadlinesModel, PropertyIncomeWithDeadlinesModel}
import models.reportDeadlines.{ReportDeadlineModel, ReportDeadlinesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class ObligationsViewSpec extends TestSupport with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  class Setup(model: IncomeSourcesWithDeadlinesModel) {
    val html: HtmlFormat.Appendable = views.html.obligations(model)(FakeRequest(), implicitly, mockAppConfig, testMtdItUser)
    val pageDocument: Document = Jsoup.parse(contentAsString(views.html.obligations(model)))
  }

  "The Deadline Reports Page" should {

    lazy val businessIncomeSource = IncomeSourcesWithDeadlinesModel(
      List(
        BusinessIncomeWithDeadlinesModel(
          business1,
          twoObligationsSuccessModel
        )
      ),
      None,
      None
    )

    lazy val piQuarterlyReturnSource = IncomeSourcesWithDeadlinesModel(
      List(),
      Some(PropertyIncomeWithDeadlinesModel(
        propertyDetails,
        reportDeadlines = reportDeadlinesDataSelfEmploymentSuccessModel
      )),
      None
    )

    lazy val twoPiQuarterlyReturnSource = IncomeSourcesWithDeadlinesModel(
      List(),
      Some(PropertyIncomeWithDeadlinesModel(
        propertyDetails,
        reportDeadlines = quarterlyObligationsDataSuccessModel
      )),
      None
    )


    lazy val quarterlyBusinessIncomeSource = IncomeSourcesWithDeadlinesModel(
      List(
        BusinessIncomeWithDeadlinesModel(
          business1,
          reportDeadlines = ReportDeadlinesModel(business1.incomeSourceId, List(quarterlyBusinessObligation))
        )
      ),
      None,
      None
    )

    lazy val eopsPropertyIncomeSource = IncomeSourcesWithDeadlinesModel(
      List(),
      Some(
        PropertyIncomeWithDeadlinesModel(
          PropertyDetailsModel("testIncomeSource", AccountingPeriodModel(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1)), None, None, None, None),
          ReportDeadlinesModel("testIncomeSource",
            List(
              ReportDeadlineModel(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 31), LocalDate.of(2020, 1, 1), "EOPS", None, "EOPS")
            )
          )
        )
      ),
      None
    )

    lazy val crystallisedIncomeSource = IncomeSourcesWithDeadlinesModel(
      List(BusinessIncomeWithDeadlinesModel(
        business1,
        ReportDeadlinesModel(business1.incomeSourceId, List(crystallisedObligation))
      )),
      None,
      crystallisedDeadlinesModel = Some(CrystallisedDeadlinesModel(ReportDeadlinesModel(testMtdItUser.mtditid, List(crystallisedObligation))))
    )


    lazy val multiCrystallisedIncomeSource = IncomeSourcesWithDeadlinesModel(
      List(BusinessIncomeWithDeadlinesModel(
        business1,
        ReportDeadlinesModel(business1.incomeSourceId, List(crystallisedObligation))
      )),
      None,
      crystallisedDeadlinesModel = Some(CrystallisedDeadlinesModel(ReportDeadlinesModel(testMtdItUser.mtditid, List(crystallisedObligationTwo, crystallisedObligation))))
    )


    lazy val eopsSEIncomeSource = IncomeSourcesWithDeadlinesModel(businessIncomeSources =
      List(BusinessIncomeWithDeadlinesModel(
        incomeSource = business1,
        reportDeadlines = ReportDeadlinesModel(business1.incomeSourceId, List(openEOPSObligation))
      )),
      propertyIncomeSource = None,
      None)

    lazy val noIncomeSource = IncomeSourcesWithDeadlinesModel(List(), None, None)

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
        eopsPropertyIncomeSource.propertyIncomeSource.get.reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations.head.obligationType shouldBe "EOPS"
        pageDocument.getElementById("eops-pi-heading").text shouldBe messages.propertyIncome
        pageDocument.getElementById("eops-pi-dates").text shouldBe messages.fromToDates("1 January 2019", "31 January 2020")
        pageDocument.getElementById("eops-pi-due-on").text shouldBe messages.dueOn
        pageDocument.getElementById("eops-pi-due-date").text shouldBe s"1 January 2020"
      }

      "not showing the eops property section when there is no property income report" in new Setup(noIncomeSource) {
        Option(pageDocument.getElementById("eopsPropertyTableRow")) shouldBe None
      }

    "display all of the correct information for the EOPS business section" when {

      "showing heading Whole tax year (final check)" in new Setup(eopsSEIncomeSource) {
        eopsPropertyIncomeSource.businessIncomeSources.foreach(
          businessIncomeSource => pageDocument.getElementById(s"eops-SEI-${businessIncomeSource.incomeSource.tradingName}-heading").text
            shouldBe businessIncomeSource.incomeSource.tradingName
        )
      }

      }

      "showing tax year dates" in new Setup(eopsSEIncomeSource) {
        pageDocument.getElementById("eops-SEI-dates").text shouldBe messages.fromToDates("6 April 2017", "5 April 2018")
      }

      "showing text due on" in new Setup(eopsSEIncomeSource) {
        pageDocument.getElementById("eops-SEI-due-on").text shouldBe messages.dueOn
      }

      "showing EOPS due date 31 October 2017 for SE income source" in new Setup(eopsSEIncomeSource) {
        pageDocument.getElementById("eops-SEI-due-date").text shouldBe "31 October 2017"
      }

    }

    "display all of the correct information for the quarterly property section" when {

      "showing the property income quarterly return title" in new Setup(piQuarterlyReturnSource) {
        pageDocument.getElementById("pi-quarterly-return-title").text shouldBe messages.propertyIncome
      }

      "showing the property income quarterly return Due title on the page" in new Setup(piQuarterlyReturnSource) {
        pageDocument.getElementById("pi-quarterly-due-on-title").text shouldBe messages.dueOn
      }

      "showing the property income quarterly return period on the page" in new Setup(piQuarterlyReturnSource) {
        val result = pageDocument.getElementById("pi-quarterly-return-period").text
        val expectedResult = "1 July 2017 to 30 September 2017"
        result shouldBe expectedResult
      }

      "showing the property income quarterly return due date" in new Setup(piQuarterlyReturnSource) {
        val result = pageDocument.getElementById("pi-quarterly-return-due-date").text
        val expectedResult = "30 October 2017"
        result shouldBe expectedResult
      }

      "showing the property income quarterly return due date most recent when there are more then one" in new Setup(twoPiQuarterlyReturnSource) {
        val result = pageDocument.getElementById("pi-quarterly-return-period").text
        val expectedResult = "1 July 2017 to 30 September 2017"
        result shouldBe expectedResult
      }
    }

    "display all of the correct information for the quarterly business section" when {

      "showing the name of the income sources" in new Setup(quarterlyBusinessIncomeSource) {
        quarterlyBusinessIncomeSource.businessIncomeSources.foreach(incomeSource =>
          pageDocument.getElementById(s"quarterly-bi-${incomeSource.incomeSource.tradingName.get}-heading").text shouldBe incomeSource.incomeSource.tradingName.get)
      }

      "showing the period of the income source" in new Setup(quarterlyBusinessIncomeSource) {
        val result = pageDocument.getElementById(s"quarterly-bi-${quarterlyBusinessIncomeSource.businessIncomeSources(0).incomeSource.tradingName.get}-period").text
        val expectedResult =
          quarterlyBusinessIncomeSource.businessIncomeSources(0).reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations(0).start.toLongDate +
            " to " +
            quarterlyBusinessIncomeSource.businessIncomeSources(0).reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations(0).end.toLongDate

        result shouldBe expectedResult
      }

      "showing the due date of the income source" in new Setup(quarterlyBusinessIncomeSource) {
        val result = pageDocument.getElementById(s"quarterly-bi-${quarterlyBusinessIncomeSource.businessIncomeSources(0).incomeSource.tradingName.get}-due").text

        val expectedResult = quarterlyBusinessIncomeSource.businessIncomeSources(0).reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations(0).due.toLongDate
        result shouldBe expectedResult
      }
    }



    "display all of the correct information for the crystallised section" when {

      "showing the title of the deadline" in new Setup(crystallisedIncomeSource){
        val result = pageDocument.getElementById("crystallised-heading").text
        val expectedResult = messages.crystallisedHeading

        result shouldBe expectedResult
      }

      "showing the period of the deadline" in new Setup(crystallisedIncomeSource){
        val result = pageDocument.getElementById("crystallised-period").text
        val expectedResult = crystallisedIncomeSource.crystallisedDeadlinesModel.get.reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations.head.start.toLongDate +
          " to " +
          crystallisedIncomeSource.crystallisedDeadlinesModel.get.reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations.head.end.toLongDate


        result shouldBe expectedResult
      }


      "showing the due date of the deadline" in new Setup(crystallisedIncomeSource){
        val result = pageDocument.getElementById("crystallised-due").text
        val expectedResult = crystallisedIncomeSource.crystallisedDeadlinesModel.get.reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations.head.due.toLongDate

        result shouldBe expectedResult
      }

    }

    "display all of the correct information for the crystallised section for multiple crystallised obligations" when {

      "showing the title of the deadline" in new Setup(multiCrystallisedIncomeSource){
        val result = pageDocument.getElementById("crystallised-heading").text
        val expectedResult = messages.crystallisedHeading

        result shouldBe expectedResult
      }

      "showing the period of the deadline" in new Setup(multiCrystallisedIncomeSource){
        val result = pageDocument.getElementById("crystallised-period").text
        val expectedResult = multiCrystallisedIncomeSource.crystallisedDeadlinesModel.get.reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations(1).start.toLongDate +
          " to " +
          multiCrystallisedIncomeSource.crystallisedDeadlinesModel.get.reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations(1).end.toLongDate


        result shouldBe expectedResult
      }


      "showing the due date of the deadline" in new Setup(multiCrystallisedIncomeSource){
        val result = pageDocument.getElementById("crystallised-due").text
        val expectedResult = multiCrystallisedIncomeSource.crystallisedDeadlinesModel.get.reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations(1).due.toLongDate

        result shouldBe expectedResult
      }

    }



  }
}

