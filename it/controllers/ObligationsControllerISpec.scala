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
import helpers.servicemocks.{AuthStub, BtaPartialStub, SelfAssessmentStub, UserDetailsStub}
import play.api.http.Status._
import utils.ImplicitDateFormatter

class ObligationsControllerISpec extends ComponentSpecBase with ImplicitDateFormatter {

  "Calling the ObligationsController" when {

    "authorised with an active enrolment" which {

      "has a single business obligation" should {

        "display a single obligation with the correct dates and status" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a ServiceInfo Partial response")
          BtaPartialStub.stubGetServiceInfoPartial()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub a single business obligation response")
          SelfAssessmentStub.stubGetBusinessObligations(testNino, testSelfEmploymentId, singleObligationsDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("Verify business details has been called")
          SelfAssessmentStub.verifyGetBusinessDetails(testNino)

          Then("Verify property details has been called")
          SelfAssessmentStub.verifyGetPropertyDetails(testNino)

          Then("Verify that business obligations has been called")
          SelfAssessmentStub.verifyGetBusinessObligations(testNino, testSelfEmploymentId)

          Then("the result should have a HTTP status of OK")
          res should have(
            httpStatus(OK)
          )

            Then("the page title should be")
          res should have(
            pageTitle("Your report deadlines")
          )

          Then("the page should display the correct user")
            //User Name
          res should have(
            elementTextByID(id = "service-info-user-name")(testUserName)
          )

          Then("the page displays one obligation")
          res should have(
            nElementsWithClass("obligation")(1)
          )

          Then("the single business obligation data is")
          res should have(
            //Check the 1st obligation data
            elementTextByID(id = "bi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-ob-1-status")("Received")
          )

          Then("the page displays the View 2017 to 2018 details link")
          res should have(
            elementTextByID(id = "estimate-link-2018")("View 2017 to 2018 details")
          )

          Then("the page displays the View annual returns link")
          res should have(
            elementTextByID(id = "sa-link")("View annual returns")
          )

          Then("the page displays the Manage account link")
          res should have(
            elementTextByID(id = "service-info-manage-account-link")("Manage account")
          )

          Then("the page displays the Message link")
          res should have(
            elementTextByID(id = "service-info-messages-link")("Messages")
          )

          Then("the page should not contain any property obligation")
          res should have(
            isElementVisibleById("pi-ob")(false)
          )
        }
      }

      "has multiple business obligations" should {

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
            // SelfAssessmentStub.stubGetObligations(testNino,testSelfEmploymentId,multipleReceivedOpenObligationsModel,multipleReceivedOpenObligationsModel)
            SelfAssessmentStub.stubGetBusinessObligations(testNino, testSelfEmploymentId, multipleReceivedOpenObligationsModel)
            SelfAssessmentStub.stubGetPropertyObligations(testNino, multipleReceivedOpenObligationsModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getObligations

            Then("the result should have a HTTP status of OK")
            res should have(
              httpStatus(OK)
            )

            Then("the correct page title is displayed")
            res should have(
              pageTitle("Your report deadlines")
            )

            Then("the correct user is displayed")
            res should have(
              elementTextByID(id = "service-info-user-name")(testUserName)
            )

            Then("the page displays four business obligations and four property obligations")
            res should have(
              nElementsWithClass("obligation")(8)
            )

            Then("the first obligation data contains")
            res should have(
              elementTextByID(id = "bi-ob-1-start")("1 October 2016"),
              elementTextByID(id = "bi-ob-1-end")("31 December 2016"),
              elementTextByID(id = "bi-ob-1-status")("Received")
            )

            Then("the second obligation data contains")
            res should have(
              elementTextByID(id = "bi-ob-2-start")("1 January 2017"),
              elementTextByID(id = "bi-ob-2-end")("31 March 2017"),
              elementTextByID(id = "bi-ob-2-status")("Overdue")
            )

            Then("the third obligation data contains")
            res should have(
              elementTextByID(id = "bi-ob-3-start")("1 April 2017"),
              elementTextByID(id = "bi-ob-3-end")("30 June 2017"),
              elementTextByID(id = "bi-ob-3-status")("Overdue")
            )

            Then("the fourth obligation data contains")
            res should have(
              elementTextByID(id = "bi-ob-4-start")("1 July 2017"),
              elementTextByID(id = "bi-ob-4-end")("30 September 2017"),
              elementTextByID(id = "bi-ob-4-status")("Due by " + LocalDate.now().plusDays(30).toLongDate)

            )

            Then("the fifth obligation data contains")
            res should have(
              elementTextByID(id = "pi-ob-1-start")("1 October 2016"),
              elementTextByID(id = "pi-ob-1-end")("31 December 2016"),
              elementTextByID(id = "pi-ob-1-status")("Received")
            )

            Then("the sixth obligation data contains")
            res should have(
              elementTextByID(id = "pi-ob-2-start")("1 January 2017"),
              elementTextByID(id = "pi-ob-2-end")("31 March 2017"),
              elementTextByID(id = "pi-ob-2-status")("Overdue")
            )

            Then("the seventh obligation data contains")
            res should have(
              elementTextByID(id = "pi-ob-3-start")("1 April 2017"),
              elementTextByID(id = "pi-ob-3-end")("30 June 2017"),
              elementTextByID(id = "pi-ob-3-status")("Overdue")
            )

            Then("the eight obligation data contains")
            res should have(
              elementTextByID(id = "pi-ob-4-start")("1 July 2017"),
              elementTextByID(id = "pi-ob-4-end")("30 September 2017"),
              elementTextByID(id = "pi-ob-4-status")("Due by " + LocalDate.now().plusDays(30).toLongDate)
            )

            Then("the View 2017 to 2018 details link is displayed")
            res should have(
              elementTextByID(id = "estimate-link-2018")("View 2017 to 2018 details")
            )

            Then("the View annual returns link displayed")
            res should have(
              elementTextByID(id = "sa-link")("View annual returns")
            )

            Then("the Manage account link displayed")
            res should have(
              elementTextByID(id = "service-info-manage-account-link")("Manage account")
            )


            Then("the Message link is displayed")
            res should have(
              elementTextByID(id = "service-info-messages-link")("Messages")
            )

            Then("the Your report deadlines link is displayed")
            res should have(
              elementTextByID(id = "page-heading")("Your report deadlines")
            )

            Then("the fifth property and business obligation data are not displayed")
            res should have(
              isElementVisibleById("pi-ob-5-status")(false),
              isElementVisibleById("bi-ob-5-status")(false)
            )

          }
        }
      }

      "has multiple obligations" should {

        "display the correct amount of obligations with the correct statuses" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a ServiceInfo Partial response")
          BtaPartialStub.stubGetServiceInfoPartial()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub multiple business obligations response")
          SelfAssessmentStub.stubGetBusinessObligations(testNino, testSelfEmploymentId, multipleObligationsDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("Verify business details has been called")
          SelfAssessmentStub.verifyGetBusinessDetails(testNino)

          Then("Verify property details has been called")
          SelfAssessmentStub.verifyGetPropertyDetails(testNino)

          Then("Verify that business obligations has been called")
          SelfAssessmentStub.verifyGetBusinessObligations(testNino, testSelfEmploymentId)

          Then("the result should have a HTTP status of OK")
          res should have(
            httpStatus(OK)
          )

          Then("the page title should be displayed")
          res should have(
            pageTitle("Your report deadlines")
          )

          Then("the page should display the logged in user")
          res should have(
            elementTextByID(id = "service-info-user-name")(testUserName)
          )

          Then("the page should contain seven obligations")
          res should have(
            nElementsWithClass("obligation")(7)
          )

          Then("the first business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-ob-1-status")("Received")
          )
          Then("the second business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-ob-2-start")("6 October 2017"),
            elementTextByID(id = "bi-ob-2-end")("5 January 2018"),
            elementTextByID(id = "bi-ob-2-status")("Overdue")
          )

          Then("the third business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-ob-3-start")("6 July 2017"),
            elementTextByID(id = "bi-ob-3-end")("5 October 2017"),
            elementTextByID(id = "bi-ob-3-status")("Due by " + LocalDate.now().plusDays(1).toLongDate)
          )

