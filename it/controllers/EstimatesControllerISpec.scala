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

import enums.{Crystallised, Estimate}
import helpers.IntegrationTestConstants._
import helpers.servicemocks._
import helpers.{ComponentSpecBase, GenericStubMethods}
import models.LastTaxCalculation
import play.api.http.Status._

class EstimatesControllerISpec extends ComponentSpecBase with GenericStubMethods {

  "Calling the EstimatesController.viewEstimatedCalculations" when {
    "isAuthorisedUser with an active enrolment, and a single, valid tax estimate" should {
      "return the correct page with tax links" in {

        isAuthorisedUser(true)

        stubUserDetails()

        stubPartial()

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse =
          LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD, Estimate)
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
        val lastTaxCalcResponse =
          LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.incomeTaxYTD, Estimate)
        val lastTaxCalcResponse2 =
          LastTaxCalculation(testCalcId2, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessModel.incomeTaxYTD, Estimate)
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
            Crystallised
          )
        val crystallisedLastTaxCalcResponse =
          LastTaxCalculation(
            testCalcId2,
            "2017-07-06T12:34:56.789Z",
            GetCalculationData.calculationDataSuccessModel.incomeTaxYTD,
            Estimate
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
            Crystallised
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

}
