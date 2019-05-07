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
import assets.BusinessDetailsIntegrationTestConstants.b1TradingName
import assets.IncomeSourceIntegrationTestConstants._
import assets.ReportDeadlinesIntegrationTestConstants._
import assets.messages.{ReportDeadlinesMessages => messages}
import config.FrontendAppConfig
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.ComponentSpecBase
import implicits.ImplicitDateFormatter
import play.api.http.Status._
import implicits.ImplicitDateFormatter
import org.jsoup.Jsoup

class ReportDeadlinesControllerISpec extends ComponentSpecBase with ImplicitDateFormatter {

  lazy val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  "Calling the ReportDeadlinesController" when {

    "the ReportDeadlines Feature is enabled" when {

      "isAuthorisedUser with an active enrolment" which {

        "has a single business obligation" should {

          "display a single obligation with the correct dates and status" in {

            And("I wiremock stub a successful Income Source Details response with single Business")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

            And("I wiremock stub a single business obligation response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, singleObligationOverdueModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testSelfEmploymentId)

            Then("the view displays the correct title, username and links")
            res should have(
              httpStatus(OK),
              pageTitle(messages.title)
            )

            Then("the page displays one obligation")
            res should have(
              nElementsWithClass("obligation")(1)
            )

            Then("the single business obligation data is")
            res should have(
              elementTextByID(id = "bi-1-ob-1-start")(singleObligationStart.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-end")(singleObligationEnd.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-status")(messages.overdue(LocalDate.now().minusDays(1)))
            )

            Then("the page should not contain any property obligations")
            res should have(
              isElementVisibleById("pi-section")(false)
            )
          }
        }

        "has business with multiple obligations and no property obligations" should {

          "display all obligations with the correct dates and status" in {

            And("I wiremock stub a successful Income Source Details response with single Business")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

            And("I wiremock stub a business obligation response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, multipleReportDeadlinesDataSuccessModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testSelfEmploymentId)

            Then("the correct title, username and links are displayed")
            res should have(
              httpStatus(OK),
              pageTitle(messages.title)

            )

            Then("the page displays six obligations")
            res should have(
              nElementsWithClass("obligation")(6)
            )

            Then("the business obligation data is")
            res should have(
              elementTextByID(id = "bi-1-ob-1-start")(deadlineStart1.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-end")(deadlineEnd1.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-status")(messages.overdue(LocalDate.now().minusDays(128))),
              elementTextByID(id = "bi-1-ob-2-start")(deadlineStart2.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-2-end")(deadlineEnd2.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-2-status")(messages.overdue(LocalDate.now().minusDays(36))),
              elementTextByID(id = "bi-1-ob-3-eops")(messages.wholeTaxYear),
              elementTextByID(id = "bi-1-ob-3-status")(messages.overdue(LocalDate.now().minusDays(36))),
              elementTextByID(id = "bi-1-ob-4-start")(deadlineStart4.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-4-end")(deadlineEnd4.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-4-status")(LocalDate.now().plusDays(30).toLongDateShort),
              elementTextByID(id = "bi-1-ob-5-start")(deadlineStart5.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-5-end")(deadlineEnd5.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-5-status")(LocalDate.now().plusDays(146).toLongDateShort),
              elementTextByID(id = "bi-1-ob-6-start")(deadlineStart6.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-6-end")(deadlineEnd6.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-6-status")(LocalDate.now().plusDays(174).toLongDateShort)
            )

            Then("the page should not contain any property obligation")
            res should have(
              isElementVisibleById("pi-section")(false)
            )

          }
        }

        "has a single property obligation" should {

          "display a single obligation with the correct dates and status" in {

            And("I wiremock stub a successful Income Source Details response with single Business")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            And("I wiremock stub a single business obligation response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, testNino, singleObligationPlusYearOpenModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testPropertyIncomeId)

            Then("the view displays the correct title, username and links")
            res should have(
              httpStatus(OK),
              pageTitle("Report deadlines")
            )

            Then("the page displays one obligation")
            res should have(
              nElementsWithClass("obligation")(1)
            )

            Then("the single property obligation data is")
            res should have(
              elementTextByID(id = "pi-ob-1-start")(singleObligationStart.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-1-end")(singleObligationEnd.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-1-status")(LocalDate.now().plusYears(1).toLongDateShort)
            )

            Then("the page should not contain any business obligations")
            res should have(
              isElementVisibleById("bi-1-section")(false)
            )
          }
        }

        "has property with multiple obligations and no business obligations" should {

          "display a multiple obligations with the correct dates and status" in {

            And("I wiremock stub a successful Income Source Details response with single Business and Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            And("I wiremock stub a single property and business obligation response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, testNino, multipleReportDeadlinesDataSuccessModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testPropertyIncomeId)

            Then("the correct title, username and links are displayed")
            res should have(
              httpStatus(OK),
              pageTitle(messages.title)
            )

            Then("the page displays six obligations")
            res should have(
              nElementsWithClass("obligation")(6)
            )

            Then("the property obligation data is")
            res should have(
              elementTextByID(id = "pi-ob-1-start")(deadlineStart1.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-1-end")(deadlineEnd1.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-1-status")(messages.overdue(LocalDate.now().minusDays(128))),
              elementTextByID(id = "pi-ob-2-start")(deadlineStart2.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-2-end")(deadlineEnd2.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-2-status")(messages.overdue(LocalDate.now().minusDays(36))),
              elementTextByID(id = "pi-ob-3-eops")(messages.wholeTaxYear),
              elementTextByID(id = "pi-ob-3-status")(messages.overdue(LocalDate.now().minusDays(36))),
              elementTextByID(id = "pi-ob-4-start")(deadlineStart4.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-4-end")(deadlineEnd4.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-4-status")(LocalDate.now().plusDays(30).toLongDateShort),
              elementTextByID(id = "pi-ob-5-start")(deadlineStart5.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-5-end")(deadlineEnd5.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-5-status")(LocalDate.now().plusDays(146).toLongDateShort),
              elementTextByID(id = "pi-ob-6-start")(deadlineStart6.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-6-end")(deadlineEnd6.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-6-status")(LocalDate.now().plusDays(174).toLongDateShort)
            )

            Then("the page should not contain any business obligations")
            res should have(
              isElementVisibleById("bi-1-section")(false)
            )

          }
        }

        "has a business and a property obligation" should {

          "display one obligation each for business and property with the correct dates and statuses" in {

            And("I wiremock stub a successful Income Source Details response with single Business and Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

            And("I wiremock stub a single business and property obligation response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, singleObligationPlusYearOpenModel )
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, testNino, singleObligationOverdueModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testSelfEmploymentId, testPropertyIncomeId)

            res should have(
              httpStatus(OK),
              pageTitle(messages.title)
            )

            Then("the page should display two obligations")
            res should have(
              nElementsWithClass("obligation")(2)
            )

            Then("the single business obligation data is")
            res should have(
              elementTextByID(id = "bi-1-ob-1-start")(singleObligationStart.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-end")(singleObligationEnd.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-status")(LocalDate.now().plusYears(1).toLongDateShort)
            )

            Then("the property obligation data is")
            res should have(
              elementTextByID(id = "pi-ob-1-start")(singleObligationStart.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-1-end")(singleObligationEnd.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-1-status")(messages.overdue(LocalDate.now().minusDays(1)))
            )

          }
        }

        "has business and property with multiple obligations for both" should {

          "display all obligations with the correct dates and status" in {

            And("I wiremock stub a successful Income Source Details response with single Business and Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

            And("I wiremock stub a single property and business obligation response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, multipleReportDeadlinesDataSuccessModel)
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, testNino, multipleReportDeadlinesDataSuccessModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testSelfEmploymentId, testPropertyIncomeId)

            Then("the correct title, username and links are displayed")
            res should have(
              httpStatus(OK),
              pageTitle(messages.title)

            )

            Then("the page displays twelve obligations")
            res should have(
              nElementsWithClass("obligation")(12)
            )

            Then("the business obligation data is")
            res should have(
              elementTextByID(id = "bi-1-ob-1-start")(deadlineStart1.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-end")(deadlineEnd1.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-status")(messages.overdue(LocalDate.now().minusDays(128))),
              elementTextByID(id = "bi-1-ob-2-start")(deadlineStart2.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-2-end")(deadlineEnd2.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-2-status")(messages.overdue(LocalDate.now().minusDays(36))),
              elementTextByID(id = "bi-1-ob-3-eops")(messages.wholeTaxYear),
              elementTextByID(id = "bi-1-ob-3-status")(messages.overdue(LocalDate.now().minusDays(36))),
              elementTextByID(id = "bi-1-ob-4-start")(deadlineStart4.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-4-end")(deadlineEnd4.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-4-status")(LocalDate.now().plusDays(30).toLongDateShort),
              elementTextByID(id = "bi-1-ob-5-start")(deadlineStart5.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-5-end")(deadlineEnd5.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-5-status")(LocalDate.now().plusDays(146).toLongDateShort),
              elementTextByID(id = "bi-1-ob-6-start")(deadlineStart6.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-6-end")(deadlineEnd6.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-6-status")(LocalDate.now().plusDays(174).toLongDateShort)
            )

            Then("the property obligation data is")
            res should have(
              elementTextByID(id = "pi-ob-1-start")(deadlineStart1.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-1-end")(deadlineEnd1.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-1-status")(messages.overdue(LocalDate.now().minusDays(128))),
              elementTextByID(id = "pi-ob-2-start")(deadlineStart2.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-2-end")(deadlineEnd2.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-2-status")(messages.overdue(LocalDate.now().minusDays(36))),
              elementTextByID(id = "pi-ob-3-eops")(messages.wholeTaxYear),
              elementTextByID(id = "pi-ob-3-status")(messages.overdue(LocalDate.now().minusDays(36))),
              elementTextByID(id = "pi-ob-4-start")(deadlineStart4.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-4-end")(deadlineEnd4.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-4-status")(LocalDate.now().plusDays(30).toLongDateShort),
              elementTextByID(id = "pi-ob-5-start")(deadlineStart5.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-5-end")(deadlineEnd5.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-5-status")(LocalDate.now().plusDays(146).toLongDateShort),
              elementTextByID(id = "pi-ob-6-start")(deadlineStart6.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-6-end")(deadlineEnd6.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-6-status")(LocalDate.now().plusDays(174).toLongDateShort)
            )

          }
        }

        "has 2 business with one obligation each" should {

          "display the obligation of each business" in {

            appConfig.features.reportDeadlinesEnabled(true)

            And("I wiremock stub a successful Income Source Details responsewith multiple Business income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              OK, multipleBusinessesResponse
            )

            And("I wiremock stub a single business obligation response for each business")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, singleObligationOverdueModel)
            IncomeTaxViewChangeStub.stubGetReportDeadlines(otherTestSelfEmploymentId, testNino, singleObligationPlusYearOpenModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testSelfEmploymentId, otherTestSelfEmploymentId)

            Then("the page should display the correct title, username and links")
            res should have(
              httpStatus(OK),
              pageTitle(messages.title)

            )

            Then("the page displays two obligations")
            res should have(
              nElementsWithClass("obligation")(2)
            )

            Then("the first business obligation data is")
            res should have(
              elementTextByID(id = "bi-1-ob-1-start")(singleObligationStart.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-end")(singleObligationEnd.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-status")(messages.overdue(LocalDate.now().minusDays(1)))
            )

            Then("the second business obligation data is")
            res should have(
              elementTextByID(id = "bi-2-ob-1-start")(singleObligationStart.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-2-ob-1-end")(singleObligationEnd.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-2-ob-1-status")(LocalDate.now().plusYears(1).toLongDateShort)
            )

            Then("the page should not contain any property obligation")
            res should have(
              isElementVisibleById("pi-section")(false)
            )

          }

        }

        "has 2 business and property with one obligation" should {

          "display the obligation of each business and property" in {

            And("I wiremock stub a successful Income Source Details response with multiple Business income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

            And("I wiremock stub multiple business obligations and a single property obligation response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, singleObligationOverdueModel)
            IncomeTaxViewChangeStub.stubGetReportDeadlines(otherTestSelfEmploymentId, testNino, singleObligationPlusYearOpenModel)
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, testNino, singleObligationOverdueModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testSelfEmploymentId, otherTestSelfEmploymentId, testPropertyIncomeId)

            Then("the page should display the correct title, username and links")
            res should have(
              httpStatus(OK),
              pageTitle(messages.title)

            )

            Then("the page displays three obligations")
            res should have(
              nElementsWithClass("obligation")(3)
            )

            Then("the first business obligation data is")
            res should have(
              elementTextByID(id = "bi-1-ob-1-start")(singleObligationStart.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-end")(singleObligationEnd.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-status")(messages.overdue(LocalDate.now().minusDays(1)))
            )

            Then("the second business obligation data is")
            res should have(
              elementTextByID(id = "bi-2-ob-1-start")(singleObligationStart.toLocalDate.toLongDateShort),
            elementTextByID(id = "bi-2-ob-1-end")(singleObligationEnd.toLocalDate.toLongDateShort),
            elementTextByID(id = "bi-2-ob-1-status")(LocalDate.now().plusYears(1).toLongDateShort)
            )

            Then("the property obligation data is")
            res should have(
              elementTextByID(id = "pi-ob-1-start")(singleObligationStart.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-1-end")(singleObligationEnd.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-1-status")(messages.overdue(LocalDate.now().minusDays(1)))
            )

          }

        }


        "has 2 business and property with multiple obligations" should {

          "display the obligation of each business and property" in {

            And("I wiremock stub a successful Income Source Details response with multiple Business income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

            And("I wiremock stub multiple business obligations and a single property obligation response")
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, singleObligationOverdueModel)
            IncomeTaxViewChangeStub.stubGetReportDeadlines(otherTestSelfEmploymentId, testNino, multipleReportDeadlinesDataSuccessModel)
            IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, testNino, singleObligationPlusYearOpenModel)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testSelfEmploymentId, otherTestSelfEmploymentId, testPropertyIncomeId)

            Then("the page should display the correct title, username and links")
            res should have(
              httpStatus(OK),
              pageTitle(messages.title)

            )

            Then("the page displays eight obligations")
            res should have(
              nElementsWithClass("obligation")(8)
            )

            Then("the first business obligation data is")
            res should have(
              elementTextByID(id = "bi-1-ob-1-start")(singleObligationStart.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-end")(singleObligationEnd.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-1-ob-1-status")(messages.overdue(LocalDate.now().minusDays(1)))
            )

            Then("the second business obligation data is")
            res should have(
              elementTextByID(id = "bi-2-ob-1-start")(deadlineStart1.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-2-ob-1-end")(deadlineEnd1.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-2-ob-1-status")(messages.overdue(LocalDate.now().minusDays(128))),
              elementTextByID(id = "bi-2-ob-2-start")(deadlineStart2.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-2-ob-2-end")(deadlineEnd2.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-2-ob-2-status")(messages.overdue(LocalDate.now().minusDays(36))),
              elementTextByID(id = "bi-2-ob-3-eops")(messages.wholeTaxYear),
              elementTextByID(id = "bi-2-ob-3-status")(messages.overdue(LocalDate.now().minusDays(36))),
              elementTextByID(id = "bi-2-ob-4-start")(deadlineStart4.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-2-ob-4-end")(deadlineEnd4.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-2-ob-4-status")(LocalDate.now().plusDays(30).toLongDateShort),
              elementTextByID(id = "bi-2-ob-5-start")(deadlineStart5.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-2-ob-5-end")(deadlineEnd5.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-2-ob-5-status")(LocalDate.now().plusDays(146).toLongDateShort),
              elementTextByID(id = "bi-2-ob-6-start")(deadlineStart6.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-2-ob-6-end")(deadlineEnd6.toLocalDate.toLongDateShort),
              elementTextByID(id = "bi-2-ob-6-status")(LocalDate.now().plusDays(174).toLongDateShort)
            )

            Then("the property obligation data is")
            res should have(
              elementTextByID(id = "pi-ob-1-start")(singleObligationStart.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-1-end")(singleObligationEnd.toLocalDate.toLongDateShort),
              elementTextByID(id = "pi-ob-1-status")(LocalDate.now().plusYears(1).toLongDateShort)
            )

          }

        }

        "has business income but returns an error response from business obligations" should {

          "Display an error message to the user with a no content error" in {

            And("I wiremock stub a successful Income Source Details response with single Business income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

            And("I wiremock stub an error for the business obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlinesNotFound(testSelfEmploymentId, testNino)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testSelfEmploymentId)

            Then("the view is displayed with an error message under the business income section")
            res should have(
              httpStatus(OK),
              pageTitle(messages.title),
              elementTextByID(id = "bi-1-section")(b1TradingName),
              elementTextByID(id = "bi-1-p1")(messages.errorp1),
              elementTextByID(id = "bi-1-p2")(messages.errorp2)
            )
          }

          "Display an error message to the user with a server error" in {

            And("I wiremock stub a successful Income Source Details response with single Business income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

            And("I wiremock stub an error for the business obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlinesError(testSelfEmploymentId, testNino)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testSelfEmploymentId)

            Then("the view is displayed with an error message under the business income section")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR),
              pageTitle("There is a problem with the service - Income Tax reporting through software - GOV.UK")
            )
          }
        }

        "has property income but returns an error response from property obligations" should {

          "Display an error message to the user on the page on a no content error" in {

            And("I wiremock stub a successful Income Source Details response with Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            And("I wiremock stub an error for the property obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlinesNotFound(testPropertyIncomeId, testNino)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testPropertyIncomeId)

            Then("the view is displayed with an error message under the property income section")
            res should have(
              httpStatus(OK),
              pageTitle(messages.title),
              elementTextByID(id = "pi-section")(messages.propertyHeading),
              elementTextByID(id = "pi-p1")(messages.errorp1),
              elementTextByID(id = "pi-p2")(messages.errorp2)
            )
          }

          "Display an error page for a server error" in {

            And("I wiremock stub a successful Income Source Details response with Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            And("I wiremock stub an error for the property obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlinesError(testPropertyIncomeId, testNino)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testPropertyIncomeId)

            Then("the view is displayed with an error message under the property income section")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR),
              pageTitle("There is a problem with the service - Income Tax reporting through software - GOV.UK")
            )
          }
        }

        "has both property income and business income but both return error responses when retrieving obligations" should {

          "Display an error message to the user when not found" in {

            And("I wiremock stub a successful Income Source Details response with single Business and Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

            And("I wiremock stub an error for the property obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlinesNotFound(testPropertyIncomeId, testNino)

            And("I wiremock stub an error for the business obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlinesNotFound(testSelfEmploymentId, testNino)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testSelfEmploymentId, testPropertyIncomeId)

            Then("an error message for property obligations is returned and the correct view is displayed")
            res should have(
              httpStatus(OK),
              pageTitle(messages.title),
              elementTextByID(id = "p1")(messages.errorp1),
              elementTextByID(id = "p2")(messages.errorp2)
            )
          }

          "Display an error message to the user when a server error occurs" in {

            And("I wiremock stub a successful Income Source Details response with single Business and Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

            And("I wiremock stub an error for the property obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlinesError(testPropertyIncomeId, testNino)

            And("I wiremock stub an error for the business obligations response")
            IncomeTaxViewChangeStub.stubGetReportDeadlinesError(testSelfEmploymentId, testNino)

            When("I call GET /report-quarterly/income-and-expenses/view/obligations")
            val res = IncomeTaxViewChangeFrontend.getReportDeadlines

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyReportDeadlinesCall(testNino, testSelfEmploymentId, testPropertyIncomeId)

            Then("an error view is displayed")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR),
              pageTitle("There is a problem with the service - Income Tax reporting through software - GOV.UK")
            )
          }
        }

      }

      unauthorisedTest("/obligations")

      "the obligations feature switch is enabled" when {

        "the user has a eops property income obligation only" in {
          appConfig.features.obligationsPageEnabled(true)

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, testNino, singleObligationEOPSPropertyModel)

          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testNino, testSelfEmploymentId)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.obligationsTitle)
          )

          Then("the page displays one eops property income obligation")
          res should have(
            elementTextByID("eops-pi-dates")("6 April 2017 to 5 July 2018"),
            elementTextByID("eops-pi-due-date")("1 January 2018"),
            isElementVisibleById("pi-quarterly-return-period")(expectedValue = false),
            isElementVisibleById("pi-quarterly-return-due-date")(expectedValue = false),
            isElementVisibleById("quarterly-bi-business-period")(expectedValue = false),
            isElementVisibleById("quarterly-bi-business-due")(expectedValue = false)
          )
        }

        "the user has no obligations" in {
          appConfig.features.obligationsPageEnabled(true)

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, testNino, noObligationsModel)

          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testNino, testSelfEmploymentId)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.title)
          )

          Then("the page displays no property obligation dates")
          res should have(
            isElementVisibleById("eops-pi-dates")(expectedValue = false),
            isElementVisibleById("eops-pi-due-date")(expectedValue = false),
            isElementVisibleById("pi-quarterly-return-period")(expectedValue = false),
            isElementVisibleById("pi-quarterly-return-due-date")(expectedValue = false),
            isElementVisibleById("quarterly-bi-business-period")(expectedValue = false),
            isElementVisibleById("quarterly-bi-business-due")(expectedValue = false)
          )
        }

        "the user has a quarterly property income obligation only" in {
          appConfig.features.obligationsPageEnabled(true)

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, testNino, singleObligationQuarterlyModel)

          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testNino, testPropertyIncomeId)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.obligationsTitle)
          )

