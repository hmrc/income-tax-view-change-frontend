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
import assets.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, propertyOnlyResponse, testValidFinancialDetailsModelJson}
import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import config.featureswitch.NewFinancialDetailsApi
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.financialDetails.{Charge, SubItem}
import play.api.http.Status._
import play.api.test.FakeRequest

class ChargeSummaryControllerISpec extends ComponentSpecBase {

  "Navigating to /report-quarterly/income-and-expenses/view/payments-due" should {

    "redirect to Home" in {
      Given("I wiremock stub a successful Income Source Details response with multiple business and property")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino)(OK, testValidFinancialDetailsModelJson(2000, 2000))

      Given("the financial api feature switch is off")
      disable(NewFinancialDetailsApi)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      Then("the result should have a HTTP status of SEE_OTHER (303) and a redirect to the home page")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.routes.HomeController.home().url)
      )
    }

    "load the page" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino)(OK, testValidFinancialDetailsModelJson(2000.00, 2000.00))

      Given("the financial api feature switch is on")
      enable(NewFinancialDetailsApi)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      AuditStub.verifyAuditContains(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
        )(FakeRequest()),
        Charge("2018", "1040000123", Some("2019-05-15"), Some("Balancing Charge Debit"), Some(3400), Some(2000),
          Some(2000), None, Some("Balancing Charge debit"), Some("SA Balancing Charge"), Some(List(SubItem(Some("001"),
            Some(100), Some("2019-05-15"), Some("01"), Some("A"), None, Some(2000),
            Some("2018-02-14"), Some("A"), Some("081203010024-000001"))))),
        None
      ).detail)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitle("Remaining balance - Business Tax account - GOV.UK")
      )
    }
  }
}
