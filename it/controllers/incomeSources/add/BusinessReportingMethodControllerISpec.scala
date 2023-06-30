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

package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import forms.incomeSources.add.AddBusinessReportingMethodForm
import helpers.ComponentSpecBase
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.incomeSourceDetails.{IncomeSourceDetailsError, LatencyDetails}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testTaxYearRange}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.{singleBusinessResponse, singleBusinessResponseInLatencyPeriod}

import java.time.LocalDate
import java.time.Month.APRIL

class BusinessReportingMethodControllerISpec extends ComponentSpecBase {
  val validIncomeSourceId = "ABC123456789"
  val businessReportingMethodShowUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.show(validIncomeSourceId).url
  val businessReportingMethodSubmitUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.submit(validIncomeSourceId).url
  val businessAddedShowUrl: String = controllers.incomeSources.add.routes.BusinessAddedController.show(validIncomeSourceId).url
  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd()
  val lastDayOfCurrentTaxYear: LocalDate = LocalDate.of(currentTaxYear, APRIL, 5)
  val taxYear1: Int = (currentTaxYear + 1)
  val taxYear2: Int = (currentTaxYear + 2)
  val quarterlyIndicator: String = "Q"
  val annuallyIndicator: String = "A"
  val latencyDetails: LatencyDetails = LatencyDetails(
    latencyEndDate = lastDayOfCurrentTaxYear.plusYears(2),
    taxYear1 = taxYear1.toString,
    latencyIndicator1 = quarterlyIndicator,
    taxYear2 = taxYear2.toString,
    latencyIndicator2 = quarterlyIndicator
  )

