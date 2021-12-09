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

import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, testChargeHistoryJson, testValidFinancialDetailsModelJson, twoDunningLocks, twoInterestLocks}
import testConstants.NextUpdatesIntegrationTestConstants._
import testConstants.messages.HomeMessages._
import audit.models.{HomeAudit, NextUpdatesResponseAuditModel}
import auth.MtdItUser
import config.featureswitch.TxmEventsApproved
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.{verifyAuditContainsDetail, verifyAuditDoesNotContainsDetail}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.nextUpdates.ObligationsModel
import play.api.http.Status._
import play.api.test.FakeRequest

class HomeControllerISpec extends ComponentSpecBase {

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None,
    multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
  )(FakeRequest())

  "Navigating to /report-quarterly/income-and-expenses/view" when {
    "Authorised" should {
      "render the home page with the payment due date with TxmEventsApproved FS enabled" in {
        enable(TxmEventsApproved)
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
          pageTitle(title),
          elementTextBySelector("#updates-tile > div > p:nth-child(2)")("4 OVERDUE UPDATES"),
          elementTextBySelector("#payments-tile > div > p:nth-child(2)")("6 OVERDUE PAYMENTS")
        )

        verifyAuditContainsDetail(HomeAudit(testUser, Some(Right(6)), Right(4)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, testSelfEmploymentId, singleObligationQuarterlyReturnModel(testSelfEmploymentId).obligations).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, otherTestSelfEmploymentId, singleObligationQuarterlyReturnModel(otherTestSelfEmploymentId).obligations).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, testPropertyId, singleObligationOverdueModel(testPropertyId).obligations).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, testMtditid, singleObligationCrystallisationModel.obligations).detail)
      }

      "render the home page with the payment due date with TxmEventsApproved FS disabled" in {
        disable(TxmEventsApproved)
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
          pageTitle(title),
          elementTextBySelector("#updates-tile > div > p:nth-child(2)")("4 OVERDUE UPDATES"),
          elementTextBySelector("#payments-tile > div > p:nth-child(2)")("6 OVERDUE PAYMENTS")
        )

        verifyAuditDoesNotContainsDetail(HomeAudit(testUser, Some(Right(6)), Right(4)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, testSelfEmploymentId, singleObligationQuarterlyReturnModel(testSelfEmploymentId).obligations).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, otherTestSelfEmploymentId, singleObligationQuarterlyReturnModel(otherTestSelfEmploymentId).obligations).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, testPropertyId, singleObligationOverdueModel(testPropertyId).obligations).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, testMtditid, singleObligationCrystallisationModel.obligations).detail)
      }

      "render the ISE page when receive an error from the backend" in {
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
    unauthorisedTest("")
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getHome)
    }
    "clearing the cache after the first call should allow the 2nd call to run through" in {
      testIncomeSourceDetailsCaching(true, 2,
        () => IncomeTaxViewChangeFrontend.getHome)
    }
  }
}
