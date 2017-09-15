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

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks._
import models.{CalculationDataErrorModel, CalculationDataModel, LastTaxCalculation}
import play.api.http.Status._

class FinancialDataControllerISpec extends ComponentSpecBase {

  "Calling the FinancialDataController.getEstimatedTaxLiability(year)" when {

    "authorised with an active enrolment, valid last calc estimate and valid breakdown response" should {

      "return the correct page with a valid total" in {

        Given("I wiremock stub an authorised user response")
        AuthStub.stubAuthorised()

        And("I wiremock stub a response from the User Details service")
        UserDetailsStub.stubGetUserDetails()

        And("I wiremock stub a ServiceInfo Partial response")
        BtaPartialStub.stubGetServiceInfoPartial()

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse = LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessModel.incomeTaxYTD)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calc = GetCalculationData.calculationDataSuccessModel
        val calculationResponse = CalculationDataModel(
          calc.incomeTaxYTD,
          calc.incomeTaxThisPeriod,
          calc.profitFromSelfEmployment,
          calc.profitFromUkLandAndProperty,
          calc.totalIncomeReceived,
          calc.proportionAllowance,
          calc.totalIncomeOnWhichTaxIsDue,
          calc.payPensionsProfitAtBRT,
          calc.incomeTaxOnPayPensionsProfitAtBRT,
          calc.payPensionsProfitAtHRT,
          calc.incomeTaxOnPayPensionsProfitAtHRT,
          calc.payPensionsProfitAtART,
          calc.incomeTaxOnPayPensionsProfitAtART,
          calc.incomeTaxDue,
          calc.nationalInsuranceClass2Amount,
          calc.totalClass4Charge,
          calc.rateBRT,
          calc.rateHRT,
          calc.rateART
        )
        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calculationResponse)

        And("I wiremock stub a successful Business Details response")
        SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

        And("I wiremock stub a successful Property Details response")
        SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

        When(s"I call GET /report-quarterly/income-and-expenses/view/estimated-tax-liability/$testYear")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        And("I verify the Business Details response has been wiremocked")
        SelfAssessmentStub.verifyGetBusinessDetails(testNino)

        And("I verify the Property Details response has been wiremocked")
        SelfAssessmentStub.verifyGetPropertyDetails(testNino)

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)
        //IncomeTaxViewChangeStub.stubGetCalcData(testNino,testYear,calculationResponse)

