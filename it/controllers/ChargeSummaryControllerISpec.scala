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

import assets.BaseIntegrationTestConstants.{testMtditid, testNino}
import assets.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, testChargeHistoryJson, testValidFinancialDetailsModelJson}
import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import config.featureswitch.{ChargeHistory, TxmEventsApproved}
import helpers.ComponentSpecBase
import helpers.servicemocks.DocumentDetailsStub.docDateDetail
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import play.api.http.Status._
import play.api.test.FakeRequest

class ChargeSummaryControllerISpec extends ComponentSpecBase {

  "Navigating to /report-quarterly/income-and-expenses/view/payments-due" should {

    "load the page with right audit events when TxmEventsApproved FS enabled" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2))

			And("I wiremock stub a charge history response")
			IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000123")(OK, testChargeHistoryJson(testMtditid, "1040000123", 2500))

      Given("the TxmEventsApproved feature switch is on")
      enable(TxmEventsApproved)

			Given("the charge history feature switch is off")
			disable(ChargeHistory)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      AuditStub.verifyAuditContainsDetail(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, Some("1234567890"),
          Some("12345-credId"), Some("Individual"), None
        )(FakeRequest()),
        docDateDetail("2018-02-14", "TRM New Charge"),
        None
      ).detail)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitle("Remaining balance - Business Tax account - GOV.UK")
      )
    }
    "load the page with right audit events when TxmEventsApproved FS disabled" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2))

      Given("the TxmEventsApproved feature switch is off")
      disable(TxmEventsApproved)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      AuditStub.verifyAuditDoesNotContainsDetail(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, Some("1234567890"),
          Some("12345-credId"), Some("Individual"), None
        )(FakeRequest()),
        docDateDetail("2018-02-14", "TRM New Charge"),
        None
      ).detail)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitle("Remaining balance - Business Tax account - GOV.UK")
      )
    }

		"load the page when the late payment interest flag is true" in {
			Given("I wiremock stub a successful Income Source Details response with property only")
			IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

			And("I wiremock stub a single financial transaction response")
			IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2))

			Given("the TxmEventsApproved feature switch is off")
			disable(TxmEventsApproved)

			val res = IncomeTaxViewChangeFrontend.getChargeSummaryLatePayment("2018", "1040000123")

			verifyIncomeSourceDetailsCall(testMtditid)

			Then("the result should have a HTTP status of OK (200) and load the correct page")
			res should have(
				httpStatus(OK),
				pageTitle("Remaining balance - Business Tax account - GOV.UK")
			)
		}
  }
}
