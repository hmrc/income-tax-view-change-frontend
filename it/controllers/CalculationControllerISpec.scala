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

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyLatestCalculationCall(testNino, testYear)

        res should have (
          httpStatus(OK),
          pageTitle(messages.estimateTitle(testYearInt)),
          elementTextByID("heading")(messages.estimateHeading(testYearInt)),
          elementTextByID("eoyEstimateHeading")("Annual estimate: £66,000"),
          elementTextByID("eoyP1")(s"This is for the ${testYearInt-1} to $testYearInt tax year."),
          isElementVisibleById("inYearCalcBreakdown")(expectedValue = true),
          elementTextByID("national-regime")("(National Regime: Scotland)"),
          elementTextByID("business-profit-data")("£200,000"),
          elementTextByID("property-income-data")("£10,000"),
          elementTextByID("dividend-income-data")("£11,000"),
          elementTextByID("savings-income-data")("£2,000"),
          elementTextByID("personal-allowance-data")("£11,500"),
          elementTextByID("dividends-allowance-data")("£500"),
          elementTextByID("savings-allowance-data")("£21"),
          elementTextByID("gift-investment-property-data")("£1,000.25"),
          elementTextByID("estimate-total-taxable-income-data")("£198,500"),
          elementTextByID("income-tax-band-BRT-label")("Income Tax (£20,000 at 20%)"),
          elementTextByID("income-tax-band-BRT-data")("£4,000"),
          elementTextByID("income-tax-band-HRT-label")("Income Tax (£100,000 at 40%)"),
          elementTextByID("income-tax-band-HRT-data")("£40,000"),
          elementTextByID("income-tax-band-ART-label")("Income Tax (£50,000 at 45%)"),
          elementTextByID("income-tax-band-ART-data")("£22,500"),
          elementTextByID("dividend-tax-band-zero-band-label")("Dividend Tax (£500 at 0%)"),
          elementTextByID("dividend-tax-band-zero-band-data")("£0"),
          elementTextByID("dividend-tax-band-basic-band-label")("Dividend Tax (£1,000 at 7.5%)"),
          elementTextByID("dividend-tax-band-basic-band-data")("£75"),
          elementTextByID("dividend-tax-band-higher-band-label")("Dividend Tax (£2,000 at 37.5%)"),
          elementTextByID("dividend-tax-band-higher-band-data")("£750"),
          elementTextByID("dividend-tax-band-additional-band-label")("Dividend Tax (£3,000 at 38.1%)"),
          elementTextByID("dividend-tax-band-additional-band-data")("£1,143"),
          elementTextByID("savings-tax-band-SSR-label")("Savings Tax (£1 at 0%)"),
          elementTextByID("savings-tax-band-SSR-data")("£0"),
          elementTextByID("savings-tax-band-ZRT-label")("Savings Tax (£20 at 0%)"),
          elementTextByID("savings-tax-band-ZRT-data")("£0"),
          elementTextByID("savings-tax-band-BRT-label")("Savings Tax (£500 at 20%)"),
          elementTextByID("savings-tax-band-BRT-data")("£100"),
          elementTextByID("savings-tax-band-HRT-label")("Savings Tax (£1,000 at 40%)"),
          elementTextByID("savings-tax-band-HRT-data")("£400"),
          elementTextByID("savings-tax-band-ART-label")("Savings Tax (£479 at 45%)"),
          elementTextByID("savings-tax-band-ART-data")("£215.55"),
          elementTextByID("nic-class2-data")("£10,000"),
          elementTextByID("nic-class4-data")("£14,000"),
          elementTextByID("tax-reliefs-data")("£500"),
          elementTextByID("your-total-estimate-data")("£90,500"),
          isElementVisibleById("total-tax-bill-data")(expectedValue = false),
          isElementVisibleById("payments-to-date-data")(expectedValue = false),
          isElementVisibleById("total-outstanding-date")(expectedValue = false),
          isElementVisibleById("total-outstanding-data")(expectedValue = false),
          elementTextByID("gift-aid-extender")("Your basic rate limit has been increased by £2,000 for Gift Aid payments.")
        )
      }
    }

    "isAuthorisedUser with an active enrolment, valid latest calc estimate, valid breakdown response and Crystallised EoY amount" when {

      "a successful response is retrieved for the financial transactions and there is an outstanding amount (unpaid)" should {

        "return the correct page with a valid total" in {

          appConfig.features.calcBreakdownEnabled(true)

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I wiremock stub a successful Get Latest Tax Calculation response")
          IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationCrystallisedResponse)

          And("I wiremock stub a successful Unpaid Financial Transactions response")
          val financialTransactions = financialTransactionsJson(1000.0).as[FinancialTransactionsModel]
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsJson(1000.0))

          When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyLatestCalculationCall(testNino, testYear)
          verifyFinancialTransactionsCall(testMtditid)

          res should have (
            httpStatus(OK),
            pageTitle(messages.billsTitle(testYearInt)),
            elementTextByID("heading")(messages.heading(testYearInt)),
            elementTextByID("whatYouOweHeading")("Tax left to pay: £1,000"),
            elementTextByID("national-regime")("(National Regime: Scotland)"),
            elementTextByID("business-profit-data")("£200,000"),
            elementTextByID("property-income-data")("£10,000"),
            elementTextByID("dividend-income-data")("£11,000"),
            elementTextByID("savings-income-data")("£2,000"),
            elementTextByID("personal-allowance-data")("£11,500"),
            elementTextByID("dividends-allowance-data")("£500"),
            elementTextByID("savings-allowance-data")("£21"),
            elementTextByID("gift-investment-property-data")("£1,000.25"),
            elementTextByID("total-taxable-income-data")("£198,500"),
            elementTextByID("income-tax-band-BRT-label")("Income Tax (£20,000 at 20%)"),
            elementTextByID("income-tax-band-BRT-data")("£4,000"),
            elementTextByID("income-tax-band-HRT-label")("Income Tax (£100,000 at 40%)"),
            elementTextByID("income-tax-band-HRT-data")("£40,000"),
            elementTextByID("income-tax-band-ART-label")("Income Tax (£50,000 at 45%)"),
            elementTextByID("income-tax-band-ART-data")("£22,500"),
            elementTextByID("dividend-tax-band-zero-band-label")("Dividend Tax (£500 at 0%)"),
            elementTextByID("dividend-tax-band-zero-band-data")("£0"),
            elementTextByID("dividend-tax-band-basic-band-label")("Dividend Tax (£1,000 at 7.5%)"),
            elementTextByID("dividend-tax-band-basic-band-data")("£75"),
            elementTextByID("dividend-tax-band-higher-band-label")("Dividend Tax (£2,000 at 37.5%)"),
            elementTextByID("dividend-tax-band-higher-band-data")("£750"),
            elementTextByID("dividend-tax-band-additional-band-label")("Dividend Tax (£3,000 at 38.1%)"),
            elementTextByID("dividend-tax-band-additional-band-data")("£1,143"),
            elementTextByID("savings-tax-band-SSR-label")("Savings Tax (£1 at 0%)"),
            elementTextByID("savings-tax-band-SSR-data")("£0"),
            elementTextByID("savings-tax-band-ZRT-label")("Savings Tax (£20 at 0%)"),
            elementTextByID("savings-tax-band-ZRT-data")("£0"),
            elementTextByID("savings-tax-band-BRT-label")("Savings Tax (£500 at 20%)"),
            elementTextByID("savings-tax-band-BRT-data")("£100"),
            elementTextByID("savings-tax-band-HRT-label")("Savings Tax (£1,000 at 40%)"),
            elementTextByID("savings-tax-band-HRT-data")("£400"),
            elementTextByID("savings-tax-band-ART-label")("Savings Tax (£479 at 45%)"),
            elementTextByID("savings-tax-band-ART-data")("£215.55"),
            elementTextByID("nic-class2-data")("£10,000"),
            elementTextByID("nic-class4-data")("£14,000"),
            elementTextByID("tax-reliefs-data")("£500"),
            elementTextByID("total-tax-bill-data")("£3,400"),
            elementTextByID("payments-to-date-data")("£2,000"),
            elementTextByID("total-outstanding-date")(s"due 31 January $testYearPlusOne"),
            elementTextByID("total-outstanding-data")("£1,000"),
            isElementVisibleById("your-total-estimate-data")(expectedValue = false),
            isElementVisibleById("gift-aid-extender")(expectedValue = false)
          )
        }
      }

      "a successful response is retrieved for the financial transactions and there is no outstanding amount (paid)" should {

        "return the correct page with a valid total" in {

          appConfig.features.calcBreakdownEnabled(true)

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I wiremock stub a successful Get Latest Tax Calculation response")
          IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationCrystallisedResponse)

          And("I wiremock stub a successful paid Financial Transactions response")
          val financialTransactions = financialTransactionsJson(0.0).as[FinancialTransactionsModel]
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsJson(0.0))

          When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyLatestCalculationCall(testNino, testYear)
          verifyFinancialTransactionsCall(testMtditid)

          res should have (
            httpStatus(OK),
            pageTitle(messages.billsTitle(testYearInt)),
            elementTextByID("heading")(messages.heading(testYearInt)),
            isElementVisibleById("whatYouOweHeading")(expectedValue = false),
            elementTextByID("national-regime")("(National Regime: Scotland)"),
            elementTextByID("business-profit-data")("£200,000"),
            elementTextByID("property-income-data")("£10,000"),
            elementTextByID("dividend-income-data")("£11,000"),
            elementTextByID("savings-income-data")("£2,000"),
            elementTextByID("personal-allowance-data")("£11,500"),
            elementTextByID("dividends-allowance-data")("£500"),
            elementTextByID("savings-allowance-data")("£21"),
            elementTextByID("gift-investment-property-data")("£1,000.25"),
            elementTextByID("total-taxable-income-data")("£198,500"),
            elementTextByID("income-tax-band-BRT-label")("Income Tax (£20,000 at 20%)"),
            elementTextByID("income-tax-band-BRT-data")("£4,000"),
            elementTextByID("income-tax-band-HRT-label")("Income Tax (£100,000 at 40%)"),
            elementTextByID("income-tax-band-HRT-data")("£40,000"),
            elementTextByID("income-tax-band-ART-label")("Income Tax (£50,000 at 45%)"),
            elementTextByID("income-tax-band-ART-data")("£22,500"),
            elementTextByID("dividend-tax-band-zero-band-label")("Dividend Tax (£500 at 0%)"),
            elementTextByID("dividend-tax-band-zero-band-data")("£0"),
            elementTextByID("dividend-tax-band-basic-band-label")("Dividend Tax (£1,000 at 7.5%)"),
            elementTextByID("dividend-tax-band-basic-band-data")("£75"),
            elementTextByID("dividend-tax-band-higher-band-label")("Dividend Tax (£2,000 at 37.5%)"),
            elementTextByID("dividend-tax-band-higher-band-data")("£750"),
            elementTextByID("dividend-tax-band-additional-band-label")("Dividend Tax (£3,000 at 38.1%)"),
            elementTextByID("dividend-tax-band-additional-band-data")("£1,143"),
            elementTextByID("savings-tax-band-SSR-label")("Savings Tax (£1 at 0%)"),
            elementTextByID("savings-tax-band-SSR-data")("£0"),
            elementTextByID("savings-tax-band-ZRT-label")("Savings Tax (£20 at 0%)"),
            elementTextByID("savings-tax-band-ZRT-data")("£0"),
            elementTextByID("savings-tax-band-BRT-label")("Savings Tax (£500 at 20%)"),
            elementTextByID("savings-tax-band-BRT-data")("£100"),
            elementTextByID("savings-tax-band-HRT-label")("Savings Tax (£1,000 at 40%)"),
            elementTextByID("savings-tax-band-HRT-data")("£400"),
            elementTextByID("savings-tax-band-ART-label")("Savings Tax (£479 at 45%)"),
            elementTextByID("savings-tax-band-ART-data")("£215.55"),
            elementTextByID("nic-class2-data")("£10,000"),
            elementTextByID("nic-class4-data")("£14,000"),
            elementTextByID("tax-reliefs-data")("£500"),
            elementTextByID("total-tax-bill-data")("£3,400"),
            elementTextByID("payments-to-date-data")("£2,000"),
            elementTextByID("total-outstanding-date")(s"due 31 January $testYearPlusOne"),
            elementTextByID("total-outstanding-data")("£0"),
            isElementVisibleById("your-total-estimate-data")(expectedValue = false),
              isElementVisibleById("gift-aid-extender")(expectedValue = false)
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
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, financialTransactionsSingleErrorJson())

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

        appConfig.features.calcBreakdownEnabled(true)

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationResponse)

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyLatestCalculationCall(testNino, testYear)

        res should have (
          httpStatus(OK),
          pageTitle(messages.estimateTitle(testYearInt)),
          elementTextByID("heading")(messages.estimateHeading(testYearInt)),
          isElementVisibleById("eoyEstimateHeading")(expectedValue = false),
          isElementVisibleById("eoyP1")(expectedValue = false),
          isElementVisibleById("inYearCalcBreakdown")(expectedValue = true),
          elementTextByID("national-regime")("(National Regime: Scotland)"),
          elementTextByID("business-profit-data")("£200,000"),
          elementTextByID("property-income-data")("£10,000"),
          elementTextByID("dividend-income-data")("£11,000"),
          elementTextByID("savings-income-data")("£2,000"),
          elementTextByID("personal-allowance-data")("£11,500"),
          elementTextByID("dividends-allowance-data")("£500"),
          elementTextByID("savings-allowance-data")("£21"),
          elementTextByID("gift-investment-property-data")("£1,000.25"),
          elementTextByID("estimate-total-taxable-income-data")("£198,500"),
          elementTextByID("income-tax-band-BRT-label")("Income Tax (£20,000 at 20%)"),
          elementTextByID("income-tax-band-BRT-data")("£4,000"),
          elementTextByID("income-tax-band-HRT-label")("Income Tax (£100,000 at 40%)"),
          elementTextByID("income-tax-band-HRT-data")("£40,000"),
          elementTextByID("income-tax-band-ART-label")("Income Tax (£50,000 at 45%)"),
          elementTextByID("income-tax-band-ART-data")("£22,500"),
          elementTextByID("dividend-tax-band-zero-band-label")("Dividend Tax (£500 at 0%)"),
          elementTextByID("dividend-tax-band-zero-band-data")("£0"),
          elementTextByID("dividend-tax-band-basic-band-label")("Dividend Tax (£1,000 at 7.5%)"),
          elementTextByID("dividend-tax-band-basic-band-data")("£75"),
          elementTextByID("dividend-tax-band-higher-band-label")("Dividend Tax (£2,000 at 37.5%)"),
          elementTextByID("dividend-tax-band-higher-band-data")("£750"),
          elementTextByID("dividend-tax-band-additional-band-label")("Dividend Tax (£3,000 at 38.1%)"),
          elementTextByID("dividend-tax-band-additional-band-data")("£1,143"),
          elementTextByID("savings-tax-band-SSR-label")("Savings Tax (£1 at 0%)"),
          elementTextByID("savings-tax-band-SSR-data")("£0"),
          elementTextByID("savings-tax-band-ZRT-label")("Savings Tax (£20 at 0%)"),
          elementTextByID("savings-tax-band-ZRT-data")("£0"),
          elementTextByID("savings-tax-band-BRT-label")("Savings Tax (£500 at 20%)"),
          elementTextByID("savings-tax-band-BRT-data")("£100"),
          elementTextByID("savings-tax-band-HRT-label")("Savings Tax (£1,000 at 40%)"),
          elementTextByID("savings-tax-band-HRT-data")("£400"),
          elementTextByID("savings-tax-band-ART-label")("Savings Tax (£479 at 45%)"),
          elementTextByID("savings-tax-band-ART-data")("£215.55"),
          elementTextByID("nic-class2-data")("£10,000"),
          elementTextByID("nic-class4-data")("£14,000"),
          elementTextByID("tax-reliefs-data")("£500"),
          elementTextByID("your-total-estimate-data")("£90,500"),
          isElementVisibleById("total-tax-bill-data")(expectedValue = false),
          isElementVisibleById("payments-to-date-data")(expectedValue = false),
          isElementVisibleById("total-outstanding-date")(expectedValue = false),
          isElementVisibleById("total-outstanding-data")(expectedValue = false),
          isElementVisibleById("gift-aid-extender")(expectedValue = false)
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
          pageTitle(messages.estimateTitle(testYearInt)),
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
          pageTitle(messages.estimateTitle(testYearInt)),
          elementTextByID("heading")(messages.estimateHeading(testYearInt)),
          isElementVisibleById("eoyEstimate")(expectedValue = false)
        )
      }
    }

    unauthorisedTest("/calculation/" + testYear)
  }
}
