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

import audit.models.{HomeAudit, NextUpdatesResponseAuditModel}
import auth.MtdItUser
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSources, IncomeSourcesNewJourney, NavBarFs}
import models.obligations.ObligationsModel
import play.api.http.Status._
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, testValidFinancialDetailsModelJson}
import testConstants.NextUpdatesIntegrationTestConstants._
import testConstants.messages.HomeMessages._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class HomeControllerISpec extends ComponentSpecBase {

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse, None,
    Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  "Navigating to /report-quarterly/income-and-expenses/view" when {
    "Authorised" should {
      "render the home page with the payment due date" in {
        disable(NavBarFs)
        enable(IncomeSources)
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        val currentObligations: ObligationsModel = ObligationsModel(Seq(
          singleObligationQuarterlyReturnModel(testSelfEmploymentId),
          singleObligationQuarterlyReturnModel(otherTestSelfEmploymentId),
          singleObligationOverdueModel(testPropertyId),
          singleObligationCrystallisationModel
        ))

        And("I wiremock stub obligation responses")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, currentObligations)

        And("I stub a successful financial details response")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, "2017-04-06", "2018-04-05")(OK,
          testValidFinancialDetailsModelJson(3400.00, 2000.00))
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, "2018-04-06", "2019-04-05")(OK,
          testValidFinancialDetailsModelJson(3400.00, 1000.00, "2019"))

        When("I call GET /report-quarterly/income-and-expenses/view")
        val res = IncomeTaxViewChangeFrontend.getHome

        verifyIncomeSourceDetailsCall(testMtditid)

        verifyNextUpdatesCall(testNino)

        Then("the result should have a HTTP status of OK (200) and the Income Tax home page")

        res should have(
          httpStatus(OK),
          pageTitleIndividual("home.heading"),
          elementTextBySelector("#updates-tile p:nth-child(2)")(overdueUpdates("4")),
          elementTextBySelector("#payments-tile p:nth-child(2)")(overduePayments("6")),
          elementTextBySelector("#income-sources-tile h2:nth-child(1)")("Income Sources")
        )

        verifyAuditContainsDetail(HomeAudit(testUser, Some(Right(6)), Right(4)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, testSelfEmploymentId, singleObligationQuarterlyReturnModel(testSelfEmploymentId).obligations).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, otherTestSelfEmploymentId, singleObligationQuarterlyReturnModel(otherTestSelfEmploymentId).obligations).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, testPropertyId, singleObligationOverdueModel(testPropertyId).obligations).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, testMtditid, singleObligationCrystallisationModel.obligations).detail)
      }
      "render the home page with Your Business tile when new IS journey FS enabled" in {
        disable(NavBarFs)
        enable(IncomeSources)
        enable(IncomeSourcesNewJourney)
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        val currentObligations: ObligationsModel = ObligationsModel(Seq(
          singleObligationQuarterlyReturnModel(testSelfEmploymentId),
          singleObligationQuarterlyReturnModel(otherTestSelfEmploymentId),
          singleObligationOverdueModel(testPropertyId),
          singleObligationCrystallisationModel
        ))

        And("I wiremock stub obligation responses")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, currentObligations)

        And("I stub a successful financial details response")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, "2017-04-06", "2018-04-05")(OK,
          testValidFinancialDetailsModelJson(3400.00, 2000.00))
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, "2018-04-06", "2019-04-05")(OK,
          testValidFinancialDetailsModelJson(3400.00, 1000.00, "2019"))

        When("I call GET /report-quarterly/income-and-expenses/view")
        val res = IncomeTaxViewChangeFrontend.getHome

        verifyIncomeSourceDetailsCall(testMtditid)

        verifyNextUpdatesCall(testNino)

        Then("the result should have a HTTP status of OK (200) and the Income Tax home page")

        res should have(
          httpStatus(OK),
          pageTitleIndividual("home.heading"),
          elementTextBySelector("#updates-tile p:nth-child(2)")(overdueUpdates("4")),
          elementTextBySelector("#payments-tile p:nth-child(2)")(overduePayments("6")),
          elementTextBySelector("#income-sources-tile h2:nth-child(1)")("Your businesses")
        )
      }

      "render the ISE page when receive an error from the backend" in {
        disable(NavBarFs)
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        And("I wiremock stub a single business obligation response")
        IncomeTaxViewChangeStub.stubGetNextUpdatesError(testNino)

        When("I call GET /report-quarterly/income-and-expenses/view")
        val res = IncomeTaxViewChangeFrontend.getHome

        verifyIncomeSourceDetailsCall(testMtditid)

        verifyNextUpdatesCall(testNino)

        Then("the result should have a HTTP status of ISE (500) and the Income Tax home page")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

//    "low confidence level user" should {
//      "redirect to ivuplift service" in {
//        enable(IvUplift)
//        AuthStub.stubAuthorised(Some(50))
//
//        When(s"I call GET /report-quarterly/income-and-expenses/view")
//        val res = IncomeTaxViewChangeFrontend.get("/")
//        val expectedRedirectUrl = "http://localhost:9948/iv-stub/uplift?origin=ITVC&confidenceLevel=250" +
//          "&completionURL=http://localhost:9081/report-quarterly/income-and-expenses/view/uplift-success?origin=PTA&failureURL=" +
//          "http://localhost:9081/report-quarterly/income-and-expenses/view/cannot-view-page"
//        Then("the http response for an unauthorised user is returned")
//
//        //println(expectedRedirectUrl)
//        res should have(
//          httpStatus(SEE_OTHER),
//          redirectURI(expectedRedirectUrl)
//        )
//      }
//    }
    unauthorisedTest("")
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getHome)
    }
  }
}
