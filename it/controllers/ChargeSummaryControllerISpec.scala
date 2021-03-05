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
import assets.FinancialTransactionsIntegrationTestConstants.financialTransactionsJson
import assets.IncomeSourceIntegrationTestConstants.{propertyOnlyResponse, testValidFinancialDetailsModelJson}
import config.featureswitch.NewFinancialDetailsApi
import helpers.ComponentSpecBase
import helpers.servicemocks.{FinancialTransactionsStub, IncomeTaxViewChangeStub}
import play.api.http.Status._

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
			Given("I wiremock stub a successful Income Source Details response with multiple business and property")
			IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

			And("I wiremock stub a single financial transaction response")
			IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino)(OK, testValidFinancialDetailsModelJson(2000, 2000))

			Given("the financial api feature switch is on")
			enable(NewFinancialDetailsApi)

			val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

			verifyIncomeSourceDetailsCall(testMtditid)

			Then("the result should have a HTTP status of OK (200) and load the correct page")
			res should have(
				httpStatus(OK),
				pageTitle("Remaining balance - Business Tax account - GOV.UK")
			)
		}
	}
}