        Then("a successful response is returned with the correct estimate")
        res should have(

          //Check for a Status OK response (200)
          httpStatus(OK),

          //Check the Page Title
          pageTitle("2017 to 2018 tax year Your in-year tax estimate"),

          //User Name
          elementTextByID(id = "service-info-user-name")(testUserName),

          //Check the estimated tax amount is correct
          elementTextByID("in-year-estimate")("£90,500"),
          elementTextByID("tax-year")("2017 to 2018 tax year"),
          elementTextByID("it-reference")("XAITSA123456"),
          elementTextByID("obligations-link")("View report deadlines"),
          elementTextByID("sa-link")("View annual returns"),
          elementTextByID("acc-period-start")("1 January 2017"),
          isElementVisibleById("calc-breakdown-inner-link")(true),

          elementTextByID("total-estimate")("£90,500")

          //Commented Out as may be required again later
          //Check the Estimated Calculation Date is correct
          //elementTextByID("in-year-estimate-date")("Estimate up to your 6 July 2017 submission")
        )
      }
    }

    "authorised with an active enrolment, valid last tax estimate response, but error in calc breakdown" should {

      "Return the estimated tax liability without the calculation breakdown" in {

        Given("an authorised user response via wiremock stub")
        AuthStub.stubAuthorised()

        And("I wiremock stub a Error Response from the User Details service")
        UserDetailsStub.stubGetUserDetailsError()

        And("I wiremock stub a ServiceInfo Partial response")
        BtaPartialStub.stubGetServiceInfoPartial()

        And("a successful Get Last Estimated Tax Liability response via wiremock stub")
        val lastTaxCalcResponse = LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessModel.incomeTaxYTD)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub an erroneous GetCalculationData response")
        val calc = GetCalculationData.calculationDataErrorModel
        val calculationResponse = CalculationDataErrorModel(calc.code, calc.message)

        IncomeTaxViewChangeStub.stubGetCalcError(testNino, testCalcId, calculationResponse)

        And("a successful Business Details response via wiremock stub")
        SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

        And("a successful Property Details response via wiremock stub")
        SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/estimated-tax-liability/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        And("verification that the Business Details response has been wiremocked ")
        SelfAssessmentStub.verifyGetBusinessDetails(testNino)

        And("verification that the Property Details response has been wiremocked ")
        SelfAssessmentStub.verifyGetPropertyDetails(testNino)

        Then("verification that the Estimated Tax Liability response has been wiremocked ")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("a successful response is returned with the correct estimate")
        res should have(

          //Check for a Status OK response (200)
          httpStatus(OK),

          //Check the Page Title
          pageTitle("2017 to 2018 tax year Your in-year tax estimate"),

          //Check the estimated tax amount is correct
          elementTextByID("in-year-estimate")("£90,500")

          //Commented Out as may be required again later
          //Check the Estimated Calculation Date is correct
          //elementTextByID("in-year-estimate-date")("Estimate up to your 6 July 2017 submission")
        )
      }
    }

    "authorised with an active enrolment no data found response from Last Calculation" should {

      "Return no data found response and render view explaining that this will be available once they've submitted income" in {

        Given("an authorised user response via wiremock stub")
        AuthStub.stubAuthorised()

        And("I wiremock stub a response from the User Details service")
        UserDetailsStub.stubGetUserDetails()

        And("I wiremock stub a ServiceInfo Partial response")
        BtaPartialStub.stubGetServiceInfoPartial()

        And("a No Data Found response from Get Last Estimated Tax Liability via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLastCalcNoData(testNino, testYear)

        And("a successful Business Details response via wiremock stub")
        SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

        And("a successful Property Details response via wiremock stub")
        SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/estimated-tax-liability/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        And("verification that the Business Details response has been wiremocked ")
        SelfAssessmentStub.verifyGetBusinessDetails(testNino)

        And("verification that the Property Details response has been wiremocked ")
        SelfAssessmentStub.verifyGetPropertyDetails(testNino)

        Then("verification that the Estimated Tax Liability response has been wiremocked ")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("a Not Found response is returned and correct view rendered")
        res should have(

          //Check for a Status OK response (200)
          httpStatus(NOT_FOUND),

          //Check the Page Title
          pageTitle("2017 to 2018 tax year Your in-year tax estimate")
        )
      }
    }

    "authorised with an active enrolment but error response from Get Last Calculation" should {

      "Render the Estimated Tax Liability Error Page" in {

        Given("an authorised user response via wiremock stub")
        AuthStub.stubAuthorised()

        And("I wiremock stub a response from the User Details service")
        UserDetailsStub.stubGetUserDetails()

        And("I wiremock stub a ServiceInfo Partial response")
        BtaPartialStub.stubGetServiceInfoPartial()

        And("an Error Response response from Get Last Estimated Tax Liability via wiremock stub")
        IncomeTaxViewChangeStub.stubGetLastCalcError(testNino, testYear)

        And("a successful Business Details response via wiremock stub")
        SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

        And("a successful Property Details response via wiremock stub")
        SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

        When(s"I make a call to GET /report-quarterly/income-and-expenses/view/estimated-tax-liability/$testYear ")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        And("verification that the Business Details response has been wiremocked ")
        SelfAssessmentStub.verifyGetBusinessDetails(testNino)

        And("verification that the Property Details response has been wiremocked ")
        SelfAssessmentStub.verifyGetPropertyDetails(testNino)

        Then("verification that the Estimated Tax Liability response has been wiremocked ")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

        Then("an Internal Server Error response is returned and correct view rendered")
        res should have(

          //Check for a Status OK response (200)
          httpStatus(OK),

          //Check the Page Title
          pageTitle("2017 to 2018 tax year Your in-year tax estimate"),

          //Check for the correct error message
          elementTextByID("p1")("We can't display your estimated tax amount at the moment."),
          elementTextByID("p2")("Try refreshing the page in a few minutes.")
        )
      }
    }



    "unauthorised" should {

      "redirect to sign in" in {

        Given("I wiremock stub an unauthorised user response")
        AuthStub.stubUnauthorised()

        When("I call GET /report-quarterly/income-and-expenses/view/estimated-tax-liability")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        Then("the http response for an unauthorised user is returned")
        res should have(

          //Check for a Redirect response SEE_OTHER (303)
          httpStatus(SEE_OTHER),

          //Check redirect location of response
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
  }
}
