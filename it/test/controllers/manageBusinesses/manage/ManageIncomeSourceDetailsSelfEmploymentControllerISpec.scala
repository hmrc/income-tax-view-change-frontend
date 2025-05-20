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

package controllers.manageBusinesses.manage

import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.{AccountingMethodJourney, DisplayBusinessStartDate, IncomeSourcesNewJourney, NavBarFs, OptInOptOutContentUpdateR17}
import models.incomeSourceDetails.ManageIncomeSourceData.incomeSourceIdField
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._

class ManageIncomeSourceDetailsSelfEmploymentControllerISpec extends ManageIncomeSourceDetailsISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/manage-your-businesses/manage/your-details?id=$thisTestSelfEmploymentIdHashed"
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Manage Self Employment business page" when {
            "URL contains a valid income source ID and user has no latency information" in {
              enable(IncomeSourcesNewJourney, DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse2)
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

              await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              sessionService.getMongoKey(incomeSourceIdField, IncomeSourceJourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessTradingName),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Address"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(addressAsString),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(businessStartDate),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Type of trade"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dd")(businessAccountingMethod)
              )
            }

            "URL contains a valid income source ID and user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
              enable(IncomeSourcesNewJourney, DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              //enable(TimeMachineAddYear)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails2))

              // TODO after reenabling TimeMachine, change the tax year range to 25-26 for the below stub
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2022-23")
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2023-24")
              CalculationListStub.stubGetLegacyCalculationList(testNino, "2023")(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())
              CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              And("Mongo storage is successfully set")
              sessionService.getMongoKey(incomeSourceIdField, IncomeSourceJourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessTradingName),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Address"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(addressAsString),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(businessStartDate),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Type of trade"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dd")(businessAccountingMethod),
                elementTextByID("change-link-1")(""),
                elementTextByID("change-link-2")("")
              )
            }

            "URL contains a valid income source ID and user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
              enable(IncomeSourcesNewJourney, DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails2))
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2022-23")
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2023-24")
              CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
              CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              And("Mongo storage is successfully set")
              sessionService.getMongoKey(incomeSourceIdField, IncomeSourceJourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessTradingName),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Address"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(addressAsString),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(businessStartDate),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Type of trade"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dd")(businessAccountingMethod),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dt")("Reporting frequency 2022 to 2023"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dd")(messagesAnnuallyGracePeriod),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(7)", "dt")("Reporting frequency 2023 to 2024"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(7)", "dd")(messagesQuarterlyGracePeriod),
                elementTextByID("change-link-1")(messagesChangeLinkText),
                elementTextByID("change-link-2")(messagesChangeLinkText)
              )
            }

            "URL has valid income source ID and user has latency information, 1st year Annual 2nd year MTD Mandatory | Voluntary and 2 tax years NC" in {
              enable(IncomeSourcesNewJourney, DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails2))
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual", "2022-23")
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2023-24")
              CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
              CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              And("Mongo storage is successfully set")
              sessionService.getMongoKey(incomeSourceIdField, IncomeSourceJourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessTradingName),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Address"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(addressAsString),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(businessStartDate),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Type of trade"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dd")(businessAccountingMethod),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dt")("Reporting frequency 2022 to 2023"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dd")(messagesAnnuallyGracePeriod),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(7)", "dt")("Reporting frequency 2023 to 2024"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(7)", "dd")(messagesQuarterlyGracePeriod),
                elementTextByID("change-link-1")(""),
                elementTextByID("change-link-2")(messagesChangeLinkText)
              )
            }

            "URL contains a valid income source ID and user has latency information, but itsa status is not mandatory or voluntary" in {
              enable(IncomeSourcesNewJourney, DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWithUnknownsInLatencyPeriod(latencyDetails))
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual", "2022-23")
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual", "2023-24")

              await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              And("Mongo storage is successfully set")
              sessionService.getMongoKey(incomeSourceIdField, IncomeSourceJourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(messagesUnknown),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Address"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(messagesUnknown),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(messagesUnknown),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Type of trade"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dd")("Cash basis accounting")
              )
            }
          }

          "render the correct MTD usage content when OptInOptOutContentUpdateR17 is enabled" in {
            enable(IncomeSourcesNewJourney, DisplayBusinessStartDate, OptInOptOutContentUpdateR17)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails2))
            CalculationListStub.stubGetLegacyCalculationList(testNino, "2023")(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
            CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

            await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

            val result = buildGETMTDClient(path, additionalCookies).futureValue
            sessionService.getMongoKey(incomeSourceIdField, IncomeSourceJourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

            result should have(
              httpStatus(OK),
              pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
              elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Business name"),
              elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessTradingName),
              elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Address"),
              elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(addressAsString),
              elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dt")("Date started"),
              elementTextBySelectorList("#manage-details-table", "div:nth-of-type(3)", "dd")(businessStartDate),
              elementTextBySelectorList("#manage-details-table", "div:nth-of-type(4)", "dt")("Type of trade"),
              elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dt")("Using Making Tax Digital for Income Tax for 2022 to 2023"),
              elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dt")("Using Making Tax Digital for Income Tax for 2023 to 2024"),

              elementTextBySelectorList("#manage-details-table", "div:nth-of-type(5)", "dd")("No"),
              elementTextBySelectorList("#manage-details-table", "div:nth-of-type(6)", "dd")("Yes"),
              elementTextByID("sign-up-link-1")("Sign up"),
              elementTextByID("opt-out-link-2")("Opt out")
            )
          }
        }
        testAuthFailures(path, mtdUserRole)

      }
    }
  }
}
