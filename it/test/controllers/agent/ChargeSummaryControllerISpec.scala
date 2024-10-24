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

package controllers.agent

import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import com.github.tomakehurst.wiremock.client.WireMock
import config.featureswitch._
import enums.ChargeType.{ITSA_ENGLAND_AND_NI, ITSA_NI, NIC4_SCOTLAND}
import enums.CodingOutType._
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuthStub.titleInternalServer
import helpers.servicemocks.ChargeItemStub.chargeItemWithInterest
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.admin.{ChargeHistory, CodingOut, PaymentAllocation}
import models.chargeHistory.ChargeHistoryModel
import models.chargeSummary.{PaymentHistoryAllocation, PaymentHistoryAllocations}
import models.claimToAdjustPoa.OtherIncomeLower
import models.financialDetails._
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.FinancialDetailsIntegrationTestConstants.financialDetailModelPartial
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.messages.ChargeSummaryMessages._
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate

class ChargeSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    WireMock.reset()
    AuditStub.stubAuditing()
    cache.removeAll()
  }

  override def afterAll(): Unit = {
    stopWiremock()
  }

  val paymentAllocation: List[PaymentHistoryAllocations] = List(
    paymentsWithCharge("SA Payment on Account 1", ITSA_NI, "2020-08-16", -10000.0),
    paymentsWithCharge("SA Payment on Account 1", NIC4_SCOTLAND, "2023-04-05", -9000.0)
  )
  val chargeHistories: List[ChargeHistoryModel] = List(ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 3, 29),
    "ITSA- POA 1", 123456789012345.67, LocalDate.of(2020, 2, 24), "amended return", Some(OtherIncomeLower.code)))
  val paymentBreakdown: List[FinancialDetail] = List(
    financialDetailModelPartial(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, dunningLock = Some("Stand over order"), interestLock = Some("Breathing Space Moratorium Act")),
    financialDetailModelPartial(originalAmount = 123.45, chargeType = NIC4_SCOTLAND, mainType = "SA Payment on Account 2", dunningLock = Some("Dunning Lock"), interestLock = Some("Manual RPI Signal")))
  val currentDate: LocalDate = LocalDate.of(2023, 4, 5)
  val currentYear: Int = currentDate.getYear
  val testArn: String = "1"
  val importantPaymentBreakdown: String = s"${messagesAPI("chargeSummary.dunning.locks.banner.title")} ${messagesAPI("chargeSummary.paymentBreakdown.heading")}"
  val paymentHistory: String = messagesAPI("chargeSummary.chargeHistory.Poa1heading")
  val lpiHistory: String = messagesAPI("chargeSummary.chargeHistory.lateInterestPayment")
  val taxYear: Int = getCurrentTaxYearEnd.getYear


  def paymentsWithCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal): PaymentHistoryAllocations =
    PaymentHistoryAllocations(
      allocations = List(
        PaymentHistoryAllocation(
          amount = Some(amount),
          dueDate = Some(LocalDate.parse(date)),
          clearingSAPDocument = Some("0123456789012"),
          clearingId = Some("PAYID01"))),
      chargeMainType = Some(mainType), chargeType = Some(chargeType))

  s"GET ok" should {
    "load the page with the right data for Payments Breakdown" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2,
        dunningLock = twoDunningLocks, interestLocks = twoInterestLocks))

      stubChargeHistorySuccess()


      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        "2018", "1040000124", clientDetailsWithConfirmation
      )

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      result should have(
        httpStatus(OK),
        pageTitleAgent("chargeSummary.paymentOnAccount1.text"),
        elementTextBySelector("#heading-payment-breakdown")(paymentBreakdownHeading),
        elementTextBySelector("dl:nth-of-type(2) dd span:nth-of-type(2)")(underReview),
        elementTextBySelector("dl:nth-of-type(2) dd div")(notCurrentlyChargingInterest)
      )
    }

    s"return $OK with correct page title and audit events" in {

      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      stubGetFinancialDetailsSuccess(Some(ITSA_NI), Some(NIC4_SCOTLAND))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        taxYear = taxYear.toString, "CHARGEID01", clientDetailsWithConfirmation
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
          None, Some("1234567890"), None, Some(Agent), Some(testArn)
        )(FakeRequest()),
        chargeItemWithInterest(),
        paymentBreakdown = paymentBreakdown,
        chargeHistories = List.empty,
        paymentAllocations = paymentAllocation,
        isLatePaymentCharge = false,
        taxYear = taxYear
      ))

      result should have(
        httpStatus(OK),
        pageTitleAgent("chargeSummary.paymentOnAccount1.text"),
        elementTextBySelector("main h2")(importantPaymentBreakdown)
      )
    }

    s"return $OK with correct page title and audit events when PaymentAllocations FS is enabled" in {

      enable(PaymentAllocation)
      disable(ChargeHistory)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      stubGetFinancialDetailsSuccess(Some(ITSA_NI), Some(NIC4_SCOTLAND))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        taxYear.toString, "CHARGEID01", clientDetailsWithConfirmation
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
          None, Some("1234567890"), None, Some(Agent), Some(testArn)
        )(FakeRequest()),
        chargeItemWithInterest(),
        paymentBreakdown = paymentBreakdown,
        chargeHistories = List.empty,
        paymentAllocations = paymentAllocation,
        isLatePaymentCharge = false,
        taxYear = taxYear
      ))

      result should have(
        httpStatus(OK),
        pageTitleAgent("chargeSummary.paymentOnAccount1.text"),
        elementTextBySelector("main h2")(importantPaymentBreakdown)
      )
    }

    s"return $OK with correct page title and audit events when ChargeHistory and PaymentAllocation FSs are enabled" in {
      enable(ChargeHistory)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
      stubGetFinancialDetailsSuccess(Some(ITSA_NI), Some(NIC4_SCOTLAND))
      stubChargeHistorySuccess()

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        taxYear.toString, "CHARGEID01", clientDetailsWithConfirmation
      )

      result should have(
        httpStatus(OK),
        pageTitleAgent("chargeSummary.paymentOnAccount1.text"),
        elementTextBySelector("main h2")(importantPaymentBreakdown),
        elementTextBySelector("main h3")(paymentHistory)
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
          None, Some("1234567890"), None, Some(Agent), Some(testArn)
        )(FakeRequest()),
        chargeItemWithInterest(),
        paymentBreakdown = paymentBreakdown,
        chargeHistories = chargeHistories,
        paymentAllocations = paymentAllocation,
        isLatePaymentCharge = false,
        taxYear = taxYear
      ))
    }

    s"return $OK with correct page title and audit events when ChargeHistory and PaymentAllocation FSs are enabled and LPI set to true" in {
      enable(ChargeHistory)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
      stubGetFinancialDetailsSuccess()

      val result = IncomeTaxViewChangeFrontend.getChargeSummaryLatePayment(
        taxYear.toString, "CHARGEID01", clientDetailsWithConfirmation
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
          None, Some("1234567890"), None, Some(Agent), Some(testArn)
        )(FakeRequest()),
        chargeItemWithInterest(),
        paymentBreakdown = List.empty,
        chargeHistories = List.empty,
        paymentAllocations = paymentAllocation,
        isLatePaymentCharge = true,
        taxYear = taxYear
      ))

      result should have(
        httpStatus(OK),
        pageTitleAgent("chargeSummary.lpi.paymentOnAccount1.text"),
        elementTextBySelector("main h2")(lpiHistory)
      )
    }

    "load the page with coding out details when coding out is enable and a coded out documentDetail id is passed" in {

      Given("the CodingOut feature switch is enabled")
      enable(CodingOut)
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
      stubChargeHistorySuccess()
      stubGetFinancialDetailsSuccessForCodingOut()

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        getCurrentTaxYearEnd.getYear.toString, "CODINGOUT01", clientDetailsWithConfirmation
      )

      verifyIncomeSourceDetailsCall(testMtditid)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      result should have(
        httpStatus(OK),
        pageTitleAgent("tax-year-summary.payments.codingOut.text"),
        elementTextBySelector("#coding-out-notice")(codingOutInsetPara),
        elementTextBySelector("#coding-out-message")(codingOutMessageWithStringMessagesArgument(getCurrentTaxYearEnd.getYear - 1, getCurrentTaxYearEnd.getYear))
      )
    }

    s"return $OK with correct page title and ChargeHistory FS is enabled and the charge history details API responds with a $NOT_FOUND" in {
      enable(ChargeHistory)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
      stubGetFinancialDetailsSuccess()

      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(NOT_FOUND, Json.parse(
        """
          |{
          |   "code": "NO_DATA_FOUND",
          |   "reason": "The remote endpoint has indicated that no match found for the reference provided."
          |}
          |""".stripMargin))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        getCurrentTaxYearEnd.getYear.toString, "CHARGEID01", clientDetailsWithConfirmation
      )

      result should have(
        httpStatus(OK),
        pageTitleAgent("chargeSummary.paymentOnAccount1.text")
      )
    }

    s"return $OK with correct page title and ChargeHistory FS is enabled and the charge history details API responds with a $FORBIDDEN" in {
      enable(ChargeHistory)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
      stubGetFinancialDetailsSuccess()

      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(FORBIDDEN, Json.parse(
        """
          |{
          |   "code": "REQUEST_NOT_PROCESSED",
          |   "reason": "The remote endpoint has indicated that request could not be processed."
          |}
          |""".stripMargin))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        getCurrentTaxYearEnd.getYear.toString, "CHARGEID01", clientDetailsWithConfirmation
      )

      result should have(
        httpStatus(OK),
        pageTitleAgent("chargeSummary.paymentOnAccount1.text")
      )
    }

    "return a technical difficulties page to the user" when {
      "ChargeHistory FS is enabled and the charge history details API responded with an error" in {
        enable(ChargeHistory)
        enable(PaymentAllocation)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
        stubGetFinancialDetailsSuccess()

        IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(INTERNAL_SERVER_ERROR, Json.parse(
          """
            |{
            |   "code": "SERVER_ERROR",
            |   "reason": "DES is currently experiencing problems that require live service intervention."
            |}
            |""".stripMargin))

        val result = IncomeTaxViewChangeFrontend.getChargeSummary(
          getCurrentTaxYearEnd.getYear.toString, "CHARGEID01", clientDetailsWithConfirmation
        )

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleAgent(titleInternalServer, isErrorPage = true)
        )
      }
    }

    "load the page with any payments you make" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2,
        dunningLock = twoDunningLocks, interestLocks = twoInterestLocks))

      stubChargeHistorySuccess()


      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        "2018", "1040000124", clientDetailsWithConfirmation
      )

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      result should have(
        httpStatus(OK),
        pageTitleAgent("chargeSummary.paymentOnAccount1.text"),
      )
    }
  }

  private def stubGetFinancialDetailsSuccess(chargeType1: Option[String] = Some(ITSA_NI),
                                             chargeType2: Option[String] = Some(ITSA_NI), isLatePaymentInterest: Boolean = false): Unit = {
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
      nino = testNino,
      from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
      to = getCurrentTaxYearEnd.toString
    )(
      status = OK,
      response = Json.toJson(FinancialDetailsModel(
        balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
        documentDetails = List(
          DocumentDetail(
            taxYear = getCurrentTaxYearEnd.getYear,
            transactionId = "CHARGEID01",
            documentDescription = Some("ITSA- POA 1"),
            documentText = Some("documentText"),
            outstandingAmount = 1.2,
            originalAmount = 123.45,
            documentDate = LocalDate.of(2018, 3, 29),
            interestFromDate = Some(LocalDate.of(2018, 4, 14)),
            interestEndDate = Some(LocalDate.of(2019, 1, 1)),
            latePaymentInterestAmount = Some(54.32),
            interestOutstandingAmount = Some(42.5),
            effectiveDateOfPayment = Some(LocalDate.of(2023, 7, 1)),
            documentDueDate = Some(LocalDate.of(2023, 7, 1))
          ),
          DocumentDetail(
            taxYear = 9999,
            transactionId =  "PAYID01",
            documentDescription =  Some("Payment On Account"),
            documentText = Some("documentText"),
            outstandingAmount =  -1.2,
            originalAmount =  -123.45,
            documentDate =  LocalDate.of(2018, 3, 29),
            paymentLot =  Some("081203010024"),
            paymentLotItem =  Some("000001"),
            effectiveDateOfPayment = Some(LocalDate.of(2023, 7, 1)),
            documentDueDate = Some(LocalDate.of(2023, 7, 1))
          )
        ),
        financialDetails = List(
          FinancialDetail(
            taxYear = getCurrentTaxYearEnd.getYear.toString,
            transactionId = Some("CHARGEID01"),
            mainType = Some("SA Payment on Account 1"),
            mainTransaction = Some("4920"),
            chargeReference = Some("ABCD1234"),
            chargeType = chargeType1,
            originalAmount = Some(123.45),
            items = Some(Seq(SubItem(Some(LocalDate.parse("2020-08-16")),
              amount = Some(10000),
              dunningLock = Some("Stand over order"),
              interestLock = Some("Manual RPI Signal"),
              clearingDate = Some(LocalDate.parse("2019-08-13")),
              clearingSAPDocument = Some("012345678912"))))
          ),
          FinancialDetail(
            taxYear = getCurrentTaxYearEnd.getYear.toString,
            transactionId = Some("CHARGEID01"),
            mainType = Some("SA Payment on Account 1"),
            mainTransaction = Some("4920"),
            chargeReference = Some("ABCD1234"),
            chargeType = chargeType2,
            originalAmount = Some(123.45),
            items = Some(Seq(SubItem(Some(currentDate),
              amount = Some(9000),
              dunningLock = Some("dunning lock"),
              interestLock = Some("Manual RPI Signal"),
              clearingDate = Some(LocalDate.parse("2019-08-13")),
              clearingSAPDocument = Some("012345678912"))))
          ),
          FinancialDetail(
            taxYear = getCurrentTaxYearEnd.getYear.toString,
            transactionId = Some("PAYID01"),
            chargeReference = Some("ABCD1234"),
            mainType = Some("Payment"),
            mainTransaction = Some("0060"),
            originalAmount = Some(123.45),
            items = Some(Seq(SubItem(
              Some(currentDate),
              amount = Some(9000),
              dunningLock = Some("dunning lock"),
              interestLock = Some("Manual RPI Signal"),
              clearingDate = Some(LocalDate.parse("2019-08-13")),
              paymentLot = Some("paymentLot"),
              paymentLotItem = Some("000001"),
              clearingSAPDocument = Some("012345678912"))))
          )
        )
      ))
    )
  }

  private def stubGetFinancialDetailsSuccessForCodingOut(): Unit = {
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
      nino = testNino,
      from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
      to = getCurrentTaxYearEnd.toString)(
      status = OK,
      response = Json.toJson(FinancialDetailsModel(
        balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
        documentDetails = List(
          DocumentDetail(
            taxYear = getCurrentTaxYearEnd.getYear,
            transactionId = "CODINGOUT01",
            documentDescription = Some("TRM New Charge"),
            documentText = Some(CODING_OUT_ACCEPTED),
            outstandingAmount = 2500.00,
            originalAmount = 2500.00,
            documentDate = LocalDate.of(2018, 3, 29),
            interestFromDate = Some(LocalDate.of(2018, 4, 14))
          )
        ),
        financialDetails = List(
          FinancialDetail(
            taxYear = getCurrentTaxYearEnd.getYear.toString,
            transactionId = Some("CODINGOUT01"),
            mainType = Some("SA Balancing Charge"),
            mainTransaction = Some("4910"),
            chargeType = Some(ITSA_NI),
            originalAmount = Some(123.45),
            items = Some(Seq(SubItem(Some(currentDate),
              amount = Some(10000), clearingDate = Some(LocalDate.parse("2019-08-13")))))
          )
        )
      ))
    )
  }

  private def stubChargeHistorySuccess() = {
    IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, Json.obj(
      "idType" -> "NINO",
      "idValue" -> testNino,
      "regimeType" -> "ITSA",
      "chargeHistoryDetails" -> Json.arr(
        Json.obj(
          "taxYear" -> getCurrentTaxYearEnd.getYear.toString,
          "documentId" -> "CHARGEID01",
          "documentDate" -> "2018-03-29",
          "documentDescription" -> "ITSA- POA 1",
          "totalAmount" -> 123456789012345.67,
          "reversalDate" -> "2020-02-24",
          "reversalReason" -> "amended return",
          "poaAdjustmentReason" -> OtherIncomeLower.code
        ))))
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

    "load the charge summary page with an UNPAID MFADebit" in {
      enable(ChargeHistory)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)

      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, financialDetailsUnpaidMFA)

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))

      val res = IncomeTaxViewChangeFrontend.getChargeSummary(s"$testTaxYear", "1040000123", clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)

      val summaryListText = "Due date OVERDUE 30 March 2018 Amount £1,200.00 Still to pay £1,200.00"
      val hmrcCreated = messagesAPI("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
      val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,200.00"

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleAgent("chargeSummary.hmrcAdjustment.text"),
        elementTextBySelector(".govuk-summary-list")(summaryListText),
        elementCountBySelector(s"#payment-link-$testTaxYear")(0),
        elementCountBySelector("#payment-history-table tr")(2),
        elementTextBySelector("#payment-history-table tr")(paymentHistoryText)
      )

    }

    "load the charge summary page with a PAID MFADebit" in {
      enable(ChargeHistory)
      disable(CodingOut)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)

      Given("I wiremock stub a successful Income Source Details response with property only")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, financialDetailsPaidMFA)

      And("I wiremock stub a charge history response")
      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))

      val res = IncomeTaxViewChangeFrontend.getChargeSummary(s"$testTaxYear", "1", clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)

      val summaryListText = "Due date 30 March 2018 Amount £1,200.00 Still to pay £0.00"
      val hmrcCreated = messagesAPI("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
      val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,200.00"
      val paymentHistoryText2 = "28 Jul 2022 Payment put towards HMRC adjustment 2018 £1,200.00"

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      res should have(
        httpStatus(OK),
        pageTitleAgent("chargeSummary.hmrcAdjustment.text"),
        elementTextBySelector(".govuk-summary-list")(summaryListText),
        elementCountBySelector(s"#payment-link-$testTaxYear")(0),
        elementCountBySelector("#payment-history-table tr")(3),
        elementTextBySelector("#payment-history-table tr:nth-child(1)")(paymentHistoryText),
        elementTextBySelector("#payment-history-table tr:nth-child(2)")(paymentHistoryText2)
      )

    }
  }


  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getChargeSummary(
          getCurrentTaxYearEnd.getYear.toString, "CHARGEID01", clientDetailsWithConfirmation
        ))
    }
  }
}
