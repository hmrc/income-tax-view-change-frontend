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

import assets.BaseIntegrationTestConstants._
import assets.ReportDeadlinesIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import config.FrontendAppConfig
import helpers.servicemocks.{IncomeTaxViewChangeStub, SelfAssessmentStub}
import helpers.{ComponentSpecBase, GenericStubMethods}
import play.api.http.Status._
import utils.ImplicitDateFormatter

class ReportDeadlinesControllerISpec extends ComponentSpecBase with ImplicitDateFormatter with GenericStubMethods {

  lazy val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  "Calling the ReportDeadlinesController" when {

    "the ReportDeadlines Feature is disabled" should {

      "Redirect to the Income Tax View Change Home Page" in {

        appConfig.features.reportDeadlinesEnabled(false)
        isAuthorisedUser(true)
        stubUserDetails()

        And("I wiremock stub a successful Income Source Details response with 1 Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub a single business obligation response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, singleReportDeadlinesDataSuccessModel)

        When("I call GET /report-quarterly/income-and-expenses/view/obligations")
        val res = IncomeTaxViewChangeFrontend.getReportDeadlines

        Then("the result should have a HTTP status of SEE_OTHER (303) and redirect to the Income Tax home page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.home().url)
        )
      }

    }

    "the ReportDeadlines Feature is enabled" when {

      "isAuthorisedUser with an active enrolment" which {

        "has a single business obligation" should {

          "display a single obligation with the correct dates and status" in {

            appConfig.features.reportDeadlinesEnabled(true)
            isAuthorisedUser(true)
            stubUserDetails()

            And("I wiremock stub a successful Income Source Details response with single Business")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

            And("I wiremock stub a single business obligation response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, singleReportDeadlinesDataSuccessModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("I verify the Income Source Details has been successfully wiremocked")
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
            verifyReportDeadlinesCall(testSelfEmploymentId)

            Then("the view displays the correct title, username and links")
            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines")

            )

            Then("the page displays one obligation")
            res should have(
              nElementsWithClass("obligation")(1)
            )

            Then("the single business obligation data is")
            res should have(
              //Check the 1st obligation data
              elementTextByID(id = "bi-1-ob-1-start")("6 Apr 2017"),
              elementTextByID(id = "bi-1-ob-1-end")("5 Jul 2017"),
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

              appConfig.features.reportDeadlinesEnabled(true)
              isAuthorisedUser(true)
              stubUserDetails()

              And("I wiremock stub a successful Income Source Details response with single Business and Property income")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                OK, businessAndPropertyResponse
              )

              And("I wiremock stub a single property and business obligation response")
              IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReceivedOpenReportDeadlinesModel)
              IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReceivedOpenReportDeadlinesModel)

              When("I call GET /report-quarterly/income-and-expenses/view/obligations")
              val res = IncomeTaxViewChangeFrontend.getReportDeadlines

              Then("I verify the Income Source Details has been successfully wiremocked")
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)

              Then("the correct title, username and links are displayed")
              res should have(
                httpStatus(OK),
                pageTitle("Report deadlines")

              )

              Then("the page displays four business obligations and four property obligations")
              res should have(
                nElementsWithClass("obligation")(10)
              )

              Then("the first business obligation displayed is")
              res should have(
                elementTextByID(id = "bi-1-ob-1-start")("1 Oct 2016"),
                elementTextByID(id = "bi-1-ob-1-end")("31 Dec 2016"),
                elementTextByID(id = "bi-1-ob-1-status")("Received")
              )

              Then("the second business obligation displayed is")
              res should have(
                elementTextByID(id = "bi-1-ob-2-start")("1 Jan 2017"),
                elementTextByID(id = "bi-1-ob-2-end")("31 Mar 2017"),
                elementTextByID(id = "bi-1-ob-2-status")(LocalDate.now().minusDays(128).toLongDateShort +" Overdue")
              )

              Then("the third business obligation displayed is")
              res should have(
                elementTextByID(id = "bi-1-ob-3-start")("1 Apr 2017"),
                elementTextByID(id = "bi-1-ob-3-end")("30 Jun 2017"),
                elementTextByID(id = "bi-1-ob-3-status")(LocalDate.now().minusDays(36).toLongDateShort +" Overdue")
              )

              Then("the fourth business obligation displayed is")
              res should have(
                elementTextByID(id = "bi-1-ob-4-eops")("Whole tax year (final check)"),
                elementTextByID(id = "bi-1-ob-4-status")(LocalDate.now().minusDays(36).toLongDateShort +" Overdue")
              )

              Then("the fifth business obligation displayed is")
              res should have(
                elementTextByID(id = "bi-1-ob-5-start")("1 Jul 2017"),
                elementTextByID(id = "bi-1-ob-5-end")("30 Sep 2017"),
                elementTextByID(id = "bi-1-ob-5-status")(LocalDate.now().plusDays(30).toLongDateShort)
              )

              Then("the first property obligation displayed is")
              res should have(
                elementTextByID(id = "pi-ob-1-start")("1 Oct 2016"),
                elementTextByID(id = "pi-ob-1-end")("31 Dec 2016"),
                elementTextByID(id = "pi-ob-1-status")("Received")
              )

              Then("the second property obligation displayed is")
              res should have(
                elementTextByID(id = "pi-ob-2-start")("1 Jan 2017"),
                elementTextByID(id = "pi-ob-2-end")("31 Mar 2017"),
                elementTextByID(id = "pi-ob-2-status")(LocalDate.now().minusDays(128).toLongDateShort + " Overdue")
              )

              Then("the third property obligation displayed is")
              res should have(
                elementTextByID(id = "pi-ob-3-start")("1 Apr 2017"),
                elementTextByID(id = "pi-ob-3-end")("30 Jun 2017"),
                elementTextByID(id = "pi-ob-3-status")(LocalDate.now().minusDays(36).toLongDateShort +" Overdue")
              )

              Then("the fourth business obligation displayed is")
              res should have(
                elementTextByID(id = "pi-ob-4-eops")("Whole tax year (final check)"),
                elementTextByID(id = "pi-ob-4-status")(LocalDate.now().minusDays(36).toLongDateShort +" Overdue")
              )

              Then("the fifth property obligation displayed is")
              res should have(
                elementTextByID(id = "pi-ob-5-start")("1 Jul 2017"),
                elementTextByID(id = "pi-ob-5-end")("30 Sep 2017"),
                elementTextByID(id = "pi-ob-5-status")(LocalDate.now().plusDays(30).toLongDateShort)
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

            appConfig.features.reportDeadlinesEnabled(true)
            isAuthorisedUser(true)
            stubUserDetails()

            And("I wiremock stub a successful Income Source Details response with single Business and Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              OK, businessAndPropertyResponse
            )

            And("I wiremock stub multiple business obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("I verify the Income Source Details has been successfully wiremocked")
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)

            Then("the view should display the title and username")
            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines")
            )

            Then("the page should contain seven obligations")
            res should have(
              nElementsWithClass("obligation")(6)
            )

            Then("the first business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-1-start")("6 Apr 2017"),
              elementTextByID(id = "bi-1-ob-1-end")("5 Jul 2017"),
              elementTextByID(id = "bi-1-ob-1-status")("Received")
            )
            Then("the second business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-2-start")("6 Oct 2017"),
              elementTextByID(id = "bi-1-ob-2-end")("5 Jan 2018"),
              elementTextByID(id = "bi-1-ob-2-status")(LocalDate.now().minusDays(1).toLongDateShort +" Overdue")
            )

            Then("the third business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-3-start")("6 Jul 2017"),
              elementTextByID(id = "bi-1-ob-3-end")("5 Oct 2017"),
              elementTextByID(id = "bi-1-ob-3-status")(LocalDate.now().plusDays(1).toLongDateShort)
            )

