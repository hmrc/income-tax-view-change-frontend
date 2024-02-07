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
import config.featureswitch.{CodingOut, CreditsRefundsRepay, MFACreditsAndDebits, NavBarFs}
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.financialDetails.{BalanceDetails, FinancialDetailsModel, WhatYouOweChargesList}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import services.DateServiceInterface
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSaUtr}
import testConstants.FinancialDetailsIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.OutstandingChargesIntegrationTestConstants._
import testConstants.messages.WhatYouOweMessages.{hmrcAdjustment, hmrcAdjustmentHeading, hmrcAdjustmentLine1}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate
import java.time.Month.APRIL

class WhatYouOweControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, paymentHistoryBusinessAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  val testTaxYear: Int = getCurrentTaxYearEnd.getYear - 1
  val testDate: LocalDate = LocalDate.parse("2022-01-01")

  val testValidOutStandingChargeResponseJsonWithAciAndBcdCharges: JsValue = Json.parse(
    s"""
       |{
       |  "outstandingCharges": [{
       |         "chargeName": "BCD",
       |         "relevantDueDate": "$testDate",
       |         "chargeAmount": 123456789012345.67,
       |         "tieBreaker": 1234
       |       },
       |       {
       |         "chargeName": "ACI",
       |         "relevantDueDate": "$testDate",
       |         "chargeAmount": 12.67,
       |         "tieBreaker": 1234
       |       }
       |  ]
       |}
       |""".stripMargin)

  val testDateService: DateServiceInterface = new DateServiceInterface {
    override def getCurrentDate(isTimeMachineEnabled: Boolean = false): LocalDate = LocalDate.of(2023, 4, 5)

    override def getCurrentTaxYearEnd(isTimeMachineEnabled: Boolean = false): Int = 2022

    override def isBeforeLastDayOfTaxYear(isTimeMachineEnabled: Boolean): Boolean = false

    override def getCurrentTaxYearStart(isTimeMachineEnabled: Boolean): LocalDate = LocalDate.of(2022, 4, 6)

    override def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate = {
      val startDateYear = startDate.getYear
      val accountingPeriodEndDate = LocalDate.of(startDateYear, APRIL, 5)

      if (startDate.isBefore(accountingPeriodEndDate) || startDate.isEqual(accountingPeriodEndDate)) {
        accountingPeriodEndDate
      } else {
        accountingPeriodEndDate.plusYears(1)
      }
    }
  }

  "Navigating to /report-quarterly/income-and-expenses/view/payments-owed" when {

    "Authorised" when {

      "render the payments due totals" in {
        disable(NavBarFs)
        Given("Display Totals feature is enabled")

        And("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


        And("I wiremock stub a financial details response with coded out documents")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelJsonCodingOut(2000, 2000, (testTaxYear - 1).toString, testDate.plusYears(1).toString))

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
        val res = IncomeTaxViewChangeFrontend.getPaymentsDue

        AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweFinancialDetailsEmptyBCDCharge, testDateService).detail)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
        IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("whatYouOwe.heading"),
        )
      }

      "YearOfMigration exists" when {
        "render the payments due page with a multiple charge from financial details and BCD and ACI charges from CESA" in {

          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


          And("I wiremock stub a multiple financial details and outstanding charges response")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
            testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString))
          IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
            "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)


          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataWithDataDueIn30Days, dateService).detail)

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          Then("the result should have a HTTP status of OK (200) and the payments due page")

          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
            isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
            isElementVisibleById("payment-details-content-0")(expectedValue = true),
            isElementVisibleById("payment-details-content-1")(expectedValue = true),
            isElementVisibleById("due-0")(expectedValue = true),
            isElementVisibleById("due-1")(expectedValue = true),
            isElementVisibleById("payment-button")(expectedValue = true),
            isElementVisibleById("sa-note-migrated")(expectedValue = true),
            isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
            isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
            isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
          )

        }

        "render the payments due page with a multiple charge, without BCD and ACI charges from CESA" in {

          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


          And("I wiremock stub a multiple financial details response")
          val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString)
          val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
            financialDetailsResponseJson)
          IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
            "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          val whatYouOweChargesList = {
            val documentDetailsForTestTaxYear = financialDetailsModel.documentDetails.filter(_.taxYear == testTaxYear)
            WhatYouOweChargesList(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              chargesList = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear).getAllDocumentDetailsWithDueDates()
            )
          }
          AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser, whatYouOweChargesList, dateService))

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
            isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
            isElementVisibleById("payment-details-content-0")(expectedValue = true),
            isElementVisibleById("payment-details-content-1")(expectedValue = true),
            isElementVisibleById("due-0")(expectedValue = true),
            isElementVisibleById("charge-interest-0")(expectedValue = false),
            isElementVisibleById("due-1")(expectedValue = true),
            isElementVisibleById("charge-interest-1")(expectedValue = false),
            isElementVisibleById(s"payment-button")(expectedValue = true),
            isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
            isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
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
              documentDetailJson(3400.00, 1000.00, (testTaxYear - 1), "ITSA- POA 1", transactionId = "transId1"),
              documentDetailJson(1000.00, 100.00, (testTaxYear - 1), "ITSA- POA 1", transactionId = "transId2", dueDate = testDate.plusDays(1).toString),
              documentDetailJson(1000.00, 0.00, (testTaxYear - 1), "ITSA - POA 2", transactionId = "transId3", dueDate = testDate.minusDays(1).toString)
            ),
            "financialDetails" -> Json.arr(
              financialDetailJson((testTaxYear - 1).toString, transactionId = "transId1"),
              financialDetailJson((testTaxYear - 1).toString, "SA Payment on Account 1", testDate.plusDays(1).toString, "transId2"),
              financialDetailJson((testTaxYear - 1).toString, "SA Payment on Account 2", testDate.minusDays(1).toString, "transId3")
            )
          )

          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)
          IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
            "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, testValidOutStandingChargeResponseJsonWithAciAndBcdCharges)


          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweWithAZeroOutstandingAmount, dateService).detail)

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
            isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
            isElementVisibleById("payment-details-content-0")(expectedValue = true),
            isElementVisibleById("payment-details-content-1")(expectedValue = true),
            isElementVisibleById("due-0")(expectedValue = true),
            isElementVisibleById("due-1")(expectedValue = true),
            isElementVisibleById("due-2")(expectedValue = false),
            isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
            isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
            isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
            isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
          )
        }

        "render the payments due page with no dunningLocks" in {

          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


          And("I wiremock stub a multiple financial details response")
          val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString, dunningLock = noDunningLock)
          val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
            financialDetailsResponseJson)
          IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
            "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          val whatYouOweChargesList = {
            val documentDetailsForTestTaxYear = financialDetailsModel.documentDetails.filter(_.taxYear == testTaxYear)
            WhatYouOweChargesList(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              chargesList = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear).getAllDocumentDetailsWithDueDates()
            )
          }
          AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser, whatYouOweChargesList, dateService))

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("disagree-with-tax-appeal-link")(expectedValue = false)
          )
        }

        "render the payments due page with a dunningLocks against a charge" in {

          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


          And("I wiremock stub a multiple financial details response")
          val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString, dunningLock = oneDunningLock)
          val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
            financialDetailsResponseJson)
          IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
            "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          val whatYouOweChargesList = {
            val documentDetailsForTestTaxYear = financialDetailsModel.documentDetails.filter(_.taxYear == testTaxYear)
            WhatYouOweChargesList(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              chargesList = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear).getAllDocumentDetailsWithDueDates()
            )
          }
          AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser, whatYouOweChargesList, dateService))

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("disagree-with-tax-appeal-link")(expectedValue = true)
          )
        }

        "render the payments due page with multiple dunningLocks" in {

          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


          And("I wiremock stub a multiple financial details response")
          val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString, dunningLock = twoDunningLocks)
          val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
            financialDetailsResponseJson)
          IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
            "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          val whatYouOweChargesList = {
            val documentDetailsForTestTaxYear = financialDetailsModel.documentDetails.filter(_.taxYear == testTaxYear)
            WhatYouOweChargesList(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              chargesList = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear).getAllDocumentDetailsWithDueDates()
            )
          }
          AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser, whatYouOweChargesList, dateService))

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
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
            testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.toString))
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

      "YearOfMigration does not exists" when {
        "render the payments due page with a no charge" in {


          Given("I wiremock stub a successful Income Source Details response with multiple business and property without year of migration")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, None))

          And("I wiremock stub a single financial details response")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06",
            s"$testTaxYear-04-05")(OK, testEmptyFinancialDetailsModelJson)


          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList, dateService).detail)

          verifyIncomeSourceDetailsCall(testMtditid)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
            isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
            isElementVisibleById("payment-details-content-0")(expectedValue = false),
            isElementVisibleById("payment-details-content-1")(expectedValue = false),
            isElementVisibleById(s"payment-button")(expectedValue = false),
            isElementVisibleById("no-payments-due")(expectedValue = true),
            isElementVisibleById("sa-note-migrated")(expectedValue = true),
            isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
            isElementVisibleById(s"payments-made-bullets")(expectedValue = false),
            isElementVisibleById(s"sa-tax-bill")(expectedValue = false)
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

          AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList, dateService).detail)

          verifyIncomeSourceDetailsCall(testMtditid)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
            isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
            isElementVisibleById("payment-details-content-0")(expectedValue = false),
            isElementVisibleById("payment-details-content-1")(expectedValue = false),
            isElementVisibleById(s"payment-button")(expectedValue = false),
            isElementVisibleById("no-payments-due")(expectedValue = true),
            isElementVisibleById("sa-note-migrated")(expectedValue = true),
            isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
            isElementVisibleById(s"payments-made-bullets")(expectedValue = false),
            isElementVisibleById(s"sa-tax-bill")(expectedValue = false)
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
            ),
            "financialDetails" -> Json.arr(

            )
          )

          IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
            "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)


          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList, dateService).detail)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
            isElementVisibleById("payment-details-content-0")(expectedValue = false),
            isElementVisibleById("payment-details-content-1")(expectedValue = false),
            isElementVisibleById(s"payment-button")(expectedValue = false),
            isElementVisibleById("no-payments-due")(expectedValue = true),
            isElementVisibleById("sa-note-migrated")(expectedValue = true),
            isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
            isElementVisibleById(s"payments-made-bullets")(expectedValue = false),
            isElementVisibleById(s"sa-tax-bill")(expectedValue = false)
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
              documentDetailJson(3400.00, 1000.00, testTaxYear, transactionId = "transId1"),
              documentDetailJson(1000.00, 0, testTaxYear, transactionId = "transId2"),
              documentDetailJson(1000.00, 3000.00, testTaxYear, transactionId = "transId3")
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

          AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweOutstandingChargesOnly, dateService).detail)

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
            isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
            isElementVisibleById("payment-details-content-0")(expectedValue = true),
            isElementVisibleById("payment-details-content-1")(expectedValue = true),
            isElementVisibleById(s"payment-button")(expectedValue = true),
            isElementVisibleById(s"no-payments-due")(expectedValue = false),
            isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
            isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
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
            testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.plusYears(1).toString))

          IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
            "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweFinancialDetailsEmptyBCDCharge, dateService).detail)

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
            isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
            isElementVisibleById("payment-details-content-0")(expectedValue = true),
            isElementVisibleById("payment-details-content-1")(expectedValue = true),
            isElementVisibleById(s"payment-button")(expectedValue = true),
            isElementVisibleById(s"no-payments-due")(expectedValue = false),
            isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
            isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
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
            testValidFinancialDetailsModelJsonCodingOut(2000, 2000, testTaxYear.toString,
              testDate.toString, 0, (getCurrentTaxYearEnd.getYear - 1).toString, 2000))

          IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
            "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
            isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
            isElementVisibleById("payment-details-content-0")(expectedValue = true),
            isElementVisibleById("payment-details-content-1")(expectedValue = true),
            isElementVisibleById(s"payment-button")(expectedValue = true),
            isElementVisibleById(s"no-payments-due")(expectedValue = false),
            isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
            isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
            isElementVisibleById("coding-out-notice")(expectedValue = true),
            isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
            isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
          )
        }

        "render the payments due page with a multiple charges ~ TxM extension" in {
          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          enable(CodingOut)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

          And("I wiremock stub a multiple financial details and outstanding charges response")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
            testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, isClass2Nic = true))

          IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
            "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataWithDataDueInSomeDays, dateService).detail)

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          Then("the result should have a HTTP status of OK (200) and the payments due page")

          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
            isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
            isElementVisibleById("payment-details-content-0")(expectedValue = true),
            isElementVisibleById("payment-details-content-1")(expectedValue = true),
            isElementVisibleById("due-0")(expectedValue = true),
            isElementVisibleById("due-1")(expectedValue = true),
            isElementVisibleById("payment-button")(expectedValue = true),
            isElementVisibleById("sa-note-migrated")(expectedValue = true),
            isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
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
            testValidFinancialDetailsModelJsonCodingOut(2000, 2000, testTaxYear.toString, testDate.plusYears(1).toString))

          IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
            "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


          When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
          val res = IncomeTaxViewChangeFrontend.getPaymentsDue

          verifyIncomeSourceDetailsCall(testMtditid)
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
          IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

          Then("the result should have a HTTP status of OK (200) and the payments due page")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("whatYouOwe.heading"),
            isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
            isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
            isElementVisibleById("payment-details-content-0")(expectedValue = true),
            isElementVisibleById("payment-details-content-1")(expectedValue = true),
            isElementVisibleById(s"payment-button")(expectedValue = true),
            isElementVisibleById(s"no-payments-due")(expectedValue = false),
            isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
            isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
            isElementVisibleById("coding-out-notice")(expectedValue = false),
            isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
            isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
          )
        }
      }

      "MFA Debits" should {
        "render the payments owed" when {
          def testMFADebits(MFADebitsEnabled: Boolean): Unit = {
            Given(s"MFADebitsEnabled is ${MFADebitsEnabled}")
            if (MFADebitsEnabled) enable(MFACreditsAndDebits) else disable(MFACreditsAndDebits)

            And("I wiremock stub a successful Income Source Details response with multiple business and property")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

            And("I wiremock stub a multiple financial details response")
            IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
              testValidFinancialDetailsModelMFADebitsJson(2000, 2000, testTaxYear.toString, testDate.plusYears(1).toString))
            IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
            val res = IncomeTaxViewChangeFrontend.getPaymentsDue
            verifyIncomeSourceDetailsCall(testMtditid)
            IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)
            IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)
            Then("The expected result is returned")
            if (MFADebitsEnabled) {
              res should have(
                httpStatus(OK),
                pageTitleIndividual("whatYouOwe.heading"),
                elementTextBySelectorList("#payments-due-table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)", "a:nth-of-type(1)")(s"$hmrcAdjustment 1"),
                elementTextBySelectorList("#payments-due-table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)", "a:nth-of-type(1)")(s"$hmrcAdjustment 2"),
                elementTextBySelectorList("#payments-due-table", "tbody", "tr:nth-of-type(3)", "td:nth-of-type(2)", "a:nth-of-type(1)")(s"$hmrcAdjustment 3"),
                elementTextBySelectorList("#payments-due-table", "tbody", "tr:nth-of-type(4)", "td:nth-of-type(2)", "a:nth-of-type(1)")(s"$hmrcAdjustment 4"),
                elementTextBySelectorList("#payment-details-content-5")(s"$hmrcAdjustmentHeading $hmrcAdjustmentLine1"))
            } else {
              res should have(
                httpStatus(OK),
                pageTitleIndividual("whatYouOwe.heading"),
                isElementVisibleById("no-payments-due")(expectedValue = true))
            }
          }

          "show What You Owe page with MFA Debits on the Payment Tab with FS ENABLED" in {
            testMFADebits(true)
          }
          "show What You Owe page with MFA Debits on the Payment Tab with FS DISABLED" in {
            testMFADebits(false)
          }
        }
      }
    }


    "API#1171 IncomeSourceDetails Caching" when {
      "caching should be ENABLED" in {
        testIncomeSourceDetailsCaching(resetCacheAfterFirstCall = false, 1,
          () => IncomeTaxViewChangeFrontend.getPaymentsDue)
      }
    }

    "render the money in your account section when balance details has available credits" in {
      enable(CreditsRefundsRepay)
      Given("I wiremock stub a successful Income Source Details response with multiple business and property")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


      And("I wiremock stub a single financial details response")
      val mixedJson = Json.obj(
        "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00, "availableCredit" -> 300.00),
        "documentDetails" -> Json.arr(
          documentDetailJson(3400.00, 1000.00, testTaxYear, "ITSA- POA 1", transactionId = "transId1"),
          documentDetailJson(1000.00, 100.00, testTaxYear, "ITSA- POA 1", transactionId = "transId2"),
          documentDetailJson(1000.00, 0.00, testTaxYear, "ITSA - POA 2", transactionId = "transId3")
        ),
        "financialDetails" -> Json.arr(
          financialDetailJson(testTaxYear.toString, transactionId = "transId1"),
          financialDetailJson(testTaxYear.toString, "SA Payment on Account 1", testDate.plusDays(1).toString, "transId2"),
          financialDetailJson(testTaxYear.toString, "SA Payment on Account 2", testDate.minusDays(1).toString, "transId3")
        )
      )

      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)
      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)


      When("I call GET /report-quarterly/income-and-expenses/view/payments-owed")
      val res = IncomeTaxViewChangeFrontend.getPaymentsDue

      Then("the result should have a HTTP status of OK (200) and the payments due page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual("whatYouOwe.heading"),
        isElementVisibleById(s"money-in-your-account")(expectedValue = true),
        elementTextBySelector("#money-in-your-account")(
          messagesAPI("whatYouOwe.moneyOnAccount") + " " +
            messagesAPI("whatYouOwe.moneyOnAccount-1") + " £300.00" + " " +
            messagesAPI("whatYouOwe.moneyOnAccount-2") + " " +
            messagesAPI("whatYouOwe.moneyOnAccount-3") + "."
        )
      )
    }

  }
}
