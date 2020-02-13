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

import assets.BaseIntegrationTestConstants.{testMtditid, testNino, testSelfEmploymentId}
import assets.FinancialTransactionsIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import config.FrontendAppConfig
import helpers.ComponentSpecBase
import helpers.servicemocks.{FinancialTransactionsStub, IncomeTaxViewChangeStub}
import implicits.ImplicitDateFormatter
import play.api.http.Status._

class PaymentDueControllerISpec extends ComponentSpecBase with ImplicitDateFormatter {

  "Navigating to /report-quarterly/income-and-expenses/view/payments-due" when {

    lazy val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

    "Authorised" should {
      "render the payments due page with a single transaction" in {
        val testTaxYear = 2018
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)


        And("I wiremock stub a single financial transaction response")
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(OK, financialTransactionsJson(2000))

        And("the payment feature switch is set to false")
        appConfig.features.paymentEnabled(false)

        When("I call GET /report-quarterly/income-and-expenses/view/payments-due")
        val res = IncomeTaxViewChangeFrontend.getPaymentsDue

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyFinancialTransactionsCall(testMtditid)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        res should have(
          httpStatus(OK),
          pageTitle("Payments Due"),
          isElementVisibleById(s"payments-due-$testTaxYear")(true),
          isElementVisibleById(s"payments-due-outstanding-$testTaxYear")(true),
          isElementVisibleById(s"payments-due-on-$testTaxYear")(true),
          isElementVisibleById(s"bills-link-$testTaxYear")(true),
          isElementVisibleById(s"payment-link-$testTaxYear")(false)
        )


      }

      "render the payments due page with multiple transactions" in {
        val testTaxYear1 = 2018
        val testTaxYear2 = 2019
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)


        And("I wiremock stub a single financial transaction response")
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2017-04-06", "2018-04-05")(OK, financialTransactionsJson(2000,"2017-04-06", "2018-04-05"))
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2018-04-06", "2019-04-05")(OK, financialTransactionsJson(3000,"2018-04-06", "2019-04-05"))

        And("the payment feature switch is set to false")
        appConfig.features.paymentEnabled(false)

        When("I call GET /report-quarterly/income-and-expenses/view/payments-due")
        val res = IncomeTaxViewChangeFrontend.getPaymentsDue

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyFinancialTransactionsCall(testMtditid, "2017-04-06", "2018-04-05")
        verifyFinancialTransactionsCall(testMtditid, "2018-04-06", "2019-04-05")

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        res should have(
          httpStatus(OK),
          pageTitle("Payments Due"),
          isElementVisibleById(s"payments-due-$testTaxYear1")(true),
          isElementVisibleById(s"payments-due-outstanding-$testTaxYear1")(true),
          isElementVisibleById(s"payments-due-on-$testTaxYear1")(true),
          isElementVisibleById(s"bills-link-$testTaxYear1")(true),
          isElementVisibleById(s"payment-link-$testTaxYear1")(false),
          isElementVisibleById(s"payments-due-$testTaxYear2")(true),
          isElementVisibleById(s"payments-due-outstanding-$testTaxYear2")(true),
          isElementVisibleById(s"payments-due-on-$testTaxYear2")(true),
          isElementVisibleById(s"bills-link-$testTaxYear2")(true),
          isElementVisibleById(s"payment-link-$testTaxYear2")(false)
        )


      }

      "render the payments due page with a single transaction and a bad Request and" in {
        val testTaxYear1 = 2018
        val testTaxYear2 = 2019
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)


        And("I wiremock stub a single financial transaction response")
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2017-04-06", "2018-04-05")(BAD_REQUEST, financialTransactionsSingleErrorJson("400"))
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2018-04-06", "2019-04-05")(OK, financialTransactionsJson(3000,"2018-04-06", "2019-04-05"))

        And("the payment feature switch is set to false")
        appConfig.features.paymentEnabled(false)

        When("I call GET /report-quarterly/income-and-expenses/view/payments-due")
        val res = IncomeTaxViewChangeFrontend.getPaymentsDue

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyFinancialTransactionsCall(testMtditid, "2017-04-06", "2018-04-05")
        verifyFinancialTransactionsCall(testMtditid, "2018-04-06", "2019-04-05")

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        res should have(
          httpStatus(OK),
          pageTitle("Payments Due"),
          isElementVisibleById(s"payments-due-$testTaxYear2")(true),
          isElementVisibleById(s"payments-due-outstanding-$testTaxYear2")(true),
          isElementVisibleById(s"payments-due-on-$testTaxYear2")(true),
          isElementVisibleById(s"bills-link-$testTaxYear2")(true),
          isElementVisibleById(s"payment-link-$testTaxYear2")(false)
        )

      }

      "render the payments due page with no transactions" in {
        val testTaxYear1 = 2018
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

        And("I wiremock stub a single financial transaction response")
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2017-04-06", "2018-04-05")(OK, emptyFinancialTransaction)
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2018-04-06", "2019-04-05")(OK, emptyFinancialTransaction)

        And("the payment feature switch is set to false")
        appConfig.features.paymentEnabled(false)

        When("I call GET /report-quarterly/income-and-expenses/view/payments-due")
        val res = IncomeTaxViewChangeFrontend.getPaymentsDue

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyFinancialTransactionsCall(testMtditid, "2017-04-06", "2018-04-05")

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        res should have(
          httpStatus(OK),
          pageTitle("Payments Due"),
          isElementVisibleById(s"payments-due-$testTaxYear1")(false),
          isElementVisibleById(s"payments-due-outstanding-$testTaxYear1")(false),
          isElementVisibleById(s"payments-due-on-$testTaxYear1")(false),
          isElementVisibleById(s"bills-link-$testTaxYear1")(false),
          isElementVisibleById(s"payment-link-$testTaxYear1")(false)
        )

      }

      "redirect to an internal server error page when transactions contain internal server error" in {
        val testTaxYear1 = 2018
        val testTaxYear2 = 2019
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)


        And("I wiremock stub a single financial transaction response")
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2017-04-06", "2018-04-05")(OK, financialTransactionsSingleErrorJson("400"))
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2018-04-06", "2019-04-05")(OK, financialTransactionsJson(3000,"2018-04-06", "2019-04-05"))

        And("the payment feature switch is set to false")
        appConfig.features.paymentEnabled(false)

        When("I call GET /report-quarterly/income-and-expenses/view/payments-due")
        val res = IncomeTaxViewChangeFrontend.getPaymentsDue

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyFinancialTransactionsCall(testMtditid, "2017-04-06", "2018-04-05")
        verifyFinancialTransactionsCall(testMtditid, "2018-04-06", "2019-04-05")

        Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )

      }
    }
  }

}
