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

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.core.NormalMode
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.manageBusinesses.add.IncomeSourceCheckDetailsView

import java.time.LocalDate

class IncomeSourceCheckDetailsViewSpec extends TestSupport {

  val checkBusinessDetailsView: IncomeSourceCheckDetailsView = app.injector.instanceOf[IncomeSourceCheckDetailsView]

  def businessViewModelMax: CheckDetailsViewModel =
    CheckBusinessDetailsViewModel(
      businessName = Some("Test Business"),
      businessStartDate = Some(LocalDate.of(2022, 1, 1)),
      businessTrade = "Test Trade",
      businessAddressLine1 = "64 Zoo Lane",
      businessPostalCode = Some("ZO0 1AN"),
      accountingPeriodEndDate = LocalDate.of(2022, 1, 1),
      businessAddressLine2 = None,
      businessAddressLine3 = Some("Cbeebies"),
      businessAddressLine4 = None,
      businessCountryCode = Some("United Kingdom")
    )

  def propertyViewModelMax(incomeSourceType: IncomeSourceType): CheckDetailsViewModel =
    CheckPropertyViewModel(
      tradingStartDate = LocalDate.of(2022, 1, 1),
      incomeSourceType = incomeSourceType
    )

  def postAction(incomeSourceType: IncomeSourceType): Call = {
    controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.submit(incomeSourceType)
  }

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType) {

    val businessName = "Test Business"
    val businessStartDate = "1 January 2022"
    val businessTrade = "Test Trade"
    val businessAddressAsString = "64 Zoo Lane Cbeebies ZO0 1AN United Kingdom"

    val backUrl: String = if (isAgent) controllers.routes.HomeController.showAgent().url else
      controllers.routes.HomeController.show().url
    val postAction: Call = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, mode = NormalMode)


    lazy val view: HtmlFormat.Appendable = {
      checkBusinessDetailsView(
        if (incomeSourceType == SelfEmployment) businessViewModelMax else propertyViewModelMax(incomeSourceType),
        isAgent = isAgent,
        postAction = postAction,
        backUrl = backUrl
      )(messages, implicitly)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  def getStartDateMessage(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => "Trading start date"
      case _ => "Start date"
    }
  }

  "IncomeSourceCheckDetails" should {
    "render the page correctly" when {
      def runPageContenttest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
        "render the heading" in new Setup(false, incomeSourceType) {
          document.getElementsByClass("govuk-heading-xl").text() shouldBe "Confirm this information is correct"
        }

        if (incomeSourceType == SelfEmployment) {

          "render the summary list" in new Setup(isAgent, incomeSourceType) {
            document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe "Business name"
            document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe "Trading start date"
            document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe "Type of trade"
            document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe "Address"

            document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe businessName
            document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe businessStartDate
            document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe businessTrade
            document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe businessAddressAsString
          }
        }
        else {
          "render the summary list" in new Setup(isAgent, incomeSourceType) {
            document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe getStartDateMessage(incomeSourceType)

            document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe businessStartDate
          }

        }

        "render the description" in new Setup(isAgent, incomeSourceType) {
          document.getElementById("check-details-description").text() shouldBe "Once you confirm these details, you will not be able to amend them in the next step and will need to contact HMRC to do so."
        }

        "render the back link" in new Setup(isAgent, incomeSourceType) {
          document.getElementById("back-fallback").text() shouldBe "Back"
          document.getElementById("back-fallback").attr("href") shouldBe backUrl

        }
        "render the continue button" in new Setup(isAgent, incomeSourceType) {
          document.getElementById("confirm-button").text() shouldBe "Confirm and continue"
        }
      }

      "individual" when {
        "Self Employment" when {
          runPageContenttest(isAgent = false, SelfEmployment)
        }
        "Uk Property" when {
          runPageContenttest(isAgent = false, UkProperty)
        }
        "Foreign Property" when {
          runPageContenttest(isAgent = false, ForeignProperty)
        }
      }
      "agent" when {
        "Self Employment" when {
          runPageContenttest(isAgent = true, SelfEmployment)
        }
        "Uk Property" when {
          runPageContenttest(isAgent = true, UkProperty)
        }
        "Foreign Property" when {
          runPageContenttest(isAgent = true, ForeignProperty)
        }
      }
    }
  }
}