          Then("the first property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-1-start")("1 October 2016"),
            elementTextByID(id = "pi-ob-1-end")("31 December 2016"),
            elementTextByID(id = "pi-ob-1-status")("Received")
          )
          Then("the second property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-2-start")("1 January 2017"),
            elementTextByID(id = "pi-ob-2-end")("31 March 2017"),
            elementTextByID(id = "pi-ob-2-status")("Overdue")
          )

          Then("the third property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-3-start")("1 April 2017"),
            elementTextByID(id = "pi-ob-3-end")("30 June 2017"),
            elementTextByID(id = "pi-ob-3-status")("Overdue")
          )

          Then("the fourth property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-4-start")("1 July 2017"),
            elementTextByID(id = "pi-ob-4-end")("30 September 2017"),
            elementTextByID(id = "pi-ob-4-status")("Due by " + LocalDate.now().plusDays(30).toLongDate)
          )
        }
      }

      "has multiple received and open business obligations" should {

        "display only one of each received and open obligations and all overdue obligations" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a ServiceInfo Partial response")
          BtaPartialStub.stubGetServiceInfoPartial()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub multiple business obligations response")
          SelfAssessmentStub.stubGetBusinessObligations(testNino, testSelfEmploymentId, multipleReceivedOpenObligationsModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("Verify business details has been called")
          SelfAssessmentStub.verifyGetBusinessDetails(testNino)

          Then("Verify property details has been called")
          SelfAssessmentStub.verifyGetPropertyDetails(testNino)

          Then("Verify that business obligations has been called")
          SelfAssessmentStub.verifyGetBusinessObligations(testNino, testSelfEmploymentId)

          Then("the result should have a HTTP status of OK")
          res should have(
            httpStatus(OK)
          )


          Then("the page title is displayed")
          res should have(
            pageTitle("Your report deadlines")
          )

          Then("the logged in user is displayed")
          res should have(
            elementTextByID(id = "service-info-user-name")(testUserName)
          )

          Then("eight obligations are displayed")
          res should have(
            nElementsWithClass("obligation")(8)
          )

          Then("first business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-ob-1-start")("1 October 2016"),
            elementTextByID(id = "bi-ob-1-end")("31 December 2016"),
            elementTextByID(id = "bi-ob-1-status")("Received")
          )

          Then("second business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-ob-2-start")("1 January 2017"),
            elementTextByID(id = "bi-ob-2-end")("31 March 2017"),
            elementTextByID(id = "bi-ob-2-status")("Overdue")
          )

          Then("third business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-ob-3-start")("1 April 2017"),
            elementTextByID(id = "bi-ob-3-end")("30 June 2017"),
            elementTextByID(id = "bi-ob-3-status")("Overdue")
          )

          Then("fourth business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-ob-4-start")("1 July 2017"),
            elementTextByID(id = "bi-ob-4-end")("30 September 2017"),
            elementTextByID(id = "bi-ob-4-status")("Due by " + LocalDate.now().plusDays(30).toLongDate)
          )

          Then("first property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-1-start")("1 October 2016"),
            elementTextByID(id = "pi-ob-1-end")("31 December 2016"),
            elementTextByID(id = "pi-ob-1-status")("Received")
          )

          Then("second property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-2-start")("1 January 2017"),
            elementTextByID(id = "pi-ob-2-end")("31 March 2017"),
            elementTextByID(id = "pi-ob-2-status")("Overdue")
          )

          Then("third property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-3-start")("1 April 2017"),
            elementTextByID(id = "pi-ob-3-end")("30 June 2017"),
            elementTextByID(id = "pi-ob-3-status")("Overdue")
          )

          Then("fourth property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-4-start")("1 July 2017"),
            elementTextByID(id = "pi-ob-4-end")("30 September 2017"),
            elementTextByID(id = "pi-ob-4-status")("Due by " + LocalDate.now().plusDays(30).toLongDate),
            isElementVisibleById("pi-ob-5-status")(false)
          )
        }
      }

      "has a single property obligation" should {

        "display a single obligation with the correct dates and status" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a ServiceInfo Partial response")
          BtaPartialStub.stubGetServiceInfoPartial()

          And("I wiremock stub no business details as an income source")
          SelfAssessmentStub.stubGetNoBusinessDetails(testNino)

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub a single business obligation response")
          SelfAssessmentStub.stubGetPropertyObligations(testNino, singleObligationsDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("Verify business details has been called")
          SelfAssessmentStub.verifyGetBusinessDetails(testNino)

          Then("Verify property details has been called")
          SelfAssessmentStub.verifyGetPropertyDetails(testNino)

          Then("Verify that business obligations has been called")
          SelfAssessmentStub.verifyGetPropertyObligations(testNino)

          Then("the result should have a HTTP status of OK")
          res should have(
            httpStatus(OK)
          )
            //Check Page Title of HTML Response Body
          Then("the page title is displayed")
          res should have(
            pageTitle("Your report deadlines")
          )

           Then("the logged in user is displayed")
          res should have(
            elementTextByID(id = "service-info-user-name")(testUserName)
          )

          Then("One property obligation is displayed")
          res should have(
            nElementsWithClass("obligation")(1)
          )

          Then("the single property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "pi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "pi-ob-1-status")("Received"),
            isElementVisibleById("bi-ob")(false)
          )
        }
      }

      "has multiple property obligations" should {

        "display the correct amount of obligations with the correct statuses" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub no business details response")
          SelfAssessmentStub.stubGetNoBusinessDetails(testNino)

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub multiple property obligations response")
          SelfAssessmentStub.stubGetPropertyObligations(testNino, multipleObligationsDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("Verify business details has been called")
          SelfAssessmentStub.verifyGetBusinessDetails(testNino)

          Then("Verify property details has been called")
          SelfAssessmentStub.verifyGetPropertyDetails(testNino)

          Then("Verify that property obligations has been called")
          SelfAssessmentStub.verifyGetPropertyObligations(testNino)

          Then("the result should have a HTTP status of OK")
          res should have(
            httpStatus(OK)
          )
            Then("the page title is displayed")
          res should have(
            pageTitle("Your report deadlines")
          )

          Then("the logged in user is displayed")
          res should have(
            elementTextByID(id = "service-info-user-name")(testUserName)
          )


          Then("three obligations are displayed")
          res should have(
            nElementsWithClass("obligation")(3)
          )

          Then("the first property obligation is")
          res should have(
            elementTextByID(id = "pi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "pi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "pi-ob-1-status")("Received")
          )

          Then("the second property obligation is")
          res should have(
            elementTextByID(id = "pi-ob-2-start")("6 October 2017"),
            elementTextByID(id = "pi-ob-2-end")("5 January 2018"),
            elementTextByID(id = "pi-ob-2-status")("Overdue")
          )

          Then("the third property obligation is")
          res should have(
            elementTextByID(id = "pi-ob-3-start")("6 July 2017"),
            elementTextByID(id = "pi-ob-3-end")("5 October 2017"),
            elementTextByID(id = "pi-ob-3-status")("Due by " + LocalDate.now().plusDays(1).toLongDate)
          )
        }
      }

      "has multiple received and open property obligations" should {

        "display only one of each received and open obligations and all overdue obligations" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub no business details response")
          SelfAssessmentStub.stubGetNoBusinessDetails(testNino)

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub multiple property open and received obligations response")
          SelfAssessmentStub.stubGetPropertyObligations(testNino, multipleReceivedOpenObligationsModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("Verify business details has been called")
          SelfAssessmentStub.verifyGetBusinessDetails(testNino)

          Then("Verify property details has been called")
          SelfAssessmentStub.verifyGetPropertyDetails(testNino)

          Then("Verify that business obligations has been called")
          SelfAssessmentStub.verifyGetPropertyObligations(testNino)

          Then("the result should have a HTTP status of OK")
          res should have(
            httpStatus(OK)
          )

          Then("the page title should be")
          res should have(
            pageTitle("Your report deadlines")
          )

          Then("the page displays the correct user")
          res should have(
            elementTextByID(id = "service-info-user-name")(testUserName)
          )

          Then("the page displays four obligations")
          res should have(

            nElementsWithClass("obligation")(4)
          )

          Then("the first obligation displayed should be")
          res should have(
            elementTextByID(id = "pi-ob-1-start")("1 October 2016"),
            elementTextByID(id = "pi-ob-1-end")("31 December 2016"),
            elementTextByID(id = "pi-ob-1-status")("Received")
          )

          Then("the second obligation displayed should be")
          res should have(
            elementTextByID(id = "pi-ob-2-start")("1 January 2017"),
            elementTextByID(id = "pi-ob-2-end")("31 March 2017"),
            elementTextByID(id = "pi-ob-2-status")("Overdue")
          )

          Then("the third obligation displayed should be")
          res should have(
            elementTextByID(id = "pi-ob-3-start")("1 April 2017"),
            elementTextByID(id = "pi-ob-3-end")("30 June 2017"),
            elementTextByID(id = "pi-ob-3-status")("Overdue")
          )
          Then("the fourth obligation dispolayed should be")
          res should have(
            elementTextByID(id = "pi-ob-4-start")("1 July 2017"),
            elementTextByID(id = "pi-ob-4-end")("30 September 2017"),
            elementTextByID(id = "pi-ob-4-status")("Due by " + LocalDate.now().plusDays(30).toLongDate)
          )
        }
      }

      "has business and property obligations" should {

        "display one obligation each for business and property with the correct dates and statuses" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a Error Response from the User Details service")
          UserDetailsStub.stubGetUserDetailsError()

          And("I wiremock stub a ServiceInfo Partial response")
          BtaPartialStub.stubGetServiceInfoPartial()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub a single business and property obligation response")
          SelfAssessmentStub.stubGetBusinessObligations(testNino, testSelfEmploymentId, singleObligationsDataSuccessModel)
          SelfAssessmentStub.stubGetPropertyObligations(testNino, singleObligationsDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("Verify business details has been called")
          SelfAssessmentStub.verifyGetBusinessDetails(testNino)

          Then("Verify property details has been called")
          SelfAssessmentStub.verifyGetPropertyDetails(testNino)

          Then("Verify that business obligations has been called")
          SelfAssessmentStub.verifyGetBusinessObligations(testNino, testSelfEmploymentId)

          Then("Verify that property obligations has been called")
          SelfAssessmentStub.verifyGetPropertyObligations(testNino)

          Then("the result should have a HTTP status of OK")
          res should have(

            //Check Status OK (200) Result
            httpStatus(OK)
          )
          Then("the page title")
          res should have(
            pageTitle("Your report deadlines")
          )

          Then("the page title")
          res should have(
            nElementsWithClass("obligation")(2)
          )

          Then("the single business obligation")
          res should have(
            elementTextByID(id = "bi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-ob-1-status")("Received")
          )

          Then("the single property obligation")
          res should have(
            elementTextByID(id = "pi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "pi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "pi-ob-1-status")("Received")
          )
        }
      }

      "has business income but returns an error response from business obligations" should {

        "Display an error message to the user" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response, with no Property Income Source")
          SelfAssessmentStub.stubGetNoPropertyDetails(testNino)

          And("I wiremock stub an error for the business obligations response")
          SelfAssessmentStub.stubBusinessObligationsError(testNino, testSelfEmploymentId)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("Verify business details has been called")
          SelfAssessmentStub.verifyGetBusinessDetails(testNino)

          Then("Verify property details has been called")
          SelfAssessmentStub.verifyGetPropertyDetails(testNino)

          Then("Verify that business obligations has been called")
          SelfAssessmentStub.verifyGetBusinessObligations(testNino, testSelfEmploymentId)

          Then("the result should have a HTTP status of Ok")
          res should have(
            httpStatus(OK)
          )

          Then("the page title is displayed")
          res should have(
            pageTitle("Your report deadlines")
          )

          Then("the business section is displayed under Business income")
          res should have(
            elementTextByID(id = "bi-section")("Business income")
          )

          Then("the page displays the following error message")
          res should have(
            elementTextByID(id = "bi-p1")("We can't display your next report due date at the moment.")
          )

          Then("the page displays the following instruction")
          res should have(
            elementTextByID(id = "bi-p2")("Try refreshing the page in a few minutes.")
          )

        }
      }

      "has property income but returns an error response from property obligations" should {

        "Display an error message to the user" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a success business details response, with no Business Income Source")
          SelfAssessmentStub.stubGetNoBusinessDetails(testNino)

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub an error for the property obligations response")
          SelfAssessmentStub.stubPropertyObligationsError(testNino)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("Verify business details has been called")
          SelfAssessmentStub.verifyGetBusinessDetails(testNino)

          Then("Verify property details has been called")
          SelfAssessmentStub.verifyGetPropertyDetails(testNino)

          Then("Verify that property obligations has been called")
          SelfAssessmentStub.verifyGetPropertyObligations(testNino)

          Then("the result should have a HTTP status of OK")
          res should have(
            httpStatus(OK)
          )
          Then("the page title is displayed")
          res should have(
            pageTitle("Your report deadlines")
          )

          Then("the property section is displayed under Property income")
          res should have(
            elementTextByID(id = "pi-section")("Property income")
          )

          Then("an error message for property obligations is displayed")
          res should have(
            elementTextByID(id = "pi-p1")("We can't display your next report due date at the moment.")
          )

          Then("the page displays the following instruction")
          res should have(
            elementTextByID(id = "pi-p2")("Try refreshing the page in a few minutes.")
          )
        }
      }

      "has both property income and business income but both return error responses when retrieving obligations" should {

        "Display an error message to the user" in {

          Given("I wiremock stub an authorised user response")
          AuthStub.stubAuthorised()

          And("I wiremock stub a response from the User Details service")
          UserDetailsStub.stubGetUserDetails()

          And("I wiremock stub a success business details response")
          SelfAssessmentStub.stubGetBusinessDetails(testNino, GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetPropertyDetails(testNino, GetPropertyDetails.successResponse())

          And("I wiremock stub an error for the property obligations response")
          SelfAssessmentStub.stubPropertyObligationsError(testNino)

          And("I wiremock stub an error for the business obligations response")
          SelfAssessmentStub.stubBusinessObligationsError(testNino, testSelfEmploymentId)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getObligations

          Then("Verify business details has been called")
          SelfAssessmentStub.verifyGetBusinessDetails(testNino)

          Then("Verify property details has been called")
          SelfAssessmentStub.verifyGetPropertyDetails(testNino)

          Then("Verify that business obligations has been called")
          SelfAssessmentStub.verifyGetBusinessObligations(testNino, testSelfEmploymentId)

          Then("Verify that property obligations has been called")
          SelfAssessmentStub.verifyGetPropertyObligations(testNino)

          Then("the result should have a HTTP status of OK")
          res should have(
            httpStatus(OK)
          )

          Then("the page title is displayed")
          res should have(
            pageTitle("Your report deadlines")
          )

          Then("an error message for property obligations is displayed")
          res should have(
            elementTextByID(id = "p1")("We can't display your next report due date at the moment."),
            elementTextByID(id = "p2")("Try refreshing the page in a few minutes.")
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