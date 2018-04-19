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
import assets.IncomeSourceIntegrationTestConstants._
import assets.ReportDeadlinesIntegrationTestConstants.multipleReportDeadlinesDataSuccessModel
import enums.{Crystallised, Estimate}
import helpers.servicemocks._
import helpers.{ComponentSpecBase, GenericStubMethods}
import models.calculation.{CalculationDataErrorModel, CalculationDataModel, LastTaxCalculation}
import models.financialTransactions.FinancialTransactionsModel
import play.api.http.Status
import play.api.http.Status._
import utils.ImplicitCurrencyFormatter._

class CalculationControllerISpec extends ComponentSpecBase with GenericStubMethods {

  def totalProfit(calc: CalculationDataModel, includeInterest: Boolean = true): String = {
    import calc.incomeReceived._
    selfEmployment + ukProperty + (if(includeInterest) bankBuildingSocietyInterest else 0)
  }.toCurrencyString

  def totalAllowance(calc: CalculationDataModel): String =
    (calc.personalAllowance + calc.savingsAndGains.startBand.taxableIncome + calc.savingsAndGains.zeroBand.taxableIncome).toCurrencyString

  def taxableIncome(calc: CalculationDataModel): String =
    (calc.totalTaxableIncome - calc.taxableDividendIncome).toCurrencyString

  "Calling the CalculationController.getEstimatedTaxLiability(year)" when {

    "isAuthorisedUser with an active enrolment, valid last calc estimate, valid breakdown response and an EoY Estimate" should {

      "return the correct page with a valid total" in {

        isAuthorisedUser(true)
        stubUserDetails()

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, businessAndPropertyResponse
        )

        And("I wiremock stub multiple open and received obligations response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse =
          LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Estimate)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = calculationDataSuccessWithEoYModel
        SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        Then("I verify the Income Source Details has been successfully wiremocked")
        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        res should have (
          httpStatus(OK),
          pageTitle("2017 to 2018 tax year"),
          elementTextByID("inYearEstimateHeading")(s"Current estimate: ${calcBreakdownResponse.totalIncomeTaxNicYtd.toCurrencyString}"),
          elementTextByID("heading")("2017 to 2018 tax year"),
          elementTextByID("sub-heading")("Estimates"),
          elementTextByID("business-profit")(totalProfit(calcBreakdownResponse)),
          elementTextByID("personal-allowance")(s"-${totalAllowance(calcBreakdownResponse)}"),
          elementTextByID("additional-allowances")("-" + calcBreakdownResponse.additionalAllowances.toCurrencyString),
          elementTextByID("taxable-income")(taxableIncome(calcBreakdownResponse)),
          elementTextByID("brt-it-calc")((calcBreakdownResponse.payPensionsProfit.basicBand.taxableIncome + calcBreakdownResponse.savingsAndGains.basicBand.taxableIncome).toCurrencyString),
          elementTextByID("brt-rate")(calcBreakdownResponse.payPensionsProfit.basicBand.taxRate.toStringNoDecimal),
          elementTextByID("brt-amount")((calcBreakdownResponse.payPensionsProfit.basicBand.taxAmount + calcBreakdownResponse.savingsAndGains.basicBand.taxAmount).toCurrencyString),
          elementTextByID("hrt-it-calc")((calcBreakdownResponse.payPensionsProfit.higherBand.taxableIncome + calcBreakdownResponse.savingsAndGains.higherBand.taxableIncome).toCurrencyString),
          elementTextByID("hrt-rate")(calcBreakdownResponse.payPensionsProfit.higherBand.taxRate.toStringNoDecimal),
          elementTextByID("hrt-amount")((calcBreakdownResponse.payPensionsProfit.higherBand.taxAmount + calcBreakdownResponse.savingsAndGains.higherBand.taxAmount).toCurrencyString),
          elementTextByID("art-it-calc")((calcBreakdownResponse.payPensionsProfit.additionalBand.taxableIncome + calcBreakdownResponse.savingsAndGains.additionalBand.taxableIncome).toCurrencyString),
          elementTextByID("art-rate")(calcBreakdownResponse.payPensionsProfit.additionalBand.taxRate.toStringNoDecimal),
          elementTextByID("art-amount")((calcBreakdownResponse.payPensionsProfit.additionalBand.taxAmount + calcBreakdownResponse.savingsAndGains.additionalBand.taxAmount).toCurrencyString),
          elementTextByID("dividend-income")(calcBreakdownResponse.incomeReceived.ukDividends.toCurrencyString),
          elementTextByID("dividend-allowance")(s"-${calcBreakdownResponse.dividends.allowance.toCurrencyString}"),
          elementTextByID("taxable-dividend-income")(calcBreakdownResponse.taxableDividendIncome.toCurrencyString),
          elementTextByID("dividend-brt-calc")(calcBreakdownResponse.dividends.basicBand.taxableIncome.toCurrencyString),
          elementTextByID("dividend-brt-rate")(calcBreakdownResponse.dividends.basicBand.taxRate.toStringNoDecimal),
          elementTextByID("dividend-brt-amount")(calcBreakdownResponse.dividends.basicBand.taxAmount.toCurrencyString),
          elementTextByID("dividend-hrt-calc")(calcBreakdownResponse.dividends.higherBand.taxableIncome.toCurrencyString),
          elementTextByID("dividend-hrt-rate")(calcBreakdownResponse.dividends.higherBand.taxRate.toStringNoDecimal),
          elementTextByID("dividend-hrt-amount")(calcBreakdownResponse.dividends.higherBand.taxAmount.toCurrencyString),
          elementTextByID("dividend-art-calc")(calcBreakdownResponse.dividends.additionalBand.taxableIncome.toCurrencyString),
          elementTextByID("dividend-art-rate")(calcBreakdownResponse.dividends.additionalBand.taxRate.toStringNoDecimal),
          elementTextByID("dividend-art-amount")(calcBreakdownResponse.dividends.additionalBand.taxAmount.toCurrencyString),
          elementTextByID("nic2-amount")(calcBreakdownResponse.nic.class2.toCurrencyString),
          elementTextByID("nic4-amount")(calcBreakdownResponse.nic.class4.toCurrencyString),
          elementTextByID("total-estimate")(calcBreakdownResponse.totalIncomeTaxNicYtd.toCurrencyString),
          isElementVisibleById("eoyEstimate")(expectedValue = true)
        )
      }
    }

