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
import helpers.servicemocks.{ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.{AccountingMethodJourney, DisplayBusinessStartDate, NavBarFs}
import models.incomeSourceDetails.{LatencyDetails, TaxYear}
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._

import java.time.LocalDate

class ManageIncomeSourceDetailsUKPropertyControllerISpec extends ManageIncomeSourceDetailsISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/manage-your-businesses/manage/your-details-uk-property"
  }

  val dateNow = LocalDate.now()
  def taxYearEnd: Int = if(dateNow.isAfter(LocalDate.of(dateNow.getYear, 4, 5))) dateNow.getYear + 1 else dateNow.getYear

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Manage UK Property page" when {
            "URL contains a valid income source ID user has no latency information" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney)
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
                elementTextBySelectorList("#manage-details-table", "div:nth-of-type(2)", "dd")(businessAccountingMethod),
                elementTextByID("up-to-two-tax-years")("")
              )
            }
            //Crystallisation doesn't matter, need to cleanup tests after discussion
            "URL contains a valid income source ID and user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              val latencyDetailsCty = LatencyDetails(dateNow.plusDays(1), taxYearEnd.toString, "Q", (taxYearEnd + 1).toString, "A")

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetailsCty))

              val taxYearShortString1 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear1.toInt).shortenTaxYearEnd
              val taxYearShortString2 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear2.toInt).shortenTaxYearEnd

              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShortString1)
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShortString2)

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
              enable(DisplayBusinessStartDate, AccountingMethodJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              val latencyDetailsCty = LatencyDetails(dateNow.plusDays(1), taxYearEnd.toString, "A", (taxYearEnd + 1).toString, "Q")

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetailsCty))

              val taxYearShortString1 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear1.toInt).shortenTaxYearEnd
              val taxYearShortString2 = TaxYear.makeTaxYearWithEndYear(latencyDetailsCty.taxYear2.toInt).shortenTaxYearEnd

              ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual", taxYearShortString1)
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShortString2)

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
              enable(DisplayBusinessStartDate, AccountingMethodJourney)
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
