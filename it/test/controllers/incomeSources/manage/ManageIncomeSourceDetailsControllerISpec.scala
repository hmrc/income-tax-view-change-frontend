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

package controllers.incomeSources.manage

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{JourneyType, Manage}
import helpers.ComponentSpecBase
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.IncomeSources
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.ManageIncomeSourceData.incomeSourceIdField
import models.incomeSourceDetails.{LatencyDetails, UIJourneySessionData}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._

import java.time.LocalDate
import java.time.Month.APRIL

class ManageIncomeSourceDetailsControllerISpec extends ComponentSpecBase with GuiceOneAppPerTest  {

  val manageSelfEmploymentShowUrl: String = controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = false, SelfEmployment, Some(testSelfEmploymentId)).url
  val manageUKPropertyShowUrl: String = controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = false, UkProperty, None).url
  val manageForeignPropertyShowUrl: String = controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = false, ForeignProperty, None).url
  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd
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

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Manage, incomeSourceType).toString)

  s"calling GET $manageSelfEmploymentShowUrl" should {
    "render the Manage Self Employment business page" when {
      "URL contains a valid income source ID and authorised user has no latency information" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse2)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentIdHashed")

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessTradingName),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Address"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(addressAsString),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Accounting method"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dd")(businessAccountingMethod)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        //enable(TimeMachineAddYear)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails))

        // TODO after reenabling TimeMachine, change the tax year range to 25-26 for the below stub
        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2024-25")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, "2023")(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentIdHashed")

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessTradingName),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Address"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(addressAsString),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Accounting method"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dd")(businessAccountingMethod),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dt")("Reporting frequency 2022 to 2023"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dd")(messagesQuarterly),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dt")("Reporting frequency 2023 to 2024"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dd")(messagesAnnually),
          elementTextByID("change-link-1")(""),
          elementTextByID("change-link-2")("")
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails2))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentIdHashed")

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessTradingName),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Address"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(addressAsString),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Accounting method"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dd")(businessAccountingMethod),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dt")("Reporting frequency 2022 to 2023"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dd")(messagesAnnually),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dt")("Reporting frequency 2023 to 2024"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dd")(messagesQuarterly),
          elementTextByID("change-link-1")(messagesChangeLinkText),
          elementTextByID("change-link-2")(messagesChangeLinkText)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, but itsa status is not mandatory or voluntary" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWithUnknownsInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual")

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentIdHashed")

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(messagesUnknown),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Address"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(messagesUnknown),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(messagesUnknown),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Accounting method"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dd")("Cash basis accounting")
        )
      }
    }
  }

  s"calling GET $manageUKPropertyShowUrl" should {
    "render the Manage UK Property page" when {
      "URL contains a valid income source ID and authorised user has no latency information" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-uk-property")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        //enable(TimeMachineAddYear)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

        // TODO after reenabling TimeMachine, change the tax year range to 25-26 for the below stub
        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2024-25")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, "2023")(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-uk-property")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod),
          elementTextByID("change-link-1")(""),
          elementTextByID("change-link-2")("")
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails2))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-uk-property")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod),
          elementTextByID("change-link-1")(messagesChangeLinkText),
          elementTextByID("change-link-2")(messagesChangeLinkText)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, but itsa status is not mandatory or voluntary" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseWithUnknownsInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual")

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-uk-property")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(messagesUnknown),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")("Cash basis accounting"),
        )
      }
    }
  }

  s"callingGET $manageForeignPropertyShowUrl" should {
    "render the Manage Foreign Property page" when {
      "URL contains a valid income source ID and authorised user has no latency information" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-foreign-property")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        //enable(TimeMachineAddYear)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

        // TODO after reenabling TimeMachine, change the tax year range to 25-26 for the below stub
        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2024-25")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, "2023")(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-foreign-property")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod),
          elementTextByID("change-link-1")(""),
          elementTextByID("change-link-2")("")
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails2))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getCalculationList returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-foreign-property")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod),
          elementTextByID("change-link-1")(messagesChangeLinkText),
          elementTextByID("change-link-2")(messagesChangeLinkText)
        )
      }
      "URL contains a valid income source ID and authorised user has latency information, but itsa status is not mandatory or voluntary" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseWithUnknownsInLatencyPeriod(latencyDetails))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details-foreign-property")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(messagesUnknown),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
          elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")("Cash basis accounting")
        )
      }
    }
  }
}
