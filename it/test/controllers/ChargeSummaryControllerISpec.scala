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

import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import config.featureswitch._
import enums.ChargeType.{ITSA_ENGLAND_AND_NI, ITSA_NI, NIC4_SCOTLAND}
import enums.CodingOutType._
import helpers.ComponentSpecBase
import helpers.servicemocks.DocumentDetailsStub.{docDateDetail, docDateDetailWithInterestAndOverdue}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.chargeHistory.ChargeHistoryModel
import models.financialDetails._
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testTaxYear}
import testConstants.FinancialDetailsIntegrationTestConstants.financialDetailModelPartial
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.messages.ChargeSummaryMessages._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate

class ChargeSummaryControllerISpec extends ComponentSpecBase {

  val paymentAllocation: List[PaymentsWithChargeType] = List(
    paymentsWithCharge("SA Balancing Charge", ITSA_NI, "2019-08-13", -10000.0, lotItem = "000001"),
    paymentsWithCharge("SA Payment on Account 1", NIC4_SCOTLAND, "2019-08-13", -9000.0, lotItem = "000001"),
    paymentsWithCharge("SA Payment on Account 2", NIC4_SCOTLAND, "2019-08-13", -8000.0, lotItem = "000001")
  )
  val chargeHistories: List[ChargeHistoryModel] = List(ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 2, 14), "ITSA- POA 1", 2500, LocalDate.of(2019, 2, 14), "Customer Request", Some("001")))
  val paymentBreakdown: List[FinancialDetail] = List(
    financialDetailModelPartial(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, mainType = "SA Balancing Charge", dunningLock = Some("Dunning Lock"), interestLock = Some("Interest Lock")),
    financialDetailModelPartial(originalAmount = 123.45, chargeType = NIC4_SCOTLAND, dunningLock = Some("Stand over order"), interestLock = Some("Breathing Space Moratorium Act")),
    financialDetailModelPartial(originalAmount = 123.45, chargeType = NIC4_SCOTLAND, mainType = "SA Payment on Account 2", dunningLock = Some("Dunning Lock"), interestLock = Some("Manual RPI Signal")))
  val importantPaymentBreakdown: String = s"${messagesAPI("chargeSummary.dunning.locks.banner.title")} ${messagesAPI("chargeSummary.paymentBreakdown.heading")}"
  val paymentHistory: String = messagesAPI("chargeSummary.chargeHistory.heading")

  def paymentsWithCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal, lotItem: String): PaymentsWithChargeType =
    PaymentsWithChargeType(
      payments = List(Payment(reference = Some("reference"), amount = Some(amount), outstandingAmount = None, method = Some("method"),
        lot = Some("lot"), lotItem = Some(lotItem), dueDate = Some(LocalDate.parse(date)), documentDate = LocalDate.parse(date), transactionId = None, documentDescription = None)),
      mainType = Some(mainType), chargeType = Some(chargeType))

  "Navigating to the Charge Summary Page" should {

    "load the page with right data for Payments Breakdown" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2,
        dunningLock = twoDunningLocks, interestLocks = twoInterestLocks))

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000124")(OK, testChargeHistoryJson(testMtditid, "1040000124", 2500))

      Given("the ChargeHistory feature switch is disabled")
      disable(ChargeHistory)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000124")

      verifyIncomeSourceDetailsCall(testMtditid)
      enable(CodingOut)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK)
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some(Individual), None
        )(FakeRequest()),
        docDateDetail("2018-02-14", "ITSA- POA 1"),
        paymentBreakdown = List(financialDetailModelPartial(chargeType = ITSA_ENGLAND_AND_NI, originalAmount = 10.34, dunningLock = Some("Stand over order"), interestLock = Some("Breathing Space Moratorium Act"))),
        chargeHistories = List.empty,
        paymentAllocations = List.empty,
        isLatePaymentCharge = false,
        taxYear = testTaxYear
      ))
    }

    "load the page with right audit events when PaymentAllocations FS on and ChargeHistory FS off" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testAuditFinancialDetailsModelJson(123.45, 1.2,
        dunningLock = oneDunningLock, interestLocks = twoInterestLocks, latePaymentInterestAmount = None))

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000123")(OK, testChargeHistoryJson(testMtditid, "1040000123", 2500))

      Given("the PaymentAllocations feature switch is on and ChargeHistory is off")
      enable(PaymentAllocation)
      disable(ChargeHistory)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some(Individual), None
        )(FakeRequest()),
        docDateDetailWithInterestAndOverdue("2018-04-14", "TRM New Charge"),
        paymentBreakdown = paymentBreakdown,
        chargeHistories = List.empty,
        paymentAllocations = paymentAllocation,
        isLatePaymentCharge = false,
        taxYear = testTaxYear
      ))

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual("chargeSummary.balancingCharge.text"),
        elementTextBySelector("main h2")(importantPaymentBreakdown)
      )
    }

    "load the page with right audit events when PaymentAllocations and ChargeHistory FS enabled" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testAuditFinancialDetailsModelJson(123.45, 1.2,
        dunningLock = oneDunningLock, interestLocks = twoInterestLocks, latePaymentInterestAmount = None))

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000123")(OK, testChargeHistoryJson(testMtditid, "1040000123", 2500))

      Given("the PaymentAllocations and ChargeHistory feature switch is on")
      enable(PaymentAllocation)
      enable(ChargeHistory)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some(Individual), None
        )(FakeRequest()),
        docDateDetailWithInterestAndOverdue("2018-04-14", "TRM New Charge"),
        paymentBreakdown = paymentBreakdown,
        chargeHistories = chargeHistories,
        paymentAllocations = paymentAllocation,
        isLatePaymentCharge = false,
        taxYear = testTaxYear
      ))

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual("chargeSummary.balancingCharge.text"),
        elementTextBySelector("main h2")(importantPaymentBreakdown)
      )
    }

    "load the page when the late payment interest flag is true and chargeHistory and paymentAllocation FS is enabled" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJsonAccruingInterest(
        123.45, 1.2, latePaymentInterestAmount = Some(54.32)))

      Given("the PaymentAllocations and ChargeHistory feature switches are on")
      enable(ChargeHistory)
      enable(PaymentAllocation)

      val res = IncomeTaxViewChangeFrontend.getChargeSummaryLatePayment("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some(Individual), None
        )(FakeRequest()),
        docDateDetailWithInterestAndOverdue("2018-02-14", "TRM New Charge"),
        paymentBreakdown = List.empty,
        chargeHistories = List.empty,
        paymentAllocations = paymentAllocation,
        isLatePaymentCharge = true,
        taxYear = testTaxYear
      ))

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual("chargeSummary.lpi.balancingCharge.text"),
        elementTextBySelector("main h2")(paymentHistory),
        elementTextBySelector("tbody tr:nth-child(1) td:nth-child(2)")(lpiCreated)
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
        pageTitleIndividual("chargeSummary.lpi.balancingCharge.text"),
        //will-revert//elementTextBySelector("main h2")(paymentHistory),
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
        pageTitleIndividual("chargeSummary.lpi.balancingCharge.text"),
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
          Json.obj("taxYear" -> 2018,
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
        pageTitleIndividual("chargeSummary.lpi.paymentOnAccount2.text"),
        elementTextBySelector("main h2")("")
      )
    }

    "load the page with coding out details when coding out is enable and a coded out documentDetail id is passed" in {
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
          Json.obj("taxYear" -> 2018,
            "transactionId" -> "CODINGOUT01",
            "documentDescription" -> "TRM New Charge",
            "documentText" -> CODING_OUT_ACCEPTED,
            "outstandingAmount" -> 2500.00,
            "originalAmount" -> 2500.00,
            "documentDate" -> "2018-03-29"
          )),
        "financialDetails" -> Json.arr()))


      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "CODINGOUT01")

      verifyIncomeSourceDetailsCall(testMtditid)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual("tax-year-summary.payments.codingOut.text"),
        elementTextBySelector("#coding-out-notice")(codingOutInsetPara),
        elementTextBySelector("#coding-out-message")(codingOutMessageWithStringMessagesArgument(2017, 2018))
      )
    }

    "load the page with any payments you make" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2,
        dunningLock = twoDunningLocks, interestLocks = twoInterestLocks))

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000124")(OK, testChargeHistoryJson(testMtditid, "1040000124", 2500))

      Given("the ChargeHistory feature switch is disabled")
      disable(ChargeHistory)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000124")

      verifyIncomeSourceDetailsCall(testMtditid)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual("chargeSummary.paymentOnAccount1.text"),
        elementTextBySelector("#payment-processing-bullets")(paymentprocessingbullet1)
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
      pageTitleIndividual("chargeSummary.balancingCharge.text")
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
      pageTitleIndividual("chargeSummary.balancingCharge.text")
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
        pageTitleIndividual(titleInternalServer, isErrorPage = true)
      )
    }
  }

  "MFADebits feature on Charge Summary Page" should {
    val financialDetailsUnpaidMFA = Json.obj(
      "balanceDetails" -> Json.obj(
        "balanceDueWithin30Days" -> 1.00,
        "overDueAmount" -> 2.00,
        "totalBalance" -> 3.00
      ),
      "documentDetails" -> Json.arr(
        Json.obj(
          "taxYear" -> testTaxYear,
          "transactionId" -> "1040000123",
          "documentDescription" -> "TRM New Charge",
          "outstandingAmount" -> 1200.00,
          "originalAmount" -> 1200.00,
          "documentDate" -> "2018-03-29",
          "effectiveDateOfPayment" -> "2018-03-30",
          "documentDueDate" -> "2018-03-30"
        )
      ),
      "financialDetails" -> Json.arr(
        Json.obj(
          "taxYear" -> s"$testTaxYear",
          "mainType" -> "ITSA Manual Penalty Pre CY-4",
          "transactionId" -> "1040000123",
          "chargeType" -> ITSA_NI,
          "originalAmount" -> 1200.00,
          "items" -> Json.arr(
            Json.obj("subItem" -> "001",
              "amount" -> 10000,
              "dueDate" -> "2018-03-30"))
        )
      )
    )

    val financialDetailsPaidMFA = Json.obj(
      "balanceDetails" -> Json.obj(
        "balanceDueWithin30Days" -> 1.00,
        "overDueAmount" -> 2.00,
        "totalBalance" -> 3.00
      ),
      "documentDetails" -> Json.arr(
        Json.obj(
          "taxYear" -> testTaxYear,
          "transactionId" -> "1",
          "documentDescription" -> "TRM New Charge",
          "outstandingAmount" -> 0,
          "originalAmount" -> 1200.00,
          "documentDate" -> "2018-03-29",
          "effectiveDateOfPayment" -> "2018-03-30",
          "documentDueDate" -> "2018-03-30"
        ),
        Json.obj(
          "taxYear" -> testTaxYear,
          "transactionId" -> "2",
          "documentDate" -> "2022-04-06",
          "documentDescription" -> "TRM New Charge",
          "documentText" -> "documentText",
          "documentDueDate" -> "2021-04-15",
          "formBundleNumber" -> "88888888",
          "totalAmount" -> 1200,
          "documentOutstandingAmount" -> 1200,
          "statisticalFlag" -> false,
          "paymentLot" -> "MA999991A",
          "paymentLotItem" -> "5",
          "effectiveDateOfPayment" -> "2018-03-30"
        )
      ),
      "financialDetails" -> Json.arr(
        Json.obj(
          "taxYear" -> s"$testTaxYear",
          "mainType" -> "ITSA Manual Penalty Pre CY-4",
          "transactionId" -> "1",
          "chargeType" -> ITSA_NI,
          "originalAmount" -> 1200.00,
          "items" -> Json.arr(
            Json.obj("subItem" -> "001",
              "amount" -> 1200,
              "dueDate" -> "2018-03-30"),
            Json.obj(
              "subItem" -> "002",
              "dueDate" -> "2022-07-28",
              "clearingDate" -> "2022-07-28",
              "amount" -> 1200,
              "paymentReference" -> "GF235687",
              "paymentAmount" -> 1200,
              "paymentMethod" -> "Payment",
              "paymentLot" -> "MA999991A",
              "paymentLotItem" -> "5"
            )
          )
        ),
        Json.obj(
          "taxYear" -> s"$testTaxYear",
          "mainType" -> "Payment on Account",
          "transactionId" -> "2",
          "chargeType" -> ITSA_NI,
          "originalAmount" -> 1200.00,
          "items" -> Json.arr(
            Json.obj("subItem" -> "001",
              "amount" -> 1200,
              "dueDate" -> "2018-03-30"),
            Json.obj(
              "subItem" -> "002",
              "dueDate" -> "2022-07-28",
              "clearingDate" -> "2022-07-28",
              "amount" -> 1200,
              "paymentReference" -> "GF235687",
              "paymentAmount" -> 1200,
              "paymentMethod" -> "Payment",
              "paymentLot" -> "MA999991A",
              "paymentLotItem" -> "5"
            )
          )
        )
      )
    )

    val docDetailUnpaid = DocumentDetail(
      taxYear = 2018,
      transactionId = "1040000124",
      documentDescription = Some("TRM New Charge"),
      documentText = Some("documentText"),
      originalAmount = Some(1200),
      outstandingAmount = Some(1200),
      documentDate = LocalDate.of(2018, 3, 29),
      effectiveDateOfPayment = Some(LocalDate.parse("2018-03-30")),
      documentDueDate = Some(LocalDate.parse("2018-03-30"))
    )
    val docDetailPaid = docDetailUnpaid.copy(outstandingAmount = Some(0))

    "load the charge summary page with an UNPAID MFADebit" in {
      Given("the MFADebitsAndCredits feature switch is enabled")
      enable(MFACreditsAndDebits)
      enable(ChargeHistory)
      enable(PaymentAllocation)

      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, financialDetailsUnpaidMFA)

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000124")(OK, testChargeHistoryJson(testMtditid, "1040000124", 2500))

      val res = IncomeTaxViewChangeFrontend.getChargeSummary(s"$testTaxYear", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      val summaryListText = "Due date OVERDUE 30 March 2018 Full payment amount £1,200.00 Remaining to pay £1,200.00"
      val hmrcCreated = messagesAPI("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
      val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,200.00"

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual("chargeSummary.hmrcAdjustment.text"),
        elementTextBySelector(".govuk-summary-list")(summaryListText),
        elementCountBySelector(s"#payment-link-$testTaxYear")(1),
        elementCountBySelector("#payment-history-table tr")(2),
        elementTextBySelector("#payment-history-table tr")(paymentHistoryText)
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some(Individual), None
        )(FakeRequest()),
        DocumentDetailWithDueDate(
          documentDetail = docDetailUnpaid,
          dueDate = Some(LocalDate.parse("2018-03-30"))
        ),
        paymentBreakdown = List(),
        chargeHistories = List.empty,
        paymentAllocations = List.empty,
        isLatePaymentCharge = false,
        isMFADebit = true,
        taxYear = testTaxYear
      ))

    }

    "load the charge summary page with a PAID MFADebit" in {
      Given("the MFADebitsAndCredits feature switch is enabled")
      enable(MFACreditsAndDebits)
      enable(ChargeHistory)
      enable(PaymentAllocation)

      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, financialDetailsPaidMFA)

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000124")(OK, testChargeHistoryJson(testMtditid, "1040000124", 2500))

      val res = IncomeTaxViewChangeFrontend.getChargeSummary(s"$testTaxYear", "1")

      verifyIncomeSourceDetailsCall(testMtditid)

      val summaryListText = "Due date 30 March 2018 Full payment amount £1,200.00 Remaining to pay £0.00"
      val hmrcCreated = messagesAPI("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
      val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,200.00"
      val paymentHistoryText2 = "28 Jul 2022 Payment put towards HMRC adjustment 2018 £1,200.00"

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual("chargeSummary.hmrcAdjustment.text"),
        elementTextBySelector(".govuk-summary-list")(summaryListText),
        elementCountBySelector(s"#payment-link-$testTaxYear")(0),
        elementCountBySelector("#payment-history-table tr")(3),
        elementTextBySelector("#payment-history-table tr:nth-child(1)")(paymentHistoryText),
        elementTextBySelector("#payment-history-table tr:nth-child(2)")(paymentHistoryText2)
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some(Individual), None
        )(FakeRequest()),
        DocumentDetailWithDueDate(
          documentDetail = docDetailPaid,
          dueDate = Some(LocalDate.parse("2018-03-30"))
        ),
        paymentBreakdown = List(),
        chargeHistories = List.empty,
        paymentAllocations = List.empty,
        isLatePaymentCharge = false,
        isMFADebit = true,
        taxYear = testTaxYear
      ))

    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123"))
    }
  }
}
