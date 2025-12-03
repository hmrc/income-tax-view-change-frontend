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

package views.manageBusinesses.add

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import models.incomeSourceDetails.viewmodels.{AddIncomeSourcesViewModel, BusinessDetailsViewModel, CeasedBusinessDetailsViewModel, PropertyDetailsViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BusinessDetailsTestConstants._
import testUtils.TestSupport
import views.html.manageBusinesses.add.AddIncomeSourcesView

class AddIncomeSourcesViewSpec extends TestSupport {
  val addIncomeSources: AddIncomeSourcesView = app.injector.instanceOf[AddIncomeSourcesView]
  val backUrl: String = controllers.routes.HomeController.show().url
  val backUrlAgent: String = controllers.routes.HomeController.showAgent().url


  val viewModelMax: AddIncomeSourcesViewModel = AddIncomeSourcesViewModel(
    soleTraderBusinesses = List(BusinessDetailsViewModel(Some(testTradeName), Some(testStartDate))),
    ukProperty = Some(PropertyDetailsViewModel(Some(testStartDate))),
    foreignProperty = Some(PropertyDetailsViewModel(Some(testStartDate))),
    ceasedBusinesses = List(
      CeasedBusinessDetailsViewModel(Some(testTradeName), SelfEmployment, Some(testStartDate3), testCessation.date.get),
      CeasedBusinessDetailsViewModel(None, UkProperty, Some(testStartDate3), testCessation2.date.get),
      CeasedBusinessDetailsViewModel(None, ForeignProperty, Some(testStartDate3), testCessation3.date.get)
    ),
    displayStartDate = true
  )

  val viewModelMin: AddIncomeSourcesViewModel = AddIncomeSourcesViewModel(
    soleTraderBusinesses = List(BusinessDetailsViewModel(None, None)),
    ukProperty = Some(PropertyDetailsViewModel(None)),
    foreignProperty = Some(PropertyDetailsViewModel(None)),
    ceasedBusinesses = List(
      CeasedBusinessDetailsViewModel(None, SelfEmployment, None, testCessation.date.get),
      CeasedBusinessDetailsViewModel(None, UkProperty, None, testCessation2.date.get)
    ),
    displayStartDate = true
  )


  class Setup(sources: AddIncomeSourcesViewModel, isAgent: Boolean) {
    val view: HtmlFormat.Appendable = if (isAgent) {
      addIncomeSources(sources, isAgent = true, backUrlAgent)
    } else {
      addIncomeSources(sources, isAgent = false, backUrl)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  "AddIncomeSources - Individual" should {
    "display data from optional fields" when {
      "Sole trader trading name and trading start date are present in the response" in new Setup(viewModelMax, isAgent = false) {
        document.getElementById("table-row-trading-name-0").text shouldBe testTradeName
        document.getElementById("table-row-trading-start-date-0").text shouldBe testStartDateFormatted
      }
      "UK property trading start date is present in the response" in new Setup(viewModelMax, isAgent = false) {
        document.getElementById("table-row-trading-start-date-uk").text shouldBe testStartDateFormatted
      }
      "Foreign property trading start date is present in the response" in new Setup(viewModelMax, isAgent = false) {
        document.getElementById("table-row-trading-start-date-foreign").text shouldBe testStartDateFormatted
      }
      "Ceased business trading name and trading start date are present in the response" in new Setup(viewModelMax, isAgent = false) {

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
  }
}


