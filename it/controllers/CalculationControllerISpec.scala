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

import enums.Crystallised
import helpers.{ComponentSpecBase, GenericStubMethods}
import helpers.IntegrationTestConstants._
import helpers.servicemocks._
import models.{CalculationDataErrorModel, LastTaxCalculation}
import play.api.http.Status._
import utils.ImplicitCurrencyFormatter._

class CalculationControllerISpec extends ComponentSpecBase with GenericStubMethods {

  "Calling the FinancialDataController.getEstimatedTaxLiability(year)" when {

    "isAuthorisedUser with an active enrolment, valid last calc estimate, valid breakdown response and an EoY Estimate" should {

      "return the correct page with a valid total" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse = LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = GetCalculationData.calculationDataSuccessWithEoYModel
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calcBreakdownResponse)

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)
        //IncomeTaxViewChangeStub.stubGetCalcData(testNino,testYear,calculationResponse)

        res should have (
          httpStatus(OK),
          pageTitle("Tax year: 2017 to 2018"),
          elementTextByID(id = "service-info-user-name")(testUserName),
          elementTextByID("inYearEstimateHeading")(s"Current estimate: ${calcBreakdownResponse.incomeTaxYTD.toCurrencyString}"),
          elementTextByID("tax-year")("Tax year: 2017 to 2018"),
          elementTextByID("it-reference")(testMtditid),
          elementTextByID("obligations-link")("View report deadlines"),
          elementTextByID("sa-link")("View annual returns"),
          elementTextByID("page-heading")("Your Income Tax estimate"),
          elementTextByID("business-profit")(calcBreakdownResponse.profitFromSelfEmployment.toCurrencyString),
          elementTextByID("property-profit")(calcBreakdownResponse.profitFromUkLandAndProperty.toCurrencyString),
          elementTextByID("personal-allowance")(s"-${calcBreakdownResponse.proportionAllowance.toCurrencyString}"),
          elementTextByID("taxable-income")(calcBreakdownResponse.totalIncomeOnWhichTaxIsDue.toCurrencyString),
          elementTextByID("nic2-amount")(calcBreakdownResponse.nationalInsuranceClass2Amount.toCurrencyString),
          elementTextByID("nic4-amount")(calcBreakdownResponse.totalClass4Charge.toCurrencyString),
          elementTextByID("total-estimate")(calcBreakdownResponse.incomeTaxYTD.toCurrencyString),
          isElementVisibleById("eoyEstimate")(expectedValue = true)
        )
      }
    }

    "isAuthorisedUser with an active enrolment, valid last calc estimate, valid breakdown response and Crystallised EoY amount" should {
      "return the correct page with a valid total" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse = LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD, calcStatus = Some(Crystallised))
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = GetCalculationData.calculationDataSuccessWithEoYModel
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calcBreakdownResponse)

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)
        //IncomeTaxViewChangeStub.stubGetCalcData(testNino,testYear,calculationResponse)

        res should have (
          httpStatus(OK),
          pageTitle("Your final submission"),
          elementTextByID(id = "service-info-user-name")(testUserName),
          elementTextByID("whatYouOweHeading")(s"What you owe: ${calcBreakdownResponse.incomeTaxYTD.toCurrencyString}"),
          elementTextByID("tax-year")("Tax year: 2017 to 2018"),
          elementTextByID("it-reference")(testMtditid),
          elementTextByID("obligations-link")("View report deadlines"),
          elementTextByID("sa-link")("View annual returns"),
          elementTextByID("page-heading")("Your finalised Income Tax bill"),
          elementTextByID("business-profit")(calcBreakdownResponse.profitFromSelfEmployment.toCurrencyString),
          elementTextByID("property-profit")(calcBreakdownResponse.profitFromUkLandAndProperty.toCurrencyString),
          elementTextByID("personal-allowance")(s"-${calcBreakdownResponse.proportionAllowance.toCurrencyString}"),
          elementTextByID("taxable-income")(calcBreakdownResponse.totalIncomeOnWhichTaxIsDue.toCurrencyString),
          elementTextByID("nic2-amount")(calcBreakdownResponse.nationalInsuranceClass2Amount.toCurrencyString),
          elementTextByID("nic4-amount")(calcBreakdownResponse.totalClass4Charge.toCurrencyString),
          elementTextByID("total-estimate")(calcBreakdownResponse.incomeTaxYTD.toCurrencyString)
        )
      }
    }

    "isAuthorisedUser with an active enrolment, valid last calc estimate, valid breakdown response but NO EoY Estimate" should {

      "return the correct page with a valid total" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse = LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessModel.incomeTaxYTD)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = GetCalculationData.calculationDataSuccessModel
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calcBreakdownResponse)

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)
        //IncomeTaxViewChangeStub.stubGetCalcData(testNino,testYear,calculationResponse)

        res should have (
          httpStatus(OK),
          pageTitle("Tax year: 2017 to 2018"),
          elementTextByID(id = "service-info-user-name")(testUserName),
          elementTextByID("inYearEstimateHeading")(s"Current estimate: ${calcBreakdownResponse.incomeTaxYTD.toCurrencyString}"),
          elementTextByID("tax-year")("Tax year: 2017 to 2018"),
          elementTextByID("it-reference")(testMtditid),
          elementTextByID("obligations-link")("View report deadlines"),
          elementTextByID("sa-link")("View annual returns"),
          elementTextByID("page-heading")("Your Income Tax estimate"),
          elementTextByID("business-profit")(calcBreakdownResponse.profitFromSelfEmployment.toCurrencyString),
          elementTextByID("property-profit")(calcBreakdownResponse.profitFromUkLandAndProperty.toCurrencyString),
          elementTextByID("personal-allowance")(s"-${calcBreakdownResponse.proportionAllowance.toCurrencyString}"),
          elementTextByID("taxable-income")(calcBreakdownResponse.totalIncomeOnWhichTaxIsDue.toCurrencyString),
          elementTextByID("nic2-amount")(calcBreakdownResponse.nationalInsuranceClass2Amount.toCurrencyString),
          elementTextByID("nic4-amount")(calcBreakdownResponse.totalClass4Charge.toCurrencyString),
          elementTextByID("total-estimate")(calcBreakdownResponse.incomeTaxYTD.toCurrencyString),
          isElementVisibleById("eoyEstimate")(expectedValue = false)
        )
      }
    }

    "isAuthorisedUser with an active enrolment, valid last tax estimate response, but error in calc breakdown" should {

      "Return the estimated tax liability without the calculation breakdown" in {

        isAuthorisedUser(true)

        stubUserDetailsError()

        stubPartial()

        And("a successful Get Last Estimated Tax Liability response via wiremock stub")
        val lastTaxCalcResponse = LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub an erroneous GetCalculationData response")
        val calc = GetCalculationData.calculationDataErrorModel
        val calculationResponse = CalculationDataErrorModel(calc.code, calc.message)

        IncomeTaxViewChangeStub.stubGetCalcError(testNino, testCalcId, calculationResponse)

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("verification that the Estimated Tax Liability response has been wiremocked ")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("a successful response is returned with the correct estimate")
        res should have(
          httpStatus(OK),
          pageTitle("Tax year: 2017 to 2018"),
          elementTextByID("inYearEstimateHeading")(s"Current estimate: ${GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD.toCurrencyString}"),
          elementTextByID("inYearP1")("This is an estimate of the tax you owe from 6 April 2017 to 6 July 2017."),
          isElementVisibleById("inYearCalcBreakdown")(expectedValue = false)
        )
      }
    }

    "isAuthorisedUser with an active enrolment no data found response from Last Calculation" should {

      "Return no data found response and render view explaining that this will be available once they've submitted income" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        And("a No Data Found response from Get Last Estimated Tax Liability via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLastCalcNoData(testNino, testYear)

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("verification that the Estimated Tax Liability response has been wiremocked ")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("a Not Found response is returned and correct view rendered")
        res should have(
          httpStatus(NOT_FOUND),
          pageTitle("Tax year: 2017 to 2018")
        )
      }
    }

    "isAuthorisedUser with an active enrolment but error response from Get Last Calculation" should {

      "Render the Estimated Tax Liability Error Page" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        And("an Error Response response from Get Last Estimated Tax Liability via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLastCalcError(testNino, testYear)

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/calculation/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("verification that the Estimated Tax Liability response has been wiremocked ")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("an Internal Server Error response is returned and correct view rendered")
        res should have(
          httpStatus(OK),
          pageTitle("Tax year: 2017 to 2018"),
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

  "Calling the CalculationController.viewEstimatedCalculations" when {
    "isAuthorisedUser with an active enrolment, and a single, valid tax estimate" should {
      "return the correct page with tax links" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse =
          LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = GetCalculationData.calculationDataSuccessWithEoYModel
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calcBreakdownResponse)


        When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
        val res = IncomeTaxViewChangeFrontend.getEstimates

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("The view should have the correct headings and a single tax estimate link")
        res should have(
          httpStatus(OK),
          pageTitle("Current estimates"),
          elementTextByID("view-estimates")("View your current estimates."),
          elementTextByID(s"estimate-$testYear")(s"Tax year: 2017 to $testYear"),
          nElementsWithClass("estimates-link")(1)
        )
      }
    }

    "isAuthorisedUser with an active enrolment, and multiple valid tax estimates" should {
      "return the correct page with tax links" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        getBizDeets(GetBusinessDetails.multipleSuccessResponse(testSelfEmploymentId, otherTestSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse = LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD)
        val lastTaxCalcResponse2 = LastTaxCalculation(testCalcId2, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessModel.incomeTaxYTD)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYearPlusOne, lastTaxCalcResponse2)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = GetCalculationData.calculationDataSuccessWithEoYModel
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calcBreakdownResponse)
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId2, calcBreakdownResponse)


        When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
        val res = IncomeTaxViewChangeFrontend.getEstimates

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYearPlusOne)

        Then("The view should have the correct headings and two tax estimate links")
        res should have(
          httpStatus(OK),
          pageTitle("Current estimates"),
          elementTextByID("view-estimates")("View your current estimates."),
          elementTextByID(s"estimate-$testYearPlusOne")(s"Tax year: $testYear to $testYearPlusOne"),
          elementTextByID(s"estimate-$testYear")(s"Tax year: 2017 to $testYear"),
          nElementsWithClass("estimates-link")(2)
        )
      }
    }

    "isAuthorisedUser with an active enrolment, with a crystallised calculation and a tax estimate" should {
      "return the correct page with just the estimate tax link" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        getBizDeets(GetBusinessDetails.multipleSuccessResponse(testSelfEmploymentId, otherTestSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse =
          LastTaxCalculation(
            testCalcId,
            "2017-07-06T12:34:56.789Z",
            GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD,
            Some(Crystallised)
          )
        val crystallisedLastTaxCalcResponse =
          LastTaxCalculation(
            testCalcId2,
            "2017-07-06T12:34:56.789Z",
            GetCalculationData.calculationDataSuccessModel.incomeTaxYTD
          )
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYearPlusOne, crystallisedLastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = GetCalculationData.calculationDataSuccessWithEoYModel
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calcBreakdownResponse)
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId2, calcBreakdownResponse)


        When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
        val res = IncomeTaxViewChangeFrontend.getEstimates

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYearPlusOne)

        Then("The view should have the correct headings and a single tax estimate link")
        res should have(
          httpStatus(OK),
          pageTitle("Current estimates"),
          elementTextByID("view-estimates")("View your current estimates."),
          nElementsWithClass("estimates-link")(1)
        )
      }
    }

    "isAuthorisedUser with an active enrolment, and no tax estimates" should {
      "return the correct page with no estimates found message" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse =
          LastTaxCalculation(
            testCalcId,
            "2017-07-06T12:34:56.789Z",
            GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD,
            Some(Crystallised)
          )
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = GetCalculationData.calculationDataSuccessWithEoYModel
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calcBreakdownResponse)


        When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
        val res = IncomeTaxViewChangeFrontend.getEstimates

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("The view should have the correct headings and a single tax estimate link")
        res should have(
          httpStatus(OK),
          pageTitle("Current estimates"),
          elementTextByID("no-estimates")("You've not got any current estimates."),
          nElementsWithClass("estimates-link")(0)
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

  "Calling the CalculationController.viewCrystallisedCalculations" when {
    "isAuthorisedUser with an active enrolment, and a single, valid crystallised estimate" should {
      "return the correct page with bills links" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse =
          LastTaxCalculation(testCalcId,
            "2017-07-06T12:34:56.789Z",
            GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD,
            Some(Crystallised)
          )
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = GetCalculationData.calculationDataSuccessWithEoYModel
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calcBreakdownResponse)


        When(s"I call GET /report-quarterly/income-and-expenses/view/bills")
        val res = IncomeTaxViewChangeFrontend.getBills

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("The view should have the correct headings and a single tax bill link")
        res should have(
          httpStatus(OK),
          pageTitle("Previous statements"),
          elementTextByID("finalised-bills")("View finalised bills."),
          elementTextByID(s"bills-link-$testYear")(s"Tax year: 2017 to $testYear"),
          nElementsWithClass("bills-link")(1),
          elementTextByID("earlier-bills")("For earlier bills, view your self assessment calculations.")
        )
      }
    }

    "isAuthorisedUser with an active enrolment, and multiple valid crystallised estimates" should {
      "return the correct page with tax links" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        getBizDeets(GetBusinessDetails.multipleSuccessResponse(testSelfEmploymentId, otherTestSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse =
          LastTaxCalculation(testCalcId,
            "2017-07-06T12:34:56.789Z",
            GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD,
            Some(Crystallised)
          )
        val lastTaxCalcResponse2 =
          LastTaxCalculation(testCalcId2,
            "2017-07-06T12:34:56.789Z",
            GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD,
            Some(Crystallised)
          )
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYearPlusOne, lastTaxCalcResponse2)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = GetCalculationData.calculationDataSuccessWithEoYModel
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calcBreakdownResponse)
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId2, calcBreakdownResponse)


        When(s"I call GET /report-quarterly/income-and-expenses/view/bills")
        val res = IncomeTaxViewChangeFrontend.getBills

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYearPlusOne)

        Then("The view should have the correct headings and two tax bill links")
        res should have(
          httpStatus(OK),
          pageTitle("Previous statements"),
          elementTextByID("finalised-bills")("View finalised bills."),
          elementTextByID(s"bills-link-$testYearPlusOne")(s"Tax year: $testYear to $testYearPlusOne"),
          elementTextByID(s"bills-link-$testYear")(s"Tax year: 2017 to $testYear"),
          nElementsWithClass("bills-link")(2),
          elementTextByID("earlier-bills")("For earlier bills, view your self assessment calculations.")
        )
      }
    }

    "isAuthorisedUser with an active enrolment, with a crystallised calculation and a tax estimate" should {
      "return the correct page with just the tax bill link" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        getBizDeets(GetBusinessDetails.multipleSuccessResponse(testSelfEmploymentId, otherTestSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse =
          LastTaxCalculation(
            testCalcId,
            "2017-07-06T12:34:56.789Z",
            GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD,
            Some(Crystallised)
          )
        val crystallisedLastTaxCalcResponse =
          LastTaxCalculation(
            testCalcId2,
            "2017-07-06T12:34:56.789Z",
            GetCalculationData.calculationDataSuccessModel.incomeTaxYTD
          )
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYearPlusOne, crystallisedLastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = GetCalculationData.calculationDataSuccessWithEoYModel
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calcBreakdownResponse)
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId2, calcBreakdownResponse)


        When(s"I call GET /report-quarterly/income-and-expenses/view/bills")
        val res = IncomeTaxViewChangeFrontend.getBills

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYearPlusOne)

        Then("The view should have the correct headings and a single tax bill link")
        res should have(
          httpStatus(OK),
          pageTitle("Previous statements"),
          elementTextByID("finalised-bills")("View finalised bills."),
          elementTextByID(s"bills-link-$testYear")(s"Tax year: 2017 to $testYear"),
          nElementsWithClass("bills-link")(1),
          elementTextByID("earlier-bills")("For earlier bills, view your self assessment calculations.")
        )
      }
    }

    "isAuthorisedUser with an active enrolment, and no tax bills" should {
      "return the correct page with no bills found message" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse =
          LastTaxCalculation(
            testCalcId,
            "2017-07-06T12:34:56.789Z",
            GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD
          )
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calcBreakdownResponse = GetCalculationData.calculationDataSuccessWithEoYModel
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calcBreakdownResponse)


        When(s"I call GET /report-quarterly/income-and-expenses/view/bills")
        val res = IncomeTaxViewChangeFrontend.getBills

        verifyBizDeetsCall()

        verifyPropDeetsCall()

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("The view should have the correct headings and a single tax estimate link")
        res should have(
          httpStatus(OK),
          pageTitle("Previous statements"),
          elementTextByID("no-bills")("You've had no bills since you started reporting through software."),
          nElementsWithClass("bills-link")(0)
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
