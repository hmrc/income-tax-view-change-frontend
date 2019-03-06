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
import config.FrontendAppConfig
import helpers.servicemocks._
import helpers.ComponentSpecBase
import models.calculation.CalculationDataModel
import models.financialTransactions.FinancialTransactionsModel
import play.api.http.Status
import play.api.http.Status._
import implicits.ImplicitCurrencyFormatter._

class CalculationControllerISpec extends ComponentSpecBase {

  lazy val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  def totalProfit(calc: CalculationDataModel, includeInterest: Boolean = true): String = {
    import calc.incomeReceived._
    selfEmployment + ukProperty + (if(includeInterest) bankBuildingSocietyInterest else 0)
  }.toCurrencyString

  def allowance(calc: CalculationDataModel): String = calc.personalAllowance.toCurrencyString

  def taxableIncome(calc: CalculationDataModel): String =
    (calc.totalTaxableIncome - calc.taxableDividendIncome).toCurrencyString

  private trait CalculationDataApiDisabled {
    appConfig.features.calcDataApiEnabled(false)
  }

  "Calling the CalculationController.getEstimatedTaxLiability(year)" when {

    "isAuthorisedUser with an active enrolment, valid last calc estimate, valid breakdown response, an EoY Estimate and feature switch is enabled" should {

      "return the correct page with a valid total" in {

        appConfig.features.calcBreakdownEnabled(true)

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, estimateLastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyLastTaxCalculationCall(testNino, testYear)
        verifyCalculationDataCall(testNino, testCalcId)

        val brtBand = calculationDataSuccessWithEoYModel.payAndPensionsProfitBands.find(_.name == "BRT").get
        val hrtBand = calculationDataSuccessWithEoYModel.payAndPensionsProfitBands.find(_.name == "HRT").get
        val artBand = calculationDataSuccessWithEoYModel.payAndPensionsProfitBands.find(_.name == "ART").get

        res should have (
          httpStatus(OK),
          pageTitle(messages.title(testYearInt)),
          elementTextByID("inYearEstimateHeading")(messages.EstimatedTaxAmount.currentEstimate(calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd.toCurrencyString)),
          elementTextByID("heading")(messages.heading(testYearInt)),
          elementTextByID("sub-heading")(messages.EstimatedTaxAmount.subHeading),
          elementTextByID("business-profit")(totalProfit(calculationDataSuccessWithEoYModel)),
          elementTextByID("personal-allowance")(s"-${allowance(calculationDataSuccessWithEoYModel)}"),
          elementTextByID("additional-allowances")("-" + calculationDataSuccessWithEoYModel.additionalAllowances.toCurrencyString),
          elementTextByID("taxable-income")(taxableIncome(calculationDataSuccessWithEoYModel)),
          elementTextByID("BRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${brtBand.income.toCurrencyString} at ${brtBand.rate.toStringNoDecimal}%)"),
          elementTextByID("BRTPpp-amount")(brtBand.amount.toCurrencyString),
          elementTextByID("HRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${hrtBand.income.toCurrencyString} at ${hrtBand.rate.toStringNoDecimal}%)"),
          elementTextByID("HRTPpp-amount")(hrtBand.amount.toCurrencyString),
          elementTextByID("ARTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${artBand.income.toCurrencyString} at ${artBand.rate.toStringNoDecimal}%)"),
          elementTextByID("ARTPpp-amount")(artBand.amount.toCurrencyString),
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
          verifyLastTaxCalculationCall(testNino, testYear)
          verifyCalculationDataCall(testNino, testCalcId)
          verifyFinancialTransactionsCall(testMtditid)

          val brtBand = calculationDataSuccessWithEoYModel.payAndPensionsProfitBands.find(_.name == "BRT").get
          val hrtBand = calculationDataSuccessWithEoYModel.payAndPensionsProfitBands.find(_.name == "HRT").get
          val artBand = calculationDataSuccessWithEoYModel.payAndPensionsProfitBands.find(_.name == "ART").get

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
            elementTextByID("personal-allowance")(s"-${allowance(calculationDataSuccessWithEoYModel)}"),
            elementTextByID("additional-allowances")("-" + calculationDataSuccessWithEoYModel.additionalAllowances.toCurrencyString),
            elementTextByID("taxable-income")(taxableIncome(calculationDataSuccessWithEoYModel)),
            elementTextByID("BRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${brtBand.income.toCurrencyString} at ${brtBand.rate.toStringNoDecimal}%)"),
            elementTextByID("BRTPpp-amount")(brtBand.amount.toCurrencyString),
            elementTextByID("HRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${hrtBand.income.toCurrencyString} at ${hrtBand.rate.toStringNoDecimal}%)"),
            elementTextByID("HRTPpp-amount")(hrtBand.amount.toCurrencyString),
            elementTextByID("ARTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${artBand.income.toCurrencyString} at ${artBand.rate.toStringNoDecimal}%)"),
            elementTextByID("ARTPpp-amount")(artBand.amount.toCurrencyString),
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
          verifyLastTaxCalculationCall(testNino, testYear)
          verifyCalculationDataCall(testNino, testCalcId)
          verifyFinancialTransactionsCall(testMtditid)

          val brtBand = calculationDataSuccessWithEoYModel.payAndPensionsProfitBands.find(_.name == "BRT").get
          val hrtBand = calculationDataSuccessWithEoYModel.payAndPensionsProfitBands.find(_.name == "HRT").get
          val artBand = calculationDataSuccessWithEoYModel.payAndPensionsProfitBands.find(_.name == "ART").get

          res should have(
            httpStatus(OK),
            pageTitle(messages.heading(testYearInt)),
            elementTextByID("heading")(messages.heading(testYearInt)),
            elementTextByID("sub-heading")(messages.CrystalisedTaxAmount.subHeading),
            isElementVisibleById("calcBreakdown")(expectedValue = false),
            elementTextByID("business-profit")(totalProfit(calculationDataSuccessWithEoYModel, includeInterest = false)),
            elementTextByID("savings-income")(calculationDataSuccessWithEoYModel.incomeReceived.bankBuildingSocietyInterest.toCurrencyString),
            elementTextByID("personal-allowance")(s"-${allowance(calculationDataSuccessWithEoYModel)}"),
            elementTextByID("additional-allowances")("-" + calculationDataSuccessWithEoYModel.additionalAllowances.toCurrencyString),
            elementTextByID("taxable-income")(taxableIncome(calculationDataSuccessWithEoYModel)),
            elementTextByID("BRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${brtBand.income.toCurrencyString} at ${brtBand.rate.toStringNoDecimal}%)"),
            elementTextByID("BRTPpp-amount")(brtBand.amount.toCurrencyString),
            elementTextByID("HRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${hrtBand.income.toCurrencyString} at ${hrtBand.rate.toStringNoDecimal}%)"),
            elementTextByID("HRTPpp-amount")(hrtBand.amount.toCurrencyString),
            elementTextByID("ARTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${artBand.income.toCurrencyString} at ${artBand.rate.toStringNoDecimal}%)"),
            elementTextByID("ARTPpp-amount")(artBand.amount.toCurrencyString),
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

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, crystallisedLastTaxCalcResponse)

          And("I wiremock stub a successful Get CalculationData response")
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

          And("I wiremock stub an errored Financial Transactions response")
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsSingleErrorJson)

          When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          verifyIncomeSourceDetailsCall(testMtditid)
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

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, estimateLastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessJson.toString())

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyLastTaxCalculationCall(testNino, testYear)
        verifyCalculationDataCall(testNino, testCalcId)

        val brtBand = calculationDataSuccessModel.payAndPensionsProfitBands.find(_.name == "BRT").get
        val hrtBand = calculationDataSuccessModel.payAndPensionsProfitBands.find(_.name == "HRT").get
        val artBand = calculationDataSuccessModel.payAndPensionsProfitBands.find(_.name == "ART").get

        res should have (
          httpStatus(OK),
          pageTitle(messages.title(testYearInt)),
          elementTextByID("inYearEstimateHeading")(messages.EstimatedTaxAmount.currentEstimate(calculationDataSuccessModel.totalIncomeTaxNicYtd.toCurrencyString)),
          elementTextByID("heading")(messages.heading(testYearInt)),
          elementTextByID("sub-heading")(messages.EstimatedTaxAmount.subHeading),
          elementTextByID("business-profit")(totalProfit(calculationDataSuccessModel)),
          elementTextByID("personal-allowance")(s"-${allowance(calculationDataSuccessModel)}"),
          elementTextByID("additional-allowances")("-" + calculationDataSuccessModel.additionalAllowances.toCurrencyString),
          elementTextByID("taxable-income")(taxableIncome(calculationDataSuccessModel)),
          elementTextByID("BRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${brtBand.income.toCurrencyString} at ${brtBand.rate.toStringNoDecimal}%)"),
          elementTextByID("BRTPpp-amount")(brtBand.amount.toCurrencyString),
          elementTextByID("HRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${hrtBand.income.toCurrencyString} at ${hrtBand.rate.toStringNoDecimal}%)"),
          elementTextByID("HRTPpp-amount")(hrtBand.amount.toCurrencyString),
          elementTextByID("ARTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${artBand.income.toCurrencyString} at ${artBand.rate.toStringNoDecimal}%)"),
          elementTextByID("ARTPpp-amount")(artBand.amount.toCurrencyString),
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

        And("a successful Get Last Estimated Tax Liability response via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, estimateLastTaxCalcResponse)

        And("I wiremock stub an erroneous response")
        SelfAssessmentStub.stubGetCalcDataError(testNino, testCalcId, calculationDataErrorModel)

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
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

        And("a No Data Found response from Get Last Estimated Tax Liability via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLastCalcNoData(testNino, testYear)

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
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

        And("an Error Response response from Get Last Estimated Tax Liability via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLastCalcError(testNino, testYear)

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
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

    "isAuthorisedUser with an active enrolment and the Get Calculation Data API feature is disabled" should {

      "return the correct page with a valid total" in new CalculationDataApiDisabled {

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, latestCalcModel)

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyLatestCalculationCall(testNino, testYear)

        res should have (
          httpStatus(OK),
          pageTitle(messages.title(testYearInt)),
          elementTextByID("heading")(messages.heading(testYearInt)),
          isElementVisibleById("eoyEstimate")(expectedValue = true),
          elementTextByID("inYearEstimateHeading")(messages.EstimatedTaxAmount.currentEstimate(latestCalcModel.displayAmount.get.toCurrencyString))
        )
      }
    }

    unauthorisedTest("/calculation/" + testYear)
  }
}