            Then("the first property obligation displayed is")
            res should have(
              elementTextByID(id = "pi-ob-1-start")("6 Apr 2017"),
              elementTextByID(id = "pi-ob-1-end")("5 Jul 2017"),
              elementTextByID(id = "pi-ob-1-status")("Received")
            )
            Then("the second property obligation displayed is")
            res should have(
              elementTextByID(id = "pi-ob-2-start")("6 Oct 2017"),
              elementTextByID(id = "pi-ob-2-end")("5 Jan 2018"),
              elementTextByID(id = "pi-ob-2-status")(LocalDate.now().minusDays(1).toLongDateShort +" Overdue")
            )

            Then("the third property obligation displayed is")
            res should have(
              elementTextByID(id = "pi-ob-3-start")("6 Jul 2017"),
              elementTextByID(id = "pi-ob-3-end")("5 Oct 2017"),
              elementTextByID(id = "pi-ob-3-status")(LocalDate.now().plusDays(1).toLongDateShort)
            )


          }
        }

        "has multiple received and open business obligations" should {

          "display only one of each received and open obligations and all overdue obligations" in {

            appConfig.features.reportDeadlinesEnabled(true)
            isAuthorisedUser(true)
            stubUserDetails()

            And("I wiremock stub a successful Income Source Details response with single Business and Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              OK, businessAndPropertyResponse
            )

            And("I wiremock stub multiple business obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReceivedOpenReportDeadlinesModel)
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReceivedOpenReportDeadlinesModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("I verify the Income Source Details has been successfully wiremocked")
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)

            Then("the view should display the title and username")
            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines")
            )

            Then("ten obligations are displayed")
            res should have(
              nElementsWithClass("obligation")(10)
            )

            Then("first business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-1-start")("1 Oct 2016"),
              elementTextByID(id = "bi-1-ob-1-end")("31 Dec 2016"),
              elementTextByID(id = "bi-1-ob-1-status")("Received")
            )

            Then("second business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-2-start")("1 Jan 2017"),
              elementTextByID(id = "bi-1-ob-2-end")("31 Mar 2017"),
              elementTextByID(id = "bi-1-ob-2-status")(LocalDate.now().minusDays(128).toLongDateShort +" Overdue")
            )

            Then("third business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-3-start")("1 Apr 2017"),
              elementTextByID(id = "bi-1-ob-3-end")("30 Jun 2017"),
              elementTextByID(id = "bi-1-ob-3-status")(LocalDate.now().minusDays(36).toLongDateShort + " Overdue")
            )


            Then("the fourth business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-4-eops")("Whole tax year (final check)"),
              elementTextByID(id = "bi-1-ob-4-status")(LocalDate.now().minusDays(36).toLongDateShort + " Overdue")
            )


            Then("Fifth business obligation displayed is")
            res should have(
              elementTextByID(id = "bi-1-ob-5-start")("1 Jul 2017"),
              elementTextByID(id = "bi-1-ob-5-end")("30 Sep 2017"),
              elementTextByID(id = "bi-1-ob-5-status")(LocalDate.now().plusDays(30).toLongDateShort)
            )

            Then("first property obligation displayed is")
            res should have(
              elementTextByID(id = "pi-ob-1-start")("1 Oct 2016"),
              elementTextByID(id = "pi-ob-1-end")("31 Dec 2016"),
              elementTextByID(id = "pi-ob-1-status")("Received")
            )

            Then("second property obligation displayed is")
            res should have(
              elementTextByID(id = "pi-ob-2-start")("1 Jan 2017"),
              elementTextByID(id = "pi-ob-2-end")("31 Mar 2017"),
              elementTextByID(id = "pi-ob-2-status")(LocalDate.now().minusDays(128).toLongDateShort +" Overdue")
            )

            Then("third property obligation displayed is")
            res should have(
              elementTextByID(id = "pi-ob-3-start")("1 Apr 2017"),
              elementTextByID(id = "pi-ob-3-end")("30 Jun 2017"),
              elementTextByID(id = "pi-ob-3-status")(LocalDate.now().minusDays(36).toLongDateShort +" Overdue")
            )

            Then("the fourth business obligation displayed is")
            res should have(
              elementTextByID(id = "pi-ob-4-eops")("Whole tax year (final check)"),
              elementTextByID(id = "pi-ob-4-status")(LocalDate.now().minusDays(36).toLongDateShort +" Overdue")
            )

            Then("fifth property obligation displayed is")
            res should have(
              elementTextByID(id = "pi-ob-5-start")("1 Jul 2017"),
              elementTextByID(id = "pi-ob-5-end")("30 Sep 2017"),
              elementTextByID(id = "pi-ob-5-status")(LocalDate.now().plusDays(30).toLongDateShort),
              isElementVisibleById("pi-ob-6-status")(false)
            )
          }
        }

        "has a single property obligation" should {

          "display a single obligation with the correct dates and status" in {

            appConfig.features.reportDeadlinesEnabled(true)
            isAuthorisedUser(true)
            stubUserDetails()

            And("I wiremock stub a successful Income Source Details response with Property only income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            And("I wiremock stub a single business obligation response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, singleReportDeadlinesDataSuccessModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("I verify the Income Source Details has been successfully wiremocked")
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            verifyReportDeadlinesCall(testPropertyIncomeId)

            Then("the view should display the title and username")
            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines")
            )

            Then("One property obligation is displayed")
            res should have(
              nElementsWithClass("obligation")(1)
            )

            Then("the single property obligation displayed is")
            res should have(
              elementTextByID(id = "pi-ob-1-start")("6 Apr 2017"),
              elementTextByID(id = "pi-ob-1-end")("5 Jul 2017"),
              elementTextByID(id = "pi-ob-1-status")("Received"),
              isElementVisibleById("bi-1-ob")(false)
            )
          }
        }

        "has multiple property obligations" should {

          "display the correct amount of obligations with the correct statuses" in {

            appConfig.features.reportDeadlinesEnabled(true)
            isAuthorisedUser(true)
            stubUserDetails()

            And("I wiremock stub a successful Income Source Details response with Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            And("I wiremock stub multiple property obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("I verify the Income Source Details has been successfully wiremocked")
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            verifyReportDeadlinesCall(testPropertyIncomeId)

            Then("the view should display the title and username")
            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines")
            )


            Then("three obligations are displayed")
            res should have(
              nElementsWithClass("obligation")(3)
            )

            Then("the first property obligation is")
            res should have(
              elementTextByID(id = "pi-ob-1-start")("6 Apr 2017"),
              elementTextByID(id = "pi-ob-1-end")("5 Jul 2017"),
              elementTextByID(id = "pi-ob-1-status")("Received")
            )

            Then("the second property obligation is")
            res should have(
              elementTextByID(id = "pi-ob-2-start")("6 Oct 2017"),
              elementTextByID(id = "pi-ob-2-end")("5 Jan 2018"),
              elementTextByID(id = "pi-ob-2-status")(LocalDate.now().minusDays(1).toLongDateShort +" Overdue")
            )

            Then("the third property obligation is")
            res should have(
              elementTextByID(id = "pi-ob-3-start")("6 Jul 2017"),
              elementTextByID(id = "pi-ob-3-end")("5 Oct 2017"),
              elementTextByID(id = "pi-ob-3-status")(LocalDate.now().plusDays(1).toLongDateShort)
            )
          }
        }

        "has multiple received and open property obligations" should {

          "display only one of each received and open obligations and all overdue obligations" in {

            appConfig.features.reportDeadlinesEnabled(true)
            isAuthorisedUser(true)
            stubUserDetails()

            And("I wiremock stub a successful Income Source Details response with single Business and Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            And("I wiremock stub multiple property open and received obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReceivedOpenReportDeadlinesModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("I verify the Income Source Details has been successfully wiremocked")
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            verifyReportDeadlinesCall(testPropertyIncomeId)

            Then("the view should display the title and username")
            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines")
            )

            Then("the page displays five obligations")
            res should have(

              nElementsWithClass("obligation")(5)
            )

            Then("the first obligation displayed should be")
            res should have(
              elementTextByID(id = "pi-ob-1-start")("1 Oct 2016"),
              elementTextByID(id = "pi-ob-1-end")("31 Dec 2016"),
              elementTextByID(id = "pi-ob-1-status")("Received")
            )

            Then("the second obligation displayed should be")
            res should have(
              elementTextByID(id = "pi-ob-2-start")("1 Jan 2017"),
              elementTextByID(id = "pi-ob-2-end")("31 Mar 2017"),
              elementTextByID(id = "pi-ob-2-status")(LocalDate.now().minusDays(128).toLongDateShort +" Overdue")
            )

            Then("the third obligation displayed should be")
            res should have(
              elementTextByID(id = "pi-ob-3-start")("1 Apr 2017"),
              elementTextByID(id = "pi-ob-3-end")("30 Jun 2017"),
              elementTextByID(id = "pi-ob-3-status")(LocalDate.now().minusDays(36).toLongDateShort +" Overdue")
            )

            Then("the fourth business obligation displayed is")
            res should have(
              elementTextByID(id = "pi-ob-4-eops")("Whole tax year (final check)"),
              elementTextByID(id = "pi-ob-4-status")(LocalDate.now().minusDays(36).toLongDateShort +" Overdue")
            )

            Then("the fifth obligation dispolayed should be")
            res should have(
              elementTextByID(id = "pi-ob-5-start")("1 Jul 2017"),
              elementTextByID(id = "pi-ob-5-end")("30 Sep 2017"),
              elementTextByID(id = "pi-ob-5-status")(LocalDate.now().plusDays(30).toLongDateShort)
            )
          }
        }

        "has business and property obligations" should {

          "display one obligation each for business and property with the correct dates and statuses" in {

            appConfig.features.reportDeadlinesEnabled(true)
            isAuthorisedUser(true)
            stubUserDetailsError()

            And("I wiremock stub a successful Income Source Details response with single Business and Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              OK, businessAndPropertyResponse
            )

            And("I wiremock stub a single business and property obligation response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, singleReportDeadlinesDataSuccessModel)
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, singleReportDeadlinesDataSuccessModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("I verify the Income Source Details has been successfully wiremocked")
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)

            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines")
            )

            Then("the page title")
            res should have(
              nElementsWithClass("obligation")(2)
            )

            Then("the single business obligation")
            res should have(
              elementTextByID(id = "bi-1-ob-1-start")("6 Apr 2017"),
              elementTextByID(id = "bi-1-ob-1-end")("5 Jul 2017"),
              elementTextByID(id = "bi-1-ob-1-status")("Received")
            )

            Then("the single property obligation")
            res should have(
              elementTextByID(id = "pi-ob-1-start")("6 Apr 2017"),
              elementTextByID(id = "pi-ob-1-end")("5 Jul 2017"),
              elementTextByID(id = "pi-ob-1-status")("Received")
            )
          }
        }

        "has 2 business with one obligation each" should {

          "display the obligation of each business" in {

            appConfig.features.reportDeadlinesEnabled(true)
            isAuthorisedUser(true)
            stubUserDetails()

            And("I wiremock stub a successful Income Source Details responsewith multiple Business income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              OK, multipleBusinessesResponse
            )

            And("I wiremock stub a single business obligation response for each business")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, singleReportDeadlinesDataSuccessModel)
            IncomeTaxViewChangeStub.stubGetReportDeadlines(otherTestSelfEmploymentId, singleReportDeadlinesDataSuccessModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("I verify the Income Source Details has been successfully wiremocked")
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            verifyReportDeadlinesCall(testSelfEmploymentId, otherTestSelfEmploymentId)

            Then("the page should display the correct title, username and links")
            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines")

            )

            Then("the page displays two obligations")
            res should have(
              nElementsWithClass("obligation")(2)
            )

            Then("the first business obligation data is")
            res should have(
              //Check the 1st obligation data
              elementTextByID(id = "bi-1-ob-1-start")("6 Apr 2017"),
              elementTextByID(id = "bi-1-ob-1-end")("5 Jul 2017"),
              elementTextByID(id = "bi-1-ob-1-status")("Received")
            )

            Then("the second business obligation data is")
            res should have(
              //Check the 1st obligation data
              elementTextByID(id = "bi-2-ob-1-start")("6 Apr 2017"),
              elementTextByID(id = "bi-2-ob-1-end")("5 Jul 2017"),
              elementTextByID(id = "bi-2-ob-1-status")("Received")
            )

            Then("the page should not contain any property obligation")
            res should have(
              isElementVisibleById("pi-ob")(false)
            )

          }

        }

        "has 2 business with multiple obligations and property with one obligation" should {

          "display the obligation of each business" in {

            appConfig.features.reportDeadlinesEnabled(true)
            isAuthorisedUser(true)
            stubUserDetails()

            And("I wiremock stub a successful Income Source Details response with multiple Business income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              OK, multipleBusinessesAndPropertyResponse
            )

            And("I wiremock stub multiple business obligations and a single property obligation response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
            IncomeTaxViewChangeStub.stubGetReportDeadlines(otherTestSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, singleReportDeadlinesDataSuccessModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("I verify the Income Source Details has been successfully wiremocked")
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            verifyReportDeadlinesCall(testSelfEmploymentId, otherTestSelfEmploymentId, testPropertyIncomeId)

            Then("the page should display the correct title, username and links")
            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines")

            )

            Then("the page displays seven obligations")
            res should have(
              nElementsWithClass("obligation")(7)
            )

            Then("the first business obligation data is")
            res should have(
              elementTextByID(id = "bi-1-ob-1-start")("6 Apr 2017"),
              elementTextByID(id = "bi-1-ob-1-end")("5 Jul 2017"),
              elementTextByID(id = "bi-1-ob-1-status")("Received"),
              elementTextByID(id = "bi-1-ob-3-start")("6 Jul 2017"),
              elementTextByID(id = "bi-1-ob-3-end")("5 Oct 2017"),
              elementTextByID(id = "bi-1-ob-3-status")(LocalDate.now().plusDays(1).toLongDateShort),
              elementTextByID(id = "bi-1-ob-2-start")("6 Oct 2017"),
              elementTextByID(id = "bi-1-ob-2-end")("5 Jan 2018"),
              elementTextByID(id = "bi-1-ob-2-status")(LocalDate.now().minusDays(1).toLongDateShort + " Overdue")
            )

            Then("the second business obligation data is")
            res should have(
              elementTextByID(id = "bi-2-ob-1-start")("6 Apr 2017"),
              elementTextByID(id = "bi-2-ob-1-end")("5 Jul 2017"),
              elementTextByID(id = "bi-2-ob-1-status")("Received"),
              elementTextByID(id = "bi-2-ob-3-start")("6 Jul 2017"),
              elementTextByID(id = "bi-2-ob-3-end")("5 Oct 2017"),
              elementTextByID(id = "bi-2-ob-3-status")(LocalDate.now().plusDays(1).toLongDateShort),
              elementTextByID(id = "bi-2-ob-2-start")("6 Oct 2017"),
              elementTextByID(id = "bi-2-ob-2-end")("5 Jan 2018"),
              elementTextByID(id = "bi-2-ob-2-status")(LocalDate.now().minusDays(1).toLongDateShort + " Overdue")
            )

            Then("the property obligation data is")
            res should have(
              elementTextByID(id = "pi-ob-1-start")("6 Apr 2017"),
              elementTextByID(id = "pi-ob-1-end")("5 Jul 2017"),
              elementTextByID(id = "pi-ob-1-status")("Received")
            )

          }

        }

        "has business income but returns an error response from business obligations" should {

          "Display an error message to the user" in {

            appConfig.features.reportDeadlinesEnabled(true)
            isAuthorisedUser(true)
            stubUserDetails()

            And("I wiremock stub a successful Income Source Details response with single Business income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

            And("I wiremock stub an error for the business obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlinesError(testSelfEmploymentId)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("I verify the Income Source Details has been successfully wiremocked")
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            verifyReportDeadlinesCall(testSelfEmploymentId)

            Then("the view is displayed with an error message under the business income section")
            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines"),
              elementTextByID(id = "bi-1-section")("business"),
              elementTextByID(id = "bi-1-p1")("We can't display your next report due date at the moment."),
              elementTextByID(id = "bi-1-p2")("Try refreshing the page in a few minutes.")
            )
          }
        }

        "has property income but returns an error response from property obligations" should {

          "Display an error message to the user" in {

            appConfig.features.reportDeadlinesEnabled(true)
            isAuthorisedUser(true)
            stubUserDetails()

            And("I wiremock stub a successful Income Source Details response with Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            And("I wiremock stub an error for the property obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlinesError(testPropertyIncomeId)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("I verify the Income Source Details has been successfully wiremocked")
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            verifyReportDeadlinesCall(testPropertyIncomeId)

            Then("the view is displayed with an error message under the property income section")
            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines"),
              elementTextByID(id = "pi-section")("Property income"),
              elementTextByID(id = "pi-p1")("We can't display your next report due date at the moment."),
              elementTextByID(id = "pi-p2")("Try refreshing the page in a few minutes.")
            )
          }
        }

        "has both property income and business income but both return error responses when retrieving obligations" should {

          "Display an error message to the user" in {

            appConfig.features.reportDeadlinesEnabled(true)
            isAuthorisedUser(true)
            stubUserDetails()

            And("I wiremock stub a successful Income Source Details response with single Business and Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              OK, businessAndPropertyResponse
            )

            And("I wiremock stub an error for the property obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlinesError(testPropertyIncomeId)

            And("I wiremock stub an error for the business obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlinesError(testSelfEmploymentId)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            Then("I verify the Income Source Details has been successfully wiremocked")
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)

            Then("an error message for property obligations is returned and the correct view is displayed")
            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines"),
              elementTextByID(id = "p1")("We can't display your next report due date at the moment."),
              elementTextByID(id = "p2")("Try refreshing the page in a few minutes.")
            )
          }
        }

      }

      "unauthorised" should {

        "redirect to sign in" in {

          appConfig.features.reportDeadlinesEnabled(true)
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
}
