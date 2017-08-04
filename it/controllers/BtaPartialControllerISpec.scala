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
import helpers.IntegrationTestConstants.GetObligationsData._
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{IncomeTaxViewChangeStub, UserDetailsStub, SelfAssessmentStub, AuthStub}
import models.LastTaxCalculation
import play.api.http.Status._
import utils.ImplicitDateFormatter

class BtaPartialControllerISpec extends ComponentSpecBase with ImplicitDateFormatter {

  "calling the BtaPartialController" when {

    "authorised with na active enrolment" which {

      "has a combination of Received business and property obligations with met = true" should {

        "display the bta partial with the correct information" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse = LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessModel.incomeTaxYTD)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub a single business obligation response")
          SelfAssessmentStub.stubGetOnlyBizObs(testNino, testSelfEmploymentId, singleObligationsDataSuccessModel)

          And("I wiremock stub a single business obligation response")
          SelfAssessmentStub.stubGetOnlyPropObs(testNino, testSelfEmploymentId, otherObligationsDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/partial")
          val res = IncomeTaxViewChangeFrontend.getBtaPartial

          Then("the result should have a HTTP status of OK and a body containing one obligation")
          res should have(

            //Check Status OK (200) Result
            httpStatus(OK),

            //Check Page Title of HTML Response Body
            isElementVisibleById("it-quarterly-reporting-heading")(true),

            //Check that only the expected obligation message is being shown
            elementTextByID("report-due")("Your latest report has been received"),

            //Check that only the expected estimate message is being shown
            elementTextByID("current-estimate-2018")("Your estimated tax amount is £90,500"),
            isElementVisibleById("current-estimate-2019")(false)
          )
        }
      }

      "has a combination of Received business and property obligations with met = false" should {

        "display the bta partial with the correct information" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse = LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessModel.incomeTaxYTD)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub obligation responses in different tax years")
          SelfAssessmentStub.stubGetObligations(testNino, testSelfEmploymentId, singleObligationPlusYearOpenModel, singleObligationOverdueModel)

          When("I call GET /report-quarterly/income-and-expenses/view/partial")
          val res = IncomeTaxViewChangeFrontend.getBtaPartial

          Then("the result should have a HTTP status of OK and a body containing one obligation")
          res should have(

            //Check Status OK (200) Result
            httpStatus(OK),

            //Check Page Title of HTML Response Body
            isElementVisibleById("it-quarterly-reporting-heading")(true),

            //Check that only the expected obligation message is being shown
            elementTextByID("report-due")("You have an overdue report"),

            //Check that only the expected estimate message is being shown
            elementTextByID("current-estimate-2018")("Your estimated tax amount is £90,500"),
            isElementVisibleById("current-estimate-2019")(false)
          )
        }
      }

      "has a multiple estimates with different tax years" should {

        "display the bta partial with the correct information" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse = LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessModel.incomeTaxYTD)
          val lastTaxCalcResponsePlusYear = LastTaxCalculation(testCalcId, "2018-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessModel.incomeTaxDue)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, "2019", lastTaxCalcResponsePlusYear)

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.otherSuccessResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub obligation responses in different tax years")
          SelfAssessmentStub.stubGetObligations(testNino, testSelfEmploymentId, singleObligationPlusYearOpenModel, singleObligationOverdueModel)

          When("I call GET /report-quarterly/income-and-expenses/view/partial")
          val res = IncomeTaxViewChangeFrontend.getBtaPartial

          Then("the result should have a HTTP status of OK and a body containing one obligation")
          res should have(

            //Check Status OK (200) Result
            httpStatus(OK),

            //Check Page Title of HTML Response Body
            isElementVisibleById("it-quarterly-reporting-heading")(true),

            //Check that only the expected obligation message is being shown
            elementTextByID("report-due")("You have an overdue report"),

            //Check that only the expected estimate message is being shown
            elementTextByID("current-estimate-2018")("Your estimated tax amount for 2017 to 2018 is £90,500"),
            elementTextByID("current-estimate-2019")("Your estimated tax amount for 2018 to 2019 is £66,500")
          )
        }
      }

    }

  }

}
