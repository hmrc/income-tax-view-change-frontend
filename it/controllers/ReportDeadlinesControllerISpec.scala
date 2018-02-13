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

import helpers.{ComponentSpecBase, GenericStubMethods}
import helpers.IntegrationTestConstants.GetReportDeadlinesData._
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuthStub, BtaPartialStub, SelfAssessmentStub, UserDetailsStub}
import org.scalatest.Assertion
import play.api.http.Status._
import play.api.libs.json.{JsNull, JsValue}
import play.api.libs.ws.WSResponse
import utils.ImplicitDateFormatter

class ReportDeadlinesControllerISpec extends ComponentSpecBase with ImplicitDateFormatter with GenericStubMethods {

  "Calling the ReportDeadlinesController" when {

    "isAuthorisedUser with an active enrolment" which {

      "has a single business obligation" should {

        "display a single obligation with the correct dates and status" in {

          isAuthorisedUser(true)

          stubUserDetails()

          stubPartial()

          getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub a single business obligation response")
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, singleReportDeadlinesDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle("Your Income Tax report deadlines"),
            elementTextByID(id = "estimate-link-2018")("View 2017 to 2018 details"),
            elementTextByID(id = "sa-link")("View annual returns")
          )

          Then("the page displays one obligation")
          res should have(
            nElementsWithClass("obligation")(1)
          )

          Then("the single business obligation data is")
          res should have(
            //Check the 1st obligation data
            elementTextByID(id = "bi-1-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-1-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-1-ob-1-status")("Received")
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

            isAuthorisedUser(true)

            stubUserDetails()

            getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

            getPropDeets(GetPropertyDetails.successResponse())

            And("I wiremock stub a single property and business obligation response")
            // SelfAssessmentStub.stubGetReportDeadlines(testNino,testSelfEmploymentId,multipleReceivedOpenReportDeadlinesModel,multipleReceivedOpenReportDeadlinesModel)
            SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, multipleReceivedOpenReportDeadlinesModel)
            SelfAssessmentStub.stubGetPropertyReportDeadlines(testNino, multipleReceivedOpenReportDeadlinesModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("the correct title, username and links are displayed")
            res should have(
              httpStatus(OK),
              pageTitle("Your Income Tax report deadlines"),
              elementTextByID(id = "estimate-link-2018")("View 2017 to 2018 details"),
              elementTextByID(id = "sa-link")("View annual returns")
            )

            Then("the page displays four business obligations and four property obligations")
            res should have(
              nElementsWithClass("obligation")(10)
            )

            Then("the first business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-1-start")("1 October 2016"),
              elementTextByID(id = "bi-1-ob-1-end")("31 December 2016"),
              elementTextByID(id = "bi-1-ob-1-status")("Received")
            )

            Then("the second business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-2-start")("1 January 2017"),
              elementTextByID(id = "bi-1-ob-2-end")("31 March 2017"),
              elementTextByID(id = "bi-1-ob-2-status")("Overdue")
            )

            Then("the third business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-3-start")("1 April 2017"),
              elementTextByID(id = "bi-1-ob-3-end")("30 June 2017"),
              elementTextByID(id = "bi-1-ob-3-status")("Overdue")
            )

            Then("the fourth business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-4-eops")("Whole tax year (final check)"),
              elementTextByID(id = "bi-1-ob-4-status")("Overdue")
            )

            Then("the fifth business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-5-start")("1 July 2017"),
              elementTextByID(id = "bi-1-ob-5-end")("30 September 2017"),
              elementTextByID(id = "bi-1-ob-5-status")("Due by " + LocalDate.now().plusDays(30).toLongDate)
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

            Then("the fourth business obligation displayed is")
            res should have(
              elementTextByID(id = "pi-ob-4-eops")("Whole tax year (final check)"),
              elementTextByID(id = "pi-ob-4-status")("Overdue")
            )

            Then("the fifth property obligation displayed is")
            res should have(
              elementTextByID(id = "pi-ob-5-start")("1 July 2017"),
              elementTextByID(id = "pi-ob-5-end")("30 September 2017"),
              elementTextByID(id = "pi-ob-5-status")("Due by " + LocalDate.now().plusDays(30).toLongDate)
            )

