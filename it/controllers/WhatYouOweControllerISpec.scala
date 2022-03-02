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

import audit.models.WhatYouOweResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{CodingOut, WhatYouOweTotals}
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.financialDetails.{BalanceDetails, FinancialDetailsModel, WhatYouOweChargesList}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSaUtr}
import testConstants.FinancialDetailsIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.OutstandingChargesIntegrationTestConstants._
import java.time.LocalDate

import testConstants.messages.WhatYouOweMessages.whatYouOwePageTitle

class WhatYouOweControllerISpec extends ComponentSpecBase {

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, paymentHistoryBusinessAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
  )(FakeRequest())

  val testTaxYear: Int = getCurrentTaxYearEnd.getYear - 1

  "Navigating to /report-quarterly/income-and-expenses/view/payments-owed" when {

    "Authorised" when {
      "WhatYouOweTotals FS is enabled" should {

        "WhatYouOweTotals FS is enabled" should {
          "render the payments due totals" in {

            Given("Display Totals feature is enabled")
            enable(WhatYouOweTotals)

            And("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a financial details response with coded out documents")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
              testValidFinancialDetailsModelJsonCodingOut(2000, 2000, testTaxYear.toString, LocalDate.now().plusYears(1).toString))

            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweFinancialDetailsEmptyBCDCharge).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitleIndividual(whatYouOwePageTitle),
            )
          }
        }

        "WhatYouOweTotals FS enabled" when {
          enable(WhatYouOweTotals)
          "YearOfMigration exists" when {
            "render the payments due page with a multiple charge from financial details and BCD and ACI charges from CESA" in {

              Given("I wiremock stub a successful Income Source Details response with multiple business and property")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


              And("I wiremock stub a multiple financial details and outstanding charges response")
              IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, LocalDate.now().toString))
              IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)


              When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
              val res = IncomeTaxViewChangeFrontend.getPaymentsDue

              AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataWithDataDueIn30Days).detail)

              verifyIncomeSourceDetailsCall(testMtditid)
              IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
              IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

              Then("the result should have a HTTP status of OK (200) and the payments due page")

              res should have(
                httpStatus(OK),
                pageTitleIndividual(whatYouOwePageTitle),
                isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
                isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
                isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
                isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
                isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
                isElementVisibleById("payment-details-content-0")(expectedValue = true),
                isElementVisibleById("payment-details-content-1")(expectedValue = true),
                isElementVisibleById("payments-due")(expectedValue = true),
                isElementVisibleById("due-in-thirty-days-type-0")(expectedValue = true),
                isElementVisibleById("due-in-thirty-days-type-1")(expectedValue = true),
                isElementVisibleById("payment-button")(expectedValue = true),
                isElementVisibleById("sa-note-migrated")(expectedValue = false),
                isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = false),
                isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
                isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
              )

            }

            "render the payments due page with a multiple charge, without BCD and ACI charges from CESA" in {

              Given("I wiremock stub a successful Income Source Details response with multiple business and property")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


              And("I wiremock stub a multiple financial details response")
              val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, LocalDate.now().minusDays(15).toString)
              val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

              IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                financialDetailsResponseJson)
              IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


              When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
              val res = IncomeTaxViewChangeFrontend.getPaymentsDue

              val whatYouOweChargesList = {
                val documentDetailsForTestTaxYear = financialDetailsModel.documentDetails.filter(_.taxYear == testTaxYear.toString)
                WhatYouOweChargesList(
                  balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
                  overduePaymentList = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear).getAllDocumentDetailsWithDueDates
                )
              }
              AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser, whatYouOweChargesList))

              verifyIncomeSourceDetailsCall(testMtditid)
              IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
              IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

              Then("the result should have a HTTP status of OK (200) and the payments due page")
              res should have(
                httpStatus(OK),
                pageTitleIndividual(whatYouOwePageTitle),
                isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
                isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
                isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
                isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
                isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
                isElementVisibleById("payment-details-content-0")(expectedValue = true),
                isElementVisibleById("payment-details-content-1")(expectedValue = true),
                isElementVisibleById("payments-due")(expectedValue = true),
                isElementVisibleById("over-due-type-0")(expectedValue = true),
                isElementVisibleById("overdue-charge-interest-0")(expectedValue = false),
                isElementVisibleById("over-due-type-1")(expectedValue = true),
                isElementVisibleById("overdue-charge-interest-1")(expectedValue = false),
                isElementVisibleById(s"payment-button")(expectedValue = true),
                isElementVisibleById(s"sa-note-migrated")(expectedValue = false),
                isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false),
                isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
                isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
              )

            }
            "render the payments due page with multiple charges and one charge equals zero" in {

              Given("I wiremock stub a successful Income Source Details response with multiple business and property")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


              And("I wiremock stub a single financial details response")
              val mixedJson = Json.obj(
                "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
                "documentDetails" -> Json.arr(
                  documentDetailJson(3400.00, 1000.00, testTaxYear.toString, "ITSA- POA 1", transactionId = "transId1"),
                  documentDetailJson(1000.00, 100.00, testTaxYear.toString, "ITSA- POA 1", transactionId = "transId2"),
                  documentDetailJson(1000.00, 0.00, testTaxYear.toString, "ITSA - POA 2", transactionId = "transId3")
                ),
                "financialDetails" -> Json.arr(
                  financialDetailJson(testTaxYear.toString, transactionId = "transId1"),
                  financialDetailJson(testTaxYear.toString, "SA Payment on Account 1", LocalDate.now().plusDays(1).toString, "transId2"),
                  financialDetailJson(testTaxYear.toString, "SA Payment on Account 2", LocalDate.now().minusDays(1).toString, "transId3")
                )
              )

              IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)
              IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)


              When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
              val res = IncomeTaxViewChangeFrontend.getPaymentsDue

              AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweWithAZeroOutstandingAmount).detail)

              verifyIncomeSourceDetailsCall(testMtditid)
              IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
              IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

              Then("the result should have a HTTP status of OK (200) and the payments due page")
              res should have(
                httpStatus(OK),
                pageTitleIndividual(whatYouOwePageTitle),
                isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
                isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
                isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
                isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
                isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
                isElementVisibleById("payment-details-content-0")(expectedValue = true),
                isElementVisibleById("payment-details-content-1")(expectedValue = true),
                isElementVisibleById("payments-due")(expectedValue = true),
                isElementVisibleById("over-due-type-0")(expectedValue = true),
                isElementVisibleById("over-due-type-1")(expectedValue = false),
                isElementVisibleById("due-in-thirty-days-type-0")(expectedValue = true),
                isElementVisibleById(s"sa-note-migrated")(expectedValue = false),
                isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false),
                isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
                isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
              )
            }

            "render the payments due page with no dunningLocks" in {

              Given("I wiremock stub a successful Income Source Details response with multiple business and property")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


              And("I wiremock stub a multiple financial details response")
              val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, LocalDate.now().minusDays(15).toString, dunningLock = noDunningLock)
              val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

              IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                financialDetailsResponseJson)
              IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

              When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
              val res = IncomeTaxViewChangeFrontend.getPaymentsDue

              val whatYouOweChargesList = {
                val documentDetailsForTestTaxYear = financialDetailsModel.documentDetails.filter(_.taxYear == testTaxYear.toString)
                WhatYouOweChargesList(
                  balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
                  overduePaymentList = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear).getAllDocumentDetailsWithDueDates
                )
              }
              AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser, whatYouOweChargesList))

              verifyIncomeSourceDetailsCall(testMtditid)
              IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
              IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

              Then("the result should have a HTTP status of OK (200) and the payments due page")
              res should have(
                httpStatus(OK),
                pageTitleIndividual(whatYouOwePageTitle),
                isElementVisibleById("disagree-with-tax-appeal-link")(expectedValue = false)
              )
            }

            "render the payments due page with a dunningLocks against a charge" in {

              Given("I wiremock stub a successful Income Source Details response with multiple business and property")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


              And("I wiremock stub a multiple financial details response")
              val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, LocalDate.now().minusDays(15).toString, dunningLock = oneDunningLock)
              val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

              IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                financialDetailsResponseJson)
              IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

              When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
              val res = IncomeTaxViewChangeFrontend.getPaymentsDue

              val whatYouOweChargesList = {
                val documentDetailsForTestTaxYear = financialDetailsModel.documentDetails.filter(_.taxYear == testTaxYear.toString)
                WhatYouOweChargesList(
                  balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
                  overduePaymentList = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear).getAllDocumentDetailsWithDueDates
                )
              }
              AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser, whatYouOweChargesList))

              verifyIncomeSourceDetailsCall(testMtditid)
              IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
              IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

              Then("the result should have a HTTP status of OK (200) and the payments due page")
              res should have(
                httpStatus(OK),
                pageTitleIndividual(whatYouOwePageTitle),
                isElementVisibleById("disagree-with-tax-appeal-link")(expectedValue = true)
              )
            }

            "render the payments due page with multiple dunningLocks" in {

              Given("I wiremock stub a successful Income Source Details response with multiple business and property")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


              And("I wiremock stub a multiple financial details response")
              val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, LocalDate.now().minusDays(15).toString, dunningLock = twoDunningLocks)
              val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

              IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                financialDetailsResponseJson)
              IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

              When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
              val res = IncomeTaxViewChangeFrontend.getPaymentsDue

              val whatYouOweChargesList = {
                val documentDetailsForTestTaxYear = financialDetailsModel.documentDetails.filter(_.taxYear == testTaxYear.toString)
                WhatYouOweChargesList(
                  balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
                  overduePaymentList = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear).getAllDocumentDetailsWithDueDates
                )
              }
              AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser, whatYouOweChargesList))

              verifyIncomeSourceDetailsCall(testMtditid)
              IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
              IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

              Then("the result should have a HTTP status of OK (200) and the payments due page")
              res should have(
                httpStatus(OK),
                pageTitleIndividual(whatYouOwePageTitle),
                isElementVisibleById("disagree-with-tax-appeal-link")(expectedValue = true)
              )
            }

            "redirect to an internal server error page when both connectors return internal server error" in {

              Given("I wiremock stub a successful Income Source Details response with multiple business and property")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
                propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


              And("I wiremock stub a single financial details response")
              IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino,
                s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())
              IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(INTERNAL_SERVER_ERROR, testOutstandingChargesErrorModelJson)


              When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
              val res = IncomeTaxViewChangeFrontend.getPaymentsDue

              verifyIncomeSourceDetailsCall(testMtditid)
              IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

              Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
              res should have(
                httpStatus(INTERNAL_SERVER_ERROR)
              )

            }
            "redirect to an internal server error page when financial connector return internal server error" in {

              Given("I wiremock stub a successful Income Source Details response with multiple business and property")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
                propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


              And("I wiremock stub a single financial details response")
              IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino,
                s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())
              IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)


              When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
              val res = IncomeTaxViewChangeFrontend.getPaymentsDue

              verifyIncomeSourceDetailsCall(testMtditid)
              IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

              Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
              res should have(
                httpStatus(INTERNAL_SERVER_ERROR)
              )

            }

            "redirect to an internal server error page when Outstanding charges connector return internal server error" in {

              Given("I wiremock stub a successful Income Source Details response with multiple business and property")
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
                propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


              And("I wiremock stub a single financial details response")
              IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, LocalDate.now().toString))
              IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(INTERNAL_SERVER_ERROR, testOutstandingChargesErrorModelJson)


              When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
              val res = IncomeTaxViewChangeFrontend.getPaymentsDue

              verifyIncomeSourceDetailsCall(testMtditid)
              IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

              Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
              res should have(
                httpStatus(INTERNAL_SERVER_ERROR)
              )

            }
          }
        }

        "YearOfMigration does not exists" when {
          "render the payments due page with a no charge" in {


            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, None))

            And("I wiremock stub a single financial details response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06",
              s"$testTaxYear-04-05")(OK, testEmptyFinancialDetailsModelJson)


            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

            verifyIncomeSourceDetailsCall(testMtditid)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitleIndividual(whatYouOwePageTitle),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
              isElementVisibleById("payment-details-content-0")(expectedValue = false),
              isElementVisibleById("payment-details-content-1")(expectedValue = false),
              isElementVisibleById("payments-due")(expectedValue = false),
              isElementVisibleById(s"payment-button")(expectedValue = false),
              isElementVisibleById("no-payments-due")(expectedValue = true),
              isElementVisibleById("sa-note-migrated")(expectedValue = true),
              isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
              isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
              isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
            )
          }
        }

        "YearOfMigration exists but not the first year" when {
          "render the payments due page with a no charge" in {

            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a single financial details response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino,
              s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, testEmptyFinancialDetailsModelJson)
            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

            verifyIncomeSourceDetailsCall(testMtditid)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitleIndividual(whatYouOwePageTitle),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
              isElementVisibleById("payment-details-content-0")(expectedValue = false),
              isElementVisibleById("payment-details-content-1")(expectedValue = false),
              isElementVisibleById("payments-due")(expectedValue = false),
              isElementVisibleById(s"payment-button")(expectedValue = false),
              isElementVisibleById("no-payments-due")(expectedValue = true),
              isElementVisibleById("sa-note-migrated")(expectedValue = true),
              isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
              isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
              isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
            )

          }
        }

        "YearOfMigration exists and No valid charges exists" when {
          "render the payments due page with a no charge" in {

            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a mixed financial details response")
            val mixedJson = Json.obj(
              "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
              "documentDetails" -> Json.arr(
                documentDetailJson(3400.00, 1000.00, testTaxYear.toString, transactionId = "transId1"),
                documentDetailJson(1000.00, 0, testTaxYear.toString, transactionId = "transId2"),
                documentDetailJson(1000.00, 3000.00, testTaxYear.toString, transactionId = "transId3")
              ),
              "financialDetails" -> Json.arr(
                financialDetailJson(testTaxYear.toString, transactionId = "transId5"),
                financialDetailJson(testTaxYear.toString, transactionId = "transId6"),
                financialDetailJson(testTaxYear.toString, transactionId = "transId7")
              )
            )

            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)


            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitleIndividual(whatYouOwePageTitle),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
              isElementVisibleById("payment-details-content-0")(expectedValue = false),
              isElementVisibleById("payment-details-content-1")(expectedValue = false),
              isElementVisibleById("payments-due")(expectedValue = false),
              isElementVisibleById(s"payment-button")(expectedValue = false),
              isElementVisibleById("no-payments-due")(expectedValue = true),
              isElementVisibleById("sa-note-migrated")(expectedValue = true),
              isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
              isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
              isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
            )

          }
        }

        "YearOfMigration exists with Invalid financial details charges and valid outstanding charges" when {
          "render the payments due page with ACI and BCD charge" in {

            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a mixed financial details response")
            val mixedJson = Json.obj(
              "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
              "documentDetails" -> Json.arr(
                documentDetailJson(3400.00, 1000.00, testTaxYear.toString, transactionId = "transId1"),
                documentDetailJson(1000.00, 0, testTaxYear.toString, transactionId = "transId2"),
                documentDetailJson(1000.00, 3000.00, testTaxYear.toString, transactionId = "transId3")
              ),
              "financialDetails" -> Json.arr(
                financialDetailJson(testTaxYear.toString, transactionId = "transId4"),
                financialDetailJson(testTaxYear.toString, transactionId = "transId5"),
                financialDetailJson(testTaxYear.toString, transactionId = "transId6")
              ))

            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

            IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)


            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweOutstandingChargesOnly).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitleIndividual(whatYouOwePageTitle),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
              isElementVisibleById("payment-details-content-0")(expectedValue = true),
              isElementVisibleById("payment-details-content-1")(expectedValue = true),
              isElementVisibleById("payments-due")(expectedValue = false),
              isElementVisibleById(s"payment-button")(expectedValue = true),
              isElementVisibleById(s"no-payments-due")(expectedValue = false),
              isElementVisibleById(s"sa-note-migrated")(expectedValue = false),
              isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false),
              isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
              isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
            )

          }
        }
        "YearOfMigration exists with valid financial details charges and invalid outstanding charges" when {
          "render the payments due page with empty BCD charge" in {

            Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a mixed financial details response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
              testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, LocalDate.now().plusYears(1).toString))

            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweFinancialDetailsEmptyBCDCharge).detail)

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitleIndividual(whatYouOwePageTitle),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
              isElementVisibleById("payment-details-content-0")(expectedValue = true),
              isElementVisibleById("payment-details-content-1")(expectedValue = true),
              isElementVisibleById("payments-due")(expectedValue = false),
              isElementVisibleById(s"payment-button")(expectedValue = true),
              isElementVisibleById(s"no-payments-due")(expectedValue = false),
              isElementVisibleById(s"sa-note-migrated")(expectedValue = false),
              isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false),
              isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
              isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
            )

          }
        }
        "CodingOut FS is enabled" when {
          "render the payments owed with a Coding out banner" in {
            Given("Coding Out feature is enabled")
            enable(CodingOut)

            And("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a financial details response with coded out documents")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
              testValidFinancialDetailsModelJsonCodingOut(2000, 2000, testTaxYear.toString, LocalDate.now().plusYears(1).toString))

            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitleIndividual(whatYouOwePageTitle),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
              isElementVisibleById("payment-details-content-0")(expectedValue = true),
              isElementVisibleById("payment-details-content-1")(expectedValue = true),
              isElementVisibleById("payments-due")(expectedValue = false),
              isElementVisibleById(s"payment-button")(expectedValue = true),
              isElementVisibleById(s"no-payments-due")(expectedValue = false),
              isElementVisibleById(s"sa-note-migrated")(expectedValue = false),
              isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false),
              isElementVisibleById("coding-out-notice")(expectedValue = true),
              isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
              isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
            )
          }
        }

        "CodingOut FS is disabled" when {
          "render the payments owed without a Coding out banner" in {

            Given("Coding Out feature is disabled")
            disable(CodingOut)

            And("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
              propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


            And("I wiremock stub a financial details response with coded out documents")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
              testValidFinancialDetailsModelJsonCodingOut(2000, 2000, testTaxYear.toString, LocalDate.now().plusYears(1).toString))

            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue

            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

            Then("the result should have a HTTP status of OK (200) and the payments due page")
            res should have(
              httpStatus(OK),
              pageTitleIndividual(whatYouOwePageTitle),
              isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
              isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
              isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
              isElementVisibleById("payment-details-content-0")(expectedValue = true),
              isElementVisibleById("payment-details-content-1")(expectedValue = true),
              isElementVisibleById("payments-due")(expectedValue = false),
              isElementVisibleById(s"payment-button")(expectedValue = true),
              isElementVisibleById(s"no-payments-due")(expectedValue = false),
              isElementVisibleById(s"sa-note-migrated")(expectedValue = false),
              isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = false),
              isElementVisibleById("coding-out-notice")(expectedValue = false),
              isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
              isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
            )
          }
        }
      }

      "DisplayTotals FS is disabled" should {
        "NOT render the payments due totals" in {

          Given("WhatYouOweTotals feature is disabled")
          disable(WhatYouOweTotals)

          And("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
            propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


          And("I wiremock stub a financial details response with coded out documents")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
            testValidFinancialDetailsModelJsonCodingOut(2000, 2000, testTaxYear.toString, LocalDate.now().plusYears(1).toString))

          IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
            "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual(whatYouOwePageTitle),
          )
        }
      }

    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getPaymentsDue)
    }
  }
}
