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

package controllers.agent.incomeSources.manage

import config.featureswitch.{IncomeSources, TimeMachineAddYear}
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{JourneyType, Manage}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.LatencyDetails
import models.incomeSourceDetails.ManageIncomeSourceData.incomeSourceIdField
import play.api.http.Status.OK
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._

import java.time.LocalDate
import java.time.Month.APRIL

class ManageIncomeSourceDetailsControllerISpec extends ComponentSpecBase {

  val manageSelfEmploymentShowAgentUrl: String = controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(testSelfEmploymentId).url
  val manageUKPropertyShowAgentUrl: String = controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showUkPropertyAgent().url
  val manageForeignPropertyShowAgentUrl: String = controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showForeignPropertyAgent().url
  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd()
  val lastDayOfCurrentTaxYear: LocalDate = LocalDate.of(currentTaxYear, APRIL, 5)
  val taxYear1: Int = currentTaxYear
  val taxYear2: Int = (currentTaxYear + 1)
  val quarterlyIndicator: String = "Q"
  val annuallyIndicator: String = "A"
  val latencyDetails: LatencyDetails = LatencyDetails(
    latencyEndDate = lastDayOfCurrentTaxYear.plusYears(1),
    taxYear1 = taxYear1.toString,
    latencyIndicator1 = quarterlyIndicator,
    taxYear2 = taxYear2.toString,
    latencyIndicator2 = annuallyIndicator
  )
  val latencyDetails2: LatencyDetails = LatencyDetails(
    latencyEndDate = lastDayOfCurrentTaxYear.plusYears(1),
    taxYear1 = taxYear1.toString,
    latencyIndicator1 = annuallyIndicator,
    taxYear2 = taxYear2.toString,
    latencyIndicator2 = quarterlyIndicator
  )
  val addressAsString: String = "64 Zoo Lane Happy Place Magical Land England ZL1 064 United Kingdom"
  val businessTradingName: String = "business"
  val businessStartDate: String = "1 January 2017"
  val businessAccountingMethod: String = "Cash basis accounting"
  val thisTestSelfEmploymentId = "ABC123456789"
  val thisTestSelfEmploymentIdHashed: String = mkIncomeSourceId(thisTestSelfEmploymentId).toHash.hash
  val messagesAnnually: String = messagesAPI("incomeSources.manage.business-manage-details.annually")
  val messagesQuarterly: String = messagesAPI("incomeSources.manage.business-manage-details.quarterly")
  val messagesChangeLinkText: String = messagesAPI("incomeSources.manage.business-manage-details.change")
  val messagesUnknown: String = messagesAPI("incomeSources.generic.unknown")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  s"calling GET $manageSelfEmploymentShowAgentUrl" should {
    "render the Manage Self Employment business page for your client" when {
      "URL contains a valid income source ID and agent's authorised user has no latency information" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        disable(TimeMachineAddYear)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse2)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentIdHashed", clientDetailsWithConfirmation)

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessTradingName),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Business address"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(addressAsString),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Accounting method for sole trader income"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dd")(businessAccountingMethod)
        )
      }
      "URL contains a valid income source ID and agent's authorised user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        enable(TimeMachineAddYear)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2024-25")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, "2023")(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentIdHashed", clientDetailsWithConfirmation)

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessTradingName),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Business address"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(addressAsString),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Accounting method for sole trader income"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dd")(businessAccountingMethod),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dt")("Income reporting method 2022-2023"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dd")(messagesQuarterly),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dt")("Income reporting method 2023-2024"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dd")(messagesAnnually)
        )
      }
      "URL contains a valid income source ID and agent's authorised user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        disable(TimeMachineAddYear)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails2))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentIdHashed", clientDetailsWithConfirmation)

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessTradingName),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Business address"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(addressAsString),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Accounting method for sole trader income"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dd")(businessAccountingMethod),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dt")("Income reporting method 2022-2023"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dd")(messagesAnnually),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dt")("Income reporting method 2023-2024"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dd")(messagesQuarterly),
          elementTextByID("change-link-1")(messagesChangeLinkText),
          elementTextByID("change-link-2")(messagesChangeLinkText)
        )
      }
      "URL contains a valid income source ID and agent's authorised user has latency information, but itsa status is not mandatory or voluntary" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWithUnknownsInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentIdHashed", clientDetailsWithConfirmation)

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(messagesUnknown),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Business address"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(messagesUnknown),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(messagesUnknown),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Accounting method for sole trader income"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dd")("Cash basis accounting")
        )
      }
    }
  }

  s"callingGET $manageUKPropertyShowAgentUrl" should {
    "render the Manage UK Property page" when {
      "URL contains a valid income source ID and authorised user has no latency information" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-uk-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method for UK property income"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        enable(TimeMachineAddYear)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2024-25")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, "2023")(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-uk-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method for UK property income"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        disable(TimeMachineAddYear)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails2))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-uk-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method for UK property income"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod),
          elementTextByID("change-link-1")(messagesChangeLinkText),
          elementTextByID("change-link-2")(messagesChangeLinkText)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, but itsa status is not mandatory or voluntary" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseWithUnknownsInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-uk-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(messagesUnknown),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method for UK property income"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")("Cash basis accounting"),
        )
      }
    }
  }

  s"callingGET $manageForeignPropertyShowAgentUrl" should {
    "render the Manage UK Property page" when {
      "URL contains a valid income source ID and authorised user has no latency information" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-foreign-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method for foreign property income"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        enable(TimeMachineAddYear)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2024-25")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, "2023")(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-foreign-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method for foreign property income"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod),
          elementTextByID("change-link-1")(""),
          elementTextByID("change-link-2")("")
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        disable(TimeMachineAddYear)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails2))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-foreign-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method for foreign property income"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod),
          elementTextByID("change-link-1")(messagesChangeLinkText),
          elementTextByID("change-link-2")(messagesChangeLinkText)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, but itsa status is not mandatory or voluntary" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseWithUnknownsInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-foreign-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(messagesUnknown),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method for foreign property income"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")("Cash basis accounting")
        )
      }
    }
  }
}