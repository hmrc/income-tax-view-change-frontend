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

import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.{IncomeSourcesFs, NavBarFs}
import models.incomeSourceDetails.ManageIncomeSourceData.incomeSourceIdField
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.DateService
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._

class ManageIncomeSourceDetailsSelfEmploymentControllerISpec extends ManageIncomeSourceDetailsISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentIdHashed"
  }

  trait Test {
    val dateService: DateService = app.injector.instanceOf[DateService]
    val taxYearShort = dateService.getCurrentTaxYear.shortenTaxYearEnd
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Manage Self Employment business page" when {
            "URL contains a valid income source ID and user has no latency information" in new Test {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse2)
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShort)

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
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(4) dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(4) dd")(businessAccountingMethod)
              )
            }
            "URL contains a valid income source ID and user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in new Test {
              //enable(TimeMachineAddYear)
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails))

              // TODO after reenabling TimeMachine, change the tax year range to 25-26 for the below stub
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShort)
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
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(4) dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(4) dd")(businessAccountingMethod),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(5) dt")("Reporting frequency 2022 to 2023"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(5) dd")(messagesQuarterly),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(6) dt")("Reporting frequency 2023 to 2024"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(6) dd")(messagesAnnually),
                elementTextByID("change-link-1")(""),
                elementTextByID("change-link-2")("")
              )
            }
            "URL contains a valid income source ID and user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in new Test {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod2(latencyDetails2))

              ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYearShort)
              CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
              CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

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
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(4) dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(4) dd")(businessAccountingMethod),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(5) dt")("Reporting frequency 2022 to 2023"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(5) dd")(messagesAnnually),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(6) dt")("Reporting frequency 2023 to 2024"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(6) dd")(messagesQuarterly),
                elementTextByID("change-link-1")(messagesChangeLinkText),
                elementTextByID("change-link-2")(messagesChangeLinkText)
              )
            }
            "URL contains a valid income source ID and user has latency information, but itsa status is not mandatory or voluntary" in new Test {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWithUnknownsInLatencyPeriod(latencyDetails))
              ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual", taxYearShort)

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
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(4) dt")("Accounting method"),
                elementTextBySelectorList("#manage-details-table .govuk-summary-list__row:nth-of-type(4) dd")("Cash basis accounting")
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
