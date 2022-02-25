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

import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.FinancialDetailsIntegrationTestConstants.financialDetailModelPartial
import testConstants.IncomeSourceIntegrationTestConstants._
import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import config.featureswitch._
import helpers.ComponentSpecBase
import helpers.servicemocks.DocumentDetailsStub.{docDateDetail, docDateDetailWithInterest}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.chargeHistory.ChargeHistoryModel
import models.financialDetails.{FinancialDetail, Payment, PaymentsWithChargeType}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import java.time.LocalDate

import testConstants.messages.ChargeSummaryMessages.{balancingPaymentTitle, lpiBalancingPayment, lpiPoa2, poa1Title, saPayment}

class ChargeSummaryControllerISpec extends ComponentSpecBase {

  def paymentsWithCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal, lotItem: String): PaymentsWithChargeType =
    PaymentsWithChargeType(
      payments = List(Payment(reference = Some("reference"), amount = Some(amount), method = Some("method"),
        lot = Some("lot"), lotItem = Some(lotItem), date = Some(date), transactionId = None)),
      mainType = Some(mainType), chargeType = Some(chargeType))

  val paymentAllocation: List[PaymentsWithChargeType] = List(
    paymentsWithCharge("SA Balancing Charge", "ITSA NI", "2019-08-13", -10000.0, lotItem = "000001"),
    paymentsWithCharge("SA Payment on Account 1", "NIC4 Scotland", "2019-08-13", -9000.0, lotItem = "000001"),
    paymentsWithCharge("SA Payment on Account 2", "NIC4 Scotland", "2019-08-13", -8000.0, lotItem = "000001")
  )

  val chargeHistories: List[ChargeHistoryModel] = List(ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 2, 14).toString, "ITSA- POA 1", 2500, LocalDate.of(2019, 2, 14), "Customer Request"))

  val paymentBreakdown: List[FinancialDetail] = List(
    financialDetailModelPartial(originalAmount = 123.45, chargeType = "ITSA England & NI", mainType = "SA Balancing Charge", dunningLock = Some("Dunning Lock"), interestLock = Some("Interest Lock")),
    financialDetailModelPartial(originalAmount = 123.45, chargeType = "NIC4 Scotland", dunningLock = Some("Stand over order"), interestLock = Some("Breathing Space Moratorium Act")),
    financialDetailModelPartial(originalAmount = 123.45, chargeType = "NIC4 Scotland", mainType = "SA Payment on Account 2", dunningLock = Some("Dunning Lock"), interestLock = Some("Manual RPI Signal")))

  "Navigating to /report-quarterly/income-and-expenses/view/payments-due" should {

    "load the page with right data for Payments Breakdown" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2,
        dunningLock = twoDunningLocks, interestLocks = twoInterestLocks))

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000124")(OK, testChargeHistoryJson(testMtditid, "1040000124", 2500))

      Given("the TxmEvents feature switches are on")
      disable(ChargeHistory)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000124")

      verifyIncomeSourceDetailsCall(testMtditid)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual(poa1Title),
        elementTextBySelector("#heading-payment-breakdown")("Payment breakdown"),
        elementTextBySelector("dl:nth-of-type(2) dd span")("Under review"),
        elementTextBySelector("dl:nth-of-type(2) dd div")("We are not currently charging interest on this payment")
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some("Individual"), None
        )(FakeRequest()),
        docDateDetail("2018-02-14", "ITSA- POA 1"),
        paymentBreakdown = List(financialDetailModelPartial(chargeType = "ITSA England & NI", originalAmount = 10.34, dunningLock = Some("Stand over order"), interestLock = Some("Breathing Space Moratorium Act"))),
        chargeHistories = List.empty,
        paymentAllocations = List.empty,
        None,
        isLatePaymentCharge = false
      ))
    }

    "load the page with right audit events when TxmEvents and PaymentAllocations FS on and ChargeHistory FS off" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testAuditFinancialDetailsModelJson(123.45, 1.2,
        dunningLock = oneDunningLock, interestLocks = twoInterestLocks))

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000123")(OK, testChargeHistoryJson(testMtditid, "1040000123", 2500))

      Given("the TxmEvents PaymentAllocations and feature switch is on")
      enable(PaymentAllocation)
      disable(ChargeHistory)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some("Individual"), None
        )(FakeRequest()),
        docDateDetailWithInterest("2018-04-14", "TRM New Charge"),
        paymentBreakdown = paymentBreakdown,
        chargeHistories = List.empty,
        paymentAllocations = paymentAllocation,
        None,
        isLatePaymentCharge = false
      ))

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual(balancingPaymentTitle),
        elementTextBySelector("main h2")("Important Payment breakdown")
      )
    }

    "load the page with right audit events when TxmEvents PaymentAllocations and ChargeHistory FS enabled" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testAuditFinancialDetailsModelJson(123.45, 1.2,
        dunningLock = oneDunningLock, interestLocks = twoInterestLocks))

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000123")(OK, testChargeHistoryJson(testMtditid, "1040000123", 2500))

      Given("the TxmEvents PaymentAllocations and ChargeHistory feature switch is on")
      enable(PaymentAllocation)
      enable(ChargeHistory)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some("Individual"), None
        )(FakeRequest()),
        docDateDetailWithInterest("2018-04-14", "TRM New Charge"),
        paymentBreakdown = paymentBreakdown,
        chargeHistories = chargeHistories,
        paymentAllocations = paymentAllocation,
        None,
        isLatePaymentCharge = false
      ))

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual(balancingPaymentTitle),
        elementTextBySelector("main h2")("Important Payment breakdown")
      )
    }

    "load the page when the late payment interest flag is true and chargeHistory and paymentAllocation FS is enabled" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJsonAccruingInterest(
        123.45, 1.2, latePaymentInterestAmount = 54.32))

      Given("the TxmEvents feature switch is on")
      enable(ChargeHistory)
      enable(PaymentAllocation)

      val res = IncomeTaxViewChangeFrontend.getChargeSummaryLatePayment("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some("Individual"), None
        )(FakeRequest()),
        docDateDetailWithInterest("2018-02-14", "TRM New Charge"),
        paymentBreakdown = List.empty,
        chargeHistories = List.empty,
        paymentAllocations = paymentAllocation,
        None,
        isLatePaymentCharge = true
      ))

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual(lpiBalancingPayment),
        elementTextBySelector("main h2")("Payment history"),
        elementTextBySelector("tbody tr:nth-child(1) td:nth-child(2)")("Late payment interest for Balancing payment created")
      )
    }

    "load the page when the late payment interest flag is true and paymentAllocation FS is enabled but chargeHistory FS is disabled" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2))

      disable(ChargeHistory)
      enable(PaymentAllocation)

      val res = IncomeTaxViewChangeFrontend.getChargeSummaryLatePayment("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual(lpiBalancingPayment),
        elementTextBySelector("main h2")("Payment history"),
        elementTextBySelector("tbody tr:nth-child(1) td:nth-child(2)")("")

      )
    }

    "load the page when the late payment interest flag is true and both paymentAllocation and chargeHistory FS are disabled" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2))

      disable(ChargeHistory)
      disable(PaymentAllocation)

      val res = IncomeTaxViewChangeFrontend.getChargeSummaryLatePayment("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual(lpiBalancingPayment),
        elementTextBySelector("main h2")("")
      )
    }

    "load the page when the late payment interest flag is true but there are no payment allocations" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, Json.obj(
        "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
        "documentDetails" -> Json.arr(
          Json.obj("taxYear" -> "2018",
            "transactionId" -> "1040001234",
            "documentDescription" -> "ITSA - POA 2",
            "outstandingAmount" -> 1.2,
            "originalAmount" -> 10.34,
            "documentDate" -> "2018-03-29",
            "interestFromDate" -> "2018-03-29",
            "interestEndDate" -> "2018-03-29",
            "latePaymentInterestAmount" -> 100.0,
            "interestOutstandingAmount" -> 80.0
          )),
        "financialDetails" -> Json.arr()))

      disable(ChargeHistory)
      enable(PaymentAllocation)

      val res = IncomeTaxViewChangeFrontend.getChargeSummaryLatePayment("2018", "1040001234")

      verifyIncomeSourceDetailsCall(testMtditid)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual(lpiPoa2),
        elementTextBySelector("main h2")("")
      )
    }

    "load the page with coding out details when coding out is enable and a coded out documentDetail id is passed" in {
      val header = "Tax year 6 April 2017 to 5 April 2018 Balancing payment collected through PAYE tax code"
      val insetPara = "If this tax cannot be collected through your PAYE tax code (opens in new tab) for any reason, you will need to pay the remaining amount. You will have 42 days to make this payment before you may charged interest and penalties."
      val summaryMessage = "This is the remaining tax you owe for the 2017 to 2018 tax year."
      val payHistoryLine1 = "29 Mar 2018 Amount collected through your PAYE tax code for 2017 to 2018 tax year £2,500.00"

      Given("the CodingOut feature switch is enabled")
      enable(CodingOut)
      enable(ChargeHistory)
      enable(PaymentAllocation)

      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response with a coded out documentDetail")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, Json.obj(
        "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
        "documentDetails" -> Json.arr(
          Json.obj("taxYear" -> "2018",
            "transactionId" -> "CODINGOUT01",
            "documentDescription" -> "TRM New Charge",
            "documentText" -> "PAYE Self Assessment",
            "outstandingAmount" -> 2500.00,
            "originalAmount" -> 2500.00,
            "documentDate" -> "2018-03-29",
            "amountCodedOut" -> 2500.00
          )),
        "financialDetails" -> Json.arr()))


      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "CODINGOUT01")

      verifyIncomeSourceDetailsCall(testMtditid)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual(saPayment),
        elementTextBySelector("h1")(header),
        elementTextBySelector("#coding-out-notice")(insetPara),
        elementTextBySelector("#coding-out-message")(summaryMessage),
        elementTextBySelector(".govuk-table tbody tr:nth-child(1)")(payHistoryLine1)
      )
    }
  }

  s"return $OK with correct page title and ChargeHistory FS is enabled and the charge history details API responds with a $NOT_FOUND" in {
    enable(ChargeHistory)
    enable(PaymentAllocation)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2))

    IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000123")(NOT_FOUND, Json.parse(
      """
        |{
        |   "code": "NO_DATA_FOUND",
        |   "reason": "The remote endpoint has indicated that no match found for the reference provided."
        |}
        |""".stripMargin))

    val result = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

    result should have(
      httpStatus(OK),
      pageTitleIndividual(balancingPaymentTitle)
    )
  }

  s"return $OK with correct page title and ChargeHistory FS is enabled and the charge history details API responds with a $FORBIDDEN" in {
    enable(ChargeHistory)
    enable(PaymentAllocation)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2))

    IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000123")(FORBIDDEN, Json.parse(
      """
        |{
        |   "code": "REQUEST_NOT_PROCESSED",
        |   "reason": "The remote endpoint has indicated that request could not be processed."
        |}
        |""".stripMargin))

    val result = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

    result should have(
      httpStatus(OK),
      pageTitleIndividual(balancingPaymentTitle)
    )
  }

  "return a technical difficulties page to the user" when {
    "ChargeHistory FS is enabled and the charge history details API responded with an error" in {
      enable(ChargeHistory)
      enable(PaymentAllocation)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2))

      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000123")(INTERNAL_SERVER_ERROR, Json.parse(
        """
          |{
          |   "code": "SERVER_ERROR",
          |   "reason": "DES is currently experiencing problems that require live service intervention."
          |}
          |""".stripMargin))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

      result should have(
        httpStatus(INTERNAL_SERVER_ERROR),
        pageTitleIndividual(titleInternalServer)
      )
    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123"))
    }
  }
}