  s"calling GET $businessReportingMethodShowUrl" should {
    "render the Business Reporting Method page" when {
      "URL contains a valid income source ID and authorised user is within latency period (Tax Year 1 NOT crystallised)" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getBusinessDetails returns a success response with latency details")
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = OK,
          response = Json.toJson(singleBusinessResponseInLatencyPeriod(latencyDetails))
        )

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        Then("user is asked to select reporting method for Tax Year 1 and Tax Year 2")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/business-reporting-method?id=$validIncomeSourceId")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.add.businessReportingMethod.heading"),
          elementTextBySelectorList("#add-business-reporting-method-form", "p:nth-of-type(1)")(s"Tax year ${taxYear1 - 1}-$taxYear1"),
          elementTextBySelectorList("#add-business-reporting-method-form", "p:nth-of-type(2)")(s"Tax year ${taxYear2 - 1}-$taxYear2")
        )
      }
      "URL contains a valid income source ID and authorised user is within latency period (Tax Year 1 is crystallised)" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getBusinessDetails returns a success response with latency details")
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = OK,
          response = Json.toJson(singleBusinessResponseInLatencyPeriod(latencyDetails))
        )

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        Then("as Tax Year 1 is crystallised, user is asked to select reporting method for Tax Year 2")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/business-reporting-method?id=$validIncomeSourceId")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.add.businessReportingMethod.heading"),
          elementTextBySelectorList("#add-business-reporting-method-form", "p:nth-of-type(1)")(s"Tax year ${taxYear2 - 1}-$taxYear2"),
          elementCountBySelector("#add-business-reporting-method-form > p:nth-of-type(2)")(0)
        )
      }
      "URL contains a valid income source ID and authorised user is within latency period (Tax Year 1 NOT crystallised - before 2023-24 Tax Year)" in {
        val taxYear2023: Int = 2023
        val taxYear2024: Int = 2024
        val latencyPeriodEndDate: LocalDate = LocalDate.of(2025, APRIL, 5)

        val latencyDetailsPreviousTaxYear: LatencyDetails = LatencyDetails(
          latencyEndDate = latencyPeriodEndDate,
          taxYear1 = taxYear2023.toString,
          latencyIndicator1 = quarterlyIndicator,
          taxYear2 = taxYear2024.toString,
          latencyIndicator2 = quarterlyIndicator
        )

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getBusinessDetails returns a success response with latency details")
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = OK,
          response = Json.toJson(singleBusinessResponseInLatencyPeriod(latencyDetailsPreviousTaxYear))
        )

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getListOfCalculationResults returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear2023.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        Then("user is asked to select reporting method for Tax Year 1 and Tax Year 2")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/business-reporting-method?id=$validIncomeSourceId")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.add.businessReportingMethod.heading"),
          elementTextBySelectorList("#add-business-reporting-method-form", "p:nth-of-type(1)")(s"Tax year ${taxYear2023 - 1}-$taxYear2023"),
          elementTextBySelectorList("#add-business-reporting-method-form", "p:nth-of-type(2)")(s"Tax year ${taxYear2024 - 1}-$taxYear2024")
        )
      }
      "URL contains a valid income source ID and authorised user is within latency period (Tax Year 1 crystallised - before 2023-24 Tax Year)" in {
        val taxYear2023: Int = 2023
        val taxYear2024: Int = 2024
        val latencyPeriodEndDate: LocalDate = LocalDate.of(2025, APRIL, 5)

        val latencyDetailsPreviousTaxYear: LatencyDetails = LatencyDetails(
          latencyEndDate = latencyPeriodEndDate,
          taxYear1 = taxYear2023.toString,
          latencyIndicator1 = quarterlyIndicator,
          taxYear2 = taxYear2024.toString,
          latencyIndicator2 = quarterlyIndicator
        )

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getBusinessDetails returns a success response with latency details")
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = OK,
          response = Json.toJson(singleBusinessResponseInLatencyPeriod(latencyDetailsPreviousTaxYear))
        )

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getListOfCalculationResults returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear2023.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        Then("as Tax Year 1 is crystallised, user is asked to select reporting method for Tax Year 2")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/business-reporting-method?id=$validIncomeSourceId")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.add.businessReportingMethod.heading"),
          elementTextBySelectorList("#add-business-reporting-method-form", "p:nth-of-type(1)")(s"Tax year ${taxYear2024 - 1}-$taxYear2024"),
          elementCountBySelector("#add-business-reporting-method-form > p:nth-of-type(2)")(0)
        )
      }
    }
    s"redirect to $businessAddedShowUrl" when {
      "authorised user is out of latency period and URL contains a valid income source ID" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getBusinessDetails returns a success response without latency details")
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = OK,
          response = Json.toJson(singleBusinessResponse)
        )

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("API 1878 getITSAStatus returns a success response with one of these statuses: Annual, No Status, Non Digital, Dormant, MTD Exempt")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("Annual")

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/business-reporting-method?id=$validIncomeSourceId")

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/income-sources/add/business-added?id=$validIncomeSourceId")
        )
      }
    }
    "redirect to /report-quarterly/income-and-expenses/view/sign-in" when {
      "called by an unauthorised user" in {
        isAuthorisedUser(false)

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/business-reporting-method?id=$validIncomeSourceId")

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI("/report-quarterly/income-and-expenses/view/sign-in")
        )
      }
    }
    "return 500 INTERNAL SERVER ERROR" when {
      "API 1896 getCalculationList returns an error" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getBusinessDetails returns a success response with latency details")
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = OK,
          response = Json.toJson(singleBusinessResponseInLatencyPeriod(latencyDetails))
        )

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1896 getCalculationList returns an error")
        CalculationListStub.stubGetCalculationListError(testNino, testTaxYearRange)

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/business-reporting-method?id=$validIncomeSourceId")

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "API 1404 getListOfCalculationResults returns an error" in {
        val taxYear2023: Int = 2023
        val taxYear2024: Int = 2024
        val latencyPeriodEndDate: LocalDate = LocalDate.of(2025, APRIL, 5)

        val latencyDetailsPreviousTaxYear: LatencyDetails = LatencyDetails(
          latencyEndDate = latencyPeriodEndDate,
          taxYear1 = taxYear2023.toString,
          latencyIndicator1 = quarterlyIndicator,
          taxYear2 = taxYear2024.toString,
          latencyIndicator2 = quarterlyIndicator
        )

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getBusinessDetails returns a success response with latency details")
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = OK,
          response = Json.toJson(singleBusinessResponseInLatencyPeriod(latencyDetailsPreviousTaxYear))
        )

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1404 getListOfCalculationResults returns an error response")
        CalculationListStub.stubGetLegacyCalculationListError(testNino, taxYear2023.toString)

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/business-reporting-method?id=$validIncomeSourceId")

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "API 1878 getITSAStatusDetails returns an error" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getBusinessDetails returns a success response with latency details")
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = OK,
          response = Json.toJson(singleBusinessResponseInLatencyPeriod(latencyDetails))
        )

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("API 1878 getITSAStatus returns a success response with one of these statuses: Annual, No Status, Non Digital, Dormant, MTD Exempt")
        ITSAStatusDetailsStub.stubGetITSAStatusDetailsError

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/business-reporting-method?id=$validIncomeSourceId")

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "API 1525 getIncomeSourceDetails returns an error" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getBusinessDetails returns a success response with latency details")
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = OK,
          response = Json.toJson(singleBusinessResponseInLatencyPeriod(latencyDetails))
        )

        And("API 1525 getIncomeSourceDetails returns an error response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(INTERNAL_SERVER_ERROR,
          IncomeSourceDetailsError(INTERNAL_SERVER_ERROR, "IF is currently experiencing problems that require live service intervention."))

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/business-reporting-method?id=$validIncomeSourceId")

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
  s"calling POST $businessReportingMethodSubmitUrl" should {
    s"redirect to $businessAddedShowUrl" when {
      "user completes the form and API 1776 updateIncomeSource returns a success response" in {
        val formData: Map[String, Seq[String]] = Map(
          "newTaxYear1ReportingMethod" -> Seq("A"),
          "newTaxYear2ReportingMethod" -> Seq("A"),
          "taxYear1" -> Seq(taxYear1.toString),
          "taxYear1ReportingMethod" -> Seq("Q"),
          "taxYear2" -> Seq(taxYear2.toString),
          "taxYear2ReportingMethod" -> Seq("Q")
        )

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getBusinessDetails returns a success response with latency details")
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = OK,
          response = Json.toJson(singleBusinessResponseInLatencyPeriod(latencyDetails))
        )

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/add/business-reporting-method?id=$validIncomeSourceId")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/income-sources/add/business-added?id=$validIncomeSourceId")
        )
      }
    }
    "return 400 BAD_REQUEST" when {
      "user submits an invalid form entry" in {
        val formData = AddBusinessReportingMethodForm(
          newTaxYear1ReportingMethod = None,
          newTaxYear2ReportingMethod = None,
          taxYear1 = Some(taxYear1.toString),
          taxYear1ReportingMethod = None,
          taxYear2 = Some(taxYear2.toString),
          taxYear2ReportingMethod = None
        )
        val formWithErrors = AddBusinessReportingMethodForm.form
          .fillAndValidate(formData)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getBusinessDetails returns a success response with latency details")
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = OK,
          response = Json.toJson(singleBusinessResponseInLatencyPeriod(latencyDetails))
        )

        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
        ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")

        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

        val result = IncomeTaxViewChangeFrontend.postAddBusinessReportingMethod(formWithErrors.value.get)()

        result should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }
}
