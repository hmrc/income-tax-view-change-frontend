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

import enums.IncomeSourceJourney.ForeignProperty
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.{AccountingMethodJourney, DisplayBusinessStartDate, NavBarFs}
import models.incomeSourceDetails.{LatencyDetails, TaxYear}
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._

import java.time.LocalDate

class ManageIncomeSourceDetailsForeignPropertyControllerISpec extends ManageIncomeSourceDetailsISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/manage-your-businesses/manage/your-details-foreign-property"
  }

  val dateNow = LocalDate.now()
  def taxYearEnd: Int = if(dateNow.isAfter(LocalDate.of(dateNow.getYear, 4, 5))) dateNow.getYear + 1 else dateNow.getYear

  mtdAllRoles.foreach { mtdUserRole =>

    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)

    s"GET $path" when {

      s"a user is a $mtdUserRole" that {

        "is authenticated, with a valid enrolment" should {

          "render the Manage Foreign Property page" when {

            "URL contains a valid income source ID and user has no latency information" in {

              enable(DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2022-23")
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2023-24")

              await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
                elementTextByID("up-to-two-tax-years")("")
              )
            }
            "URL contains a valid income source ID and user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
              //enable(TimeMachineAddYear)
              enable(DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

              // TODO after reenabling TimeMachine, change the tax year range to 25-26 for the below stub
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2022-23")
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2023-24")

              CalculationListStub.stubGetLegacyCalculationList(testNino, "2023")(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())
              CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
                elementTextByID("change-link-1")(""),
                elementTextByID("change-link-2")("")
              )
            }
            "URL contains a valid income source ID and user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              val latencyDetailsCty = LatencyDetails(dateNow.plusDays(1), taxYearEnd.toString, "Q", (taxYearEnd + 1).toString, "A")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetailsCty))
              val taxYearShortString1 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear1.toInt).shortenTaxYearEnd
              val taxYearShortString2 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear2.toInt).shortenTaxYearEnd

              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShortString1)
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShortString2)
              CalculationListStub.stubGetLegacyCalculationList(testNino, latencyDetailsCty.taxYear1)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
              CalculationListStub.stubGetCalculationList(testNino, taxYearShortString1)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
                elementTextByID("change-link-1")(messagesChangeLinkText),
                elementTextByID("change-link-2")(messagesChangeLinkText),
                elementTextByID("up-to-two-tax-years")("Because this is still a new business, you can change how often you report for it for up to 2 tax years. From April 2027, you could be required to report quarterly.")
              )
            }

            "URL has valid income source ID and user has latency information, 1st year Annual 2nd year MTD Mandatory | Voluntary and 2 tax years NC" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              val latencyDetailsCty = LatencyDetails(dateNow.plusDays(1), taxYearEnd.toString, "A", (taxYearEnd + 1).toString, "Q")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetailsCty))

              val taxYearShortString1 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear1.toInt).shortenTaxYearEnd
              val taxYearShortString2 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear2.toInt).shortenTaxYearEnd

              ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual", taxYearShortString1)
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShortString2)
              CalculationListStub.stubGetLegacyCalculationList(testNino, latencyDetailsCty.taxYear1)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
                elementTextByID("change-link-1")(""),
                elementTextByID("change-link-2")(messagesChangeLinkText)
              )
            }

            "URL contains a valid income source ID and user has latency information, but itsa status is not mandatory or voluntary" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseWithUnknownsInLatencyPeriod(latencyDetails))

              ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual", "2022-23")
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual", "2023-24")

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(messagesUnknown)
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
