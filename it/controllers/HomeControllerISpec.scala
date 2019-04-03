/*
 * Copyright 2017 HM Revenue & Customs
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
package controllers

import assets.BaseIntegrationTestConstants._
import assets.CalcDataIntegrationTestConstants._
import assets.FinancialTransactionsIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse , singleBusinessResponse}
import assets.ReportDeadlinesIntegrationTestConstants._
import assets.messages.HomeMessages._
import helpers.ComponentSpecBase
import helpers.servicemocks.{IncomeTaxViewChangeStub,FinancialTransactionsStub}
import implicits.ImplicitDateFormatter
import play.api.http.Status._
import play.api.libs.json.JsValue

class HomeControllerISpec extends ComponentSpecBase with ImplicitDateFormatter {

  "Navigating to /report-quarterly/income-and-expenses/view" when {

    "Authorised" should {

      "render the home page with the payment due date" in {

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        And("I wiremock stub a single business obligation response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, singleObligationQuarterlyReturnModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, testNino, singleObligationOverdueModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino, testNino, singleObligationCrystallisationModel)
        IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationResponseCrystallised)
        IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYearPlusOne, taxCalculationResponseCrystallised)
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(OK, financialTransactionsJson(2000))

        When("I call GET /report-quarterly/income-and-expenses/view")
        val res = IncomeTaxViewChangeFrontend.getHome

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyReportDeadlinesCall(testNino, testSelfEmploymentId)

        Then("the result should have a HTTP status of OK (200) and the Income Tax home page")
        res should have(
          httpStatus(OK),
          pageTitle(title),
          elementTextByID("updates-card-body-date")(veryOverdueDate.toLongDate),
          elementTextByID("income-tax-payment-card-body-date")("14 February 2018")
        )
      }

      "render the home page without the payment due date" in {

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        And("I wiremock stub a single business obligation response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, singleObligationQuarterlyReturnModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, testNino, singleObligationOverdueModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino, testNino, singleObligationCrystallisationModel)
        IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationResponse)
        IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYearPlusOne, taxCalculationResponse)
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(OK, financialTransactionsJson(2000))

        When("I call GET /report-quarterly/income-and-expenses/view")
        val res = IncomeTaxViewChangeFrontend.getHome

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyReportDeadlinesCall(testNino, testSelfEmploymentId)

        Then("the result should have a HTTP status of OK (200) and the Income Tax home page")
        res should have(
          httpStatus(OK),
          pageTitle(title),
          elementTextByID("updates-card-body-date")(veryOverdueDate.toLongDate),
          elementTextByID("income-tax-payment-card-body-date")("No payments due.")
        )
      }

      "render the ISE page when receive an error from the backend" in {

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        And("I wiremock stub a single business obligation response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, singleObligationQuarterlyReturnModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, testNino, singleObligationOverdueModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino, testNino, singleObligationCrystallisationModel)
        IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationResponseCrystallised)
        IncomeTaxViewChangeStub.stubGetLatestCalcError(testNino, testYearPlusOne)
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(OK, financialTransactionWithoutDueDatesJson(0))

        When("I call GET /report-quarterly/income-and-expenses/view")
        val res = IncomeTaxViewChangeFrontend.getHome

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyReportDeadlinesCall(testNino, testSelfEmploymentId)

        Then("the result should have a HTTP status of ISE (500) and the Income Tax home page")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
    unauthorisedTest("")
  }
}