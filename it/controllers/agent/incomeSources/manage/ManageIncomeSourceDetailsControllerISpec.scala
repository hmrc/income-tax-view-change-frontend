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

import helpers.agent.ComponentSpecBase
import config.featureswitch.IncomeSources
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.incomeSourceDetails.LatencyDetails
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testNino, testSelfEmploymentId, testTaxYearRange}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyOnlyResponse, singleBusinessResponse, singleBusinessResponse2, singleBusinessResponseInLatencyPeriod, singleBusinessResponseInLatencyPeriod2, singleBusinessResponseWithUnknownsInLatencyPeriod, singleForeignPropertyResponseInLatencyPeriod, singleForeignPropertyResponseWithUnknownsInLatencyPeriod, singleUKPropertyResponseInLatencyPeriod, singleUKPropertyResponseWithUnknownsInLatencyPeriod, ukPropertyOnlyResponse}

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
  val messagesAnnually: String = messagesAPI("incomeSources.manage.business-manage-details.annually")
  val messagesQuarterly: String = messagesAPI("incomeSources.manage.business-manage-details.quarterly")
  val messagesChangeLinkText: String = messagesAPI("incomeSources.manage.business-manage-details.change")
  val messagesUnknown: String = messagesAPI("incomeSources.generic.unknown")

  s"calling GET $manageSelfEmploymentShowAgentUrl" should {
    "render the Manage Self Employment business page for your client" when {
      "URL contains a valid income source ID and agent's authorised user has no latency information" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse2)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentId", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextByID("business-name")(businessTradingName),
          elementTextByID("business-address")(addressAsString),
          elementTextByID("business-date-started")(businessStartDate),
          elementTextByID("business-accounting-method")(businessAccountingMethod)
        )
      }
      "URL contains a valid income source ID and agent's authorised user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentId", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextByID("business-name")(businessTradingName),
          elementTextByID("business-address")(addressAsString),
          elementTextByID("business-date-started")(businessStartDate),
          elementTextByID("business-accounting-method")(businessAccountingMethod),
          elementTextByID("reporting-method-1")(messagesQuarterly),
          elementTextByID("reporting-method-2")(messagesAnnually)
        )
      }
      "URL contains a valid income source ID and agent's authorised user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails2))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentId", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextByID("business-name")(businessTradingName),
          elementTextByID("business-address")(addressAsString),
          elementTextByID("business-date-started")(businessStartDate),
          elementTextByID("business-accounting-method")(businessAccountingMethod),
          elementTextByID("reporting-method-1")(messagesAnnually),
          elementTextByID("reporting-method-2")(messagesQuarterly),
          elementTextByID("change-link-1")(messagesChangeLinkText),
          elementTextByID("change-link-2")(messagesChangeLinkText)
        )
      }
      "URL contains a valid income source ID and agent's authorised user has latency information, but itsa status is not mandatory or voluntary" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWithUnknownsInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentId", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextByID("business-name")(messagesUnknown),
          elementTextByID("business-address")(messagesUnknown),
          elementTextByID("business-date-started")(messagesUnknown),
          elementTextByID("business-accounting-method")(messagesUnknown),
          elementTextByID("reporting-method-1")(""),
          elementTextByID("reporting-method-2")("")
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

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-uk-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextByID("business-date-started")(businessStartDate),
          elementTextByID("business-accounting-method")(businessAccountingMethod)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)


        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-uk-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextByID("business-date-started")(businessStartDate),
          elementTextByID("business-accounting-method")(businessAccountingMethod),
          elementTextByID("change-link-1")(""),
          elementTextByID("change-link-2")("")
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1525 getIncomeSourceDetails returns a success response")
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
          elementTextByID("business-date-started")(businessStartDate),
          elementTextByID("business-accounting-method")(businessAccountingMethod),
          elementTextByID("change-link-1")(messagesChangeLinkText),
          elementTextByID("change-link-2")(messagesChangeLinkText)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, but itsa status is not mandatory or voluntary" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseWithUnknownsInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-uk-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextByID("business-date-started")(messagesUnknown),
          elementTextByID("business-accounting-method")(messagesUnknown)
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

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-foreign-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextByID("business-date-started")(businessStartDate),
          elementTextByID("business-accounting-method")(businessAccountingMethod)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)


        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-foreign-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextByID("business-date-started")(businessStartDate),
          elementTextByID("business-accounting-method")(businessAccountingMethod),
          elementTextByID("change-link-1")(""),
          elementTextByID("change-link-2")("")
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1525 getIncomeSourceDetails returns a success response")
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
          elementTextByID("business-date-started")(businessStartDate),
          elementTextByID("business-accounting-method")(businessAccountingMethod),
          elementTextByID("change-link-1")(messagesChangeLinkText),
          elementTextByID("change-link-2")(messagesChangeLinkText)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, but itsa status is not mandatory or voluntary" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseWithUnknownsInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-foreign-property", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.manage.business-manage-details.heading"),
          elementTextByID("business-date-started")(messagesUnknown),
          elementTextByID("business-accounting-method")(messagesUnknown)
        )
      }
    }
  }
}