          Then("the page displays the property obligation dates")
          res should have(
            isElementVisibleById("pi-quarterly-return-period")(expectedValue = true),
            isElementVisibleById("pi-quarterly-return-due-date")(expectedValue = true),
            isElementVisibleById("eops-pi-dates")(expectedValue = false),
            isElementVisibleById("eops-pi-due-date")(expectedValue = false),
            isElementVisibleById("quarterly-bi-business-period")(expectedValue = false),
            isElementVisibleById("quarterly-bi-business-due")(expectedValue = false)
          )
        }

        "the user has a quarterly business income obligation only" in {
          appConfig.features.obligationsPageEnabled(true)

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, singleObligationQuarterlyModel)

          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testNino, testPropertyIncomeId)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.obligationsTitle)
          )

          Then("the page displays the property obligation dates")
          res should have(
            isElementVisibleById("pi-quarterly-return-period")(expectedValue = false),
            isElementVisibleById("pi-quarterly-return-due-date")(expectedValue = false),
            isElementVisibleById("eops-pi-dates")(expectedValue = false),
            isElementVisibleById("eops-pi-due-date")(expectedValue = false),
            isElementVisibleById("quarterly-bi-business-period")(expectedValue = true),
            isElementVisibleById("quarterly-bi-business-due")(expectedValue = true)
          )
        }

        "the user has multiple quarterly business income obligations only" in {
          appConfig.features.obligationsPageEnabled(true)

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, singleObligationQuarterlyModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(otherTestSelfEmploymentId, testNino, singleObligationQuarterlyModel)

          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testNino, testPropertyIncomeId)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.obligationsTitle)
          )

          Then("the page displays the property obligation dates")
          res should have(
            isElementVisibleById("pi-quarterly-return-period")(expectedValue = false),
            isElementVisibleById("pi-quarterly-return-due-date")(expectedValue = false),
            isElementVisibleById("eops-pi-dates")(expectedValue = false),
            isElementVisibleById("eops-pi-due-date")(expectedValue = false),
            isElementVisibleById("quarterly-bi-business-period")(expectedValue = true),
            isElementVisibleById("quarterly-bi-business-due")(expectedValue = true),
            isElementVisibleById("quarterly-bi-secondBusiness-period")(expectedValue = true),
            isElementVisibleById("quarterly-bi-secondBusiness-due")(expectedValue = true)
          )
        }

        "the user has a eops SE income obligation only" in {
          appConfig.features.obligationsPageEnabled(true)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, SEIncomeSourceEOPSModel)

          val res= IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testNino, testSelfEmploymentId)

          Then("the view displays the correct title")
          res should have(
            httpStatus(OK),
            pageTitle(messages.obligationsTitle)
          )

          Then("the page displays SE income source obligation dates")
          res should have(
            elementTextByID("eops-SEI-dates")("6 April 2017 to 5 April 2018"),
            elementTextByID("eops-SEI-due-date")("31 January 2018")
          )

          Then("the page displays no property obligation dates")
          res should have(
            isElementVisibleById("eops-pi-dates")(expectedValue = false),
            isElementVisibleById("eops-pi-due-date")(expectedValue = false)
          )

        }

        "the user has a Crystallised obligation only" in {
          appConfig.features.obligationsPageEnabled(true)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testMtditid, testNino, crystallisedEOPSModel)

          val res= IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testNino, testMtditid)

          Then("the view displays the correct title")
          res should have(
            httpStatus(OK),
            pageTitle(messages.obligationsTitle)
          )

          Then("the page displays crystallised obligation information")
          res should have(
            isElementVisibleById("crystallised-heading")(expectedValue = true),
            isElementVisibleById("crystallised-period")(expectedValue = true),
            isElementVisibleById("crystallised-due-on")(expectedValue = true),
            isElementVisibleById("crystallised-due")(expectedValue = true)
          )

          Then("the page displays no property obligation dates")
          res should have(
            isElementVisibleById("eops-pi-dates")(expectedValue = false),
            isElementVisibleById("eops-pi-due-date")(expectedValue = false)
          )

          Then("the page displays no income obligation dates")
          res should have(
            isElementVisibleById("eops-SEI-dates")(expectedValue = false),
            isElementVisibleById("eops-SEI-due-date")(expectedValue = false)
          )

        }

      }
    }

    "the ReportDeadlines Feature is disabled" should {

      "Redirect to the Income Tax View Change Home Page" in {

        appConfig.features.reportDeadlinesEnabled(false)

        And("I wiremock stub a successful Income Source Details response with 1 Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub a single business obligation response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, testNino, singleObligationOverdueModel)

        When("I call GET /report-quarterly/income-and-expenses/view/obligations")
        val res = IncomeTaxViewChangeFrontend.getReportDeadlines

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyReportDeadlinesCall(testNino, testSelfEmploymentId)

        Then("the result should have a HTTP status of SEE_OTHER (303) and redirect to the Income Tax home page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.home().url)
        )
      }

    }

  }
}
