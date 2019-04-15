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
import assets.messages.{CalculationMessages => messages}
import config.FrontendAppConfig
import helpers.ComponentSpecBase
import helpers.servicemocks._
import implicits.ImplicitCurrencyFormatter._
import models.calculation.CalculationDataModel
import models.financialTransactions.FinancialTransactionsModel
import play.api.http.Status
import play.api.http.Status._

class CalculationControllerISpec extends ComponentSpecBase {

  lazy val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  def totalProfit(calc: CalculationDataModel, includeInterest: Boolean = true): String = {
    import calc.incomeReceived._
    selfEmployment + ukProperty + (if(includeInterest) bankBuildingSocietyInterest else 0)
  }.toCurrencyString

  def allowance(calc: CalculationDataModel): String = calc.personalAllowance.toCurrencyString
  def savingsAllowance(calc: CalculationDataModel): String = calc.savingsAllowanceSummaryData.toCurrencyString

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

        And("I wiremock stub a successful Get Last latest Tax calculation response")
        IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationResponseWithEOY)

        And("I wiremock stub a successful Get CalculationData response")
        SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyLatestCalculationCall(testNino, testYear)

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
          elementTextByID("savings-allowance")(s"-${savingsAllowance(calculationDataSuccessWithEoYModel)}"),
          elementTextByID("additional-allowances")("-" + calculationDataSuccessWithEoYModel.additionalAllowances.toCurrencyString),
          elementTextByID("taxable-income")(taxableIncome(calculationDataSuccessWithEoYModel)),
          elementTextByID("BRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${brtBand.income.toCurrencyString} at ${brtBand.rate.toStringNoDecimal}%)"),
          elementTextByID("BRTPpp-amount")(brtBand.taxAmount.toCurrencyString),
          elementTextByID("HRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${hrtBand.income.toCurrencyString} at ${hrtBand.rate.toStringNoDecimal}%)"),
          elementTextByID("HRTPpp-amount")(hrtBand.taxAmount.toCurrencyString),
          elementTextByID("ARTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${artBand.income.toCurrencyString} at ${artBand.rate.toStringNoDecimal}%)"),
          elementTextByID("ARTPpp-amount")(artBand.taxAmount.toCurrencyString),
          elementTextByID("dividend-income")(calculationDataSuccessWithEoYModel.incomeReceived.ukDividends.toCurrencyString),
          elementTextByID("dividend-allowance")(s"-${calculationDataSuccessWithEoYModel.dividends.totalAmount.toCurrencyString}"),
          elementTextByID("taxable-dividend-income")(calculationDataSuccessWithEoYModel.taxableDividendIncome.toCurrencyString),
          elementTextByID("srtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.startBand.taxableIncome.toCurrencyString),
          elementTextByID("srtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.startBand.taxRate.toStringNoDecimal),
          elementTextByID("srtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.startBand.taxAmount.toCurrencyString),
          elementTextByID("zrtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.zeroBand.taxableIncome.toCurrencyString),
          elementTextByID("zrtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.zeroBand.taxRate.toStringNoDecimal),
          elementTextByID("zrtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.zeroBand.taxAmount.toCurrencyString),
          elementTextByID("brtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxableIncome.toCurrencyString),
          elementTextByID("brtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxRate.toStringNoDecimal),
          elementTextByID("brtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxAmount.toCurrencyString),
          elementTextByID("hrtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxableIncome.toCurrencyString),
          elementTextByID("hrtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxRate.toStringNoDecimal),
          elementTextByID("hrtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxAmount.toCurrencyString),
          elementTextByID("artSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxableIncome.toCurrencyString),
          elementTextByID("artSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxRate.toStringNoDecimal),
          elementTextByID("artSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxAmount.toCurrencyString),
          elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(0).name}-calc")(calculationDataSuccessWithEoYModel.dividends.band(0).income.toCurrencyString),
          elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(0).name}-rate")(calculationDataSuccessWithEoYModel.dividends.band(0).rate.toStringNoDecimal),
          elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(0).name}-amount")(calculationDataSuccessWithEoYModel.dividends.band(0).amount.toCurrencyString),
          elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(1).name}-calc")(calculationDataSuccessWithEoYModel.dividends.band(1).income.toCurrencyString),
          elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(1).name}-rate")(calculationDataSuccessWithEoYModel.dividends.band(1).rate.toStringNoDecimal),
          elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(1).name}-amount")(calculationDataSuccessWithEoYModel.dividends.band(1).amount.toCurrencyString),
          elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(2).name}-calc")(calculationDataSuccessWithEoYModel.dividends.band(2).income.toCurrencyString),
          elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(2).name}-rate")(calculationDataSuccessWithEoYModel.dividends.band(2).rate.toStringNoDecimal),
          elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(2).name}-amount")(calculationDataSuccessWithEoYModel.dividends.band(2).amount.toCurrencyString),
          elementTextByID("nic2-amount")(calculationDataSuccessWithEoYModel.nic.class2.toCurrencyString),
          elementTextByID("nic4-amount")(calculationDataSuccessWithEoYModel.nic.class4.toCurrencyString),
          elementTextByID("total-estimate")(calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd.toCurrencyString),
          elementTextByID("business-profit-bbs-interest")(calculationDataSuccessWithEoYModel.incomeReceived.bankBuildingSocietyInterest.toCurrencyString),
          isElementVisibleById("eoyEstimate")(expectedValue = true),
          isElementVisibleById("gift-aid")(expectedValue = false),
          isElementVisibleById("business-profit-self-employed-section")(expectedValue = true),
          isElementVisibleById("business-profit-property-section")(expectedValue = true),
          isElementVisibleById("business-profit-bbs-interest-section")(expectedValue = true)

        )
      }
    }

    "isAuthorisedUser with an active enrolment, valid latest calc estimate, valid breakdown response and Crystallised EoY amount" when {

      "a successful response is retrieved for the financial transactions and there is an outstanding amount (unpaid)" should {

        "return the correct page with a valid total" in {

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I wiremock stub a successful Get Latest Tax Calculation response")
          IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationCrystallisedResponse)

          And("I wiremock stub a successful Get CalculationData response")
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

          And("I wiremock stub a successful Unpaid Financial Transactions response")
          val financialTransactions = financialTransactionsJson(1000.0).as[FinancialTransactionsModel]
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsJson(1000.0))

          When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyLatestCalculationCall(testNino, testYear)
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
            elementTextByID("personal-allowance")(s"-${allowance(calculationDataSuccessWithEoYModel)}"),
            elementTextByID("savings-allowance")(s"-${savingsAllowance(calculationDataSuccessWithEoYModel)}"),
            elementTextByID("additional-allowances")("-" + calculationDataSuccessWithEoYModel.additionalAllowances.toCurrencyString),
            elementTextByID("taxable-income")(taxableIncome(calculationDataSuccessWithEoYModel)),
            elementTextByID("BRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${brtBand.income.toCurrencyString} at ${brtBand.rate.toStringNoDecimal}%)"),
            elementTextByID("BRTPpp-amount")(brtBand.taxAmount.toCurrencyString),
            elementTextByID("HRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${hrtBand.income.toCurrencyString} at ${hrtBand.rate.toStringNoDecimal}%)"),
            elementTextByID("HRTPpp-amount")(hrtBand.taxAmount.toCurrencyString),
            elementTextByID("ARTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${artBand.income.toCurrencyString} at ${artBand.rate.toStringNoDecimal}%)"),
            elementTextByID("ARTPpp-amount")(artBand.taxAmount.toCurrencyString),
            elementTextByID("srtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.startBand.taxableIncome.toCurrencyString),
            elementTextByID("srtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.startBand.taxRate.toStringNoDecimal),
            elementTextByID("srtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.startBand.taxAmount.toCurrencyString),
            elementTextByID("zrtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.zeroBand.taxableIncome.toCurrencyString),
            elementTextByID("zrtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.zeroBand.taxRate.toStringNoDecimal),
            elementTextByID("zrtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.zeroBand.taxAmount.toCurrencyString),
            elementTextByID("brtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxableIncome.toCurrencyString),
            elementTextByID("brtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxRate.toStringNoDecimal),
            elementTextByID("brtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxAmount.toCurrencyString),
            elementTextByID("hrtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxableIncome.toCurrencyString),
            elementTextByID("hrtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxRate.toStringNoDecimal),
            elementTextByID("hrtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxAmount.toCurrencyString),
            elementTextByID("artSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxableIncome.toCurrencyString),
            elementTextByID("artSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxRate.toStringNoDecimal),
            elementTextByID("artSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxAmount.toCurrencyString),
            elementTextByID("dividend-income")(calculationDataSuccessWithEoYModel.incomeReceived.ukDividends.toCurrencyString),
            elementTextByID("dividend-allowance")(s"-£2,500"),
            elementTextByID("taxable-dividend-income")(calculationDataSuccessWithEoYModel.taxableDividendIncome.toCurrencyString),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(0).name}-calc")(calculationDataSuccessWithEoYModel.dividends.band(0).income.toCurrencyString),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(0).name}-rate")(calculationDataSuccessWithEoYModel.dividends.band(0).rate.toStringNoDecimal),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(0).name}-amount")(calculationDataSuccessWithEoYModel.dividends.band(0).amount.toCurrencyString),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(1).name}-calc")(calculationDataSuccessWithEoYModel.dividends.band(1).income.toCurrencyString),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(1).name}-rate")(calculationDataSuccessWithEoYModel.dividends.band(1).rate.toStringNoDecimal),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(1).name}-amount")(calculationDataSuccessWithEoYModel.dividends.band(1).amount.toCurrencyString),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(2).name}-calc")(calculationDataSuccessWithEoYModel.dividends.band(2).income.toCurrencyString),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(2).name}-rate")(calculationDataSuccessWithEoYModel.dividends.band(2).rate.toStringNoDecimal),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(2).name}-amount")(calculationDataSuccessWithEoYModel.dividends.band(2).amount.toCurrencyString),
            elementTextByID("nic2-amount")(calculationDataSuccessWithEoYModel.nic.class2.toCurrencyString),
            elementTextByID("nic4-amount")(calculationDataSuccessWithEoYModel.nic.class4.toCurrencyString),
            elementTextByID("total-estimate")(calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd.toCurrencyString),
            elementTextByID("payment")("-" + financialTransactions.financialTransactions.get.head.clearedAmount.get.toCurrencyString),
            elementTextByID("owed")(financialTransactions.financialTransactions.get.head.outstandingAmount.get.toCurrencyString),
            elementTextByID("business-profit-bbs-interest")(calculationDataSuccessWithEoYModel.incomeReceived.bankBuildingSocietyInterest.toCurrencyString),
            isElementVisibleById("business-profit-self-employed-section")(expectedValue = true),
            isElementVisibleById("business-profit-property-section")(expectedValue = true),
            isElementVisibleById("business-profit-bbs-interest-section")(expectedValue = true)
          )
        }
      }

      "a successful response is retrieved for the financial transactions and there is no outstanding amount (paid)" should {

        "return the correct page with a valid total" in {

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I wiremock stub a successful Get Latest Tax Calculation response")
          IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationCrystallisedResponse)

          And("I wiremock stub a successful Get CalculationData response")
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

          And("I wiremock stub a successful paid Financial Transactions response")
          val financialTransactions = financialTransactionsJson(0.0).as[FinancialTransactionsModel]
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsJson(0.0))

          When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyLatestCalculationCall(testNino, testYear)
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
            elementTextByID("personal-allowance")(s"-${allowance(calculationDataSuccessWithEoYModel)}"),
            elementTextByID("additional-allowances")("-" + calculationDataSuccessWithEoYModel.additionalAllowances.toCurrencyString),
            elementTextByID("savings-allowance")(s"-${savingsAllowance(calculationDataSuccessWithEoYModel)}"),
            elementTextByID("taxable-income")(taxableIncome(calculationDataSuccessWithEoYModel)),
            elementTextByID("BRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${brtBand.income.toCurrencyString} at ${brtBand.rate.toStringNoDecimal}%)"),
            elementTextByID("BRTPpp-amount")(brtBand.taxAmount.toCurrencyString),
            elementTextByID("HRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${hrtBand.income.toCurrencyString} at ${hrtBand.rate.toStringNoDecimal}%)"),
            elementTextByID("HRTPpp-amount")(hrtBand.taxAmount.toCurrencyString),
            elementTextByID("ARTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${artBand.income.toCurrencyString} at ${artBand.rate.toStringNoDecimal}%)"),
            elementTextByID("ARTPpp-amount")(artBand.taxAmount.toCurrencyString),
            elementTextByID("srtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.startBand.taxableIncome.toCurrencyString),
            elementTextByID("srtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.startBand.taxRate.toStringNoDecimal),
            elementTextByID("srtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.startBand.taxAmount.toCurrencyString),
            elementTextByID("zrtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.zeroBand.taxableIncome.toCurrencyString),
            elementTextByID("zrtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.zeroBand.taxRate.toStringNoDecimal),
            elementTextByID("zrtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.zeroBand.taxAmount.toCurrencyString),
            elementTextByID("brtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxableIncome.toCurrencyString),
            elementTextByID("brtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxRate.toStringNoDecimal),
            elementTextByID("brtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxAmount.toCurrencyString),
            elementTextByID("hrtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxableIncome.toCurrencyString),
            elementTextByID("hrtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxRate.toStringNoDecimal),
            elementTextByID("hrtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxAmount.toCurrencyString),
            elementTextByID("artSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxableIncome.toCurrencyString),
            elementTextByID("artSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxRate.toStringNoDecimal),
            elementTextByID("artSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxAmount.toCurrencyString),
            elementTextByID("dividend-income")(calculationDataSuccessWithEoYModel.incomeReceived.ukDividends.toCurrencyString),
            elementTextByID("dividend-allowance")(s"-£2,500"),
            elementTextByID("taxable-dividend-income")(calculationDataSuccessWithEoYModel.taxableDividendIncome.toCurrencyString),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(0).name}-calc")(calculationDataSuccessWithEoYModel.dividends.band(0).income.toCurrencyString),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(0).name}-rate")(calculationDataSuccessWithEoYModel.dividends.band(0).rate.toStringNoDecimal),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(0).name}-amount")(calculationDataSuccessWithEoYModel.dividends.band(0).amount.toCurrencyString),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(1).name}-calc")(calculationDataSuccessWithEoYModel.dividends.band(1).income.toCurrencyString),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(1).name}-rate")(calculationDataSuccessWithEoYModel.dividends.band(1).rate.toStringNoDecimal),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(1).name}-amount")(calculationDataSuccessWithEoYModel.dividends.band(1).amount.toCurrencyString),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(2).name}-calc")(calculationDataSuccessWithEoYModel.dividends.band(2).income.toCurrencyString),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(2).name}-rate")(calculationDataSuccessWithEoYModel.dividends.band(2).rate.toStringNoDecimal),
            elementTextByID(s"dividend-${calculationDataSuccessWithEoYModel.dividends.band(2).name}-amount")(calculationDataSuccessWithEoYModel.dividends.band(2).amount.toCurrencyString),
            elementTextByID("nic2-amount")(calculationDataSuccessWithEoYModel.nic.class2.toCurrencyString),
            elementTextByID("nic4-amount")(calculationDataSuccessWithEoYModel.nic.class4.toCurrencyString),
            elementTextByID("total-estimate")(calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd.toCurrencyString),
            elementTextByID("payment")("-" + financialTransactions.financialTransactions.get.head.clearedAmount.get.toCurrencyString),
            elementTextByID("owed")(financialTransactions.financialTransactions.get.head.outstandingAmount.get.toCurrencyString),
            elementTextByID("business-profit-bbs-interest")(calculationDataSuccessWithEoYModel.incomeReceived.bankBuildingSocietyInterest.toCurrencyString),
            isElementVisibleById("business-profit-self-employed-section")(expectedValue = true),
            isElementVisibleById("business-profit-property-section")(expectedValue = true),
            isElementVisibleById("business-profit-bbs-interest-section")(expectedValue = true)
          )
        }
      }

      "an error response is retrieve for the financial transactions" should {

        "return an Internal Server Error page" in {

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I wiremock stub a successful Get Latest Tax Calculation response")
          IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationCrystallisedResponse)

          And("I wiremock stub a successful Get CalculationData response")
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

          And("I wiremock stub an errored Financial Transactions response")
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsSingleErrorJson)

          When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyLatestCalculationCall(testNino, testYear)
          verifyFinancialTransactionsCall(testMtditid)

          res should have(httpStatus(INTERNAL_SERVER_ERROR))

        }
      }
    }

    "isAuthorisedUser with an active enrolment, valid latest calc estimate, valid breakdown response but NO EoY Estimate" should {

      "return the correct page with a valid total" in {

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationResponse)

        And("I wiremock stub a successful Get CalculationData response")
        SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessJson.toString())

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyLatestCalculationCall(testNino, testYear)

        val brtBand = calculationDataSuccessModel.payAndPensionsProfitBands.find(_.name == "BRT").get
        val hrtBand = calculationDataSuccessModel.payAndPensionsProfitBands.find(_.name == "HRT").get
        val artBand = calculationDataSuccessModel.payAndPensionsProfitBands.find(_.name == "ART").get

        res should have (
          httpStatus(OK),
          pageTitle(messages.title(testYearInt)),
          elementTextByID("heading")(messages.heading(testYearInt)),
          elementTextByID("sub-heading")(messages.EstimatedTaxAmount.subHeading),
          elementTextByID("business-profit")(totalProfit(calculationDataSuccessModel)),
          elementTextByID("personal-allowance")(s"-${allowance(calculationDataSuccessModel)}"),
          elementTextByID("savings-allowance")(s"-${savingsAllowance(calculationDataSuccessWithEoYModel)}"),
          elementTextByID("additional-allowances")("-" + calculationDataSuccessModel.additionalAllowances.toCurrencyString),
          elementTextByID("taxable-income")(taxableIncome(calculationDataSuccessModel)),
          elementTextByID("BRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${brtBand.income.toCurrencyString} at ${brtBand.rate.toStringNoDecimal}%)"),
          elementTextByID("BRTPpp-amount")(brtBand.taxAmount.toCurrencyString),
          elementTextByID("HRTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${hrtBand.income.toCurrencyString} at ${hrtBand.rate.toStringNoDecimal}%)"),
          elementTextByID("HRTPpp-amount")(hrtBand.taxAmount.toCurrencyString),
          elementTextByID("ARTPpp-it-calc-heading")(s"Pay, Pensions, Profit Income Tax (${artBand.income.toCurrencyString} at ${artBand.rate.toStringNoDecimal}%)"),
          elementTextByID("ARTPpp-amount")(artBand.taxAmount.toCurrencyString),
          elementTextByID("srtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.startBand.taxableIncome.toCurrencyString),
          elementTextByID("srtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.startBand.taxRate.toStringNoDecimal),
          elementTextByID("srtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.startBand.taxAmount.toCurrencyString),
          elementTextByID("zrtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.zeroBand.taxableIncome.toCurrencyString),
          elementTextByID("zrtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.zeroBand.taxRate.toStringNoDecimal),
          elementTextByID("zrtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.zeroBand.taxAmount.toCurrencyString),
          elementTextByID("brtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxableIncome.toCurrencyString),
          elementTextByID("brtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxRate.toStringNoDecimal),
          elementTextByID("brtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.basicBand.taxAmount.toCurrencyString),
          elementTextByID("hrtSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxableIncome.toCurrencyString),
          elementTextByID("hrtSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxRate.toStringNoDecimal),
          elementTextByID("hrtSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.higherBand.taxAmount.toCurrencyString),
          elementTextByID("artSi-it-calc")(calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxableIncome.toCurrencyString),
          elementTextByID("artSi-rate")(calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxRate.toStringNoDecimal),
          elementTextByID("artSi-amount")(calculationDataSuccessWithEoYModel.savingsAndGains.additionalBand.taxAmount.toCurrencyString),
          elementTextByID("dividend-income")(calculationDataSuccessModel.incomeReceived.ukDividends.toCurrencyString),
          elementTextByID("dividend-allowance")(s"-${calculationDataSuccessModel.dividends.totalAmount.toCurrencyString}"),
          elementTextByID("taxable-dividend-income")(calculationDataSuccessModel.taxableDividendIncome.toCurrencyString),
          elementTextByID(s"dividend-${calculationDataSuccessModel.dividends.band(0).name}-calc")(calculationDataSuccessModel.dividends.band(0).income.toCurrencyString),
          elementTextByID(s"dividend-${calculationDataSuccessModel.dividends.band(0).name}-rate")(calculationDataSuccessModel.dividends.band(0).rate.toStringNoDecimal),
          elementTextByID(s"dividend-${calculationDataSuccessModel.dividends.band(0).name}-amount")(calculationDataSuccessModel.dividends.band(0).amount.toCurrencyString),
          elementTextByID(s"dividend-${calculationDataSuccessModel.dividends.band(1).name}-calc")(calculationDataSuccessModel.dividends.band(1).income.toCurrencyString),
          elementTextByID(s"dividend-${calculationDataSuccessModel.dividends.band(1).name}-rate")(calculationDataSuccessModel.dividends.band(1).rate.toStringNoDecimal),
          elementTextByID(s"dividend-${calculationDataSuccessModel.dividends.band(1).name}-amount")(calculationDataSuccessModel.dividends.band(1).amount.toCurrencyString),
          elementTextByID(s"dividend-${calculationDataSuccessModel.dividends.band(2).name}-calc")(calculationDataSuccessModel.dividends.band(2).income.toCurrencyString),
          elementTextByID(s"dividend-${calculationDataSuccessModel.dividends.band(2).name}-rate")(calculationDataSuccessModel.dividends.band(2).rate.toStringNoDecimal),
          elementTextByID(s"dividend-${calculationDataSuccessModel.dividends.band(2).name}-amount")(calculationDataSuccessModel.dividends.band(2).amount.toCurrencyString),
          elementTextByID("gift-aid")(calculationDataSuccessModel.giftAid.paymentsMade.toCurrencyString),
          elementTextByID("nic2-amount")(calculationDataSuccessModel.nic.class2.toCurrencyString),
          elementTextByID("nic4-amount")(calculationDataSuccessModel.nic.class4.toCurrencyString),
          elementTextByID("total-estimate")(calculationDataSuccessModel.totalIncomeTaxNicYtd.toCurrencyString),
          elementTextByID("business-profit-bbs-interest")(calculationDataSuccessModel.incomeReceived.bankBuildingSocietyInterest.toCurrencyString),
          isElementVisibleById("eoyEstimate")(expectedValue = false),
          isElementVisibleById("business-profit-self-employed-section")(expectedValue = true),
          isElementVisibleById("business-profit-property-section")(expectedValue = true),
          isElementVisibleById("business-profit-bbs-interest-section")(expectedValue = true)
        )
      }
    }

    "isAuthorisedUser with an active enrolment but error response from Get Last Calculation" should {

      "Render the Estimated Tax Liability Error Page" in {

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("an Error Response response from Get Latest Calculation via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLatestCalcError(testNino, testYear)

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyLatestCalculationCall(testNino, testYear)

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
        IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, latestCalcModelJson)

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyLatestCalculationCall(testNino, testYear)

        res should have (
          httpStatus(OK),
          pageTitle(messages.title(testYearInt)),
          elementTextByID("heading")(messages.heading(testYearInt)),
          isElementVisibleById("eoyEstimate")(expectedValue = false)
        )
      }
    }

    unauthorisedTest("/calculation/" + testYear)
  }
}
