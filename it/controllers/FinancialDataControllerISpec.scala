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
import helpers.servicemocks.{AuthStub, IncomeTaxViewChangeStub, SelfAssessmentStub}
import models.{LastTaxCalculation, CalculationDataModel}
import play.api.http.Status._
import play.api.Logger

class FinancialDataControllerISpec extends ComponentSpecBase {

  "Calling the FinancialDataController.getEstimatedTaxLiability(year)" when {

    "authorised with an active enrolment" should {

      "return the correct page with a valid total" in {

        Given("I wiremock stub an authorised user response")
        AuthStub.stubAuthorised()

        And("I wiremock stub a successful Get Last Estimated Tax Liability response")
        val lastTaxCalcResponse = LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", 1800.00)
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

        And("I wiremock stub a successful Get CalculationData response")
        val calc = GetCalculationData.calculationDataSuccessModel
        val calculationResponse = CalculationDataModel(calc.incomeTaxYTD, calc.incomeTaxThisPeriod, calc.profitFromSelfEmployment, calc.profitFromUkLandAndProperty,
          calc.totalIncomeReceived, calc.personalAllowance, calc.totalIncomeOnWhichTaxIsDue, calc.payPensionsProfitAtBRT, calc.incomeTaxOnPayPensionsProfitAtBRT,
          calc.payPensionsProfitAtHRT, calc.incomeTaxOnPayPensionsProfitAtHRT, calc.payPensionsProfitAtART, calc.incomeTaxOnPayPensionsProfitAtART, calc.incomeTaxDue,
          calc.nicTotal,calc.rateBRT,calc.rateHRT,calc.rateART)

        IncomeTaxViewChangeStub.stubGetCalcData(testNino, testCalcId, calculationResponse)

        And("I wiremock stub a successful Business Details response")
        SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

        And("I wiremock stub a successful Property Details response")
        SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

        When(s"I call GET /check-your-income-tax-and-expenses/estimated-tax-liability/$testYear")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

        And("I verify the Business Details response has been wiremocked")
        SelfAssessmentStub.verifyGetBusinessDetails(testNino)

        And("I verify the Property Details response has been wiremocked")
        SelfAssessmentStub.verifyGetPropertyDetails(testNino)

        Then("I verify the Estimated Tax Liability response has been wiremocked")
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)
        
        Then("a successful response is returned with the correct estimate")
        res should have(

          //Check for a Status OK response (200)
          httpStatus(OK),

          //Check the Page Title
          pageTitle("2017/18 - Business Tax Account"),

          //Check the estimated tax amount is correct
          elementTextByID("in-year-estimate")("£1,800.00")
        )
      }
    }
    "unauthorised" should {

      "redirect to sign in" in {

        Given("I wiremock stub an unauthorised user response")
        AuthStub.stubUnauthorised()

        When("I call GET /check-your-income-tax-and-expenses/estimated-tax-liability")
        val res = IncomeTaxViewChangeFrontend.getFinancialData(testYear)

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
