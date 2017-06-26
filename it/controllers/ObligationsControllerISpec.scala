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

import java.time.LocalDate

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.GetObligationsData._
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuthStub, SelfAssessmentStub}
import play.api.http.Status._
import utils.ImplicitDateFormatter

class ObligationsControllerISpec extends ComponentSpecBase with ImplicitDateFormatter {

  "Calling the ObligationsController" when {

    "authorised with an active enrolment" which {

      "has a single business and single obligation" should {

        "display a single obligation with the correct dates and status" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a single business obligation response")
          SelfAssessmentStub.stubGetObligations(testNino, testSelfEmploymentId, singleObligationsDataSuccessModel)

          When("I call GET /check-your-income-tax-and-expenses/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("the result should have a HTTP status of OK and a body containing one obligation")
          res should have(

            //Check Status OK (200) Result
            httpStatus(OK),

            //Check Page Title of HTML Response Body
            pageTitle("Your Income Tax reports"),

            //Check one obligation section is returned
            nElementsWithClass("obligation")(1),

            //Check the 1st obligation data
            elementTextByID(id = "bi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-ob-1-status")("Received")
          )
        }
      }

      "has multiple obligations" should {

        "display the correct amount of obligations with the correct statuses" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub multiple business obligations response")
          SelfAssessmentStub.stubGetObligations(testNino, testSelfEmploymentId, multipleObligationsDataSuccessModel)

          When("I call GET /check-your-income-tax-and-expenses/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("the result should have a HTTP status of OK and a body containing one obligation")
          res should have(

            //Check Status OK (200) Result
            httpStatus(OK),

            //Check Page Title of HTML Response Body
            pageTitle("Your Income Tax reports"),

            //Check three Obligation sections are returned
            nElementsWithClass("obligation")(3),

            //Check first obligation
            elementTextByID(id = "bi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-ob-1-status")("Received"),

            //Check second obligation
            elementTextByID(id = "bi-ob-2-start")("6 July 2017"),
            elementTextByID(id = "bi-ob-2-end")("5 October 2017"),
            elementTextByID(id = "bi-ob-2-status")("Due by " + LocalDate.now().plusDays(1).toLongDate),

            //Check third obligation
            elementTextByID(id = "bi-ob-3-start")("6 October 2017"),
            elementTextByID(id = "bi-ob-3-end")("5 January 2018"),
            elementTextByID(id = "bi-ob-3-status")("Overdue")
          )
        }
      }
    }

    "unauthorised" should {

      "redirect to sign in" in {

        Given("I wiremock stub an unatuhorised user response")
        AuthStub.stubUnauthorised()

        When("I call GET /check-your-income-tax-and-expenses/obligations")
        val res = IncomeTaxViewChangeFrontend.getObligations

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