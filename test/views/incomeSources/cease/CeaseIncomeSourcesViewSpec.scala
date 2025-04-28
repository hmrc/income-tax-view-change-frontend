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

package views.incomeSources.cease

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import implicits.ImplicitDateFormatter
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.viewmodels.{CeaseBusinessDetailsViewModel, CeaseIncomeSourcesViewModel, CeasePropertyDetailsViewModel, CeasedBusinessDetailsViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.BusinessDetailsTestConstants._
import testUtils.TestSupport
import views.html.incomeSources.cease.CeaseIncomeSources

class CeaseIncomeSourcesViewSpec extends TestSupport with ImplicitDateFormatter {
  val ceaseIncomeSources: CeaseIncomeSources = app.injector.instanceOf[CeaseIncomeSources]

  class Setup(isAgent: Boolean, missingValues: Boolean = false, displayStartDate: Boolean = true) {
    val viewModel = if (missingValues) {
      CeaseIncomeSourcesViewModel(
        soleTraderBusinesses = List(CeaseBusinessDetailsViewModel( mkIncomeSourceId(testSelfEmploymentId), None, None)),
        ukProperty = Some(CeasePropertyDetailsViewModel(None)),
        foreignProperty = Some(CeasePropertyDetailsViewModel(None)),
        ceasedBusinesses = List(
          CeasedBusinessDetailsViewModel(None, SelfEmployment, None, testCessation.date.get),
          CeasedBusinessDetailsViewModel(None, ForeignProperty, None, testCessation2.date.get),
          CeasedBusinessDetailsViewModel(None, UkProperty, None, testCessation3.date.get)
        ),
        displayStartDate = displayStartDate
      )
    } else {
      CeaseIncomeSourcesViewModel(
        soleTraderBusinesses = List(CeaseBusinessDetailsViewModel(mkIncomeSourceId(testSelfEmploymentId), Some(testTradeName), Some(testStartDate))),
        ukProperty = Some(CeasePropertyDetailsViewModel(Some(testStartDate))),
        foreignProperty = Some(CeasePropertyDetailsViewModel(Some(testStartDate))),
        ceasedBusinesses = List(
          CeasedBusinessDetailsViewModel(tradingName = Some(testTradeName), incomeSourceType = SelfEmployment, tradingStartDate = Some(testStartDate3), cessationDate = testCessation3.date.get),
          CeasedBusinessDetailsViewModel(tradingName = None, incomeSourceType = ForeignProperty, tradingStartDate = Some(testStartDate), cessationDate = testCessation.date.get),
          CeasedBusinessDetailsViewModel(tradingName = None, incomeSourceType = UkProperty, tradingStartDate = Some(testStartDate2), cessationDate = testCessation2.date.get)
        ),
        displayStartDate = displayStartDate
      )
    }

    lazy val view: HtmlFormat.Appendable = if (isAgent) {
      ceaseIncomeSources(
        viewModel,
        true,
        controllers.routes.HomeController.showAgent().url)(implicitly, agentUserConfirmedClient())
    } else {
      ceaseIncomeSources(
        viewModel,
        false,
        controllers.routes.HomeController.show().url)(implicitly, individualUser)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))

    def getCeaseSoleTraderBusinessURL(incomeSourceId: String): String = {
      if (isAgent) controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showAgent(Some(incomeSourceId), SelfEmployment).url
      else controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(Some(incomeSourceId), SelfEmployment).url
    }

    def getCeaseUkPropertyURL: String = {
      if (isAgent) controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.showAgent(UkProperty).url
      else controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.show(UkProperty).url
    }

    def getCeaseForeignPropertyURL: String = {
      if (isAgent) controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.showAgent(ForeignProperty).url
      else controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.show(ForeignProperty).url
    }
  }

  "ceaseIncomeSources - Individual" should {

    val id = mkIncomeSourceId(testSelfEmploymentId).toHash.hash

    "render heading" in new Setup(false) {
      document.getElementById("heading").text() shouldBe "Cease an income source"
    }
    "render Self employment table" when {
      "all fields have a value" in new Setup(false) {
        val table = document.getElementById("sole-trader-businesses-table")

        document.getElementById("self-employment-h1").text() shouldBe "Self employment (sole trader)"
        table.getElementById("table-row-trading-name-0").text() shouldBe testTradeName
        table.getElementById("table-row-trading-start-date-0").text() shouldBe testStartDate.toLongDate
        table.getElementById("cease-link-business-0").attr("href") shouldBe getCeaseSoleTraderBusinessURL(id)
      }

      "unknown is shown on missing fields" in new Setup(false, missingValues = true) {
        val table = document.getElementById("sole-trader-businesses-table")

        document.getElementById("self-employment-h1").text() shouldBe "Self employment (sole trader)"
        table.getElementById("table-row-trading-name-0").text() shouldBe "Unknown"
        table.getElementById("table-row-trading-start-date-0").text() shouldBe "Unknown"
        table.getElementById("cease-link-business-0").attr("href") shouldBe getCeaseSoleTraderBusinessURL(id)
      }

      "display start date is set to false" in new Setup(false, displayStartDate = false) {
        val table = document.getElementById("sole-trader-businesses-table")

        Option(table.getElementById("table-row-trading-start-date-0")) shouldBe None
      }
    }

    "render UK Property table" should {
      "all fields have value" in new Setup(false) {
        val table = document.getElementById("uk-property-table")

        document.getElementById("uk-property-h1").text() shouldBe "UK property"
        document.getElementById("uk-property-p1").text() shouldBe "You should only cease your UK property if you no longer receive any income from it."
        table.getElementById("table-head-date-started-uk").text() shouldBe "Date started"
        table.getElementById("table-row-trading-start-date-uk").text() shouldBe testStartDate.toLongDate
        table.getElementById("cease-link-uk").attr("href") shouldBe getCeaseUkPropertyURL
      }

      "unknown is shown on missing fields" in new Setup(false, missingValues = true) {
        val table = document.getElementById("uk-property-table")

        document.getElementById("uk-property-h1").text() shouldBe "UK property"
        document.getElementById("uk-property-p1").text() shouldBe "You should only cease your UK property if you no longer receive any income from it."
        table.getElementById("table-head-date-started-uk").text() shouldBe "Date started"
        table.getElementById("table-row-trading-start-date-uk").text() shouldBe "Unknown"
        table.getElementById("cease-link-uk").attr("href") shouldBe getCeaseUkPropertyURL
      }

      "display start date is set to false" in new Setup(false, displayStartDate = false) {
        val table = document.getElementById("uk-property-table")

        table.getElementById("table-head-date-started-uk").text() shouldBe "Date started"
        table.getElementById("table-row-trading-start-date-uk").text() shouldBe "Unknown"
      }
    }

    "render Foreign Property table" should {
      "all fields have value" in new Setup(false) {
        val table = document.getElementById("foreign-property-table")

        document.getElementById("foreign-property-h1").text() shouldBe "Foreign property"
        document.getElementById("foreign-property-p1").text() shouldBe "You should only cease your foreign property if you no longer receive any income from it."
        table.getElementById("table-head-date-started-foreign").text() shouldBe "Date started"
        table.getElementById("table-row-trading-start-date-foreign").text() shouldBe testStartDate.toLongDate
        table.getElementById("cease-link-foreign").attr("href") shouldBe getCeaseForeignPropertyURL
      }

      "unknown is shown on missing fields" in new Setup(false, missingValues = true) {
        val table = document.getElementById("foreign-property-table")

        document.getElementById("foreign-property-h1").text() shouldBe "Foreign property"
        document.getElementById("foreign-property-p1").text() shouldBe "You should only cease your foreign property if you no longer receive any income from it."
        table.getElementById("table-head-date-started-foreign").text() shouldBe "Date started"
        table.getElementById("table-row-trading-start-date-foreign").text() shouldBe "Unknown"
        table.getElementById("cease-link-foreign").attr("href") shouldBe getCeaseForeignPropertyURL
      }

      "display start date is set to false" in new Setup(false, displayStartDate = false) {
        val table = document.getElementById("foreign-property-table")

        table.getElementById("table-head-date-started-foreign").text() shouldBe "Date started"
        table.getElementById("table-row-trading-start-date-foreign").text() shouldBe "Unknown"
      }
    }

    "render ceased business table" should {
      "all fields have value" in new Setup(false) {
        val table = document.getElementById("ceased-businesses-table")

        document.getElementById("ceased-businesses-heading").text() shouldBe "Businesses that have ceased"

        table.getElementById("ceased-businesses-table-head-name").text() shouldBe "Business name"
        table.getElementById("ceased-businesses-table-head-date-started").text() shouldBe "Date started"
        table.getElementById("ceased-businesses-table-head-date-ended").text() shouldBe "Date ended"

        table.getElementById("ceased-business-table-row-trading-name-0").text() shouldBe testTradeName
        table.getElementById("ceased-business-table-row-date-started-0").text() shouldBe testStartDate3.toLongDate
        table.getElementById("ceased-business-table-row-date-ended-0").text() shouldBe testCessation3.date.get.toLongDate

        table.getElementById("ceased-business-table-row-trading-name-1").text() shouldBe columnOneUkProperty
        table.getElementById("ceased-business-table-row-date-started-1").text() shouldBe testStartDate2.toLongDate
        table.getElementById("ceased-business-table-row-date-ended-1").text() shouldBe testCessation2.date.get.toLongDate

        table.getElementById("ceased-business-table-row-trading-name-2").text() shouldBe columnOneForeignProperty
        table.getElementById("ceased-business-table-row-date-started-2").text() shouldBe testStartDate.toLongDate
        table.getElementById("ceased-business-table-row-date-ended-2").text() shouldBe testCessation.date.get.toLongDate
      }

      "unknown is shown on missing fields" in new Setup(false, missingValues = true) {
        val table = document.getElementById("ceased-businesses-table")

        document.getElementById("ceased-businesses-heading").text() shouldBe "Businesses that have ceased"

        table.getElementById("ceased-businesses-table-head-name").text() shouldBe "Business name"
        table.getElementById("ceased-businesses-table-head-date-started").text() shouldBe "Date started"
        table.getElementById("ceased-businesses-table-head-date-ended").text() shouldBe "Date ended"

        table.getElementById("ceased-business-table-row-trading-name-0").text() shouldBe columnOneUkProperty
        table.getElementById("ceased-business-table-row-date-started-0").text() shouldBe "Unknown"
        table.getElementById("ceased-business-table-row-date-ended-0").text() shouldBe testCessation3.date.get.toLongDate

        table.getElementById("ceased-business-table-row-trading-name-1").text() shouldBe columnOneForeignProperty
        table.getElementById("ceased-business-table-row-date-started-1").text() shouldBe "Unknown"
        table.getElementById("ceased-business-table-row-date-ended-1").text() shouldBe testCessation2.date.get.toLongDate

        table.getElementById("ceased-business-table-row-trading-name-2").text() shouldBe "Sole trader business"
        table.getElementById("ceased-business-table-row-date-started-2").text() shouldBe "Unknown"
        table.getElementById("ceased-business-table-row-date-ended-2").text() shouldBe testCessation.date.get.toLongDate
      }

      "display start date is set to false" in new Setup(false, displayStartDate = false) {
        val table = document.getElementById("ceased-businesses-table")

        Option(table.getElementById("ceased-businesses-table-head-date-started")) shouldBe None
        Option(table.getElementById("ceased-business-table-row-date-started-0")) shouldBe None
        Option(table.getElementById("ceased-business-table-row-date-started-1")) shouldBe None
        Option(table.getElementById("ceased-business-table-row-date-started-2")) shouldBe None
      }
    }
  }

  "ceaseIncomeSources - Agent" should {

    val id = mkIncomeSourceId(testSelfEmploymentId).toHash.hash

    "render heading" in new Setup(true) {
      document.getElementById("heading").text() shouldBe "Cease an income source"
    }
    "render Self employment table" when {
      "all fields have value" in new Setup(true) {
        val table = document.getElementById("sole-trader-businesses-table")

        document.getElementById("self-employment-h1").text() shouldBe "Self employment (sole trader)"
        table.getElementById("table-row-trading-name-0").text() shouldBe testTradeName
        table.getElementById("table-row-trading-start-date-0").text() shouldBe testStartDate.toLongDate
        table.getElementById("cease-link-business-0").attr("href") shouldBe getCeaseSoleTraderBusinessURL(id)
      }

      "unknown is shown on missing fields" in new Setup(true, missingValues = true) {
        val table = document.getElementById("sole-trader-businesses-table")

        document.getElementById("self-employment-h1").text() shouldBe "Self employment (sole trader)"
        table.getElementById("table-row-trading-name-0").text() shouldBe "Unknown"
        table.getElementById("table-row-trading-start-date-0").text() shouldBe "Unknown"
        table.getElementById("cease-link-business-0").attr("href") shouldBe getCeaseSoleTraderBusinessURL(id)
      }

      "display start date is set to false" in new Setup(false, displayStartDate = false) {
        val table = document.getElementById("sole-trader-businesses-table")

        Option(table.getElementById("table-row-trading-start-date-0")) shouldBe None
      }

    }

    "render UK Property table" should {
      "all fields have value" in new Setup(true) {
        val table = document.getElementById("uk-property-table")

        document.getElementById("uk-property-h1").text() shouldBe "UK property"
        document.getElementById("uk-property-p1").text() shouldBe "You should only cease your UK property if you no longer receive any income from it."
        table.getElementById("table-head-date-started-uk").text() shouldBe "Date started"
        table.getElementById("table-row-trading-start-date-uk").text() shouldBe testStartDate.toLongDate
        table.getElementById("cease-link-uk").attr("href") shouldBe getCeaseUkPropertyURL
      }

      "unknown is shown on missing fields" in new Setup(true, missingValues = true) {
        val table = document.getElementById("uk-property-table")

        document.getElementById("uk-property-h1").text() shouldBe "UK property"
        document.getElementById("uk-property-p1").text() shouldBe "You should only cease your UK property if you no longer receive any income from it."
        table.getElementById("table-head-date-started-uk").text() shouldBe "Date started"
        table.getElementById("table-row-trading-start-date-uk").text() shouldBe "Unknown"
        table.getElementById("cease-link-uk").attr("href") shouldBe getCeaseUkPropertyURL
      }

      "display start date is set to false" in new Setup(false, displayStartDate = false) {
        val table = document.getElementById("uk-property-table")

        table.getElementById("table-head-date-started-uk").text() shouldBe "Date started"
        table.getElementById("table-row-trading-start-date-uk").text() shouldBe "Unknown"
      }
    }

    "render Foreign Property table" should {
      "all fields have value" in new Setup(true) {
        val table = document.getElementById("foreign-property-table")

        document.getElementById("foreign-property-h1").text() shouldBe "Foreign property"
        document.getElementById("foreign-property-p1").text() shouldBe "You should only cease your foreign property if you no longer receive any income from it."
        table.getElementById("table-head-date-started-foreign").text() shouldBe "Date started"
        table.getElementById("table-row-trading-start-date-foreign").text() shouldBe testStartDate.toLongDate
        table.getElementById("cease-link-foreign").attr("href") shouldBe getCeaseForeignPropertyURL
      }

      "unknown is shown on missing fields" in new Setup(true, missingValues = true) {
        val table = document.getElementById("foreign-property-table")

        document.getElementById("foreign-property-h1").text() shouldBe "Foreign property"
        document.getElementById("foreign-property-p1").text() shouldBe "You should only cease your foreign property if you no longer receive any income from it."
        table.getElementById("table-head-date-started-foreign").text() shouldBe "Date started"
        table.getElementById("table-row-trading-start-date-foreign").text() shouldBe "Unknown"
        table.getElementById("cease-link-foreign").attr("href") shouldBe getCeaseForeignPropertyURL
      }

      "display start date is set to false" in new Setup(false, displayStartDate = false) {
        val table = document.getElementById("foreign-property-table")

        table.getElementById("table-head-date-started-foreign").text() shouldBe "Date started"
        table.getElementById("table-row-trading-start-date-foreign").text() shouldBe "Unknown"
      }
    }

    "render ceased business table" should {
      "all fields have value" in new Setup(true) {
        val table = document.getElementById("ceased-businesses-table")

        document.getElementById("ceased-businesses-heading").text() shouldBe "Businesses that have ceased"

        table.getElementById("ceased-businesses-table-head-name").text() shouldBe "Business name"
        table.getElementById("ceased-businesses-table-head-date-started").text() shouldBe "Date started"
        table.getElementById("ceased-businesses-table-head-date-ended").text() shouldBe "Date ended"

        table.getElementById("ceased-business-table-row-trading-name-0").text() shouldBe testTradeName
        table.getElementById("ceased-business-table-row-date-started-0").text() shouldBe testStartDate3.toLongDate
        table.getElementById("ceased-business-table-row-date-ended-0").text() shouldBe testCessation3.date.get.toLongDate

        table.getElementById("ceased-business-table-row-trading-name-1").text() shouldBe columnOneUkProperty
        table.getElementById("ceased-business-table-row-date-started-1").text() shouldBe testStartDate2.toLongDate
        table.getElementById("ceased-business-table-row-date-ended-1").text() shouldBe testCessation2.date.get.toLongDate

        table.getElementById("ceased-business-table-row-trading-name-2").text() shouldBe columnOneForeignProperty
        table.getElementById("ceased-business-table-row-date-started-2").text() shouldBe testStartDate.toLongDate
        table.getElementById("ceased-business-table-row-date-ended-2").text() shouldBe testCessation.date.get.toLongDate
      }

      "unknown is shown on missing fields" in new Setup(true, missingValues = true) {
        val table = document.getElementById("ceased-businesses-table")

        document.getElementById("ceased-businesses-heading").text() shouldBe "Businesses that have ceased"

        table.getElementById("ceased-businesses-table-head-name").text() shouldBe "Business name"
        table.getElementById("ceased-businesses-table-head-date-started").text() shouldBe "Date started"
        table.getElementById("ceased-businesses-table-head-date-ended").text() shouldBe "Date ended"

        table.getElementById("ceased-business-table-row-trading-name-0").text() shouldBe columnOneUkProperty
        table.getElementById("ceased-business-table-row-date-started-0").text() shouldBe "Unknown"
        table.getElementById("ceased-business-table-row-date-ended-0").text() shouldBe testCessation3.date.get.toLongDate

        table.getElementById("ceased-business-table-row-trading-name-1").text() shouldBe columnOneForeignProperty
        table.getElementById("ceased-business-table-row-date-started-1").text() shouldBe "Unknown"
        table.getElementById("ceased-business-table-row-date-ended-1").text() shouldBe testCessation2.date.get.toLongDate

        table.getElementById("ceased-business-table-row-trading-name-2").text() shouldBe "Sole trader business"
        table.getElementById("ceased-business-table-row-date-started-2").text() shouldBe "Unknown"
        table.getElementById("ceased-business-table-row-date-ended-2").text() shouldBe testCessation.date.get.toLongDate
      }

      "display start date is set to false" in new Setup(false, displayStartDate = false) {
        val table = document.getElementById("ceased-businesses-table")

        Option(table.getElementById("ceased-businesses-table-head-date-started")) shouldBe None
        Option(table.getElementById("ceased-business-table-row-date-started-0")) shouldBe None
        Option(table.getElementById("ceased-business-table-row-date-started-1")) shouldBe None
        Option(table.getElementById("ceased-business-table-row-date-started-2")) shouldBe None
      }
    }
  }
}