    "isAuthorisedUser with an active enrolment, valid last calc estimate, valid breakdown response and Crystallised EoY amount" when {

      "a successful response is retrieved for the financial transactions and there is an outstanding amount (unpaid)" should {

        "return the correct page with a valid total" in {

          isAuthorisedUser(true)
          stubUserDetails()

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            OK, businessAndPropertyResponse
          )

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse =
            LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Crystallised)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

          And("I wiremock stub a successful Get CalculationData response")
          val calcBreakdownResponse = calculationDataSuccessWithEoYModel
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

          And("I wiremock stub a successful Unpaid Financial Transactions response")
          val financialTransactions = financialTransactionsJson(1000.0).as[FinancialTransactionsModel]
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsJson(1000.0))

          When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
          val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

          Then("I verify the Income Source Details has been successfully wiremocked")
          IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

          verifyFinancialTransactionsCall()

          Then("I verify the Estimated Tax Liability response has been wiremocked")
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

          res should have(
            httpStatus(OK),
            pageTitle("2017 to 2018 tax year"),
            elementTextByID("heading")("2017 to 2018 tax year"),
            elementTextByID("sub-heading")("Bills"),
            elementTextByID("whatYouOweHeading")(s"${calcBreakdownResponse.totalIncomeTaxNicYtd.toCurrencyString}"),
            isElementVisibleById("calcBreakdown")(expectedValue = true),
            elementTextByID("calcBreakdown")("How this figure was calculated"),
            elementTextByID("business-profit")(totalProfit(calcBreakdownResponse, includeInterest = false)),
            elementTextByID("savings-income")(calcBreakdownResponse.incomeReceived.bankBuildingSocietyInterest.toCurrencyString),
            elementTextByID("personal-allowance")(s"-${totalAllowance(calcBreakdownResponse)}"),
            elementTextByID("additional-allowances")("-" + calcBreakdownResponse.additionalAllowances.toCurrencyString),
            elementTextByID("taxable-income")(taxableIncome(calcBreakdownResponse)),
            elementTextByID("brt-it-calc")((calcBreakdownResponse.payPensionsProfit.basicBand.taxableIncome + calcBreakdownResponse.savingsAndGains.basicBand.taxableIncome).toCurrencyString),
            elementTextByID("brt-rate")(calcBreakdownResponse.payPensionsProfit.basicBand.taxRate.toStringNoDecimal),
            elementTextByID("brt-amount")((calcBreakdownResponse.payPensionsProfit.basicBand.taxAmount + calcBreakdownResponse.savingsAndGains.basicBand.taxAmount).toCurrencyString),
            elementTextByID("hrt-it-calc")((calcBreakdownResponse.payPensionsProfit.higherBand.taxableIncome + calcBreakdownResponse.savingsAndGains.higherBand.taxableIncome).toCurrencyString),
            elementTextByID("hrt-rate")(calcBreakdownResponse.payPensionsProfit.higherBand.taxRate.toStringNoDecimal),
            elementTextByID("hrt-amount")((calcBreakdownResponse.payPensionsProfit.higherBand.taxAmount + calcBreakdownResponse.savingsAndGains.higherBand.taxAmount).toCurrencyString),
            elementTextByID("art-it-calc")((calcBreakdownResponse.payPensionsProfit.additionalBand.taxableIncome + calcBreakdownResponse.savingsAndGains.additionalBand.taxableIncome).toCurrencyString),
            elementTextByID("art-rate")(calcBreakdownResponse.payPensionsProfit.additionalBand.taxRate.toStringNoDecimal),
            elementTextByID("art-amount")((calcBreakdownResponse.payPensionsProfit.additionalBand.taxAmount + calcBreakdownResponse.savingsAndGains.additionalBand.taxAmount).toCurrencyString),
            elementTextByID("dividend-income")(calcBreakdownResponse.incomeReceived.ukDividends.toCurrencyString),
            elementTextByID("dividend-allowance")(s"-${calcBreakdownResponse.dividends.allowance.toCurrencyString}"),
            elementTextByID("taxable-dividend-income")(calcBreakdownResponse.taxableDividendIncome.toCurrencyString),
            elementTextByID("dividend-brt-calc")(calcBreakdownResponse.dividends.basicBand.taxableIncome.toCurrencyString),
            elementTextByID("dividend-brt-rate")(calcBreakdownResponse.dividends.basicBand.taxRate.toStringNoDecimal),
            elementTextByID("dividend-brt-amount")(calcBreakdownResponse.dividends.basicBand.taxAmount.toCurrencyString),
            elementTextByID("dividend-hrt-calc")(calcBreakdownResponse.dividends.higherBand.taxableIncome.toCurrencyString),
            elementTextByID("dividend-hrt-rate")(calcBreakdownResponse.dividends.higherBand.taxRate.toStringNoDecimal),
            elementTextByID("dividend-hrt-amount")(calcBreakdownResponse.dividends.higherBand.taxAmount.toCurrencyString),
            elementTextByID("dividend-art-calc")(calcBreakdownResponse.dividends.additionalBand.taxableIncome.toCurrencyString),
            elementTextByID("dividend-art-rate")(calcBreakdownResponse.dividends.additionalBand.taxRate.toStringNoDecimal),
            elementTextByID("dividend-art-amount")(calcBreakdownResponse.dividends.additionalBand.taxAmount.toCurrencyString),
            elementTextByID("nic2-amount")(calcBreakdownResponse.nic.class2.toCurrencyString),
            elementTextByID("nic4-amount")(calcBreakdownResponse.nic.class4.toCurrencyString),
            elementTextByID("total-estimate")(calcBreakdownResponse.totalIncomeTaxNicYtd.toCurrencyString),
            elementTextByID("payment")("-" + financialTransactions.financialTransactions.get.head.clearedAmount.get.toCurrencyString),
            elementTextByID("owed")(financialTransactions.financialTransactions.get.head.outstandingAmount.get.toCurrencyString)
          )
        }
      }

      "a successful response is retrieved for the financial transactions and there is no outstanding amount (paid)" should {

        "return the correct page with a valid total" in {

          isAuthorisedUser(true)
          stubUserDetails()

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            OK, businessAndPropertyResponse
          )

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse =
            LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Crystallised)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

          And("I wiremock stub a successful Get CalculationData response")
          val calcBreakdownResponse = calculationDataSuccessWithEoYModel
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

          And("I wiremock stub a successful paid Financial Transactions response")
          val financialTransactions = financialTransactionsJson(0.0).as[FinancialTransactionsModel]
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsJson(0.0))

          When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
          val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

          Then("I verify the Income Source Details has been successfully wiremocked")
          IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

          verifyFinancialTransactionsCall()

          Then("I verify the Estimated Tax Liability response has been wiremocked")
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

          res should have(
            httpStatus(OK),
            pageTitle("2017 to 2018 tax year"),
            elementTextByID("sub-heading")("Bills"),
            elementTextByID("heading")("2017 to 2018 tax year"),
            isElementVisibleById("calcBreakdown")(expectedValue = false),
            elementTextByID("business-profit")(totalProfit(calcBreakdownResponse, includeInterest = false)),
            elementTextByID("savings-income")(calcBreakdownResponse.incomeReceived.bankBuildingSocietyInterest.toCurrencyString),
            elementTextByID("personal-allowance")(s"-${totalAllowance(calcBreakdownResponse)}"),
            elementTextByID("additional-allowances")("-" + calcBreakdownResponse.additionalAllowances.toCurrencyString),
            elementTextByID("taxable-income")(taxableIncome(calcBreakdownResponse)),
            elementTextByID("brt-it-calc")((calcBreakdownResponse.payPensionsProfit.basicBand.taxableIncome + calcBreakdownResponse.savingsAndGains.basicBand.taxableIncome).toCurrencyString),
            elementTextByID("brt-rate")(calcBreakdownResponse.payPensionsProfit.basicBand.taxRate.toStringNoDecimal),
            elementTextByID("brt-amount")((calcBreakdownResponse.payPensionsProfit.basicBand.taxAmount + calcBreakdownResponse.savingsAndGains.basicBand.taxAmount).toCurrencyString),
            elementTextByID("hrt-it-calc")((calcBreakdownResponse.payPensionsProfit.higherBand.taxableIncome + calcBreakdownResponse.savingsAndGains.higherBand.taxableIncome).toCurrencyString),
            elementTextByID("hrt-rate")(calcBreakdownResponse.payPensionsProfit.higherBand.taxRate.toStringNoDecimal),
            elementTextByID("hrt-amount")((calcBreakdownResponse.payPensionsProfit.higherBand.taxAmount + calcBreakdownResponse.savingsAndGains.higherBand.taxAmount).toCurrencyString),
            elementTextByID("art-it-calc")((calcBreakdownResponse.payPensionsProfit.additionalBand.taxableIncome + calcBreakdownResponse.savingsAndGains.additionalBand.taxableIncome).toCurrencyString),
            elementTextByID("art-rate")(calcBreakdownResponse.payPensionsProfit.additionalBand.taxRate.toStringNoDecimal),
            elementTextByID("art-amount")((calcBreakdownResponse.payPensionsProfit.additionalBand.taxAmount + calcBreakdownResponse.savingsAndGains.additionalBand.taxAmount).toCurrencyString),
            elementTextByID("dividend-income")(calcBreakdownResponse.incomeReceived.ukDividends.toCurrencyString),
            elementTextByID("dividend-allowance")(s"-${calcBreakdownResponse.dividends.allowance.toCurrencyString}"),
            elementTextByID("taxable-dividend-income")(calcBreakdownResponse.taxableDividendIncome.toCurrencyString),
            elementTextByID("dividend-brt-calc")(calcBreakdownResponse.dividends.basicBand.taxableIncome.toCurrencyString),
            elementTextByID("dividend-brt-rate")(calcBreakdownResponse.dividends.basicBand.taxRate.toStringNoDecimal),
            elementTextByID("dividend-brt-amount")(calcBreakdownResponse.dividends.basicBand.taxAmount.toCurrencyString),
            elementTextByID("dividend-hrt-calc")(calcBreakdownResponse.dividends.higherBand.taxableIncome.toCurrencyString),
            elementTextByID("dividend-hrt-rate")(calcBreakdownResponse.dividends.higherBand.taxRate.toStringNoDecimal),
            elementTextByID("dividend-hrt-amount")(calcBreakdownResponse.dividends.higherBand.taxAmount.toCurrencyString),
            elementTextByID("dividend-art-calc")(calcBreakdownResponse.dividends.additionalBand.taxableIncome.toCurrencyString),
            elementTextByID("dividend-art-rate")(calcBreakdownResponse.dividends.additionalBand.taxRate.toStringNoDecimal),
            elementTextByID("dividend-art-amount")(calcBreakdownResponse.dividends.additionalBand.taxAmount.toCurrencyString),
            elementTextByID("nic2-amount")(calcBreakdownResponse.nic.class2.toCurrencyString),
            elementTextByID("nic4-amount")(calcBreakdownResponse.nic.class4.toCurrencyString),
            elementTextByID("total-estimate")(calcBreakdownResponse.totalIncomeTaxNicYtd.toCurrencyString),
            elementTextByID("payment")("-" + financialTransactions.financialTransactions.get.head.clearedAmount.get.toCurrencyString),
            elementTextByID("owed")(financialTransactions.financialTransactions.get.head.outstandingAmount.get.toCurrencyString)
          )
        }
      }

      "an error response is retrieve for the financial transactions" should {

        "return an Internal Server Error page" in {

          isAuthorisedUser(true)
          stubUserDetails()

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            OK, businessAndPropertyResponse
          )

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse =
            LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Crystallised)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

          And("I wiremock stub a successful Get CalculationData response")
          val calcBreakdownResponse = calculationDataSuccessWithEoYModel
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

          And("I wiremock stub an errored Financial Transactions response")
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsSingleErrorJson)

          When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
          val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

          Then("I verify the Income Source Details has been successfully wiremocked")
          IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

          verifyFinancialTransactionsCall()

          Then("I verify the Estimated Tax Liability response has been wiremocked")
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

          res should have(httpStatus(INTERNAL_SERVER_ERROR))

        }
      }
    }

    "isAuthorisedUser with an active enrolment, valid last calc estimate, valid breakdown response but NO EoY Estimate" should {

      "return the correct page with a valid total" in {

        isAuthorisedUser(true)
        stubUserDetails()

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, businessAndPropertyResponse
        )

        And("I wiremock stub multiple open and received obligations response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse =
          LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", calculationDataSuccessModel.totalIncomeTaxNicYtd, Estimate)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = calculationDataSuccessModel
        SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessJson.toString())

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        Then("I verify the Income Source Details has been successfully wiremocked")
        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)
        //IncomeTaxViewChangeStub.stubGetCalcData(testNino,testYear,calculationResponse)

        res should have (
          httpStatus(OK),
          pageTitle("2017 to 2018 tax year"),
          elementTextByID("inYearEstimateHeading")(s"Current estimate: ${calcBreakdownResponse.totalIncomeTaxNicYtd.toCurrencyString}"),
          elementTextByID("heading")("2017 to 2018 tax year"),
          elementTextByID("sub-heading")("Estimates"),
          elementTextByID("business-profit")(totalProfit(calcBreakdownResponse)),
          elementTextByID("personal-allowance")(s"-${totalAllowance(calcBreakdownResponse)}"),
          elementTextByID("additional-allowances")("-" + calcBreakdownResponse.additionalAllowances.toCurrencyString),
          elementTextByID("taxable-income")(taxableIncome(calcBreakdownResponse)),
          elementTextByID("brt-it-calc")((calcBreakdownResponse.payPensionsProfit.basicBand.taxableIncome + calcBreakdownResponse.savingsAndGains.basicBand.taxableIncome).toCurrencyString),
          elementTextByID("brt-rate")(calcBreakdownResponse.payPensionsProfit.basicBand.taxRate.toStringNoDecimal),
          elementTextByID("brt-amount")((calcBreakdownResponse.payPensionsProfit.basicBand.taxAmount + calcBreakdownResponse.savingsAndGains.basicBand.taxAmount).toCurrencyString),
          elementTextByID("hrt-it-calc")((calcBreakdownResponse.payPensionsProfit.higherBand.taxableIncome + calcBreakdownResponse.savingsAndGains.higherBand.taxableIncome).toCurrencyString),
          elementTextByID("hrt-rate")(calcBreakdownResponse.payPensionsProfit.higherBand.taxRate.toStringNoDecimal),
          elementTextByID("hrt-amount")((calcBreakdownResponse.payPensionsProfit.higherBand.taxAmount + calcBreakdownResponse.savingsAndGains.higherBand.taxAmount).toCurrencyString),
          elementTextByID("art-it-calc")((calcBreakdownResponse.payPensionsProfit.additionalBand.taxableIncome + calcBreakdownResponse.savingsAndGains.additionalBand.taxableIncome).toCurrencyString),
          elementTextByID("art-rate")(calcBreakdownResponse.payPensionsProfit.additionalBand.taxRate.toStringNoDecimal),
          elementTextByID("art-amount")((calcBreakdownResponse.payPensionsProfit.additionalBand.taxAmount + calcBreakdownResponse.savingsAndGains.additionalBand.taxAmount).toCurrencyString),
          elementTextByID("dividend-income")(calcBreakdownResponse.incomeReceived.ukDividends.toCurrencyString),
          elementTextByID("dividend-allowance")(s"-${calcBreakdownResponse.dividends.allowance.toCurrencyString}"),
          elementTextByID("taxable-dividend-income")(calcBreakdownResponse.taxableDividendIncome.toCurrencyString),
          elementTextByID("dividend-brt-calc")(calcBreakdownResponse.dividends.basicBand.taxableIncome.toCurrencyString),
          elementTextByID("dividend-brt-rate")(calcBreakdownResponse.dividends.basicBand.taxRate.toStringNoDecimal),
          elementTextByID("dividend-brt-amount")(calcBreakdownResponse.dividends.basicBand.taxAmount.toCurrencyString),
          elementTextByID("dividend-hrt-calc")(calcBreakdownResponse.dividends.higherBand.taxableIncome.toCurrencyString),
          elementTextByID("dividend-hrt-rate")(calcBreakdownResponse.dividends.higherBand.taxRate.toStringNoDecimal),
          elementTextByID("dividend-hrt-amount")(calcBreakdownResponse.dividends.higherBand.taxAmount.toCurrencyString),
          elementTextByID("dividend-art-calc")(calcBreakdownResponse.dividends.additionalBand.taxableIncome.toCurrencyString),
          elementTextByID("dividend-art-rate")(calcBreakdownResponse.dividends.additionalBand.taxRate.toStringNoDecimal),
          elementTextByID("dividend-art-amount")(calcBreakdownResponse.dividends.additionalBand.taxAmount.toCurrencyString),
          elementTextByID("nic2-amount")(calcBreakdownResponse.nic.class2.toCurrencyString),
          elementTextByID("nic4-amount")(calcBreakdownResponse.nic.class4.toCurrencyString),
          elementTextByID("total-estimate")(calcBreakdownResponse.totalIncomeTaxNicYtd.toCurrencyString),
          isElementVisibleById("eoyEstimate")(expectedValue = false)
        )
      }
    }

    "isAuthorisedUser with an active enrolment, valid last tax estimate response, but error in calc breakdown" should {

      "Return the estimated tax liability without the calculation breakdown" in {

        isAuthorisedUser(true)
        stubUserDetailsError()

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, businessAndPropertyResponse
        )

        And("I wiremock stub multiple open and received obligations response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

        And("a successful Get Last Estimated Tax Liability response via wiremock stub")
        val lastTaxCalcResponse =
          LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Estimate)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub an erroneous response")
        val calc = calculationDataErrorModel
        val calculationResponse = CalculationDataErrorModel(calc.code, calc.message)

        SelfAssessmentStub.stubGetCalcError(testNino, testCalcId, calculationResponse)

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        Then("I verify the Income Source Details has been successfully wiremocked")
        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        Then("verification that the Estimated Tax Liability response has been wiremocked ")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("a successful response is returned with the correct estimate")
        res should have(
          httpStatus(OK),
          pageTitle("2017 to 2018 tax year"),
          elementTextByID("inYearEstimateHeading")(s"Current estimate: ${calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd.toCurrencyString}"),
          elementTextByID("inYearP1")("This is for 6 April 2017 to 6 July 2017."),
          isElementVisibleById("inYearCalcBreakdown")(expectedValue = false)
        )
      }
    }

    "isAuthorisedUser with an active enrolment no data found response from Last Calculation" should {

      "Return no data found response and render view explaining that this will be available once they've submitted income" in {

        isAuthorisedUser(true)
        stubUserDetails()

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, businessAndPropertyResponse
        )

        And("I wiremock stub multiple open and received obligations response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

        And("a No Data Found response from Get Last Estimated Tax Liability via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLastCalcNoData(testNino, testYear)

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        Then("I verify the Income Source Details has been successfully wiremocked")
        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        Then("verification that the Estimated Tax Liability response has been wiremocked ")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("a Not Found response is returned and correct view rendered")
        res should have(
          httpStatus(NOT_FOUND),
          pageTitle("2017 to 2018 tax year")
        )
      }
    }

    "isAuthorisedUser with an active enrolment but error response from Get Last Calculation" should {

      "Render the Estimated Tax Liability Error Page" in {

        isAuthorisedUser(true)
        stubUserDetails()

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, businessAndPropertyResponse
        )

        And("I wiremock stub multiple open and received obligations response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

        And("an Error Response response from Get Last Estimated Tax Liability via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLastCalcError(testNino, testYear)

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        Then("I verify the Income Source Details has been successfully wiremocked")
        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        Then("verification that the Estimated Tax Liability response has been wiremocked ")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("an Internal Server Error response is returned and correct view rendered")
        res should have(
          httpStatus(OK),
          pageTitle("2017 to 2018 tax year"),
          elementTextByID("p1")("We can't display your estimated tax amount at the moment."),
          elementTextByID("p2")("Try refreshing the page in a few minutes.")
        )
      }
    }

    "unauthorised" should {

      "redirect to sign in" in {

        isAuthorisedUser(false)

        When("I call GET /report-quarterly/income-and-expenses/view/calculation")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        Then("the http response for an unauthorised user is returned")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
  }
}
