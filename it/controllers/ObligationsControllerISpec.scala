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
import helpers.servicemocks.{AuthStub, SelfAssessmentStub, UserDetailsStub}
import play.api.http.Status._
import utils.ImplicitDateFormatter

class ObligationsControllerISpec extends ComponentSpecBase with ImplicitDateFormatter {

  "Calling the ObligationsController" when {

    "authorised with an active enrolment" which {

      "has a single business and single obligation" should {

        "display a single obligation with the correct dates and status" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub a single business obligation response")
          SelfAssessmentStub.stubGetOnlyBizObs(testNino, testSelfEmploymentId, singleObligationsDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("the result should have a HTTP status of OK and a body containing one obligation")
          res should have(

            //Check Status OK (200) Result
            httpStatus(OK),

            //Check Page Title of HTML Response Body
            pageTitle("Your report deadlines"),

            //User Name
            elementTextByID(id = "service-info-user-name")(testUserName),

            //Check one obligation section is returned
            nElementsWithClass("obligation")(1),

            //Check the 1st obligation data
            elementTextByID(id = "bi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-ob-1-status")("Received"),
            elementTextByID(id = "estimate-link-2018")("View 2017 to 2018 details"),
            elementTextByID(id = "sa-link")("View annual returns"),
            elementTextByID(id = "service-info-manage-account-link")("Manage account"),
            elementTextByID(id = "service-info-messages-link")("Messages"),
            isElementVisibleById("pi-ob")(false)
          )
        }
      }


      "has business and property with multiple obligations for both" should {

        "display a single obligation with the correct dates and status" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub a single property and business obligation response")
          SelfAssessmentStub.stubGetObligations(testNino,testSelfEmploymentId,multipleReceivedOpenObligationsModel,multipleReceivedOpenObligationsModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("the result should have a HTTP status of OK and a body containing one obligation each for business and property")
          res should have(

            //Check Status OK (200) Result
            httpStatus(OK),

            //Check Page Title of HTML Response Body
            pageTitle("Your report deadlines"),

            //User Name
            elementTextByID(id = "service-info-user-name")(testUserName),

            //Check one obligation section is returned
            nElementsWithClass("obligation")(8),

            //Check the 1st obligation data
            elementTextByID(id = "bi-ob-1-start")("1 October 2016"),
            elementTextByID(id = "bi-ob-1-end")("31 December 2016"),
            elementTextByID(id = "bi-ob-1-status")("Received"),

            elementTextByID(id = "bi-ob-2-start")("1 January 2017"),
            elementTextByID(id = "bi-ob-2-end")("31 March 2017"),
            elementTextByID(id = "bi-ob-2-status")("Overdue"),

            elementTextByID(id = "bi-ob-3-start")("1 April 2017"),
            elementTextByID(id = "bi-ob-3-end")("30 June 2017"),
            elementTextByID(id = "bi-ob-3-status")("Overdue"),

            elementTextByID(id = "bi-ob-4-start")("1 July 2017"),
            elementTextByID(id = "bi-ob-4-end")("30 September 2017"),
            elementTextByID(id = "bi-ob-4-status")("Due by 31 October 2017"),

            elementTextByID(id = "estimate-link-2018")("View 2017 to 2018 details"),
            elementTextByID(id = "sa-link")("View annual returns"),
            elementTextByID(id = "service-info-manage-account-link")("Manage account"),
            elementTextByID(id = "service-info-messages-link")("Messages"),

            elementTextByID(id = "pi-ob-1-start")("1 October 2016"),
            elementTextByID(id = "pi-ob-1-end")("31 December 2016"),
            elementTextByID(id = "pi-ob-1-status")("Received"),

            elementTextByID(id = "pi-ob-2-start")("1 January 2017"),
            elementTextByID(id = "pi-ob-2-end")("31 March 2017"),
            elementTextByID(id = "pi-ob-2-status")("Overdue"),

            elementTextByID(id = "pi-ob-3-start")("1 April 2017"),
            elementTextByID(id = "pi-ob-3-end")("30 June 2017"),
            elementTextByID(id = "pi-ob-3-status")("Overdue"),

            elementTextByID(id = "pi-ob-4-start")("1 July 2017"),
            elementTextByID(id = "pi-ob-4-end")("30 September 2017"),
            elementTextByID(id = "pi-ob-4-status")("Due by 31 October 2017")

          )
        }
      }


      "has multiple obligations" should {

        "display the correct amount of obligations with the correct statuses" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub multiple business obligations response")
          SelfAssessmentStub.stubGetOnlyBizObs(testNino, testSelfEmploymentId, multipleObligationsDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("the result should have a HTTP status of OK and a body containing one obligation")
          res should have(

            //Check Status OK (200) Result
            httpStatus(OK),

            //Check Page Title of HTML Response Body
            pageTitle("Your report deadlines"),

            //User Name
            elementTextByID(id = "service-info-user-name")(testUserName),

            //Check three Obligation sections are returned
            nElementsWithClass("obligation")(3),

            //Check first obligation
            elementTextByID(id = "bi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-ob-1-status")("Received"),

            //Check second obligation
            elementTextByID(id = "bi-ob-2-start")("6 October 2017"),
            elementTextByID(id = "bi-ob-2-end")("5 January 2018"),
            elementTextByID(id = "bi-ob-2-status")("Overdue"),


            //Check third obligation
            elementTextByID(id = "bi-ob-3-start")("6 July 2017"),
            elementTextByID(id = "bi-ob-3-end")("5 October 2017"),
            elementTextByID(id = "bi-ob-3-status")("Due by " + LocalDate.now().plusDays(1).toLongDate)
          )
        }
      }

      "has multiple received and open obligations" should {

        "display only one of each received and open obligations and all overdue obligations" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub multiple business obligations response")
          SelfAssessmentStub.stubGetOnlyBizObs(testNino, testSelfEmploymentId, multipleReceivedOpenObligationsModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("the result should have a HTTP status of OK and a body containing 1 received, 2 overdue and 1 open obligation")
          res should have(

            //Check Status OK (200) Result
            httpStatus(OK),

            //Check Page Title of HTML Response Body
            pageTitle("Your report deadlines"),

            //User Name
            elementTextByID(id = "service-info-user-name")(testUserName),

            //Check four Obligation sections are returned
            nElementsWithClass("obligation")(4),

            //Check first obligation
            elementTextByID(id = "bi-ob-1-start")("1 October 2016"),
            elementTextByID(id = "bi-ob-1-end")("31 December 2016"),
            elementTextByID(id = "bi-ob-1-status")("Received"),

            //Check second obligation
            elementTextByID(id = "bi-ob-2-start")("1 January 2017"),
            elementTextByID(id = "bi-ob-2-end")("31 March 2017"),
            elementTextByID(id = "bi-ob-2-status")("Overdue"),

            //Check third obligation
            elementTextByID(id = "bi-ob-3-start")("1 April 2017"),
            elementTextByID(id = "bi-ob-3-end")("30 June 2017"),
            elementTextByID(id = "bi-ob-3-status")("Overdue"),

            //Check third obligation
            elementTextByID(id = "bi-ob-4-start")("1 July 2017"),
            elementTextByID(id = "bi-ob-4-end")("30 September 2017"),
            elementTextByID(id = "bi-ob-4-status")("Due by 31 October 2017")
          )
        }
      }

      "has a single property obligation" should {

        "display a single obligation with the correct dates and status" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub no business details as an income source")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.emptyBusinessDetailsResponse())

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub a single business obligation response")
          SelfAssessmentStub.stubGetOnlyPropObs(testNino, testSelfEmploymentId, singleObligationsDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("the result should have a HTTP status of OK and a body containing one obligation")
          res should have(

            //Check Status OK (200) Result
            httpStatus(OK),

            //Check Page Title of HTML Response Body
            pageTitle("Your report deadlines"),

            //User Name
            elementTextByID(id = "service-info-user-name")(testUserName),

            //Check one obligation section is returned
            nElementsWithClass("obligation")(1),

            //Check the 1st obligation data
            elementTextByID(id = "pi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "pi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "pi-ob-1-status")("Received"),
            isElementVisibleById("bi-ob")(false)
          )
        }
      }

      "has business and property obligations" should {

        "display one obligation each for business and property with the correct dates and statuses" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a Error Response from the User Details service")
          UserDetailsStub.stubGetUserDetailsError()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub a single business and property obligation response")
          SelfAssessmentStub.stubGetObligations(testNino, testSelfEmploymentId, singleObligationsDataSuccessModel, singleObligationsDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("the result should have a HTTP status of OK and a body containing one obligation for both property and business")
          res should have(

            //Check Status OK (200) Result
            httpStatus(OK),

            //Check Page Title of HTML Response Body
            pageTitle("Your report deadlines"),

            //Check two obligation sections are returned
            nElementsWithClass("obligation")(2),

            //Check the business obligation data
            elementTextByID(id = "bi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-ob-1-status")("Received"),

            //Check the business obligation data
            elementTextByID(id = "pi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "pi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "pi-ob-1-status")("Received")
          )
        }
      }
    }

    "unauthorised" should {

      "redirect to sign in" in {

        Given("I wiremock stub an unatuhorised user response")
        AuthStub.stubUnauthorised()

        When("I call GET /report-quarterly/income-and-expenses/view/obligations")
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