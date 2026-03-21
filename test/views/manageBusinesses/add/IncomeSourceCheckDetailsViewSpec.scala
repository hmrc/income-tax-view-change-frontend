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
      businessCountryCode = Some("GB"),
      addressId = Some("testAddressId")
    )

  def addressOnFileSelectedExistingViewModel: CheckBusinessDetailsViewModel =
    CheckBusinessDetailsViewModel(
      businessName = Some("Test Business"),
      businessStartDate = Some(LocalDate.of(2022, 1, 1)),
      businessTrade = "Test Trade",
      accountingPeriodEndDate = LocalDate.of(2022, 1, 1),
      businessAddressLine1 = "15 Market Street",
      businessAddressLine2 = Some("Manchester"),
      businessAddressLine3 = None,
      businessAddressLine4 = None,
      businessPostalCode = Some("M1 1AA"),
      businessCountryCode = Some("GB"),
      addressId = Some("selected-address-id"),
      isAddingNewAddress = false,
      isNoAddressOnFile = false
    )

  def addressOnFileNewUkAddressManualViewModel: CheckBusinessDetailsViewModel =
    CheckBusinessDetailsViewModel(
      businessName = Some("Test Business"),
      businessStartDate = Some(LocalDate.of(2022, 1, 1)),
      businessTrade = "Test Trade",
      accountingPeriodEndDate = LocalDate.of(2022, 1, 1),
      businessAddressLine1 = "10 Downing Street",
      businessAddressLine2 = None,
      businessAddressLine3 = None,
      businessAddressLine4 = None,
      businessPostalCode = Some("SW1A 2AA"),
      businessCountryCode = Some("GB"),
      addressId = None,
      isAddingNewAddress = true,
      isNoAddressOnFile = false
    )

  def addressOnFileNewUkAddressFromLookupViewModel: CheckBusinessDetailsViewModel =
    CheckBusinessDetailsViewModel(
      businessName = Some("Test Business"),
      businessStartDate = Some(LocalDate.of(2022, 1, 1)),
      businessTrade = "Test Trade",
      accountingPeriodEndDate = LocalDate.of(2022, 1, 1),
      businessAddressLine1 = "10 Downing Street",
      businessAddressLine2 = None,
      businessAddressLine3 = None,
      businessAddressLine4 = None,
      businessPostalCode = Some("SW1A 2AA"),
      businessCountryCode = Some("GB"),
      addressId = Some("lookup-result-id"),
      isAddingNewAddress = true,
      isNoAddressOnFile = false
    )

  def addressOnFileNewInternationalAddressViewModel: CheckBusinessDetailsViewModel =
    CheckBusinessDetailsViewModel(
      businessName = Some("Test Business"),
      businessStartDate = Some(LocalDate.of(2022, 1, 1)),
      businessTrade = "Test Trade",
      accountingPeriodEndDate = LocalDate.of(2022, 1, 1),
      businessAddressLine1 = "1 Example Street",
      businessAddressLine2 = Some("Paris"),
      businessAddressLine3 = None,
      businessAddressLine4 = None,
      businessPostalCode = None,
      businessCountryCode = Some("FR"),
      addressId = None,
      isAddingNewAddress = true,
      isNoAddressOnFile = false
    )

  def noAddressOnFileLookupUkViewModel: CheckBusinessDetailsViewModel =
    CheckBusinessDetailsViewModel(
      businessName = Some("Test Business"),
      businessStartDate = Some(LocalDate.of(2022, 1, 1)),
      businessTrade = "Test Trade",
      accountingPeriodEndDate = LocalDate.of(2022, 1, 1),
      businessAddressLine1 = "20 High Street",
      businessAddressLine2 = Some("London"),
      businessAddressLine3 = None,
      businessAddressLine4 = None,
      businessPostalCode = Some("AB1 2CD"),
      businessCountryCode = Some("GB"),
      addressId = Some("lookup-address-id"),
      isAddingNewAddress = false,
      isNoAddressOnFile = true
    )

  def noAddressOnFileManualUkViewModel: CheckBusinessDetailsViewModel =
    CheckBusinessDetailsViewModel(
      businessName = Some("Test Business"),
      businessStartDate = Some(LocalDate.of(2022, 1, 1)),
      businessTrade = "Test Trade",
      accountingPeriodEndDate = LocalDate.of(2022, 1, 1),
      businessAddressLine1 = "5 Test Road",
      businessAddressLine2 = None,
      businessAddressLine3 = None,
      businessAddressLine4 = None,
      businessPostalCode = Some("TE1 1ST"),
      businessCountryCode = Some("GB"),
      addressId = None,
      isAddingNewAddress = false,
      isNoAddressOnFile = true
    )

  def noAddressOnFileManualInternationalViewModel: CheckBusinessDetailsViewModel =
    CheckBusinessDetailsViewModel(
      businessName = Some("Test Business"),
      businessStartDate = Some(LocalDate.of(2022, 1, 1)),
      businessTrade = "Test Trade",
      accountingPeriodEndDate = LocalDate.of(2022, 1, 1),
      businessAddressLine1 = "1 Example Street",
      businessAddressLine2 = Some("Paris"),
      businessAddressLine3 = None,
      businessAddressLine4 = None,
      businessPostalCode = None,
      businessCountryCode = Some("FR"),
      addressId = None,
      isAddingNewAddress = false,
      isNoAddressOnFile = true
    )

  def propertyViewModelMax(incomeSourceType: IncomeSourceType): CheckDetailsViewModel =
    CheckPropertyViewModel(
      tradingStartDate = LocalDate.of(2022, 1, 1),
      incomeSourceType = incomeSourceType
    )

  def postAction(incomeSourceType: IncomeSourceType): Call = {
    controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.submit(incomeSourceType)
  }

  class Setup(isAgent: Boolean,
              incomeSourceType: IncomeSourceType,
              viewModel: CheckDetailsViewModel = null,
              overseasBusinessAddressEnabled: Boolean = false) {

    val businessName = "Test Business"
    val businessStartDate = "1 January 2022"
    val businessTrade = "Test Trade"
    val businessAddressAsString = "64 Zoo Lane Cbeebies ZO0 1AN United Kingdom"

    val backUrl: String = if (isAgent) controllers.routes.HomeController.showAgent().url else
      controllers.routes.HomeController.show().url
    val postAction: Call = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, mode = NormalMode)


    lazy val view: HtmlFormat.Appendable = {
      checkBusinessDetailsView(
        Option(viewModel).getOrElse {
          if (incomeSourceType == SelfEmployment) businessViewModelMax else propertyViewModelMax(incomeSourceType)
        },
        isAgent = isAgent,
        postAction = postAction,
        backUrl = backUrl,
        isTriggeredMigration = false,
        overseasBusinessAddressEnabled = overseasBusinessAddressEnabled
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

          "renders the correct rows when user selects an existing address from their address on file and OverseasBusinessAddress is enabled" in new Setup(
            isAgent = false,
            incomeSourceType = SelfEmployment,
            viewModel = addressOnFileSelectedExistingViewModel,
            overseasBusinessAddressEnabled = true
          ) {
            document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe "Business name"
            document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe "Trading start date"
            document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe "Type of trade"
            document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe "Address"

            document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe "15 Market Street Manchester M1 1AA United Kingdom"

            document.body().text() should not include "Sole trader business address"
            document.body().text() should not include "Is the new address in the UK?"
            document.body().text() should not include "Is the address of your sole trader business in the UK?"
            document.body().text() should not include "Added address for this business"
            document.body().text() should not include "Added international address for this business"

            document.getElementById("change-business-address-link").attr("href") should include("/choose-address")
          }

          "renders the correct rows when user adds a new UK address via postcode lookup from their address on file and OverseasBusinessAddress is enabled" in new Setup(
            isAgent = false,
            incomeSourceType = SelfEmployment,
            viewModel = addressOnFileNewUkAddressFromLookupViewModel,
            overseasBusinessAddressEnabled = true
          ) {
            document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe "Business name"
            document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe "Trading start date"
            document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe "Type of trade"
            document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe "Added address for this business"

            document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe "Test Business"
            document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe "1 January 2022"
            document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe "Test Trade"
            document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe "10 Downing Street SW1A 2AA United Kingdom"

            document.body().text() should not include "Added international address for this business"
            document.body().text() should not include "Address"

            document.getElementById("change-added-business-address-link").attr("href") should include("/choose-address")
          }

          "renders the correct rows when user adds a new UK address manually from their address on file and OverseasBusinessAddress is enabled" in new Setup(
            isAgent = false,
            incomeSourceType = SelfEmployment,
            viewModel = addressOnFileNewUkAddressManualViewModel,
            overseasBusinessAddressEnabled = true
          ) {
            document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe "Business name"
            document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe "Trading start date"
            document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe "Type of trade"
            document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe "Added address for this business"

            document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe "Test Business"
            document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe "1 January 2022"
            document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe "Test Trade"
            document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe "10 Downing Street SW1A 2AA United Kingdom"

            document.body().text() should not include "Added international address for this business"
            document.body().text() should not include "Address"

            document.getElementById("change-added-business-address-link").attr("href") should include("/choose-address")
          }


          "renders the correct rows when user adds a new international address and OverseasBusinessAddress is enabled" in new Setup(
            isAgent = false,
            incomeSourceType = SelfEmployment,
            viewModel = addressOnFileNewInternationalAddressViewModel,
            overseasBusinessAddressEnabled = true
          ) {

            document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe "Business name"
            document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe "Trading start date"
            document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe "Type of trade"

            document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe "Test Business"
            document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe "1 January 2022"
            document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe "Test Trade"

            document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe "Added international address for this business"

            document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe "1 Example Street Paris FR"

            document.body().text() should not include "Address"
            document.body().text() should not include "Added address for this business"

            document.getElementById("change-added-business-address-link").attr("href") should include("/choose-address")
          }

          "renders the correct rows when user has no address on file and finds a UK address via postcode lookup and OverseasBusinessAddress is enabled" in new Setup(
            isAgent = false,
            incomeSourceType = SelfEmployment,
            viewModel = noAddressOnFileLookupUkViewModel,
            overseasBusinessAddressEnabled = true
          ) {
            document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe "Business name"
            document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe "Trading start date"
            document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe "Type of trade"

            document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe "Test Business"
            document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe "1 January 2022"
            document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe "Test Trade"

            document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe "Added address for this business"

            document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe "20 High Street London AB1 2CD United Kingdom"

            document.body().text() should not include "Address"
            document.body().text() should not include "Added international address for this business"

            document.getElementById("change-added-business-address-link").attr("href") should include("/is-the-new-address-in-the-uk")
          }

          "renders the correct rows when user has no address on file and manually enters a UK address and OverseasBusinessAddress is enabled" in new Setup(
            isAgent = false,
            incomeSourceType = SelfEmployment,
            viewModel = noAddressOnFileManualUkViewModel,
            overseasBusinessAddressEnabled = true
          ) {
            document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe "Business name"
            document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe "Trading start date"
            document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe "Type of trade"

            document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe "Test Business"
            document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe "1 January 2022"
            document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe "Test Trade"

            document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe "Added address for this business"

            document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe "5 Test Road TE1 1ST United Kingdom"

            document.body().text() should not include "Address"
            document.body().text() should not include "Added international address for this business"

            document.getElementById("change-added-business-address-link").attr("href") should include("/is-the-new-address-in-the-uk")
          }

          "renders the correct rows when user has no address on file and manually enters an international address and OverseasBusinessAddress is enabled" in new Setup(
            isAgent = false,
            incomeSourceType = SelfEmployment,
            viewModel = noAddressOnFileManualInternationalViewModel,
            overseasBusinessAddressEnabled = true
          ) {
            document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe "Business name"
            document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe "Trading start date"
            document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe "Type of trade"

            document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe "Test Business"
            document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe "1 January 2022"
            document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe "Test Trade"

            document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe "Added international address for this business"

            document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe "1 Example Street Paris FR"

            document.body().text() should not include "Address"
            document.body().text() should not include "Added address for this business"

            document.getElementById("change-added-business-address-link").attr("href") should include("/is-the-new-address-in-the-uk")
          }

          "renders only the Address row with no overseas rows when OverseasBusinessAddress switch is disabled" in new Setup(
            isAgent = false,
            incomeSourceType = SelfEmployment,
            viewModel = addressOnFileNewInternationalAddressViewModel,
            overseasBusinessAddressEnabled = false
          ) {
            document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe "Address"
            document.body().text() should not include "Added international address for this business"

            document.getElementById("change-business-address-link").attr("href") should include("/change-business-address-lookup")
          }
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
