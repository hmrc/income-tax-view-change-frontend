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

import enums.IncomeSourceJourney.{SelfEmployment, UkProperty}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.{AccountingMethodJourney, DisplayBusinessStartDate, IncomeSourcesNewJourney, NavBarFs}
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._

class ManageIncomeSourceDetailsUKPropertyControllerISpec extends ManageIncomeSourceDetailsISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/manage-your-businesses/manage/your-details-uk-property"
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Manage UK Property page" when {
            "URL contains a valid income source ID user has no latency information" in {
              enable(IncomeSourcesNewJourney)
              enable(DisplayBusinessStartDate)
              enable(AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

              await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod)
              )
            }
            "URL contains a valid income source ID and user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
              //enable(TimeMachineAddYear)
              enable(IncomeSourcesNewJourney)
              enable(DisplayBusinessStartDate)
              enable(AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

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
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod),
                elementTextByID("change-link-1")(""),
                elementTextByID("change-link-2")("")
              )
            }
            "URL contains a valid income source ID and user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
              enable(IncomeSourcesNewJourney)
              enable(DisplayBusinessStartDate)
              enable(AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails2))
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2022-23")
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2023-24")
              CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
              CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod),
                elementTextByID("change-link-1")(messagesChangeLinkText),
                elementTextByID("change-link-2")(messagesChangeLinkText)
              )
            }

            "URL has valid income source ID and user has latency information, 1st year Annual 2nd year MTD Mandatory | Voluntary and 2 tax years NC" in {
              enable(IncomeSourcesNewJourney)
              enable(DisplayBusinessStartDate)
              enable(AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails2))

              ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual", "2022-23")
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2023-24")
              CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
              CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(businessStartDate),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod),
                elementTextByID("change-link-1")(""),
                elementTextByID("change-link-2")(messagesChangeLinkText)
              )
            }

            "URL contains a valid income source ID and user has latency information, but itsa status is not mandatory or voluntary" in {
              enable(IncomeSourcesNewJourney)
              enable(DisplayBusinessStartDate)
              enable(AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseWithUnknownsInLatencyPeriod(latencyDetails))
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual", "2022-23")
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual", "2023-24")

              await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.manage.business-manage-details.heading"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dt")("Date started"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(1)", "dd")(messagesUnknown),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")("Cash basis accounting"),
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
