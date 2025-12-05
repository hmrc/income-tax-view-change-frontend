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
import models.admin.{AccountingMethodJourney, DisplayBusinessStartDate, NavBarFs, OptInOptOutContentUpdateR17}
import models.incomeSourceDetails.ManageIncomeSourceData.incomeSourceIdField
import models.incomeSourceDetails.{LatencyDetails, TaxYear}
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._

import java.time.LocalDate

class ManageIncomeSourceDetailsSelfEmploymentControllerISpec extends ManageIncomeSourceDetailsISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/manage-your-businesses/manage/your-details?id=$thisTestSelfEmploymentIdHashed"
  }

  val dateNow = LocalDate.now()

  def taxYearEnd: Int = if (dateNow.isAfter(LocalDate.of(dateNow.getYear, 4, 5))) dateNow.getYear + 1 else dateNow.getYear

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Manage Self Employment business page" when {
            "URL contains a valid income source ID and user has no latency information" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney)
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
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(1) dt")("Business name"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(1) dd")(businessTradingName),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(2) dt")("Address"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(2) dd")(addressAsString),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(3) dt")("Date started"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(3) dd")(businessStartDate),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(4) dt")("Type of trade"),
                elementTextByID("up-to-two-tax-years")("")
              )
            }

            "URL contains a valid income source ID and user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails2))

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
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(1) dt")("Business name"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(1) dd")(businessTradingName),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(2) dt")("Address"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(2) dd")(addressAsString),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(3) dt")("Date started"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(3) dd")(businessStartDate),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(4) dt")("Type of trade"),
                elementTextByID("change-link-1")(""),
                elementTextByID("change-link-2")("")
              )
            }

            "URL contains a valid income source ID and user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {

              enable(DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              val latencyDetailsCty = LatencyDetails(dateNow.plusDays(1), taxYearEnd.toString, "A", (taxYearEnd + 1).toString, "Q")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetailsCty))
              val taxYearShortString1 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear1.toInt).shortenTaxYearEnd
              val taxYearShortString2 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear2.toInt).shortenTaxYearEnd

              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShortString1)
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShortString2)

              CalculationListStub.stubGetLegacyCalculationList(testNino, latencyDetailsCty.taxYear1)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
              CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

              sessionService.getMongoKey(incomeSourceIdField, IncomeSourceJourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading")
              )
            }

            "URL has valid income source ID and user has latency information, 1st year Annual 2nd year MTD Mandatory | Voluntary and 2 tax years NC" in {

              enable(DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              val latencyDetailsCty = LatencyDetails(dateNow.plusDays(1), taxYearEnd.toString, "A", (taxYearEnd + 1).toString, "Q")

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetailsCty))

              val taxYearShortString1 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear1.toInt).shortenTaxYearEnd
              val taxYearShortString2 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear2.toInt).shortenTaxYearEnd

              ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual", taxYearShortString1)
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShortString2)

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              sessionService.getMongoKey(incomeSourceIdField, IncomeSourceJourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

              result should
                have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading")
                )
            }

            "URL contains a valid income source ID and user has latency information, but itsa status is not mandatory or voluntary" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney)
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
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(1) dt")("Business name"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(1) dd")(messagesUnknown),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(2) dt")("Address"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(2) dd")(messagesUnknown),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(3) dt")("Date started"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(3) dd")(messagesUnknown),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(4) dt")("Type of trade")
              )
            }
          }

          "render the correct MTD usage content when OptInOptOutContentUpdateR17 is enabled" in {
            enable(DisplayBusinessStartDate, OptInOptOutContentUpdateR17)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            val latencyDetailsCty = LatencyDetails(dateNow.plusDays(1), taxYearEnd.toString, "A", (taxYearEnd + 1).toString, "Q")

            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetailsCty))

            val taxYearShortString1 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear1.toInt).shortenTaxYearEnd
            val taxYearShortString2 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear2.toInt).shortenTaxYearEnd

            val taxYear1ToString = s"${latencyDetailsCty.taxYear1.toInt - 1} to ${latencyDetailsCty.taxYear1}"
            val taxYear2ToString = s"${latencyDetailsCty.taxYear2.toInt - 1} to ${latencyDetailsCty.taxYear2}"

            ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShortString1)
            ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShortString2)

            await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

            val result = buildGETMTDClient(path, additionalCookies).futureValue
            sessionService.getMongoKey(incomeSourceIdField, IncomeSourceJourneyType(Manage, SelfEmployment)).futureValue shouldBe Right(Some(thisTestSelfEmploymentId))

            result should have(
              httpStatus(OK),
              pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
              elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(1) dt")("Business name"),
              elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(1) dd")(businessTradingName),
              elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(2) dt")("Address"),
              elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(2) dd")(addressAsString),
              elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(3) dt")("Date started"),
              elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(3) dd")(businessStartDate),
              elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(4) dt")("Type of trade"),
              elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(5) dt")(s"Using Making Tax Digital for Income Tax for $taxYear1ToString"),
              elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(6) dt")(s"Using Making Tax Digital for Income Tax for $taxYear2ToString"),

              elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(5) dd")("No"),
              elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(6) dd")("Yes"),
              elementTextByID("sign-up-link-1")("Sign up"),
              elementTextByID("opt-out-link-2")("Opt out"),
              elementTextByID("up-to-two-tax-years")("Because this is still a new business, for up to 2 tax years you can choose if you want to use Making Tax Digital for Income Tax. From April 2027, you could be required to use the service.")
            )
          }
        }
        testAuthFailures(path, mtdUserRole)

      }
    }
  }
}
