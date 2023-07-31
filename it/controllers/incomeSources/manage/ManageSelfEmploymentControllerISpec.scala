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

import config.featureswitch.IncomeSources
import forms.incomeSources.add.AddBusinessReportingMethodForm
import helpers.ComponentSpecBase
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.incomeSourceDetails.{IncomeSourceDetailsError, LatencyDetails}
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSelfEmploymentId, testTaxYearRange}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.{singleBusinessResponse, singleBusinessResponseInLatencyPeriod}

import java.time.LocalDate
import java.time.Month.APRIL
class ManageSelfEmploymentControllerISpec extends ComponentSpecBase {

  val manageSelfEmploymentShowUrl: String = controllers.incomeSources.manage.routes.ManageSelfEmploymentController.show(testSelfEmploymentId).url
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
    latencyIndicator2 = quarterlyIndicator
  )
  val addressAsString: String = "64 Zoo Lane, Happy Place, Magical Land, England, ZL1 064, UK"
  val businessTradingName: String = "business"
  val businessStartDate: String = "1 January 2017"
  val businessAccountingMethod: String = "Cash basis accounting"
  val thisTestSelfEmploymentId = "ABC123456789"


  s"calling GET $manageSelfEmploymentShowUrl" should {
    "render the Manage Self Employment business page" when {
      "URL contains a valid income source ID and authorised user has no latency information" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        println("up to here11111111111")

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        println("up to here22222222222")

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        println("up to here33333333333")

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/manage/your-details?id=$thisTestSelfEmploymentId")

        println("LLLLLLLLLLL" + result)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.manage.business-manage-details.heading"),
          elementTextByID("business-name")(businessTradingName),
          elementTextByID("business-address")(addressAsString),
          elementTextByID("business-date-started")(businessStartDate),
          elementTextByID("business-accounting-method")(businessAccountingMethod)

        )
      }
//      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and two tax years crystallised" in {
//        Given("Income Sources FS is enabled")
//        enable(IncomeSources)
//
//        And("API 1525 getIncomeSourceDetails returns a success response")
//        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod(latencyDetails))
//
//        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
//        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")
//
//        And("API 1404 getCalculationList returns a success response")
//        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
//
//        And("API 1896 getCalculationList returns a success response")
//        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
//
//        Then("user is asked to select reporting method for Tax Year 1 and Tax Year 2")
//        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/business-reporting-method?id=$testSelfEmploymentId")
//
//        result should have(
//          httpStatus(OK),
//          pageTitleIndividual("incomeSources.add.businessReportingMethod.heading"),
//          elementTextBySelectorList("#add-business-reporting-method-form", "div:nth-of-type(3)", "legend")(s"Tax year ${taxYear1 - 1}-$taxYear1"),
//          elementTextBySelectorList("#add-business-reporting-method-form", "div:nth-of-type(7)", "legend")(s"Tax year ${taxYear2 - 1}-$taxYear2")
//        )
//      }
      "URL contains a valid income source ID and authorised user has latency information, itsa status mandatory/voluntary and 2 tax years not crystallised" in {

      }
      "URL contains a valid income source ID and authorised user has latency information, but itsa status is not mandatory or voluntary" in {

      }
    }

  }

}