            Then("the sixth property and business obligation data are not displayed")
            res should have(
              isElementVisibleById("pi-ob-6-status")(false),
              isElementVisibleById("bi-1-ob-6-status")(false)
            )

          }
        }
      }

      "has multiple obligations" should {

        "display the correct amount of obligations with the correct statuses" in {

          isAuthorisedUser(true)

          stubUserDetails()

          stubPartial()

          getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub multiple business obligations response")
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          SelfAssessmentStub.stubGetPropertyReportDeadlines(testNino, multipleReportDeadlinesDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId)

          Then("the view should display the title and username")
          res should have(
            httpStatus(OK),
            pageTitle("Your Income Tax report deadlines")
          )

          Then("the page should contain seven obligations")
          res should have(
            nElementsWithClass("obligation")(6)
          )

          Then("the first business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-1-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-1-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-1-ob-1-status")("Received")
          )
          Then("the second business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-1-ob-2-start")("6 October 2017"),
            elementTextByID(id = "bi-1-ob-2-end")("5 January 2018"),
            elementTextByID(id = "bi-1-ob-2-status")("Overdue")
          )

          Then("the third business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-1-ob-3-start")("6 July 2017"),
            elementTextByID(id = "bi-1-ob-3-end")("5 October 2017"),
            elementTextByID(id = "bi-1-ob-3-status")("Due by " + LocalDate.now().plusDays(1).toLongDate)
          )

          Then("the first property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "pi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "pi-ob-1-status")("Received")
          )
          Then("the second property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-2-start")("6 October 2017"),
            elementTextByID(id = "pi-ob-2-end")("5 January 2018"),
            elementTextByID(id = "pi-ob-2-status")("Overdue")
          )

          Then("the third property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-3-start")("6 July 2017"),
            elementTextByID(id = "pi-ob-3-end")("5 October 2017"),
            elementTextByID(id = "pi-ob-3-status")("Due by " + LocalDate.now().plusDays(1).toLongDate)
          )


        }
      }

      "has multiple received and open business obligations" should {

        "display only one of each received and open obligations and all overdue obligations" in {

          isAuthorisedUser(true)

          stubUserDetails()

          stubPartial()

          getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub multiple business obligations response")
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, multipleReceivedOpenReportDeadlinesModel)
          SelfAssessmentStub.stubGetPropertyReportDeadlines(testNino, multipleReceivedOpenReportDeadlinesModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId)

          Then("the view should display the title and username")
          res should have(
            httpStatus(OK),
            pageTitle("Your Income Tax report deadlines")
          )

          Then("ten obligations are displayed")
          res should have(
            nElementsWithClass("obligation")(10)
          )

          Then("first business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-1-ob-1-start")("1 October 2016"),
            elementTextByID(id = "bi-1-ob-1-end")("31 December 2016"),
            elementTextByID(id = "bi-1-ob-1-status")("Received")
          )

          Then("second business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-1-ob-2-start")("1 January 2017"),
            elementTextByID(id = "bi-1-ob-2-end")("31 March 2017"),
            elementTextByID(id = "bi-1-ob-2-status")("Overdue")
          )

          Then("third business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-1-ob-3-start")("1 April 2017"),
            elementTextByID(id = "bi-1-ob-3-end")("30 June 2017"),
            elementTextByID(id = "bi-1-ob-3-status")("Overdue")
          )


          Then("the fourth business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-1-ob-4-eops")("Whole tax year (final check)"),
            elementTextByID(id = "bi-1-ob-4-status")("Overdue")
          )


          Then("Fifth business obligation displayed is")
          res should have(
            elementTextByID(id = "bi-1-ob-5-start")("1 July 2017"),
            elementTextByID(id = "bi-1-ob-5-end")("30 September 2017"),
            elementTextByID(id = "bi-1-ob-5-status")("Due by " + LocalDate.now().plusDays(30).toLongDate)
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

          Then("the fourth business obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-4-eops")("Whole tax year (final check)"),
            elementTextByID(id = "pi-ob-4-status")("Overdue")
          )

          Then("fifth property obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-5-start")("1 July 2017"),
            elementTextByID(id = "pi-ob-5-end")("30 September 2017"),
            elementTextByID(id = "pi-ob-5-status")("Due by " + LocalDate.now().plusDays(30).toLongDate),
            isElementVisibleById("pi-ob-6-status")(false)
          )
        }
      }

      "has a single property obligation" should {

        "display a single obligation with the correct dates and status" in {

          isAuthorisedUser(true)

          stubUserDetails()

          stubPartial()

          getBizDeets()

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub a single business obligation response")
          SelfAssessmentStub.stubGetPropertyReportDeadlines(testNino, singleReportDeadlinesDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyPropObsCall()

          Then("the view should display the title and username")
          res should have(
            httpStatus(OK),
            pageTitle("Your Income Tax report deadlines")
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
            isElementVisibleById("bi-1-ob")(false)
          )
        }
      }

      "has multiple property obligations" should {

        "display the correct amount of obligations with the correct statuses" in {

          isAuthorisedUser(true)

          stubUserDetails()

          getBizDeets()

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub multiple property obligations response")
          SelfAssessmentStub.stubGetPropertyReportDeadlines(testNino, multipleReportDeadlinesDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyPropObsCall()

          Then("the view should display the title and username")
          res should have(
            httpStatus(OK),
            pageTitle("Your Income Tax report deadlines")
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

          isAuthorisedUser(true)

          stubUserDetails()

          getBizDeets()

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub multiple property open and received obligations response")
          SelfAssessmentStub.stubGetPropertyReportDeadlines(testNino, multipleReceivedOpenReportDeadlinesModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyPropObsCall()

          Then("the view should display the title and username")
          res should have(
            httpStatus(OK),
            pageTitle("Your Income Tax report deadlines")
          )

          Then("the page displays five obligations")
          res should have(

            nElementsWithClass("obligation")(5)
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

          Then("the fourth business obligation displayed is")
          res should have(
            elementTextByID(id = "pi-ob-4-eops")("Whole tax year (final check)"),
            elementTextByID(id = "pi-ob-4-status")("Overdue")
          )

          Then("the fifth obligation dispolayed should be")
          res should have(
            elementTextByID(id = "pi-ob-5-start")("1 July 2017"),
            elementTextByID(id = "pi-ob-5-end")("30 September 2017"),
            elementTextByID(id = "pi-ob-5-status")("Due by " + LocalDate.now().plusDays(30).toLongDate)
          )
        }
      }

      "has business and property obligations" should {

        "display one obligation each for business and property with the correct dates and statuses" in {

          isAuthorisedUser(true)

          stubUserDetailsError()

          stubPartial()

          getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub a single business and property obligation response")
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, singleReportDeadlinesDataSuccessModel)
          SelfAssessmentStub.stubGetPropertyReportDeadlines(testNino, singleReportDeadlinesDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId)

          verifyPropObsCall()

          res should have(
            httpStatus(OK),
            pageTitle("Your Income Tax report deadlines")
          )

          Then("the page title")
          res should have(
            nElementsWithClass("obligation")(2)
          )

          Then("the single business obligation")
          res should have(
            elementTextByID(id = "bi-1-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-1-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-1-ob-1-status")("Received")
          )

          Then("the single property obligation")
          res should have(
            elementTextByID(id = "pi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "pi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "pi-ob-1-status")("Received")
          )
        }
      }

      "has 2 businesses with one obligation each" should {

        "display the obligation of each businesses" in {

          isAuthorisedUser(true)

          stubUserDetails()

          stubPartial()

          getBizDeets(GetBusinessDetails.multipleSuccessResponse(testSelfEmploymentId, otherTestSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetNoPropertyDetails(testNino)

          And("I wiremock stub a single business obligation response for each business")
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, singleReportDeadlinesDataSuccessModel)
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, otherTestSelfEmploymentId, singleReportDeadlinesDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId, otherTestSelfEmploymentId)

          Then("the page should display the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle("Your Income Tax report deadlines"),
            elementTextByID(id = "estimate-link-2018")("View 2017 to 2018 details"),
            elementTextByID(id = "sa-link")("View annual returns")
          )

          Then("the page displays two obligations")
          res should have(
            nElementsWithClass("obligation")(2)
          )

          Then("the first business obligation data is")
          res should have(
            //Check the 1st obligation data
            elementTextByID(id = "bi-1-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-1-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-1-ob-1-status")("Received")
          )

          Then("the second business obligation data is")
          res should have(
            //Check the 1st obligation data
            elementTextByID(id = "bi-2-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-2-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-2-ob-1-status")("Received")
          )

          Then("the page should not contain any property obligation")
          res should have(
            isElementVisibleById("pi-ob")(false)
          )

        }

      }

      "has 2 businesses with multiple obligations and property with one obligation" should {

        "display the obligation of each businesses" in {

          isAuthorisedUser(true)

          stubUserDetails()

          stubPartial()

          getBizDeets(GetBusinessDetails.multipleSuccessResponse(testSelfEmploymentId, otherTestSelfEmploymentId))

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub multiple business obligations and a single property obligation response")
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, otherTestSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          SelfAssessmentStub.stubGetPropertyReportDeadlines(testNino, singleReportDeadlinesDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId, otherTestSelfEmploymentId)

          Then("the page should display the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle("Your Income Tax report deadlines"),
            elementTextByID(id = "estimate-link-2018")("View 2017 to 2018 details"),
            elementTextByID(id = "sa-link")("View annual returns")
          )

          Then("the page displays seven obligations")
          res should have(
            nElementsWithClass("obligation")(7)
          )

          Then("the first business obligation data is")
          res should have(
            elementTextByID(id = "bi-1-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-1-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-1-ob-1-status")("Received"),
            elementTextByID(id = "bi-1-ob-3-start")("6 July 2017"),
            elementTextByID(id = "bi-1-ob-3-end")("5 October 2017"),
            elementTextByID(id = "bi-1-ob-3-status")("Due by " + LocalDate.now().plusDays(1).toLongDate),
            elementTextByID(id = "bi-1-ob-2-start")("6 October 2017"),
            elementTextByID(id = "bi-1-ob-2-end")("5 January 2018"),
            elementTextByID(id = "bi-1-ob-2-status")("Overdue")
          )

          Then("the second business obligation data is")
          res should have(
            elementTextByID(id = "bi-2-ob-1-start")("6 April 2017"),
            elementTextByID(id = "bi-2-ob-1-end")("5 July 2017"),
            elementTextByID(id = "bi-2-ob-1-status")("Received"),
            elementTextByID(id = "bi-2-ob-3-start")("6 July 2017"),
            elementTextByID(id = "bi-2-ob-3-end")("5 October 2017"),
            elementTextByID(id = "bi-2-ob-3-status")("Due by " + LocalDate.now().plusDays(1).toLongDate),
            elementTextByID(id = "bi-2-ob-2-start")("6 October 2017"),
            elementTextByID(id = "bi-2-ob-2-end")("5 January 2018"),
            elementTextByID(id = "bi-2-ob-2-status")("Overdue")
          )

          Then("the property obligation data is")
          res should have(
            elementTextByID(id = "pi-ob-1-start")("6 April 2017"),
            elementTextByID(id = "pi-ob-1-end")("5 July 2017"),
            elementTextByID(id = "pi-ob-1-status")("Received")
          )

        }

      }

      "has business income but returns an error response from business obligations" should {

        "Display an error message to the user" in {

          isAuthorisedUser(true)

          stubUserDetails()

          getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response, with no Property Income Source")
          SelfAssessmentStub.stubGetNoPropertyDetails(testNino)

          And("I wiremock stub an error for the business obligations response")
          SelfAssessmentStub.stubBusinessReportDeadlinesError(testNino, testSelfEmploymentId)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId)

          Then("the view is displayed with an error message under the business income section")
          res should have(
            httpStatus(OK),
            pageTitle("Your Income Tax report deadlines"),
            elementTextByID(id = "bi-1-section")("business"),
            elementTextByID(id = "bi-1-p1")("We can't display your next report due date at the moment."),
            elementTextByID(id = "bi-1-p2")("Try refreshing the page in a few minutes.")
          )
        }
      }

      "has property income but returns an error response from property obligations" should {

        "Display an error message to the user" in {

          isAuthorisedUser(true)

          stubUserDetails()

          getBizDeets()

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub an error for the property obligations response")
          SelfAssessmentStub.stubPropertyReportDeadlinesError(testNino)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyPropObsCall()

          Then("the view is displayed with an error message under the property income section")
          res should have(
            httpStatus(OK),
            pageTitle("Your Income Tax report deadlines"),
            elementTextByID(id = "pi-section")("Property income"),
            elementTextByID(id = "pi-p1")("We can't display your next report due date at the moment."),
            elementTextByID(id = "pi-p2")("Try refreshing the page in a few minutes.")
          )
        }
      }

      "has both property income and business income but both return error responses when retrieving obligations" should {

        "Display an error message to the user" in {

          isAuthorisedUser(true)

          stubUserDetails()

          getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub an error for the property obligations response")
          SelfAssessmentStub.stubPropertyReportDeadlinesError(testNino)

          And("I wiremock stub an error for the business obligations response")
          SelfAssessmentStub.stubBusinessReportDeadlinesError(testNino, testSelfEmploymentId)

          When("I call GET /report-quarterly/income-and-expenses/view/obligations")
          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId)

          verifyPropObsCall()

          Then("an error message for property obligations is returned and the correct view is displayed")
          res should have(
            httpStatus(OK),
            pageTitle("Your Income Tax report deadlines"),
            elementTextByID(id = "p1")("We can't display your next report due date at the moment."),
            elementTextByID(id = "p2")("Try refreshing the page in a few minutes.")
          )
        }
      }

    }

    "unauthorised" should {

      "redirect to sign in" in {

        isAuthorisedUser(false)

        When("I call GET /report-quarterly/income-and-expenses/view/obligations")
        val res = IncomeTaxViewChangeFrontend.getReportDeadlines

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
  }

}
