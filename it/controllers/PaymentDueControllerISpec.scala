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
import assets.BaseIntegrationTestConstants.{testMtditid, testNino, testSaUtr}
import assets.FinancialTransactionsIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import assets.OutstandingChargesIntegrationTestConstants._
import assets.messages.{PaymentsDueMessages => messages}
import audit.models.{WhatYouOweRequestAuditModel, WhatYouOweResponseAuditModel}
import auth.MtdItUser
import config.featureswitch.{NewFinancialDetailsApi, Payment, TxmEventsApproved}
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, FinancialTransactionsStub, IncomeTaxViewChangeStub}
import play.api.http.Status._
import assets.FinancialDetailsIntegrationTestConstants._
import play.api.libs.json.Json
import play.api.test.FakeRequest

class PaymentDueControllerISpec extends ComponentSpecBase {

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None,
    paymentHistoryBusinessAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
  )(FakeRequest())

  "Navigating to /report-quarterly/income-and-expenses/view/payments-owed" when {

    "Authorised" when {
      "NewFinancialDetailsApi FS is disabled" should {
        "render the payments due page with a single transaction" in {
          disable(NewFinancialDetailsApi)
          val testTaxYear = 2018
          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)


          And("I wiremock stub a single financial transaction response")
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(OK, financialTransactionsJson(2000))

          And("the payment feature switch is set to false")
          disable(Payment)

          When("I call GET /report-quarterly/income-and-expenses/view/payments-due")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyFinancialTransactionsCall(testMtditid)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitle("Payments due - Business Tax account - GOV.UK"),
            isElementVisibleById(s"payments-due-$testTaxYear")(expectedValue = true),
            isElementVisibleById(s"payments-due-outstanding-$testTaxYear")(expectedValue = true),
            isElementVisibleById(s"payments-due-on-$testTaxYear")(expectedValue = true),
            isElementVisibleById(s"bills-link-$testTaxYear")(expectedValue = true),
            isElementVisibleById(s"payment-link-$testTaxYear")(expectedValue = false)
          )

        }

        "render the payments due page with multiple transactions" in {
          disable(NewFinancialDetailsApi)
          val testTaxYear1 = 2018
          val testTaxYear2 = 2019
          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)


          And("I wiremock stub a single financial transaction response")
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2017-04-06", "2018-04-05")(OK, financialTransactionsJson(2000, "2017-04-06", "2018-04-05"))
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2018-04-06", "2019-04-05")(OK, financialTransactionsJson(3000, "2018-04-06", "2019-04-05"))

          And("the payment feature switch is set to false")
          disable(Payment)

          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyFinancialTransactionsCall(testMtditid, "2017-04-06", "2018-04-05")
          verifyFinancialTransactionsCall(testMtditid, "2018-04-06", "2019-04-05")

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitle("Payments due - Business Tax account - GOV.UK"),
            isElementVisibleById(s"payments-due-$testTaxYear1")(expectedValue = true),
            isElementVisibleById(s"payments-due-outstanding-$testTaxYear1")(expectedValue = true),
            isElementVisibleById(s"payments-due-on-$testTaxYear1")(expectedValue = true),
            isElementVisibleById(s"bills-link-$testTaxYear1")(expectedValue = true),
            isElementVisibleById(s"payment-link-$testTaxYear1")(expectedValue = false),
            isElementVisibleById(s"payments-due-$testTaxYear2")(expectedValue = true),
            isElementVisibleById(s"payments-due-outstanding-$testTaxYear2")(expectedValue = true),
            isElementVisibleById(s"payments-due-on-$testTaxYear2")(expectedValue = true),
            isElementVisibleById(s"bills-link-$testTaxYear2")(expectedValue = true),
            isElementVisibleById(s"payment-link-$testTaxYear2")(expectedValue = false)
          )


        }

        "render the payments due page where there is a mix of paid, unpaid and non charge transactions" in {

          disable(NewFinancialDetailsApi)
          disable(Payment)
          val testTaxYear = 2019
          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

          And("I wiremock stub a single financial transaction response")
          val mixedJson = financialTransactionsJson(1000, "2018-04-06", "2019-04-05") ++ Json.obj(
            "financialTransactions" -> Json.arr(
              transactionJson(None, Some(1000.00), "2018-04-06", "2019-04-05"),
              transactionJson(Some(3000.00), Some(1000.00), "2018-04-06", "2019-04-05"),
              transactionJson(Some(-3000.00), Some(-1000.00), "2018-04-06", "2019-04-05")
            )
          )

          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2018-04-06", "2019-04-05")(OK, mixedJson)

          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyFinancialTransactionsCall(testMtditid, "2017-04-06", "2018-04-05")
          verifyFinancialTransactionsCall(testMtditid, "2018-04-06", "2019-04-05")

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitle("Payments due - Business Tax account - GOV.UK"),
            elementTextByID("p1")(messages.description),
            isElementVisibleById(s"payments-due-$testTaxYear")(expectedValue = true),
            isElementVisibleById(s"payments-due-outstanding-$testTaxYear")(expectedValue = true),
            isElementVisibleById(s"payments-due-on-$testTaxYear")(expectedValue = true),
            isElementVisibleById(s"bills-link-$testTaxYear")(expectedValue = true),
            isElementVisibleById(s"payment-link-$testTaxYear")(expectedValue = false)
          )

        }

        "render the payments due page with a single transaction and a not found" in {
          disable(NewFinancialDetailsApi)
          val testTaxYear1 = 2018
          val testTaxYear2 = 2019
          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)


          And("I wiremock stub a single financial transaction response")
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2017-04-06", "2018-04-05")(NOT_FOUND, financialTransactionsSingleErrorJson("400"))
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2018-04-06", "2019-04-05")(OK, financialTransactionsJson(3000, "2018-04-06", "2019-04-05"))

          And("the payment feature switch is set to false")
          disable(Payment)

          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyFinancialTransactionsCall(testMtditid, "2017-04-06", "2018-04-05")
          verifyFinancialTransactionsCall(testMtditid, "2018-04-06", "2019-04-05")

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitle("Payments due - Business Tax account - GOV.UK"),
            isElementVisibleById(s"payments-due-$testTaxYear2")(expectedValue = true),
            isElementVisibleById(s"payments-due-outstanding-$testTaxYear2")(expectedValue = true),
            isElementVisibleById(s"payments-due-on-$testTaxYear2")(expectedValue = true),
            isElementVisibleById(s"bills-link-$testTaxYear2")(expectedValue = true),
            isElementVisibleById(s"payment-link-$testTaxYear2")(expectedValue = false)
          )

        }

        "render the payments due page with no transactions" in {
          disable(NewFinancialDetailsApi)
          val testTaxYear1 = 2018
          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

          And("I wiremock stub a single financial transaction response")
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2017-04-06", "2018-04-05")(OK, emptyFinancialTransaction)
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2018-04-06", "2019-04-05")(OK, emptyFinancialTransaction)

          And("the payment feature switch is set to false")
          disable(Payment)

          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyFinancialTransactionsCall(testMtditid, "2017-04-06", "2018-04-05")

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitle("Payments due - Business Tax account - GOV.UK"),
            isElementVisibleById(s"payments-due-$testTaxYear1")(expectedValue = false),
            isElementVisibleById(s"payments-due-outstanding-$testTaxYear1")(expectedValue = false),
            isElementVisibleById(s"payments-due-on-$testTaxYear1")(expectedValue = false),
            isElementVisibleById(s"bills-link-$testTaxYear1")(expectedValue = false),
            isElementVisibleById(s"payment-link-$testTaxYear1")(expectedValue = false)
          )

        }

        "redirect to an internal server error page when transactions contain internal server error" in {
          val testTaxYear1 = 2018
          val testTaxYear2 = 2019
          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)


          And("I wiremock stub a single financial transaction response")
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2017-04-06", "2018-04-05")(INTERNAL_SERVER_ERROR,
            financialTransactionsSingleErrorJson("500"))
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid, "2018-04-06", "2019-04-05")(OK,
            financialTransactionsJson(3000, "2018-04-06", "2019-04-05"))

          And("the payment feature switch is set to false")
          disable(Payment)

          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
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

      "NewFinancialDetailsApi FS is  and with TxmEventsApproved FS enabled" when {
        "YearOfMigration exists" when {
          "render the payments due page with a multiple charge from financial details and BCD and ACI charges from CESA" in {
            enable(NewFinancialDetailsApi)
            enable(TxmEventsApproved)
            val testTaxYear = LocalDate.now().getYear.toString

            Given("I wiremock stub a successful Income Source Details response with multiple business and property")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear.toInt - 1, Some(testTaxYear)))


            And("I wiremock stub a multiple financial details and outstanding charges response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")(OK,
              testValidFinancialDetailsModelJson(2000, 2000, testTaxYear, LocalDate.now().toString))
            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear.toInt - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

            And("the payment feature switch is set to enabled")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataWithDataDueIn30Days).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear.toInt - 1).toString)

            Then("the result should have a HTTP status of OK (200) and the payments due page")

            res should have(
              httpStatus(OK),
              pageTitle("What you owe - Business Tax account - GOV.UK"),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
              isElementVisibleById("payment-details-content-0")(expectedValue = true),
              isElementVisibleById("payment-details-content-1")(expectedValue = true),
              isElementVisibleById("over-due-payments-heading")(expectedValue = false),
              isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = true),
              isElementVisibleById("due-in-thirty-days-type-0")(expectedValue = true),
              isElementVisibleById("due-in-thirty-days-type-1")(expectedValue = true),
              isElementVisibleById("future-payments-heading")(expectedValue = false),
              isElementVisibleById("payment-days-note")(expectedValue = true),
              isElementVisibleById("credit-on-account")(expectedValue = true),
              isElementVisibleById("payment-button")(expectedValue = true),
              isElementVisibleById("sa-note-migrated")(expectedValue = true),
              isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = false)
            )

          }

          "render the payments due page with a multiple charge, without BCD and ACI charges from CESA and payment disabled" in {
            enable(NewFinancialDetailsApi)
            enable(TxmEventsApproved)
            val testTaxYear = LocalDate.now().getYear.toString

            Given("I wiremock stub a successful Income Source Details response with multiple business and property")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear.toInt - 1, Some(testTaxYear)))


            And("I wiremock stub a multiple financial details response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")(OK,
              testValidFinancialDetailsModelJson(2000, 2000, testTaxYear, LocalDate.now().minusDays(15).toString))
            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear.toInt - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            And("the payment feature switch is set to disbaled")
            disable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataFullDataWithoutOutstandingCharges).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear.toInt - 1).toString)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitle("What you owe - Business Tax account - GOV.UK"),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
              isElementVisibleById("payment-details-content-0")(expectedValue = true),
              isElementVisibleById("payment-details-content-1")(expectedValue = true),
              isElementVisibleById("over-due-payments-heading")(expectedValue = true),
              isElementVisibleById("over-due-type-0")(expectedValue = true),
              isElementVisibleById("over-due-type-1")(expectedValue = true),
              isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
              isElementVisibleById("future-payments-heading")(expectedValue = false),
              isElementVisibleById(s"payment-days-note")(expectedValue = false),
              isElementVisibleById(s"credit-on-account")(expectedValue = false),
              isElementVisibleById(s"payment-button")(expectedValue = false),
              isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
              isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false)
            )

          }
          "render the payments due page with multiple charges and one charge equals zero" in {
            val testTaxYear = LocalDate.now().getYear
            enable(NewFinancialDetailsApi)
            enable(TxmEventsApproved)

            Given("I wiremock stub a successful Income Source Details response with multiple business and property")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a single financial details response")
            val mixedJson = Json.obj(
              "documentDetails" -> Json.arr(
                documentDetailJson(3400.00, 1000.00, testTaxYear.toString),
                documentDetailJson(1000.00, 100.00, testTaxYear.toString, "ITSA- POA 1"),
                documentDetailJson(1000.00, 0.00, testTaxYear.toString, "ITSA - POA 2")
              ),
              "financialDetails" -> Json.arr(
                financialDetailJson(testTaxYear.toString),
                financialDetailJson(testTaxYear.toString, "SA Payment on Account 1", LocalDate.now().plusDays(1).toString),
                financialDetailJson(testTaxYear.toString, "SA Payment on Account 2", LocalDate.now().minusDays(1).toString)
              )
            )

            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)
            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

            And("the payment feature switch is set to enabled")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweWithAZeroOutstandingAmount).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitle("What you owe - Business Tax account - GOV.UK"),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
              isElementVisibleById("payment-details-content-0")(expectedValue = true),
              isElementVisibleById("payment-details-content-1")(expectedValue = true),
              isElementVisibleById("over-due-payments-heading")(expectedValue = false),
              isElementVisibleById("over-due-type-0")(expectedValue = false),
              isElementVisibleById("over-due-type-1")(expectedValue = false),
              isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = true),
              isElementVisibleById("due-in-thirty-days-type-0")(expectedValue = true),
              isElementVisibleById("future-payments-heading")(expectedValue = false),
              isElementVisibleById(s"payment-days-note")(expectedValue = true),
              isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
              isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false)
            )
          }

          "redirect to an internal server error page when both connectors return internal server error" in {
            val testTaxYear = LocalDate.now().getYear
            enable(NewFinancialDetailsApi)
            enable(TxmEventsApproved)

            Given("I wiremock stub a successful Income Source Details response with multiple business and property")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a single financial details response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino,
              s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())
            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(INTERNAL_SERVER_ERROR, testOutstandingChargesErrorModelJson)


            And("the payment feature switch is set to false")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")

            Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR)
            )

          }
          "redirect to an internal server error page when financial connector return internal server error" in {
            val testTaxYear = LocalDate.now().getYear
            enable(NewFinancialDetailsApi)
            enable(TxmEventsApproved)

            Given("I wiremock stub a successful Income Source Details response with multiple business and property")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a single financial details response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino,
              s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())
            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)


            And("the payment feature switch is set to false")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")

            Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR)
            )

          }

          "redirect to an internal server error page when Outstanding charges connector return internal server error" in {
            val testTaxYear = LocalDate.now().getYear
            enable(NewFinancialDetailsApi)
            enable(TxmEventsApproved)

            Given("I wiremock stub a successful Income Source Details response with multiple business and property")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a single financial details response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")(OK,
              testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, LocalDate.now().toString))
            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(INTERNAL_SERVER_ERROR, testOutstandingChargesErrorModelJson)


            And("the payment feature switch is set to false")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")

            Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR)
            )

          }
        }

        "NewFinancialDetailsApi FS is enabled and with TxmEventsApproved FS disabled" when {
          "YearOfMigration exists" when {
            "render the payments due page with a multiple charge from financial details and BCD and ACI charges from CESA" in {
              enable(NewFinancialDetailsApi)
              disable(TxmEventsApproved)
              val testTaxYear = LocalDate.now().getYear.toString

              Given("I wiremock stub a successful Income Source Details response with multiple business and property")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear.toInt - 1, Some(testTaxYear)))


              And("I wiremock stub a multiple financial details and outstanding charges response")
              IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")(OK,
                testValidFinancialDetailsModelJson(2000, 2000, testTaxYear, LocalDate.now().toString))
              IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                "utr", testSaUtr.toLong, (testTaxYear.toInt - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

              And("the payment feature switch is set to enabled")
              enable(Payment)

              When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
              val res = IncomeTaxViewChangeFrontend.getPaymentsDue

              AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataWithDataDueIn30Days).detail)

              verifyIncomeSourceDetailsCall(testMtditid)
              IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")
              IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear.toInt - 1).toString)

              Then("the result should have a HTTP status of OK (200) and the payments due page")

              res should have(
                httpStatus(OK),
                pageTitle("What you owe - Business Tax account - GOV.UK"),
                isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
                isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
                isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
                isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
                isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
                isElementVisibleById("payment-details-content-0")(expectedValue = true),
                isElementVisibleById("payment-details-content-1")(expectedValue = true),
                isElementVisibleById("over-due-payments-heading")(expectedValue = false),
                isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = true),
                isElementVisibleById("due-in-thirty-days-type-0")(expectedValue = true),
                isElementVisibleById("due-in-thirty-days-type-1")(expectedValue = true),
                isElementVisibleById("future-payments-heading")(expectedValue = false),
                isElementVisibleById("payment-days-note")(expectedValue = true),
                isElementVisibleById("credit-on-account")(expectedValue = true),
                isElementVisibleById("payment-button")(expectedValue = true),
                isElementVisibleById("sa-note-migrated")(expectedValue = true),
                isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = false)
              )

            }

            "render the payments due page with a multiple charge, without BCD and ACI charges from CESA and payment disabled" in {
              enable(NewFinancialDetailsApi)
              disable(TxmEventsApproved)
              val testTaxYear = LocalDate.now().getYear.toString

              Given("I wiremock stub a successful Income Source Details response with multiple business and property")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear.toInt - 1, Some(testTaxYear)))


              And("I wiremock stub a multiple financial details response")
              IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")(OK,
                testValidFinancialDetailsModelJson(2000, 2000, testTaxYear, LocalDate.now().minusDays(15).toString))
              IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                "utr", testSaUtr.toLong, (testTaxYear.toInt - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

              And("the payment feature switch is set to disbaled")
              disable(Payment)

              When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
              val res = IncomeTaxViewChangeFrontend.getPaymentsDue

              AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataFullDataWithoutOutstandingCharges).detail)

              verifyIncomeSourceDetailsCall(testMtditid)
              IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")
              IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear.toInt - 1).toString)

              Then("the result should have a HTTP status of OK (200) and the payments due page")
              res should have(
                httpStatus(OK),
                pageTitle("What you owe - Business Tax account - GOV.UK"),
                isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
                isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
                isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
                isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
                isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
                isElementVisibleById("payment-details-content-0")(expectedValue = true),
                isElementVisibleById("payment-details-content-1")(expectedValue = true),
                isElementVisibleById("over-due-payments-heading")(expectedValue = true),
                isElementVisibleById("over-due-type-0")(expectedValue = true),
                isElementVisibleById("over-due-type-1")(expectedValue = true),
                isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
                isElementVisibleById("future-payments-heading")(expectedValue = false),
                isElementVisibleById(s"payment-days-note")(expectedValue = false),
                isElementVisibleById(s"credit-on-account")(expectedValue = false),
                isElementVisibleById(s"payment-button")(expectedValue = false),
                isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
                isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false)
              )

            }
            "render the payments due page with multiple charges and one charge equals zero" in {
              val testTaxYear = LocalDate.now().getYear
              enable(NewFinancialDetailsApi)
              disable(TxmEventsApproved)

              Given("I wiremock stub a successful Income Source Details response with multiple business and property")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


              And("I wiremock stub a single financial details response")
              val mixedJson = Json.obj(
                "documentDetails" -> Json.arr(
                  documentDetailJson(3400.00, 1000.00, testTaxYear.toString),
                  documentDetailJson(1000.00, 100.00, testTaxYear.toString, "ITSA- POA 1"),
                  documentDetailJson(1000.00, 0.00, testTaxYear.toString, "ITSA - POA 2")
                ),
                "financialDetails" -> Json.arr(
                  financialDetailJson(testTaxYear.toString),
                  financialDetailJson(testTaxYear.toString, "SA Payment on Account 1", LocalDate.now().plusDays(1).toString),
                  financialDetailJson(testTaxYear.toString, "SA Payment on Account 2", LocalDate.now().minusDays(1).toString)
                )
              )

              IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)
              IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

              And("the payment feature switch is set to enabled")
              enable(Payment)

              When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
              val res = IncomeTaxViewChangeFrontend.getPaymentsDue

              AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweWithAZeroOutstandingAmount).detail)

              verifyIncomeSourceDetailsCall(testMtditid)
              IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
              IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

              Then("the result should have a HTTP status of OK (200) and the payments due page")
              res should have(
                httpStatus(OK),
                pageTitle("What you owe - Business Tax account - GOV.UK"),
                isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
                isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
                isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
                isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
                isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
                isElementVisibleById("payment-details-content-0")(expectedValue = true),
                isElementVisibleById("payment-details-content-1")(expectedValue = true),
                isElementVisibleById("over-due-payments-heading")(expectedValue = false),
                isElementVisibleById("over-due-type-0")(expectedValue = false),
                isElementVisibleById("over-due-type-1")(expectedValue = false),
                isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = true),
                isElementVisibleById("due-in-thirty-days-type-0")(expectedValue = true),
                isElementVisibleById("future-payments-heading")(expectedValue = false),
                isElementVisibleById(s"payment-days-note")(expectedValue = true),
                isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
                isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false)
              )
            }
          }
        }
        "YearOfMigration does not exists" when {
          "render the payments due page with a no charge" in {
            val testTaxYear = LocalDate.now().getYear

            enable(NewFinancialDetailsApi)
            enable(TxmEventsApproved)

            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, None))

            And("I wiremock stub a single financial details response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear - 1}-04-06",
              s"$testTaxYear-04-05")(OK, testEmptyFinancialDetailsModelJson)

            And("the payment feature switch is set to enabled")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

            verifyIncomeSourceDetailsCall(testMtditid)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitle("What you owe - Business Tax account - GOV.UK"),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
              isElementVisibleById("payment-details-content-0")(expectedValue = false),
              isElementVisibleById("payment-details-content-1")(expectedValue = false),
              isElementVisibleById("over-due-payments-heading")(expectedValue = false),
              isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
              isElementVisibleById("future-payments-heading")(expectedValue = false),
              isElementVisibleById("payment-days-note")(expectedValue = true),
              isElementVisibleById("credit-on-account")(expectedValue = true),
              isElementVisibleById(s"payment-button")(expectedValue = false),
              isElementVisibleById("no-payments-due")(expectedValue = true),
              isElementVisibleById("sa-note-migrated")(expectedValue = true),
              isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true)
            )
          }
        }

        "YearOfMigration exists but not the first year" when {
          "render the payments due page with a no charge" in {
            val testTaxYear = LocalDate.now().getYear - 3

            enable(NewFinancialDetailsApi)
            enable(TxmEventsApproved)

            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a single financial details response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino,
              s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, testEmptyFinancialDetailsModelJson)
            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


            And("the payment feature switch is set to enabled")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

            verifyIncomeSourceDetailsCall(testMtditid)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitle("What you owe - Business Tax account - GOV.UK"),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
              isElementVisibleById("payment-details-content-0")(expectedValue = false),
              isElementVisibleById("payment-details-content-1")(expectedValue = false),
              isElementVisibleById("over-due-payments-heading")(expectedValue = false),
              isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
              isElementVisibleById("future-payments-heading")(expectedValue = false),
              isElementVisibleById("payment-days-note")(expectedValue = true),
              isElementVisibleById("credit-on-account")(expectedValue = true),
              isElementVisibleById(s"payment-button")(expectedValue = false),
              isElementVisibleById("no-payments-due")(expectedValue = true),
              isElementVisibleById("sa-note-migrated")(expectedValue = true),
              isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true)
            )

          }
        }

        "YearOfMigration exists and No valid charges exists with with TxmEventsApproved FS enabled" when {
          "render the payments due page with a no charge" in {
            val testTaxYear = LocalDate.now().getYear
            enable(NewFinancialDetailsApi)
            enable(TxmEventsApproved)

            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a mixed financial details response")
            val mixedJson = Json.obj(
              "documentDetails" -> Json.arr(
                documentDetailJson(3400.00, 1000.00, testTaxYear.toString, "test"),
                documentDetailJson(1000.00, 0, testTaxYear.toString, "4444"),
                documentDetailJson(1000.00, 3000.00, testTaxYear.toString, "5555")
              ),
              "financialDetails" -> Json.arr(
                financialDetailJson(testTaxYear.toString, "test"),
                financialDetailJson(testTaxYear.toString, "4444"),
                financialDetailJson(testTaxYear.toString, "5555")
              )
            )

            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)

            And("the payment feature switch is set to enabled")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear - 1}-04-06", s"${testTaxYear}-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitle("What you owe - Business Tax account - GOV.UK"),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
              isElementVisibleById("payment-details-content-0")(expectedValue = false),
              isElementVisibleById("payment-details-content-1")(expectedValue = false),
              isElementVisibleById("over-due-payments-heading")(expectedValue = false),
              isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
              isElementVisibleById("future-payments-heading")(expectedValue = false),
              isElementVisibleById("payment-days-note")(expectedValue = true),
              isElementVisibleById("credit-on-account")(expectedValue = true),
              isElementVisibleById(s"payment-button")(expectedValue = false),
              isElementVisibleById("no-payments-due")(expectedValue = true),
              isElementVisibleById("sa-note-migrated")(expectedValue = true),
              isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true)
            )

          }
        }

        "YearOfMigration exists and No valid charges exists with TxmEventsApproved FS disabled" when {
          "render the payments due page with a no charge" in {
            val testTaxYear = LocalDate.now().getYear
            enable(NewFinancialDetailsApi)
            disable(TxmEventsApproved)

            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a mixed financial details response")
            val mixedJson = Json.obj(
              "documentDetails" -> Json.arr(
                documentDetailJson(3400.00, 1000.00, testTaxYear.toString, "test"),
                documentDetailJson(1000.00, 0, testTaxYear.toString, "4444"),
                documentDetailJson(1000.00, 3000.00, testTaxYear.toString, "5555")
              ),
              "financialDetails" -> Json.arr(
                financialDetailJson(testTaxYear.toString, "test"),
                financialDetailJson(testTaxYear.toString, "4444"),
                financialDetailJson(testTaxYear.toString, "5555")
              )
            )

            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)

            And("the payment feature switch is set to enabled")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear - 1}-04-06", s"${testTaxYear}-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitle("What you owe - Business Tax account - GOV.UK"),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
              isElementVisibleById("payment-details-content-0")(expectedValue = false),
              isElementVisibleById("payment-details-content-1")(expectedValue = false),
              isElementVisibleById("over-due-payments-heading")(expectedValue = false),
              isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
              isElementVisibleById("future-payments-heading")(expectedValue = false),
              isElementVisibleById("payment-days-note")(expectedValue = true),
              isElementVisibleById("credit-on-account")(expectedValue = true),
              isElementVisibleById(s"payment-button")(expectedValue = false),
              isElementVisibleById("no-payments-due")(expectedValue = true),
              isElementVisibleById("sa-note-migrated")(expectedValue = true),
              isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true)
            )

          }
        }

        "YearOfMigration exists with Invalid financial details charges and valid outstanding charges" when {
          "render the payments due page with ACI and BCD charge" in {
            val testTaxYear = LocalDate.now().getYear
            enable(NewFinancialDetailsApi)
            enable(TxmEventsApproved)

            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a mixed financial details response")
            val mixedJson = Json.obj(
              "documentDetails" -> Json.arr(
                documentDetailJson(3400.00, 1000.00, testTaxYear.toString, "test"),
                documentDetailJson(1000.00, 0, testTaxYear.toString, "3333"),
                documentDetailJson(1000.00, 3000.00, testTaxYear.toString, "4444")
              ),
              "financialDetails" -> Json.arr(
                financialDetailJson(testTaxYear.toString, "test"),
                financialDetailJson(testTaxYear.toString, "3333"),
                financialDetailJson(testTaxYear.toString, "4444")
              ))

            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)

            And("the payment feature switch is set to enabled")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweOutstandingChargesOnly).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitle("What you owe - Business Tax account - GOV.UK"),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
              isElementVisibleById("payment-details-content-0")(expectedValue = true),
              isElementVisibleById("payment-details-content-1")(expectedValue = true),
              isElementVisibleById("over-due-payments-heading")(expectedValue = false),
              isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
              isElementVisibleById("future-payments-heading")(expectedValue = false),
              isElementVisibleById(s"payment-days-note")(expectedValue = true),
              isElementVisibleById(s"credit-on-account")(expectedValue = true),
              isElementVisibleById(s"payment-button")(expectedValue = true),
              isElementVisibleById(s"no-payments-due")(expectedValue = false),
              isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
              isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false)
            )

          }
        }
        "YearOfMigration exists with Invalid financial details charges and valid outstanding charges and with TxmEventsApproved FS disabled" when {
          "render the payments due page with ACI and BCD charge" in {
            val testTaxYear = LocalDate.now().getYear
            enable(NewFinancialDetailsApi)
            disable(TxmEventsApproved)

            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a mixed financial details response")
            val mixedJson = Json.obj(
              "documentDetails" -> Json.arr(
                documentDetailJson(3400.00, 1000.00, testTaxYear.toString, "test"),
                documentDetailJson(1000.00, 0, testTaxYear.toString, "3333"),
                documentDetailJson(1000.00, 3000.00, testTaxYear.toString, "4444")
              ),
              "financialDetails" -> Json.arr(
                financialDetailJson(testTaxYear.toString, "test"),
                financialDetailJson(testTaxYear.toString, "3333"),
                financialDetailJson(testTaxYear.toString, "4444")
              ))

            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)

            And("the payment feature switch is set to enabled")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweOutstandingChargesOnly).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitle("What you owe - Business Tax account - GOV.UK"),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
              isElementVisibleById("payment-details-content-0")(expectedValue = true),
              isElementVisibleById("payment-details-content-1")(expectedValue = true),
              isElementVisibleById("over-due-payments-heading")(expectedValue = false),
              isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
              isElementVisibleById("future-payments-heading")(expectedValue = false),
              isElementVisibleById(s"payment-days-note")(expectedValue = true),
              isElementVisibleById(s"credit-on-account")(expectedValue = true),
              isElementVisibleById(s"payment-button")(expectedValue = true),
              isElementVisibleById(s"no-payments-due")(expectedValue = false),
              isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
              isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false)
            )

          }
        }
        "YearOfMigration exists with valid financial details charges and invalid outstanding charges" when {
          "render the payments due page with empty BCD charge" in {
            val testTaxYear = LocalDate.now().getYear
            enable(NewFinancialDetailsApi)
            enable(TxmEventsApproved)

            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a mixed financial details response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")(OK,
              testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, LocalDate.now().plusYears( 1).toString))

            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            And("the payment feature switch is set to enabled")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweFinancialDetailsEmptyBCDCharge).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitle("What you owe - Business Tax account - GOV.UK"),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
              isElementVisibleById("payment-details-content-0")(expectedValue = true),
              isElementVisibleById("payment-details-content-1")(expectedValue = true),
              isElementVisibleById("over-due-payments-heading")(expectedValue = false),
              isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
              isElementVisibleById("future-payments-heading")(expectedValue = true),
              isElementVisibleById("future-payments-type-0")(expectedValue = true),
              isElementVisibleById("future-payments-type-1")(expectedValue = true),
              isElementVisibleById(s"payment-days-note")(expectedValue = true),
              isElementVisibleById(s"credit-on-account")(expectedValue = true),
              isElementVisibleById(s"payment-button")(expectedValue = true),
              isElementVisibleById(s"no-payments-due")(expectedValue = false),
              isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
              isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false)
            )

          }
        }
        "YearOfMigration exists with valid financial details charges and invalid outstanding charges and with TxmEventsApproved FS enabled" when {
          "render the payments due page with empty BCD charge" in {
            val testTaxYear = LocalDate.now().getYear
            enable(NewFinancialDetailsApi)
            disable(TxmEventsApproved)

            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a mixed financial details response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear.toInt - 1}-04-06", s"${testTaxYear.toInt}-04-05")(OK,
              testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, LocalDate.now().plusYears( 1).toString))

            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            And("the payment feature switch is set to enabled")
            enable(Payment)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

            AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweFinancialDetailsEmptyBCDCharge).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitle("What you owe - Business Tax account - GOV.UK"),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
              isElementVisibleById("payment-details-content-0")(expectedValue = true),
              isElementVisibleById("payment-details-content-1")(expectedValue = true),
              isElementVisibleById("over-due-payments-heading")(expectedValue = false),
              isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
              isElementVisibleById("future-payments-heading")(expectedValue = true),
              isElementVisibleById("future-payments-type-0")(expectedValue = true),
              isElementVisibleById("future-payments-type-1")(expectedValue = true),
              isElementVisibleById(s"payment-days-note")(expectedValue = true),
              isElementVisibleById(s"credit-on-account")(expectedValue = true),
              isElementVisibleById(s"payment-button")(expectedValue = true),
              isElementVisibleById(s"no-payments-due")(expectedValue = false),
              isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
              isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false)
            )
          }
        }
      }
    }
  }

}
