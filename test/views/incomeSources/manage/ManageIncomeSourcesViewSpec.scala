/*
 * Copyright 2025 HM Revenue & Customs
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

package views.incomeSources.manage

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import models.core.IncomeSourceId
import models.incomeSourceDetails.viewmodels.{CeasedBusinessDetailsViewModel, ViewBusinessDetailsViewModel, ViewIncomeSourcesViewModel, ViewPropertyDetailsViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BusinessDetailsTestConstants._
import testUtils.TestSupport
import views.html.incomeSources.manage.ManageIncomeSources

class ManageIncomeSourcesViewSpec extends TestSupport {
  val manageIncomeSources: ManageIncomeSources = app.injector.instanceOf[ManageIncomeSources]
  val backUrl: String = controllers.routes.HomeController.show().url
  val backUrlAgent: String = controllers.routes.HomeController.showAgent().url

  def viewModelMax(displayStartDate: Boolean = true): ViewIncomeSourcesViewModel = ViewIncomeSourcesViewModel(
    viewSoleTraderBusinesses = List(ViewBusinessDetailsViewModel(IncomeSourceId("TestID"), Some("Testing"),Some(testTradeName), Some(testStartDate))),
    viewUkProperty = Some(ViewPropertyDetailsViewModel(Some(testStartDate))),
    viewForeignProperty = Some(ViewPropertyDetailsViewModel(Some(testStartDate))),
    viewCeasedBusinesses = List(
      CeasedBusinessDetailsViewModel(Some(testTradeName), SelfEmployment, Some(testStartDate3), testCessation.date.get),
      CeasedBusinessDetailsViewModel(None, UkProperty, Some(testStartDate3), testCessation2.date.get),
      CeasedBusinessDetailsViewModel(None, ForeignProperty, Some(testStartDate3), testCessation3.date.get)
    ),
    displayStartDate = displayStartDate
  )

  val viewModelMin: ViewIncomeSourcesViewModel = ViewIncomeSourcesViewModel(
    viewSoleTraderBusinesses = List(ViewBusinessDetailsViewModel(IncomeSourceId("TestID"), None, None, None)),
    viewUkProperty = Some(ViewPropertyDetailsViewModel(None)),
    viewForeignProperty = Some(ViewPropertyDetailsViewModel(None)),
    viewCeasedBusinesses = List(
      CeasedBusinessDetailsViewModel(None, SelfEmployment, None, testCessation.date.get),
      CeasedBusinessDetailsViewModel(None, UkProperty, None, testCessation2.date.get)
    ),
    displayStartDate = true
  )

  class Setup(sources: ViewIncomeSourcesViewModel, isAgent: Boolean) {
    val view: HtmlFormat.Appendable = if (isAgent) {
      manageIncomeSources(sources, isAgent = true, backUrlAgent)
    } else {
      manageIncomeSources(sources, isAgent = false, backUrl)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  "ManageIncomeSources - Individual" should {
    "display data from optional fields" when {
      "Sole trader trading name and trading start date are present in the response" in new Setup(viewModelMax(), isAgent = false) {
        document.getElementById("table-row-trading-name-0").text shouldBe testTradeName
        document.getElementById("table-row-trading-start-date-0").text shouldBe testStartDateFormatted
      }
      "UK property trading start date is present in the response" in new Setup(viewModelMax(), isAgent = false) {
        document.getElementById("table-row-trading-start-date-uk").text shouldBe testStartDateFormatted
      }
      "Foreign property trading start date is present in the response" in new Setup(viewModelMax(), isAgent = false) {
        document.getElementById("table-row-trading-start-date-foreign").text shouldBe testStartDateFormatted
      }
      "Ceased business trading name and trading start date are present in the response" in new Setup(viewModelMax(), isAgent = false) {
        document.getElementById("ceased-business-table-row-trading-name-0").text shouldBe columnOneForeignProperty
        document.getElementById("ceased-business-table-row-date-started-0").text shouldBe testStartDate3Formatted
        document.getElementById("ceased-business-table-row-date-ended-0").text shouldBe testCessationDate3

        document.getElementById("ceased-business-table-row-trading-name-1").text shouldBe columnOneUkProperty
        document.getElementById("ceased-business-table-row-date-started-1").text shouldBe testStartDate3Formatted
        document.getElementById("ceased-business-table-row-date-ended-1").text shouldBe testCessationDate2

        document.getElementById("ceased-business-table-row-trading-name-2").text shouldBe testTradeName
        document.getElementById("ceased-business-table-row-date-started-2").text shouldBe testStartDate3Formatted
        document.getElementById("ceased-business-table-row-date-ended-2").text shouldBe testCessationDate
      }
    }

    "display `Unknown` when data missing from optional fields" when {
      "Sole trader trading name and trading start date are not present in the response" in new Setup(viewModelMin, isAgent = false) {
        document.getElementById("table-row-trading-name-0").text shouldBe testUnknownValue
        document.getElementById("table-row-trading-start-date-0").text shouldBe testUnknownValue
      }
      "UK property trading start date is not present in the response" in new Setup(viewModelMin, isAgent = false) {
        document.getElementById("table-row-trading-start-date-uk").text shouldBe testUnknownValue
      }
      "Foreign property trading start date is not present in the response" in new Setup(viewModelMin, isAgent = false) {
        document.getElementById("table-row-trading-start-date-foreign").text shouldBe testUnknownValue
      }
      "Ceased business trading name and trading start date are not present in the response" in new Setup(viewModelMin, isAgent = false) {
        document.getElementById("ceased-business-table-row-trading-name-0").text shouldBe columnOneUkProperty
        document.getElementById("ceased-business-table-row-date-started-0").text shouldBe testUnknownValue
        document.getElementById("ceased-business-table-row-date-ended-0").text shouldBe testCessationDate2

        document.getElementById("ceased-business-table-row-trading-name-1").text shouldBe testUnknownSoleTraderBusinessValue
        document.getElementById("ceased-business-table-row-date-started-1").text shouldBe testUnknownValue
        document.getElementById("ceased-business-table-row-date-ended-1").text shouldBe testCessationDate
      }
    }

    "not display trading start date" when {
      "displayStartDate is set to false" in new Setup(viewModelMax(false), isAgent = false) {
        Option(document.getElementById("table-row-trading-start-date-0")) shouldBe None
        document.getElementById("table-row-trading-start-date-foreign").text() shouldBe testUnknownValue
        document.getElementById("table-row-trading-start-date-uk").text() shouldBe testUnknownValue
      }
    }
  }

  "ManageIncomeSources - Agent" should {
    "display data from optional fields" when {
      "Sole trader trading name and trading start date are present in the response" in new Setup(viewModelMax(), isAgent = true) {
        document.getElementById("table-row-trading-name-0").text shouldBe testTradeName
        document.getElementById("table-row-trading-start-date-0").text shouldBe testStartDateFormatted
      }
      "UK property trading start date is present in the response" in new Setup(viewModelMax(), isAgent = true) {
        document.getElementById("table-row-trading-start-date-uk").text shouldBe testStartDateFormatted
      }
      "Foreign property trading start date is present in the response" in new Setup(viewModelMax(), isAgent = true) {
        document.getElementById("table-row-trading-start-date-foreign").text shouldBe testStartDateFormatted
      }
      "Ceased business trading name and trading start date are present in the response" in new Setup(viewModelMax(), isAgent = true) {
        document.getElementById("ceased-business-table-row-trading-name-0").text shouldBe columnOneForeignProperty
        document.getElementById("ceased-business-table-row-date-started-0").text shouldBe testStartDate3Formatted
        document.getElementById("ceased-business-table-row-date-ended-0").text shouldBe testCessationDate3

        document.getElementById("ceased-business-table-row-trading-name-1").text shouldBe columnOneUkProperty
        document.getElementById("ceased-business-table-row-date-started-1").text shouldBe testStartDate3Formatted
        document.getElementById("ceased-business-table-row-date-ended-1").text shouldBe testCessationDate2

        document.getElementById("ceased-business-table-row-trading-name-2").text shouldBe testTradeName
        document.getElementById("ceased-business-table-row-date-started-2").text shouldBe testStartDate3Formatted
        document.getElementById("ceased-business-table-row-date-ended-2").text shouldBe testCessationDate
      }
    }

    "display `Unknown` when data missing from optional fields" when {
      "Sole trader trading name and trading start date are not present in the response" in new Setup(viewModelMin, isAgent = true) {
        document.getElementById("table-row-trading-name-0").text shouldBe testUnknownValue
        document.getElementById("table-row-trading-start-date-0").text shouldBe testUnknownValue
      }
      "UK property trading start date is not present in the response" in new Setup(viewModelMin, isAgent = true) {
        document.getElementById("table-row-trading-start-date-uk").text shouldBe testUnknownValue
      }
      "Foreign property trading start date is not present in the response" in new Setup(viewModelMin, isAgent = true) {
        document.getElementById("table-row-trading-start-date-foreign").text shouldBe testUnknownValue
      }
      "Ceased business trading name and trading start date are not present in the response" in new Setup(viewModelMin, isAgent = true) {
        document.getElementById("ceased-business-table-row-trading-name-0").text shouldBe columnOneUkProperty
        document.getElementById("ceased-business-table-row-date-started-0").text shouldBe testUnknownValue
        document.getElementById("ceased-business-table-row-date-ended-0").text shouldBe testCessationDate2

        document.getElementById("ceased-business-table-row-trading-name-1").text shouldBe testUnknownSoleTraderBusinessValue
        document.getElementById("ceased-business-table-row-date-started-1").text shouldBe testUnknownValue
        document.getElementById("ceased-business-table-row-date-ended-1").text shouldBe testCessationDate
      }
    }

    "not display trading start date" when {
      "displayStartDate is set to false" in new Setup(viewModelMax(false), isAgent = true) {
        Option(document.getElementById("table-row-trading-start-date-0")) shouldBe None
        document.getElementById("table-row-trading-start-date-foreign").text() shouldBe testUnknownValue
        document.getElementById("table-row-trading-start-date-uk").text() shouldBe testUnknownValue
      }
    }
  }
}
