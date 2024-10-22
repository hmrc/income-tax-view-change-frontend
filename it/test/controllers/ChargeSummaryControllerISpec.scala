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
import enums.ChargeType.{ITSA_ENGLAND_AND_NI, ITSA_NI, NIC4_SCOTLAND}
import enums.CodingOutType._
import helpers.ComponentSpecBase
import helpers.servicemocks.ChargeItemStub.{chargeItemWithInterestAndOverdue, docDetail}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.admin.{ChargeHistory, CodingOut, PaymentAllocation}
import models.chargeHistory.ChargeHistoryModel
import models.chargeSummary.{PaymentHistoryAllocation, PaymentHistoryAllocations}
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.DateService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testTaxYear}
import testConstants.FinancialDetailsIntegrationTestConstants.financialDetailModelPartial
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.messages.ChargeSummaryMessages._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate

class ChargeSummaryControllerISpec extends ComponentSpecBase {

  override implicit val dateService: DateService = app.injector.instanceOf[DateService]

  val paymentAllocation: List[PaymentHistoryAllocations] = List(
    paymentsWithCharge("SA Balancing Charge", ITSA_NI, dateService.getCurrentDate.plusDays(20).toString, -10000.0),
    paymentsWithCharge("SA Payment on Account 1", NIC4_SCOTLAND, dateService.getCurrentDate.plusDays(20).toString, -9000.0),
    paymentsWithCharge("SA Payment on Account 2", NIC4_SCOTLAND, dateService.getCurrentDate.plusDays(20).toString, -8000.0)
  )
  val chargeHistories: List[ChargeHistoryModel] = List(ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 2, 14), "ITSA- POA 1", 2500, LocalDate.of(2019, 2, 14), "Customer Request", Some("001")))
  val paymentBreakdown: List[FinancialDetail] = List(
    financialDetailModelPartial(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, mainType = "SA Balancing Charge", dunningLock = Some("Dunning Lock"), interestLock = Some("Interest Lock")),
    financialDetailModelPartial(originalAmount = 123.45, chargeType = NIC4_SCOTLAND, dunningLock = Some("Stand over order"), interestLock = Some("Breathing Space Moratorium Act")),
    financialDetailModelPartial(originalAmount = 123.45, chargeType = NIC4_SCOTLAND, mainType = "SA Payment on Account 2", dunningLock = Some("Dunning Lock"), interestLock = Some("Manual RPI Signal")))
  val importantPaymentBreakdown: String = s"${messagesAPI("chargeSummary.dunning.locks.banner.title")} ${messagesAPI("chargeSummary.paymentBreakdown.heading")}"
  val paymentHistory: String = messagesAPI("chargeSummary.chargeHistory.heading")
  val lpiHistory: String = messagesAPI("chargeSummary.chargeHistory.lateInterestPayment")

  def paymentsWithCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal): PaymentHistoryAllocations =
    PaymentHistoryAllocations(
      allocations = List(PaymentHistoryAllocation(
        amount = Some(amount),
        dueDate = Some(LocalDate.parse(date)),
        clearingSAPDocument = Some("012345678901"), clearingId = Some("012345678901"))),
      chargeMainType = Some(mainType), chargeType = Some(chargeType))

  "Navigating to the Charge Summary Page" should {

    "load the page with right data for Payments Breakdown" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2,
        dunningLock = twoDunningLocks, interestLocks = twoInterestLocks))

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))

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
        docDetail(PaymentOnAccountOne),
        paymentBreakdown = List(financialDetailModelPartial(chargeType = ITSA_ENGLAND_AND_NI, originalAmount = 10.34, dunningLock = Some("Stand over order"), interestLock = Some("Breathing Space Moratorium Act"))),
        chargeHistories = List.empty,
        paymentAllocations = List.empty,
        isLatePaymentCharge = false,
        taxYear = testTaxYear
      )(dateService))
    }

    "load the page with right audit events when PaymentAllocations FS on and ChargeHistory FS off" in {
      Given("the PaymentAllocations feature switch is on and ChargeHistory is off")
      enable(PaymentAllocation)
      disable(ChargeHistory)

      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testAuditFinancialDetailsModelJson(123.45, 1.2,
        dunningLock = oneDunningLock, interestLocks = twoInterestLocks, latePaymentInterestAmount = None,
        dueDate = dateService.getCurrentDate.plusDays(20).toString))

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some(Individual), None
        )(FakeRequest()),
        chargeItemWithInterestAndOverdue(BalancingCharge, None,
          Some(dateService.getCurrentDate.plusDays(20))),
        paymentBreakdown = paymentBreakdown,
        chargeHistories = List.empty,
        paymentAllocations = paymentAllocation,
        isLatePaymentCharge = false,
        taxYear = testTaxYear
      )(dateService))

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

      val json = testAuditFinancialDetailsModelJson(123.45, 1.2,
        dunningLock = oneDunningLock, interestLocks = twoInterestLocks, latePaymentInterestAmount = None, dueDate =
          dateService.getCurrentDate.plusDays(20).toString)
      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, json)

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))

      Given("the PaymentAllocations and ChargeHistory feature switch is on")
      enable(PaymentAllocation)
      enable(ChargeHistory)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      val x = chargeItemWithInterestAndOverdue(BalancingCharge, None, Some(dateService.getCurrentDate.plusDays(20)))

      val expectedAuditEvent = ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some(Individual), None
        )(FakeRequest()),
        x,
        paymentBreakdown = paymentBreakdown,
        chargeHistories = chargeHistories,
        paymentAllocations = paymentAllocation,
        isLatePaymentCharge = false,
        taxYear = testTaxYear
      )(dateService)

      AuditStub.verifyAuditEvent(expectedAuditEvent)

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

      AuditStub.verifyAuditEvent(
        ChargeSummaryAudit(
          mtdItUser = MtdItUser(
            testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
            Some("12345-credId"), Some(Individual), None
          )(FakeRequest()),
          chargeItem = chargeItemWithInterestAndOverdue(BalancingCharge, None, Some(LocalDate.of(2018, 2, 14))),
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
        elementTextBySelector("main h2")(lpiHistory),
        elementTextBySelector("tbody tr:nth-child(1) td:nth-child(2)")(lpiCreated)
      )
    }

    "load the page when the late payment interest flag is true and paymentAllocation FS is enabled but chargeHistory FS is disabled" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelWithPaymentAllocationJson(10.34, 1.2))

      disable(ChargeHistory)
      enable(PaymentAllocation)

      val res = IncomeTaxViewChangeFrontend.getChargeSummaryLatePayment("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual("chargeSummary.lpi.balancingCharge.text"),
        elementTextBySelector("main h2")(lpiHistory),
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
        "financialDetails" -> Json.arr(
          Json.obj(
            "transactionId" -> "1040001234",
            "taxYear" -> "2018",
            "mainTransaction" -> "4930"
          )
        )))

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
        "financialDetails" -> Json.arr(
          Json.obj(
            "transactionId" -> "CODINGOUT01",
            "taxYear" -> "2018",
            "mainTransaction" -> "4910"
          )
        )))


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

    "load the page with any payments you make if the charge is not a Review & Reconcile or POA charge" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2,
        dunningLock = twoDunningLocks, interestLocks = twoInterestLocks))

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))

      Given("the ChargeHistory feature switch is disabled")
      disable(ChargeHistory)

      val res = IncomeTaxViewChangeFrontend.getChargeSummary("2018", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual("chargeSummary.balancingCharge.text"),
      )
    }
  }

  s"return $OK with correct page title and ChargeHistory FS is enabled and the charge history details API responds with a $NOT_FOUND" in {
    enable(ChargeHistory)
    enable(PaymentAllocation)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2))

    IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(NOT_FOUND, Json.parse(
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

    IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(FORBIDDEN, Json.parse(
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

      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(INTERNAL_SERVER_ERROR, Json.parse(
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

    "When Original Amount value is missing from financial details / document details" in {

      enable(ChargeHistory)
      enable(PaymentAllocation)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testFinancialDetailsModelWithMissingOriginalAmountJson())

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
          "mainTransaction" -> "4002",
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
          "outstandingAmount" -> 0,
          "originalAmount" -> 1200.00,
          "documentText" -> "documentText",
          "documentDueDate" -> "2021-04-15",
          "formBundleNumber" -> "88888888",
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
          "mainTransaction" -> "4002",
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
              "clearingSAPDocument" -> "012345678912"
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
              "paymentLotItem" -> "5",
              "clearingSAPDocument" -> "012345678912"
            )
          )
        )
      )
    )

    val chargeItemUnpaid: ChargeItem = ChargeItem(
      transactionId = "1040000124",
      taxYear = TaxYear.forYearEnd(2018),
      transactionType = MfaDebitCharge,
      subTransactionType = None,
      documentDate = LocalDate.of(2018, 3, 29),
      dueDate = Some(LocalDate.parse("2018-03-30")),
      originalAmount = 1200,
      outstandingAmount = 1200,
      interestOutstandingAmount = None,
      latePaymentInterestAmount = None,
      interestFromDate = None,
      interestEndDate = None,
      interestRate = None,
      lpiWithDunningLock = None,
      amountCodedOut = None,
      dunningLock = false
    )

    val chargeItemPaid = chargeItemUnpaid.copy(outstandingAmount = 0)

    "load the charge summary page with an UNPAID MFADebit" in {
      enable(ChargeHistory)
      enable(PaymentAllocation)

      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, financialDetailsUnpaidMFA)

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))

      val res = IncomeTaxViewChangeFrontend.getChargeSummary(s"$testTaxYear", "1040000123")

      verifyIncomeSourceDetailsCall(testMtditid)

      val summaryListText = "Due date OVERDUE 30 March 2018 Amount £1,200.00 Still to pay £1,200.00"
      val hmrcCreated = messagesAPI("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
      val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,200.00"

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleIndividual("chargeSummary.hmrcAdjustment.text"),
        elementTextBySelector(".govuk-summary-list")(summaryListText),
        elementCountBySelector("#payment-history-table tr")(2),
        elementTextBySelector("#payment-history-table tr")(paymentHistoryText)
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some(Individual), None
        )(FakeRequest()),
        chargeItemUnpaid.copy(
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
      enable(ChargeHistory)
      enable(PaymentAllocation)

      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, financialDetailsPaidMFA)

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))

      val res = IncomeTaxViewChangeFrontend.getChargeSummary(s"$testTaxYear", "1")

      verifyIncomeSourceDetailsCall(testMtditid)

      val summaryListText = "Due date 30 March 2018 Amount £1,200.00 Still to pay £0.00"
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
        chargeItemPaid.copy(
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
}
