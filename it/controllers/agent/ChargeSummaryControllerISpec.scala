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
import config.featureswitch._
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuthStub.titleInternalServer
import helpers.servicemocks.DocumentDetailsStub.docDateDetailWithInterest
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.chargeHistory.ChargeHistoryModel
import models.financialDetails._
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.FinancialDetailsIntegrationTestConstants.financialDetailModelPartial
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.messages.ChargeSummaryMessages.{codingOutInsetPara, codingOutMessage, notCurrentlyChargingInterest, paymentBreakdownHeading, underReview}

import java.time.LocalDate


class ChargeSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {


  def paymentsWithCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal, lotItem: String): PaymentsWithChargeType =
    PaymentsWithChargeType(
      payments = List(Payment(reference = Some("reference"), amount = Some(amount), method = Some("method"),
        lot = Some("lot"), lotItem = Some(lotItem), date = Some(date), documentDescription = None,  transactionId = None)),
      mainType = Some(mainType), chargeType = Some(chargeType))

  val paymentAllocation: List[PaymentsWithChargeType] = List(
    paymentsWithCharge("SA Payment on Account 1", "ITSA NI", "2019-08-13", -10000.0, lotItem = "000001"),
    paymentsWithCharge("SA Payment on Account 2", "NIC4 Scotland", "2019-08-13", -9000.0, lotItem = "000001")
  )

  val chargeHistories: List[ChargeHistoryModel] = List(ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 3, 29).toString,
    "ITSA- POA 1", 123456789012345.67, LocalDate.of(2020, 2, 24), "amended return"))

  val paymentBreakdown: List[FinancialDetail] = List(
    financialDetailModelPartial(originalAmount = 123.45, chargeType = "ITSA England & NI", dunningLock = Some("Stand over order"), interestLock = Some("Breathing Space Moratorium Act")),
    financialDetailModelPartial(originalAmount = 123.45, chargeType = "NIC4 Scotland", mainType = "SA Payment on Account 2", dunningLock = Some("Dunning Lock"), interestLock = Some("Manual RPI Signal")))

  

  val currentYear: Int = LocalDate.now().getYear
  val testArn: String = "1"

  val importantPaymentBreakdown: String = s"${messagesAPI("chargeSummary.dunning.locks.banner.title")} ${messagesAPI("chargeSummary.paymentBreakdown.heading")}"
  val paymentHistory: String = messagesAPI("chargeSummary.chargeHistory.heading")

  s"GET ok" should {
    "load the page with right data for Payments Breakdown" in {
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
        elementTextBySelector("dl:nth-of-type(2) dd span")(underReview),
        elementTextBySelector("dl:nth-of-type(2) dd div")(notCurrentlyChargingInterest)
      )
    }

    s"return $OK with correct page title and audit events" in {

      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      stubGetFinancialDetailsSuccess(Some("ITSA NI"), Some("NIC4 Scotland"))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        getCurrentTaxYearEnd.getYear.toString, "testId", clientDetailsWithConfirmation
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
          None, Some("1234567890"), None, Some("Agent"), Some(testArn)
        )(FakeRequest()),
        docDateDetailWithInterest(LocalDate.of(2019, 1, 1).toString, "ITSA- POA 1"),
        paymentBreakdown = paymentBreakdown,
        chargeHistories = List.empty,
        paymentAllocations = List.empty,
        isLatePaymentCharge = false
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

      stubGetFinancialDetailsSuccess(Some("ITSA NI"), Some("NIC4 Scotland"))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        getCurrentTaxYearEnd.getYear.toString, "testId", clientDetailsWithConfirmation
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
          None, Some("1234567890"), None, Some("Agent"), Some(testArn)
        )(FakeRequest()),
        docDateDetailWithInterest(LocalDate.of(2019, 1, 1).toString, "ITSA- POA 1"),
        paymentBreakdown = paymentBreakdown,
        chargeHistories = List.empty,
        paymentAllocations = paymentAllocation,
        isLatePaymentCharge = false
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
      stubGetFinancialDetailsSuccess(Some("ITSA NI"), Some("NIC4 Scotland"))
      stubChargeHistorySuccess()

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        getCurrentTaxYearEnd.getYear.toString, "testId", clientDetailsWithConfirmation
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
          None, Some("1234567890"), None, Some("Agent"), Some(testArn)
        )(FakeRequest()),
        docDateDetailWithInterest(LocalDate.of(2019, 1, 1).toString, "ITSA- POA 1"),
        paymentBreakdown = paymentBreakdown,
        chargeHistories = chargeHistories,
        paymentAllocations = paymentAllocation,
        isLatePaymentCharge = false
      ))
    }

    s"return $OK with correct page title and audit events when ChargeHistory and PaymentAllocation FSs are enabled and LPI set to true" in {
      enable(ChargeHistory)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
      stubGetFinancialDetailsSuccess()

      val result = IncomeTaxViewChangeFrontend.getChargeSummaryLatePayment(
        getCurrentTaxYearEnd.getYear.toString, "testId", clientDetailsWithConfirmation
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
          None, Some("1234567890"), None, Some("Agent"), Some(testArn)
        )(FakeRequest()),
        docDateDetailWithInterest(LocalDate.of(2019, 1, 1).toString, "ITSA- POA 1"),
        paymentBreakdown = List.empty,
        chargeHistories = List.empty,
        paymentAllocations = paymentAllocation,
        isLatePaymentCharge = true
      ))

      result should have(
        httpStatus(OK),
        pageTitleAgent("chargeSummary.lpi.paymentOnAccount1.text"),
        elementTextBySelector("main h2")(paymentHistory)
      )
    }

    "load the page with coding out details when coding out is enable and a coded out documentDetail id is passed" in {

      Given("the CodingOut feature switch is enabled")
      enable(CodingOut)
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
      stubChargeHistorySuccess()
      stubGetFinancialDetailsSuccessForCodingOut

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        getCurrentTaxYearEnd.getYear.toString, "CODINGOUT01", clientDetailsWithConfirmation
      )

      verifyIncomeSourceDetailsCall(testMtditid)

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      result should have(
        httpStatus(OK),
        pageTitleAgent("tax-year-summary.payments.codingOut.text"),
        elementTextBySelector("#coding-out-notice")(codingOutInsetPara),
        elementTextBySelector("#coding-out-message")(codingOutMessage(getCurrentTaxYearEnd.getYear - 1, getCurrentTaxYearEnd.getYear))
      )
    }

    s"return $OK with correct page title and ChargeHistory FS is enabled and the charge history details API responds with a $NOT_FOUND" in {
      enable(ChargeHistory)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
      stubGetFinancialDetailsSuccess()

      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "testId")(NOT_FOUND, Json.parse(
        """
          |{
          |   "code": "NO_DATA_FOUND",
          |   "reason": "The remote endpoint has indicated that no match found for the reference provided."
          |}
          |""".stripMargin))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        getCurrentTaxYearEnd.getYear.toString, "testId", clientDetailsWithConfirmation
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

      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "testId")(FORBIDDEN, Json.parse(
        """
          |{
          |   "code": "REQUEST_NOT_PROCESSED",
          |   "reason": "The remote endpoint has indicated that request could not be processed."
          |}
          |""".stripMargin))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        getCurrentTaxYearEnd.getYear.toString, "testId", clientDetailsWithConfirmation
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

        IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "testId")(INTERNAL_SERVER_ERROR, Json.parse(
          """
            |{
            |   "code": "SERVER_ERROR",
            |   "reason": "DES is currently experiencing problems that require live service intervention."
            |}
            |""".stripMargin))

        val result = IncomeTaxViewChangeFrontend.getChargeSummary(
          getCurrentTaxYearEnd.getYear.toString, "testId", clientDetailsWithConfirmation
        )

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleAgent(titleInternalServer)
        )
      }
    }

  }

  private def stubGetFinancialDetailsSuccess(chargeType1: Option[String] = Some("ITSA NI"),
                                             chargeType2: Option[String] = Some("ITSA NI"), isLatePaymentInterest: Boolean = false): Unit = {
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
      nino = testNino,
      from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
      to = getCurrentTaxYearEnd.toString
    )(
      status = OK,
      response = Json.toJson(FinancialDetailsModel(
        balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
        documentDetails = List(
          DocumentDetail(
            taxYear = getCurrentTaxYearEnd.getYear.toString,
            transactionId = "testId",
            documentDescription = Some("ITSA- POA 1"),
            documentText = Some("documentText"),
            outstandingAmount = Some(1.2),
            originalAmount = Some(123.45),
            documentDate = LocalDate.of(2018, 3, 29),
            interestFromDate = Some(LocalDate.of(2018, 4, 14)),
            interestEndDate = Some(LocalDate.of(2019, 1, 1)),
            latePaymentInterestAmount = Some(54.32),
            interestOutstandingAmount = Some(42.5)
          )
        ),
        financialDetails = List(
          FinancialDetail(
            taxYear = getCurrentTaxYearEnd.getYear.toString,
            transactionId = Some("testId"),
            mainType = Some("SA Payment on Account 1"),
            chargeType = chargeType1,
            originalAmount = Some(123.45),
            items = Some(Seq(SubItem(Some(LocalDate.now.toString), paymentLotItem = Some("000001"), paymentLot = Some("paymentLot"),
              amount = Some(10000), clearingDate = Some("2019-08-13"), dunningLock = Some("Stand over order"), interestLock = Some("Manual RPI Signal"))))
          ),
          FinancialDetail(
            taxYear = getCurrentTaxYearEnd.getYear.toString,
            transactionId = Some("testId"),
            mainType = Some("SA Payment on Account 2"),
            chargeType = chargeType2,
            originalAmount = Some(123.45),
            items = Some(Seq(SubItem(Some(LocalDate.now.toString), paymentLotItem = Some("000001"), paymentLot = Some("paymentLot"),
              amount = Some(9000), clearingDate = Some("2019-08-13"), dunningLock = Some("dunning lock"), interestLock = Some("Manual RPI Signal"))))
          )
        ),
        codingDetails = None
      ))
    )
  }

  private def stubGetFinancialDetailsSuccessForCodingOut: Unit = {
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
      nino = testNino,
      from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
      to = getCurrentTaxYearEnd.toString)(
      status = OK,
      response = Json.toJson(FinancialDetailsModel(
        balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
        documentDetails = List(
          DocumentDetail(
            taxYear = getCurrentTaxYearEnd.getYear.toString,
            transactionId = "CODINGOUT01",
            documentDescription = Some("TRM New Charge"),
            documentText = Some("PAYE Self Assessment"),
            outstandingAmount = Some(2500.00),
            originalAmount = Some(2500.00),
            documentDate = LocalDate.of(2018, 3, 29),
            interestFromDate = Some(LocalDate.of(2018, 4, 14))
          )
        ),
        financialDetails = List(
          FinancialDetail(
            taxYear = getCurrentTaxYearEnd.getYear.toString,
            transactionId = Some("CODINGOUT01"),
            mainType = Some("SA Payment on Account 1"),
            chargeType = Some("ITSA NI"),
            originalAmount = Some(123.45),
            items = Some(Seq(SubItem(Some(LocalDate.now.toString),
              amount = Some(10000), clearingDate = Some("2019-08-13"))))
          )
        ),
        codingDetails = Some(List(CodingDetails(taxYearReturn = "2021", amountCodedOut = 2500, taxYearCoding = "2020")))
      ))
    )
  }

  private def stubChargeHistorySuccess() = {
    IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "testId")(OK, Json.obj(
      "idType" -> "MTDBSA",
      "idValue" -> testMtditid,
      "regimeType" -> "ITSA",
      "chargeHistoryDetails" -> Json.arr(
        Json.obj(
          "taxYear" -> getCurrentTaxYearEnd.getYear.toString,
          "documentId" -> "testId",
          "documentDate" -> "2018-03-29",
          "documentDescription" -> "ITSA- POA 1",
          "totalAmount" -> 123456789012345.67,
          "reversalDate" -> "2020-02-24",
          "reversalReason" -> "amended return"
        ))))
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getChargeSummary(
          getCurrentTaxYearEnd.getYear.toString, "testId", clientDetailsWithConfirmation
        ))
    }
  }
}
