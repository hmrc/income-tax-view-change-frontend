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

import assets.messages.{CalculationMessages => messages}
import assets.BaseIntegrationTestConstants._
import assets.CalcDataIntegrationTestConstants._
import assets.FinancialTransactionsIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import assets.LastTaxCalcIntegrationTestConstants._
import assets.ReportDeadlinesIntegrationTestConstants.multipleReportDeadlinesDataSuccessModel
import helpers.servicemocks._
import helpers.ComponentSpecBase
import models.calculation.CalculationDataModel
import models.financialTransactions.FinancialTransactionsModel
import play.api.http.Status
import play.api.http.Status._
import utils.ImplicitCurrencyFormatter._

class CalculationControllerISpec extends ComponentSpecBase {

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

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub multiple open and received obligations response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, estimateLastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)
        verifyLastTaxCalculationCall(testNino, testYear)
        verifyCalculationDataCall(testNino, testCalcId)

        res should have (
          httpStatus(OK),
          pageTitle(messages.title(testYearInt)),
          elementTextByID("inYearEstimateHeading")(messages.EstimatedTaxAmount.currentEstimate(calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd.toCurrencyString)),
          elementTextByID("heading")(messages.heading(testYearInt)),
          elementTextByID("sub-heading")(messages.EstimatedTaxAmount.subHeading),
          elementTextByID("business-profit")(totalProfit(calculationDataSuccessWithEoYModel)),
          elementTextByID("personal-allowance")(s"-${totalAllowance(calculationDataSuccessWithEoYModel)}"),
          elementTextByID("additional-allowances")("-" + calculationDataSuccessWithEoYModel.additionalAllowances.toCurrencyString),
          elementTextByID("taxable-income")(taxableIncome(calculationDataSuccessWithEoYModel)),
          elementTextByID("brt-it-calc")((calculationDataSuccessWithEoYModel.payPensionsProfit.basicBand.taxableIncome + calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxableIncome).toCurrencyString),
          elementTextByID("brt-rate")(calculationDataSuccessWithEoYModel.payPensionsProfit.basicBand.taxRate.toStringNoDecimal),
          elementTextByID("brt-amount")((calculationDataSuccessWithEoYModel.payPensionsProfit.basicBand.taxAmount + calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxAmount).toCurrencyString),
          elementTextByID("hrt-it-calc")((calculationDataSuccessWithEoYModel.payPensionsProfit.higherBand.taxableIncome + calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxableIncome).toCurrencyString),
          elementTextByID("hrt-rate")(calculationDataSuccessWithEoYModel.payPensionsProfit.higherBand.taxRate.toStringNoDecimal),
          elementTextByID("hrt-amount")((calculationDataSuccessWithEoYModel.payPensionsProfit.higherBand.taxAmount + calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxAmount).toCurrencyString),
          elementTextByID("art-it-calc")((calculationDataSuccessWithEoYModel.payPensionsProfit.additionalBand.taxableIncome + calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxableIncome).toCurrencyString),
          elementTextByID("art-rate")(calculationDataSuccessWithEoYModel.payPensionsProfit.additionalBand.taxRate.toStringNoDecimal),
          elementTextByID("art-amount")((calculationDataSuccessWithEoYModel.payPensionsProfit.additionalBand.taxAmount + calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxAmount).toCurrencyString),
          elementTextByID("dividend-income")(calculationDataSuccessWithEoYModel.incomeReceived.ukDividends.toCurrencyString),
          elementTextByID("dividend-allowance")(s"-${calculationDataSuccessWithEoYModel.dividends.allowance.toCurrencyString}"),
          elementTextByID("taxable-dividend-income")(calculationDataSuccessWithEoYModel.taxableDividendIncome.toCurrencyString),
          elementTextByID("dividend-brt-calc")(calculationDataSuccessWithEoYModel.dividends.basicBand.taxableIncome.toCurrencyString),
          elementTextByID("dividend-brt-rate")(calculationDataSuccessWithEoYModel.dividends.basicBand.taxRate.toStringNoDecimal),
          elementTextByID("dividend-brt-amount")(calculationDataSuccessWithEoYModel.dividends.basicBand.taxAmount.toCurrencyString),
          elementTextByID("dividend-hrt-calc")(calculationDataSuccessWithEoYModel.dividends.higherBand.taxableIncome.toCurrencyString),
          elementTextByID("dividend-hrt-rate")(calculationDataSuccessWithEoYModel.dividends.higherBand.taxRate.toStringNoDecimal),
          elementTextByID("dividend-hrt-amount")(calculationDataSuccessWithEoYModel.dividends.higherBand.taxAmount.toCurrencyString),
          elementTextByID("dividend-art-calc")(calculationDataSuccessWithEoYModel.dividends.additionalBand.taxableIncome.toCurrencyString),
          elementTextByID("dividend-art-rate")(calculationDataSuccessWithEoYModel.dividends.additionalBand.taxRate.toStringNoDecimal),
          elementTextByID("dividend-art-amount")(calculationDataSuccessWithEoYModel.dividends.additionalBand.taxAmount.toCurrencyString),
          elementTextByID("nic2-amount")(calculationDataSuccessWithEoYModel.nic.class2.toCurrencyString),
          elementTextByID("nic4-amount")(calculationDataSuccessWithEoYModel.nic.class4.toCurrencyString),
          elementTextByID("total-estimate")(calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd.toCurrencyString),
          isElementVisibleById("eoyEstimate")(expectedValue = true)
        )
      }
    }

    "isAuthorisedUser with an active enrolment, valid last calc estimate, valid breakdown response and Crystallised EoY amount" when {

      "a successful response is retrieved for the financial transactions and there is an outstanding amount (unpaid)" should {

        "return the correct page with a valid total" in {

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, crystallisedLastTaxCalcResponse)

          And("I wiremock stub a successful Get CalculationData response")
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

          And("I wiremock stub a successful Unpaid Financial Transactions response")
          val financialTransactions = financialTransactionsJson(1000.0).as[FinancialTransactionsModel]
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsJson(1000.0))

          When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)
          verifyLastTaxCalculationCall(testNino, testYear)
          verifyCalculationDataCall(testNino, testCalcId)
          verifyFinancialTransactionsCall(testMtditid)

          res should have(
            httpStatus(OK),
            pageTitle(messages.title(testYearInt)),
            elementTextByID("heading")(messages.heading(testYearInt)),
            elementTextByID("sub-heading")(messages.CrystalisedTaxAmount.subHeading),
            elementTextByID("whatYouOweHeading")(calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd.toCurrencyString),
            isElementVisibleById("calcBreakdown")(expectedValue = true),
            elementTextByID("calcBreakdown")(messages.CrystalisedTaxAmount.calcBreakdown),
            elementTextByID("business-profit")(totalProfit(calculationDataSuccessWithEoYModel, includeInterest = false)),
            elementTextByID("savings-income")(calculationDataSuccessWithEoYModel.incomeReceived.bankBuildingSocietyInterest.toCurrencyString),
            elementTextByID("personal-allowance")(s"-${totalAllowance(calculationDataSuccessWithEoYModel)}"),
            elementTextByID("additional-allowances")("-" + calculationDataSuccessWithEoYModel.additionalAllowances.toCurrencyString),
            elementTextByID("taxable-income")(taxableIncome(calculationDataSuccessWithEoYModel)),
            elementTextByID("brt-it-calc")((calculationDataSuccessWithEoYModel.payPensionsProfit.basicBand.taxableIncome + calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxableIncome).toCurrencyString),
            elementTextByID("brt-rate")(calculationDataSuccessWithEoYModel.payPensionsProfit.basicBand.taxRate.toStringNoDecimal),
            elementTextByID("brt-amount")((calculationDataSuccessWithEoYModel.payPensionsProfit.basicBand.taxAmount + calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxAmount).toCurrencyString),
            elementTextByID("hrt-it-calc")((calculationDataSuccessWithEoYModel.payPensionsProfit.higherBand.taxableIncome + calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxableIncome).toCurrencyString),
            elementTextByID("hrt-rate")(calculationDataSuccessWithEoYModel.payPensionsProfit.higherBand.taxRate.toStringNoDecimal),
            elementTextByID("hrt-amount")((calculationDataSuccessWithEoYModel.payPensionsProfit.higherBand.taxAmount + calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxAmount).toCurrencyString),
            elementTextByID("art-it-calc")((calculationDataSuccessWithEoYModel.payPensionsProfit.additionalBand.taxableIncome + calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxableIncome).toCurrencyString),
            elementTextByID("art-rate")(calculationDataSuccessWithEoYModel.payPensionsProfit.additionalBand.taxRate.toStringNoDecimal),
            elementTextByID("art-amount")((calculationDataSuccessWithEoYModel.payPensionsProfit.additionalBand.taxAmount + calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxAmount).toCurrencyString),
            elementTextByID("dividend-income")(calculationDataSuccessWithEoYModel.incomeReceived.ukDividends.toCurrencyString),
            elementTextByID("dividend-allowance")(s"-${calculationDataSuccessWithEoYModel.dividends.allowance.toCurrencyString}"),
            elementTextByID("taxable-dividend-income")(calculationDataSuccessWithEoYModel.taxableDividendIncome.toCurrencyString),
            elementTextByID("dividend-brt-calc")(calculationDataSuccessWithEoYModel.dividends.basicBand.taxableIncome.toCurrencyString),
            elementTextByID("dividend-brt-rate")(calculationDataSuccessWithEoYModel.dividends.basicBand.taxRate.toStringNoDecimal),
            elementTextByID("dividend-brt-amount")(calculationDataSuccessWithEoYModel.dividends.basicBand.taxAmount.toCurrencyString),
            elementTextByID("dividend-hrt-calc")(calculationDataSuccessWithEoYModel.dividends.higherBand.taxableIncome.toCurrencyString),
            elementTextByID("dividend-hrt-rate")(calculationDataSuccessWithEoYModel.dividends.higherBand.taxRate.toStringNoDecimal),
            elementTextByID("dividend-hrt-amount")(calculationDataSuccessWithEoYModel.dividends.higherBand.taxAmount.toCurrencyString),
            elementTextByID("dividend-art-calc")(calculationDataSuccessWithEoYModel.dividends.additionalBand.taxableIncome.toCurrencyString),
            elementTextByID("dividend-art-rate")(calculationDataSuccessWithEoYModel.dividends.additionalBand.taxRate.toStringNoDecimal),
            elementTextByID("dividend-art-amount")(calculationDataSuccessWithEoYModel.dividends.additionalBand.taxAmount.toCurrencyString),
            elementTextByID("nic2-amount")(calculationDataSuccessWithEoYModel.nic.class2.toCurrencyString),
            elementTextByID("nic4-amount")(calculationDataSuccessWithEoYModel.nic.class4.toCurrencyString),
            elementTextByID("total-estimate")(calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd.toCurrencyString),
            elementTextByID("payment")("-" + financialTransactions.financialTransactions.get.head.clearedAmount.get.toCurrencyString),
            elementTextByID("owed")(financialTransactions.financialTransactions.get.head.outstandingAmount.get.toCurrencyString)
          )
        }
      }

      "a successful response is retrieved for the financial transactions and there is no outstanding amount (paid)" should {

        "return the correct page with a valid total" in {

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, crystallisedLastTaxCalcResponse)

          And("I wiremock stub a successful Get CalculationData response")
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

          And("I wiremock stub a successful paid Financial Transactions response")
          val financialTransactions = financialTransactionsJson(0.0).as[FinancialTransactionsModel]
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsJson(0.0))

          When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)
          verifyLastTaxCalculationCall(testNino, testYear)
          verifyCalculationDataCall(testNino, testCalcId)
          verifyFinancialTransactionsCall(testMtditid)

          res should have(
            httpStatus(OK),
            pageTitle(messages.heading(testYearInt)),
            elementTextByID("heading")(messages.heading(testYearInt)),
            elementTextByID("sub-heading")(messages.CrystalisedTaxAmount.subHeading),
            isElementVisibleById("calcBreakdown")(expectedValue = false),
            elementTextByID("business-profit")(totalProfit(calculationDataSuccessWithEoYModel, includeInterest = false)),
            elementTextByID("savings-income")(calculationDataSuccessWithEoYModel.incomeReceived.bankBuildingSocietyInterest.toCurrencyString),
            elementTextByID("personal-allowance")(s"-${totalAllowance(calculationDataSuccessWithEoYModel)}"),
            elementTextByID("additional-allowances")("-" + calculationDataSuccessWithEoYModel.additionalAllowances.toCurrencyString),
            elementTextByID("taxable-income")(taxableIncome(calculationDataSuccessWithEoYModel)),
            elementTextByID("brt-it-calc")((calculationDataSuccessWithEoYModel.payPensionsProfit.basicBand.taxableIncome + calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxableIncome).toCurrencyString),
            elementTextByID("brt-rate")(calculationDataSuccessWithEoYModel.payPensionsProfit.basicBand.taxRate.toStringNoDecimal),
            elementTextByID("brt-amount")((calculationDataSuccessWithEoYModel.payPensionsProfit.basicBand.taxAmount + calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxAmount).toCurrencyString),
            elementTextByID("hrt-it-calc")((calculationDataSuccessWithEoYModel.payPensionsProfit.higherBand.taxableIncome + calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxableIncome).toCurrencyString),
            elementTextByID("hrt-rate")(calculationDataSuccessWithEoYModel.payPensionsProfit.higherBand.taxRate.toStringNoDecimal),
            elementTextByID("hrt-amount")((calculationDataSuccessWithEoYModel.payPensionsProfit.higherBand.taxAmount + calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxAmount).toCurrencyString),
            elementTextByID("art-it-calc")((calculationDataSuccessWithEoYModel.payPensionsProfit.additionalBand.taxableIncome + calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxableIncome).toCurrencyString),
            elementTextByID("art-rate")(calculationDataSuccessWithEoYModel.payPensionsProfit.additionalBand.taxRate.toStringNoDecimal),
            elementTextByID("art-amount")((calculationDataSuccessWithEoYModel.payPensionsProfit.additionalBand.taxAmount + calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxAmount).toCurrencyString),
            elementTextByID("dividend-income")(calculationDataSuccessWithEoYModel.incomeReceived.ukDividends.toCurrencyString),
            elementTextByID("dividend-allowance")(s"-${calculationDataSuccessWithEoYModel.dividends.allowance.toCurrencyString}"),
            elementTextByID("taxable-dividend-income")(calculationDataSuccessWithEoYModel.taxableDividendIncome.toCurrencyString),
            elementTextByID("dividend-brt-calc")(calculationDataSuccessWithEoYModel.dividends.basicBand.taxableIncome.toCurrencyString),
            elementTextByID("dividend-brt-rate")(calculationDataSuccessWithEoYModel.dividends.basicBand.taxRate.toStringNoDecimal),
            elementTextByID("dividend-brt-amount")(calculationDataSuccessWithEoYModel.dividends.basicBand.taxAmount.toCurrencyString),
            elementTextByID("dividend-hrt-calc")(calculationDataSuccessWithEoYModel.dividends.higherBand.taxableIncome.toCurrencyString),
            elementTextByID("dividend-hrt-rate")(calculationDataSuccessWithEoYModel.dividends.higherBand.taxRate.toStringNoDecimal),
            elementTextByID("dividend-hrt-amount")(calculationDataSuccessWithEoYModel.dividends.higherBand.taxAmount.toCurrencyString),
            elementTextByID("dividend-art-calc")(calculationDataSuccessWithEoYModel.dividends.additionalBand.taxableIncome.toCurrencyString),
            elementTextByID("dividend-art-rate")(calculationDataSuccessWithEoYModel.dividends.additionalBand.taxRate.toStringNoDecimal),
            elementTextByID("dividend-art-amount")(calculationDataSuccessWithEoYModel.dividends.additionalBand.taxAmount.toCurrencyString),
            elementTextByID("nic2-amount")(calculationDataSuccessWithEoYModel.nic.class2.toCurrencyString),
            elementTextByID("nic4-amount")(calculationDataSuccessWithEoYModel.nic.class4.toCurrencyString),
            elementTextByID("total-estimate")(calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd.toCurrencyString),
            elementTextByID("payment")("-" + financialTransactions.financialTransactions.get.head.clearedAmount.get.toCurrencyString),
            elementTextByID("owed")(financialTransactions.financialTransactions.get.head.outstandingAmount.get.toCurrencyString)
          )
        }
      }

      "an error response is retrieve for the financial transactions" should {

        "return an Internal Server Error page" in {

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, crystallisedLastTaxCalcResponse)

          And("I wiremock stub a successful Get CalculationData response")
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

          And("I wiremock stub an errored Financial Transactions response")
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsSingleErrorJson)

          When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)
          verifyLastTaxCalculationCall(testNino, testYear)
          verifyCalculationDataCall(testNino, testCalcId)
          verifyFinancialTransactionsCall(testMtditid)

          res should have(httpStatus(INTERNAL_SERVER_ERROR))

        }
      }
    }

    "isAuthorisedUser with an active enrolment, valid last calc estimate, valid breakdown response but NO EoY Estimate" should {

      "return the correct page with a valid total" in {

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub multiple open and received obligations response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, estimateLastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessJson.toString())

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)
        verifyLastTaxCalculationCall(testNino, testYear)
        verifyCalculationDataCall(testNino, testCalcId)

        res should have (
          httpStatus(OK),
          pageTitle(messages.title(testYearInt)),
          elementTextByID("inYearEstimateHeading")(messages.EstimatedTaxAmount.currentEstimate(calculationDataSuccessModel.totalIncomeTaxNicYtd.toCurrencyString)),
          elementTextByID("heading")(messages.heading(testYearInt)),
          elementTextByID("sub-heading")(messages.EstimatedTaxAmount.subHeading),
          elementTextByID("business-profit")(totalProfit(calculationDataSuccessModel)),
          elementTextByID("personal-allowance")(s"-${totalAllowance(calculationDataSuccessModel)}"),
          elementTextByID("additional-allowances")("-" + calculationDataSuccessModel.additionalAllowances.toCurrencyString),
          elementTextByID("taxable-income")(taxableIncome(calculationDataSuccessModel)),
          elementTextByID("brt-it-calc")((calculationDataSuccessModel.payPensionsProfit.basicBand.taxableIncome + calculationDataSuccessModel.savingsAndGains.basicBand.taxableIncome).toCurrencyString),
          elementTextByID("brt-rate")(calculationDataSuccessModel.payPensionsProfit.basicBand.taxRate.toStringNoDecimal),
          elementTextByID("brt-amount")((calculationDataSuccessModel.payPensionsProfit.basicBand.taxAmount + calculationDataSuccessModel.savingsAndGains.basicBand.taxAmount).toCurrencyString),
          elementTextByID("hrt-it-calc")((calculationDataSuccessModel.payPensionsProfit.higherBand.taxableIncome + calculationDataSuccessModel.savingsAndGains.higherBand.taxableIncome).toCurrencyString),
          elementTextByID("hrt-rate")(calculationDataSuccessModel.payPensionsProfit.higherBand.taxRate.toStringNoDecimal),
          elementTextByID("hrt-amount")((calculationDataSuccessModel.payPensionsProfit.higherBand.taxAmount + calculationDataSuccessModel.savingsAndGains.higherBand.taxAmount).toCurrencyString),
          elementTextByID("art-it-calc")((calculationDataSuccessModel.payPensionsProfit.additionalBand.taxableIncome + calculationDataSuccessModel.savingsAndGains.additionalBand.taxableIncome).toCurrencyString),
          elementTextByID("art-rate")(calculationDataSuccessModel.payPensionsProfit.additionalBand.taxRate.toStringNoDecimal),
          elementTextByID("art-amount")((calculationDataSuccessModel.payPensionsProfit.additionalBand.taxAmount + calculationDataSuccessModel.savingsAndGains.additionalBand.taxAmount).toCurrencyString),
          elementTextByID("dividend-income")(calculationDataSuccessModel.incomeReceived.ukDividends.toCurrencyString),
          elementTextByID("dividend-allowance")(s"-${calculationDataSuccessModel.dividends.allowance.toCurrencyString}"),
          elementTextByID("taxable-dividend-income")(calculationDataSuccessModel.taxableDividendIncome.toCurrencyString),
          elementTextByID("dividend-brt-calc")(calculationDataSuccessModel.dividends.basicBand.taxableIncome.toCurrencyString),
          elementTextByID("dividend-brt-rate")(calculationDataSuccessModel.dividends.basicBand.taxRate.toStringNoDecimal),
          elementTextByID("dividend-brt-amount")(calculationDataSuccessModel.dividends.basicBand.taxAmount.toCurrencyString),
          elementTextByID("dividend-hrt-calc")(calculationDataSuccessModel.dividends.higherBand.taxableIncome.toCurrencyString),
          elementTextByID("dividend-hrt-rate")(calculationDataSuccessModel.dividends.higherBand.taxRate.toStringNoDecimal),
          elementTextByID("dividend-hrt-amount")(calculationDataSuccessModel.dividends.higherBand.taxAmount.toCurrencyString),
          elementTextByID("dividend-art-calc")(calculationDataSuccessModel.dividends.additionalBand.taxableIncome.toCurrencyString),
          elementTextByID("dividend-art-rate")(calculationDataSuccessModel.dividends.additionalBand.taxRate.toStringNoDecimal),
          elementTextByID("dividend-art-amount")(calculationDataSuccessModel.dividends.additionalBand.taxAmount.toCurrencyString),
          elementTextByID("nic2-amount")(calculationDataSuccessModel.nic.class2.toCurrencyString),
          elementTextByID("nic4-amount")(calculationDataSuccessModel.nic.class4.toCurrencyString),
          elementTextByID("total-estimate")(calculationDataSuccessModel.totalIncomeTaxNicYtd.toCurrencyString),
          isElementVisibleById("eoyEstimate")(expectedValue = false)
        )
      }
    }

    "isAuthorisedUser with an active enrolment, valid last tax estimate response, but error in calc breakdown" should {

      "Return the estimated tax liability without the calculation breakdown" in {

        stubUserDetailsError()

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub multiple open and received obligations response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

        And("a successful Get Last Estimated Tax Liability response via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, estimateLastTaxCalcResponse)

        And("I wiremock stub an erroneous response")
        SelfAssessmentStub.stubGetCalcDataError(testNino, testCalcId, calculationDataErrorModel)

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)
        verifyLastTaxCalculationCall(testNino, testYear)
        verifyCalculationDataCall(testNino, testCalcId)

        Then("a successful response is returned with the correct estimate")
        res should have(
          httpStatus(OK),
          pageTitle(messages.title(testYearInt)),
          elementTextByID("inYearEstimateHeading")(messages.EstimatedTaxAmount.currentEstimate(calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd.toCurrencyString)),
          elementTextByID("inYearP1")(messages.EstimatedTaxAmount.inYearp1("6 July 2017", testYearInt)),
          isElementVisibleById("inYearCalcBreakdown")(expectedValue = false)
        )
      }
    }

    "isAuthorisedUser with an active enrolment no data found response from Last Calculation" should {

      "Return no data found response and render view explaining that this will be available once they've submitted income" in {

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub multiple open and received obligations response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

        And("a No Data Found response from Get Last Estimated Tax Liability via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLastCalcNoData(testNino, testYear)

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)
        verifyLastTaxCalculationCall(testNino, testYear)

        Then("a Not Found response is returned and correct view rendered")
        res should have(
          httpStatus(NOT_FOUND),
          pageTitle(messages.title(testYearInt))
        )
      }
    }

    "isAuthorisedUser with an active enrolment but error response from Get Last Calculation" should {

      "Render the Estimated Tax Liability Error Page" in {

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub multiple open and received obligations response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

        And("an Error Response response from Get Last Estimated Tax Liability via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLastCalcError(testNino, testYear)

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)
        verifyLastTaxCalculationCall(testNino, testYear)

        Then("an Internal Server Error response is returned and correct view rendered")
        res should have(
          httpStatus(OK),
          pageTitle(messages.title(testYearInt)),
          elementTextByID("p1")(messages.internalServerErrorp1),
          elementTextByID("p2")(messages.internalServerErrorp2)
        )
      }
    }

    unauthorisedTest("/calculation/" + testYear)
  }
}
