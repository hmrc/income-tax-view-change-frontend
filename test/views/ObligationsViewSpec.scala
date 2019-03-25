/*
 * Copyright 2019 HM Revenue & Customs
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
import models.incomeSourceDetails.{BusinessDetailsModel, PropertyDetailsModel}
import models.incomeSourcesWithDeadlines.{BusinessIncomeWithDeadlinesModel, IncomeSourcesWithDeadlinesModel, PropertyIncomeWithDeadlinesModel}
import models.reportDeadlines.{EopsObligation, ReportDeadlineModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class ObligationsViewSpec extends TestSupport with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  private def pageSetup(model: IncomeSourcesWithDeadlinesModel) = new {
    lazy val page: HtmlFormat.Appendable = views.html.obligations(model)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

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
        None
    )

    lazy val piQuarterlyReturnSource = IncomeSourcesWithDeadlinesModel(
      List(),
      Some(PropertyIncomeWithDeadlinesModel(
        propertyDetails,
        reportDeadlines  = obligationsDataSuccessModel
      ))
    )

    lazy val twoPiQuarterlyReturnSource = IncomeSourcesWithDeadlinesModel(
      List(),
      Some(PropertyIncomeWithDeadlinesModel(
        propertyDetails,
        reportDeadlines  = quarterlyObligationsDataSuccessModel
      ))
    )


    lazy val quarterlyBusinessIncomeSource = IncomeSourcesWithDeadlinesModel(
      List(
        BusinessIncomeWithDeadlinesModel(
          business1,
          reportDeadlines = ReportDeadlinesModel(List(quarterlyBusinessObligation))
        )
      ),
      None
    )

    lazy val eopsPropertyIncomeSource = IncomeSourcesWithDeadlinesModel(
      List(),
      Some(
        PropertyIncomeWithDeadlinesModel(
          PropertyDetailsModel("testIncomeSource", AccountingPeriodModel(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1)), None, None, None, None),
          ReportDeadlinesModel(
            List(
              ReportDeadlineModel(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 31), LocalDate.of(2020, 1, 1), "EOPS")
            )
          )
        )
      )
    )

    lazy val eopsSEIncomeSource = IncomeSourcesWithDeadlinesModel(businessIncomeSources =
      List(BusinessIncomeWithDeadlinesModel(
        incomeSource = business1,
        reportDeadlines = ReportDeadlinesModel(List(openEOPSObligation))
      )),
      propertyIncomeSource = None)

    lazy val noIncomeSource = IncomeSourcesWithDeadlinesModel(List(), None)

    val setup = pageSetup(businessIncomeSource)
    import setup._

    //Main content section
    "show the breadcrumb trail on the page" in {
      document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
      document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
      document.getElementById("breadcrumb-obligations").text shouldBe breadcrumbMessages.obligations
    }

    s"show the title ${messages.title} on the page" in {
      document.getElementById("page-heading").text shouldBe messages.title
    }


    s"show the Sub heading ${messages.subTitle} on page" in {
      document.getElementById("page-sub-heading").text shouldBe messages.subTitle
    }

    //Declarations section
    "show the heading for the declarations section" in new Setup(eopsPropertyIncomeSource) {
      pageDocument.getElementById("declarations-heading").text shouldBe messages.declarationsHeading
    }

    //Heading and dropdown subsection
    "show the Declaration heading and drop down section on the page" in new Setup(eopsPropertyIncomeSource) {
      pageDocument.getElementById("declaration-dropdown-title").text shouldBe messages.declerationDropDown
      pageDocument.getElementById("declarations-dropdown-list-one").text shouldBe messages.declarationDropdownListOne
      pageDocument.getElementById("declarations-dropdown-list-two").text shouldBe messages.declarationDropdownListTwo
    }

    //Property income EOPS subsection
    "show the eops property income section" in new Setup(eopsPropertyIncomeSource) {
      eopsPropertyIncomeSource.propertyIncomeSource.get.reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations(0).obligationType shouldBe EopsObligation
      pageDocument.getElementById("eops-pi-heading").text shouldBe messages.propertyIncome
      pageDocument.getElementById("eops-pi-dates").text shouldBe messages.fromToDates("1 January 2019", "31 January 2020")
      pageDocument.getElementById("eops-pi-due-on").text shouldBe messages.dueOn
      pageDocument.getElementById("eops-pi-due-date").text shouldBe s"1 January 2020"
    }

    "not show the eops property section when there is no property income report" in new Setup(noIncomeSource) {
      Option(pageDocument.getElementById("eopsPropertyTableRow")) shouldBe None
    }

    //Income source EOPS subsection

    "Show EOPS SE income source" when {
      lazy val setup = new Setup(eopsSEIncomeSource)
      import setup._
    "has heading Whole tax year (final check)" in {
        pageDocument.getElementById("eops-SEI-heading").text shouldBe messages.SEIncomeSource
    }

    "has tax year dates" in  {
      pageDocument.getElementById("eops-SEI-dates").text shouldBe messages.fromToDates("6 April 2017", "5 April 2018")
    }

    "has text due on" in {
      pageDocument.getElementById("eops-SEI-due-on").text shouldBe messages.dueOn
    }

    "has EOPS due date 31 October 2017 for SE income source" in {
      pageDocument.getElementById("eops-SEI-due-date").text shouldBe "31 October 2017"
    }
  }

    //Quarterly returns section

    //Heading and dropdown subsection
    "show the Quarterly heading and drop down section on the page" in {
      document.getElementById("quarterly-dropdown-title").text shouldBe messages.quarterlyDropDown
    }

    //Property income quarterly subsection

   "show the property income quarterly return title" in new Setup(piQuarterlyReturnSource) {
     pageDocument.getElementById("pi-quarterly-return-title").text shouldBe messages.propertyIncome
   }

   "show the property income quarterly return Due title on the page" in new Setup(piQuarterlyReturnSource) {
     pageDocument.getElementById("pi-quarterly-due-on-title").text shouldBe messages.dueOn
   }

   "show the property income quarterly return period on the page" in new Setup(piQuarterlyReturnSource) {
     val result = pageDocument.getElementById("pi-quarterly-return-period").text
     val expectedResult = "1 July 2017 to 30 September 2017"
     result shouldBe expectedResult
   }

   "show the property income quarterly return due date" in new Setup(piQuarterlyReturnSource) {
     val result = pageDocument.getElementById("pi-quarterly-return-due-date").text
     val expectedResult = "30 October 2017"
     result shouldBe expectedResult
   }

    "show the property income quarterly return due date most recent when there are more then one" in new Setup(twoPiQuarterlyReturnSource) {
      val result = pageDocument.getElementById("pi-quarterly-return-period").text
      val expectedResult = "1 July 2017 to 30 September 2017"
      result shouldBe expectedResult
    }

    //Income source quarterly subsection
    "show the name of the income sources" in new Setup(quarterlyBusinessIncomeSource){
      quarterlyBusinessIncomeSource.businessIncomeSources.foreach(incomeSource =>
        pageDocument.getElementById(s"quarterly-bi-${incomeSource.incomeSource.tradingName.get}-heading").text shouldBe incomeSource.incomeSource.tradingName.get)
    }

    "show the period of the income source" in new Setup(quarterlyBusinessIncomeSource) {
        val result = pageDocument.getElementById(s"quarterly-bi-${quarterlyBusinessIncomeSource.businessIncomeSources(0).incomeSource.tradingName.get}-period").text
        val expectedResult =
          quarterlyBusinessIncomeSource.businessIncomeSources(0).reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations(0).start.toLongDate +
            " to " +
            quarterlyBusinessIncomeSource.businessIncomeSources(0).reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations(0).end.toLongDate

      result shouldBe  expectedResult
    }

    "show the due date of the income source" in new Setup(quarterlyBusinessIncomeSource){
      val result = pageDocument.getElementById(s"quarterly-bi-${quarterlyBusinessIncomeSource.businessIncomeSources(0).incomeSource.tradingName.get}-due").text

      val expectedResult = quarterlyBusinessIncomeSource.businessIncomeSources(0).reportDeadlines.asInstanceOf[ReportDeadlinesModel].obligations(0).due.toLongDate
      result shouldBe  expectedResult
    }

  }
}

