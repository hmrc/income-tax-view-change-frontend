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

package views.incomeSources.add

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.incomeSources.add.IncomeSourceCheckDetails

import java.time.LocalDate

class IncomeSourceCheckDetailsViewSpec extends TestSupport {

  val checkBusinessDetailsView: IncomeSourceCheckDetails = app.injector.instanceOf[IncomeSourceCheckDetails]

  def businessViewModelMax: CheckDetailsViewModel = CheckBusinessDetailsViewModel(
    businessName = Some("Test Business"),
    businessStartDate = Some(LocalDate.of(2022, 1, 1)),
    businessTrade = "Test Trade",
    businessAddressLine1 = "64 Zoo Lane",
    businessPostalCode = Some("ZO0 1AN"),
    incomeSourcesAccountingMethod = Some("ACCRUALS"),
    accountingPeriodEndDate = LocalDate.of(2022, 1, 1),
    businessAddressLine2 = None,
    businessAddressLine3 = Some("Cbeebies"),
    businessAddressLine4 = None,
    businessCountryCode = Some("United Kingdom"),
    cashOrAccrualsFlag = Some("ACCRUALS"),
    showedAccountingMethod = true
  )

  def propertyViewModelMax(incomeSourceType: IncomeSourceType): CheckDetailsViewModel = CheckPropertyViewModel(
    tradingStartDate = LocalDate.of(2022, 1, 1),
    cashOrAccrualsFlag = Some("ACCRUALS"),
    incomeSourceType = incomeSourceType
  )

  def postAction(incomeSourceType: IncomeSourceType): Call = {
    controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.submit(incomeSourceType)
  }

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType, showAccountingMethod: Boolean) {

    val businessName = "Test Business"
    val businessStartDate = "1 January 2022"
    val businessTrade = "Test Trade"
    val businessAddressAsString = "64 Zoo Lane Cbeebies ZO0 1AN United Kingdom"
    val businessAccountingMethod = "Traditional accounting"

    val backUrl: String = if (isAgent) controllers.routes.HomeController.showAgent().url else
      controllers.routes.HomeController.show().url
    val postAction: Call = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)


    lazy val view: HtmlFormat.Appendable = {
      checkBusinessDetailsView(
        if (incomeSourceType == SelfEmployment) businessViewModelMax else propertyViewModelMax(incomeSourceType),
        isAgent = isAgent,
        postAction = postAction,
        backUrl = backUrl,
        displayAccountingMethod = showAccountingMethod
      )(messages, implicitly)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  def getMessage(incomeSourceType: IncomeSourceType, key:String): String = {
    incomeSourceType match {
      case SelfEmployment => messages(s"check-business-details.$key")
      case UkProperty => messages(s"incomeSources.add.checkUKPropertyDetails.$key")
      case ForeignProperty => messages(s"incomeSources.add.foreign-property-check-details.$key")
    }
  }

  "IncomeSourceCheckDetails" should {
    "render the page correctly" when {
      def runPageContentTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Unit = {
        "render the heading" in new Setup(false, incomeSourceType, true) {
          document.getElementsByClass("govuk-heading-l").text() shouldBe messages("check-business-details.title")
        }

        if (incomeSourceType == SelfEmployment) {

          "render the summary list" in new Setup(isAgent, incomeSourceType, true) {
            document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("check-business-details.business-name")
            document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe messages("check-business-details.start-date")
            document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe messages("check-business-details.business-description")
            document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe messages("check-business-details.business-address")
            document.getElementsByClass("govuk-summary-list__key").eq(4).text() shouldBe messages("check-business-details.accounting-method")

            document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe businessName
            document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe businessStartDate
            document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe businessTrade
            document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe businessAddressAsString
            document.getElementsByClass("govuk-summary-list__value").eq(4).text() shouldBe businessAccountingMethod
          }
        }
          else {
          "render the summary list" in new Setup(isAgent, incomeSourceType, true) {
            document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe getMessage(incomeSourceType, "start-date")
            document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe getMessage(incomeSourceType, "accounting-method")

            document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe businessStartDate
            document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe businessAccountingMethod
        }

        }
        "render the back link" in new Setup(isAgent, incomeSourceType, true) {
          document.getElementById("back-fallback").text() shouldBe messages("base.back")
          document.getElementById("back-fallback").attr("href") shouldBe backUrl

        }
        "render the continue button" in new Setup(isAgent, incomeSourceType, true) {
          document.getElementById("confirm-button").text() shouldBe messages("base.confirm-and-continue")
        }
      }

      "individual" when {
        "Self Employment" when {
          runPageContentTest(isAgent = false, SelfEmployment)
        }
        "Uk Property" when {
          runPageContentTest(isAgent = false, UkProperty)
        }
        "Foreign Property" when {
          runPageContentTest(isAgent = false, ForeignProperty)
        }
      }
      "agent" when {
        "Self Employment" when {
          runPageContentTest(isAgent = true, SelfEmployment)
        }
        "Uk Property" when {
          runPageContentTest(isAgent = true, UkProperty)
        }
        "Foreign Property" when {
          runPageContentTest(isAgent = true, ForeignProperty)
        }
      }
    }

    "render the page without the accounting method" when {
      def runPageContentTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Unit = {
        "render the heading" in new Setup(false, incomeSourceType, true) {
          document.getElementsByClass("govuk-heading-l").text() shouldBe messages("check-business-details.title")
        }

        "obfuscate the accounting method" in new Setup(isAgent, incomeSourceType, false) {
          document.getElementsByClass("govuk-summary-list__value").eq(4).text().length shouldBe 0
        }

      }

      "individual" when {
        "Self Employment" when {
          runPageContentTest(isAgent = false, SelfEmployment)
        }
        "Uk Property" when {
          runPageContentTest(isAgent = false, UkProperty)
        }
        "Foreign Property" when {
          runPageContentTest(isAgent = false, ForeignProperty)
        }
      }
      "agent" when {
        "Self Employment" when {
          runPageContentTest(isAgent = true, SelfEmployment)
        }
        "Uk Property" when {
          runPageContentTest(isAgent = true, UkProperty)
        }
        "Foreign Property" when {
          runPageContentTest(isAgent = true, ForeignProperty)
        }
      }
    }
  }
}
