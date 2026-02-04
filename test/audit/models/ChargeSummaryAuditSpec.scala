/*
 * Copyright 2023 HM Revenue & Customs
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

package audit.models

import authV2.AuthActionsTestData.{defaultMTDITUser, getMinimalMTDITUser}
import enums.ChargeType.*
import enums.CodingOutType.{CODING_OUT_ACCEPTED, CODING_OUT_CANCELLED}
import forms.IncomeSourcesFormsSpec.commonAuditDetails
import models.chargeHistory.ChargeHistoryModel
import models.chargeSummary.{PaymentHistoryAllocation, PaymentHistoryAllocations}
import models.financialDetails.*
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json._
import play.api.test.FakeRequest
import services.DateService
import testConstants.BaseTestConstants.*
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants.{MFADebitsDocumentDetails, financialDetail}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.{LocalDate, LocalDateTime, LocalTime}

class ChargeSummaryAuditSpec extends AnyWordSpecLike with Matchers with PaymentSharedFunctions with ChargeConstants {

  def normalise(js: JsValue): JsValue = js match {

    case JsObject(fields) =>
      JsObject(
        fields.collect { case (k, v) if v != JsNull => k -> normalise(v) }
          .toSeq.sortBy(_._1)
      )

    case JsArray(values) =>
      val normValues = values.collect { case v if v != JsNull => normalise(v) }
      if (normValues.forall(_.isInstanceOf[JsObject])) {
        JsArray(normValues.sortBy(_.toString))
      } else {
        JsArray(normValues)
      }
    case JsNumber(n) =>
      JsNumber(n.bigDecimal.stripTrailingZeros())

    case other => other
  }

  def assertJsonEquals(actual: JsValue, expected: JsValue): Assertion =
    normalise(actual) shouldEqual normalise(expected)
  implicit val dateService: DateService = app.injector.instanceOf[DateService]

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val transactionName: String = "charge-summary"
  val auditType: String = "ChargeSummary"
  lazy val fixedDate : LocalDate = LocalDate.of(2022, 1, 7)

  val docDetail: DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some("ITSA- POA 1"),
    documentText = Some("documentText"),
    originalAmount = 10.34,
    outstandingAmount = 0,
    documentDate = LocalDate.of(2018, 3, 29)
  )

  val docDetailWithCodingOutAccepted: DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some("TRM Amend Charge"),
    documentText = Some(CODING_OUT_ACCEPTED.name),
    originalAmount = 10.34,
    outstandingAmount = 0,
    documentDate = LocalDate.of(2018, 3, 29)
  )

  val docDetailWithCodingOutRejected: DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some("TRM Amend Charge"),
    documentText = Some(CODING_OUT_CANCELLED.name),
    originalAmount = 10.34,
    outstandingAmount = 0,
    documentDate = LocalDate.of(2018, 3, 29)
  )

  val docDetailWithInterest: DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some("ITSA- POA 1"),
    documentText = Some("documentText"),
    originalAmount = 10.34,
    outstandingAmount = 0,
    documentDate = LocalDate.of(2018, 3, 29),
    accruingInterestAmount = Some(54.32),
    interestOutstandingAmount = Some(2),
    interestFromDate = Some(LocalDate.of(2021, 10, 6)),
    interestEndDate = Some(LocalDate.of(2022, 1, 6))
  )
  val paymentAllocation: List[PaymentHistoryAllocations] = List(
    paymentsWithCharge("SA Payment on Account 1", ITSA_NI, "2018-03-30", -1500.0),
    paymentsWithCharge("SA Payment on Account 1", NIC4_SCOTLAND, "2018-03-31", -1600.0)
  )
  val chargeHistoryModel: ChargeHistoryModel = ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 7, 6), "documentDescription", 1500,
    LocalDateTime.of(LocalDate.of(2018, 7, 6), LocalTime.of(9, 30, 45)), "amended return", None)

  paymentAllocation.map(_.getPaymentAllocationTextInChargeSummary)
  val chargeHistoryModel2: ChargeHistoryModel = ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 7, 6), "documentDescription", 1500,
    LocalDateTime.of(LocalDate.of(2018, 7, 6), LocalTime.of(9, 30, 45)), "Customer Request", None)
  val chargeHistory: List[ChargeHistoryModel] = List(
    chargeHistoryModel,
    chargeHistoryModel2)
  val paymentBreakdowns: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, dunningLock = Some("Stand over order"), interestLock = Some("Manual RPI Signal")),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, interestLock = Some("Breathing Space Moratorium Act")),
    financialDetail(originalAmount = 3456.78, chargeType = VOLUNTARY_NIC2_NI, dunningLock = Some("Stand over order")),
    financialDetail(originalAmount = 9876.54, chargeType = CGT))




  val chargeItemWithNoInterest: ChargeItem = ChargeItem(
    transactionId = "1040000124",
    taxYear = TaxYear.forYearEnd(taxYear),
    transactionType = PoaOneDebit,
    codedOutStatus = None,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(fixedDate),
    originalAmount = 10.34,
    outstandingAmount = 0.0,
    interestOutstandingAmount = None,
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    interestFromDate = None,
    interestEndDate = None,
    interestRate = None,
    lpiWithDunningLock = None,
    amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = None,
    chargeReference = Some("chargeRef")
  )
  val chargeItemWithCodingOutAccepted: ChargeItem = chargeItemWithNoInterest.copy(
    transactionType = BalancingCharge,
    codedOutStatus = Some(Accepted)
  )
  val chargeItemWithCodingOutRejected: ChargeItem =  chargeItemWithNoInterest.copy(
    transactionType = BalancingCharge,
    codedOutStatus = Some(Cancelled)
  )
  val chargeItemWithInterest: ChargeItem = chargeItemWithNoInterest.copy(
    accruingInterestAmount = Some(54.32),
    interestOutstandingAmount = Some(2),
    interestFromDate = Some(LocalDate.of(2021, 10, 6)),
    interestEndDate = Some(LocalDate.of(2022, 1, 6))
  )



  val chargeSummaryAuditMin: ChargeSummaryAudit = ChargeSummaryAudit(
    getMinimalMTDITUser(None, IncomeSourceDetailsModel("nino", "mtditid", None, List.empty, List.empty, "1")),
    chargeItem = chargeItemWithNoInterest,
    paymentBreakdown = List.empty,
    chargeHistories = List.empty,
    paymentAllocations = List.empty,
    isLatePaymentCharge = false,
    taxYear = taxYearTyped
  )

  def paymentsWithCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal): PaymentHistoryAllocations =
    PaymentHistoryAllocations(
      allocations = List(
        PaymentHistoryAllocation(
          amount = Some(amount),
          dueDate = Some(LocalDate.parse(date)),
          clearingSAPDocument = None,
          clearingId = None,
          taxYear = None
        )), chargeMainType = Some(mainType), chargeType = Some(chargeType))


  def chargeSummaryAuditFull(userType: Option[AffinityGroup] = Some(Agent),
                             chargeItem: ChargeItem, paymentBreakdown: List[FinancialDetail],
                             chargeHistories: List[ChargeHistoryModel], paymentAllocations: List[PaymentHistoryAllocations],
                             agentReferenceNumber: Option[String] = Some("agentReferenceNumber"), isLateInterestCharge: Boolean = true): ChargeSummaryAudit = ChargeSummaryAudit(
    mtdItUser = defaultMTDITUser(userType, IncomeSourceDetailsModel("nino", "mtditid", None, Nil, Nil, "1")),
    chargeItem = chargeItem,
    paymentBreakdown = if (!isLateInterestCharge) paymentBreakdowns else List.empty,
    chargeHistories = if (!isLateInterestCharge) chargeHistory else List.empty,
    paymentAllocations = paymentAllocation,
    isLatePaymentCharge = isLateInterestCharge,
    taxYear = taxYearTyped
  )

  "ChargeSummaryAudit(mtdItUser, charge, agentReferenceNumber)" should {

    s"have the correct transaction name of '$transactionName'" in {
      chargeSummaryAuditFull(None,
        chargeItemWithInterest,
        paymentBreakdown = paymentBreakdowns,
        chargeHistories = chargeHistory,
        paymentAllocations = paymentAllocation,
        agentReferenceNumber = Some(testArn)
      ).transactionName mustBe transactionName
    }

    s"have the correct audit event type of '$auditType'" in {
      chargeSummaryAuditFull(None,
        chargeItemWithInterest,
        paymentBreakdown = paymentBreakdowns,
        chargeHistories = chargeHistory,
        paymentAllocations = paymentAllocation,
        agentReferenceNumber = Some(testArn)
      ).auditType mustBe auditType
    }

    "have the correct details for the audit event" when {
      "the charge summary audit has all detail" when {
        "there are charge details" in {
          chargeSummaryAuditFull(
            userType = Some(Agent),
            chargeItemWithInterest
            .copy(accruingInterestAmount = None),
            paymentBreakdown = paymentBreakdowns,
            chargeHistories = chargeHistory,
            paymentAllocations = paymentAllocation,
            agentReferenceNumber = Some(testArn),
            isLateInterestCharge = false
          ).detail mustBe commonAuditDetails(Agent) ++ Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetailWithInterest.remainingToPay,
              "fullPaymentAmount" -> docDetailWithInterest.originalAmount,
              "dueDate" -> chargeItemWithNoInterest.dueDate,
              "chargeType" -> getChargeType(chargeItemWithInterest, latePaymentCharge = false),
              "interestPeriod" -> "2021-10-06 to 2022-01-06",
              "endTaxYear" -> taxYear,
              "overdue" -> chargeItemWithNoInterest.isOverdue()
            ),
            "paymentBreakdown" -> Json.arr(
              Json.obj(
                "breakdownType" -> messages("chargeSummary.paymentBreakdown.incomeTax"),
                "total" -> 123.45,
                "chargeUnderReview" -> true,
                "interestLock" -> true
              ),
              Json.obj(
                "breakdownType" -> messages("chargeSummary.paymentBreakdown.nic2"),
                "total" -> 2345.67,
                "chargeUnderReview" -> false,
                "interestLock" -> true
              ),
              Json.obj(
                "breakdownType" -> messages("chargeSummary.paymentBreakdown.vcnic2"),
                "total" -> 3456.78,
                "chargeUnderReview" -> true,
                "interestLock" -> false
              ),
              Json.obj(
                "breakdownType" -> messages("chargeSummary.paymentBreakdown.cgt"),
                "total" -> 9876.54,
                "chargeUnderReview" -> false,
                "interestLock" -> false
              )
            ),
            "paymentAllocationsChargeHistory" -> Json.arr(
              Json.obj(
                "amount" -> 1500,
                "date" -> "2018-03-30",
                "description" -> messages("paymentAllocation.paymentAllocations.poa1.incomeTax")),
              Json.obj(
                "amount" -> 1600,
                "date" -> "2018-03-31",
                "description" -> messages("paymentAllocation.paymentAllocations.poa1.nic4"))
            ),
            "chargeHistory" -> Json.arr(
              Json.obj(
                "date" -> "2018-07-06T09:30:45Z",
                "description" -> messages("chargeSummary.chargeHistory.amend.paymentOnAccount1.text"),
                "amount" -> 1500
              ),
              Json.obj(
                "date" -> "2018-07-06T09:30:45Z",
                "description" -> messages("chargeSummary.chargeHistory.request.paymentOnAccount1.text"),
                "amount" -> 1500
              )
            )
          )
        }

        "there are charge details with coding out accepted" in {
          assertJsonEquals(chargeSummaryAuditFull(
            userType = Some(Agent),
            chargeItemWithCodingOutAccepted,
            paymentBreakdown = paymentBreakdowns,
            chargeHistories = chargeHistory,
            paymentAllocations = paymentAllocation,
            agentReferenceNumber = Some("agentReferenceNumber"),
            isLateInterestCharge = false
          ).detail, commonAuditDetails(Agent) ++ Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetailWithCodingOutAccepted.remainingToPay,
              "fullPaymentAmount" -> docDetailWithCodingOutRejected.originalAmount,
              "dueDate" -> chargeItemWithCodingOutAccepted.dueDate,
              "chargeType" -> getChargeType(chargeItemWithCodingOutAccepted, latePaymentCharge = false),
              "endTaxYear" -> taxYear,
              "overdue" -> chargeItemWithCodingOutAccepted.isOverdue()
            ),
            "paymentBreakdown" -> Json.arr(
              Json.obj(
                "breakdownType" -> messages("chargeSummary.paymentBreakdown.incomeTax"),
                "total" -> 123.45,
                "chargeUnderReview" -> true,
                "interestLock" -> true
              ),
              Json.obj(
                "breakdownType" -> messages("chargeSummary.paymentBreakdown.nic2"),
                "total" -> 2345.67,
                "chargeUnderReview" -> false,
                "interestLock" -> true
              ),
              Json.obj(
                "breakdownType" -> messages("chargeSummary.paymentBreakdown.vcnic2"),
                "total" -> 3456.78,
                "chargeUnderReview" -> true,
                "interestLock" -> false
              ),
              Json.obj(
                "breakdownType" -> messages("chargeSummary.paymentBreakdown.cgt"),
                "total" -> 9876.54,
                "chargeUnderReview" -> false,
                "interestLock" -> false
              )
            ),
            "paymentAllocationsChargeHistory" -> Json.arr(
              Json.obj(
                "amount" -> 1500,
                "date" -> "2018-03-30",
                "description" -> s"Amount collected through your PAYE tax code for ${taxYear + 1} to ${taxYear + 2} tax year"),
              Json.obj(
                "amount" -> 1600,
                "date" -> "2018-03-31",
                "description" -> s"Amount collected through your PAYE tax code for ${taxYear + 1} to ${taxYear+ 2} tax year")
            ),
            "chargeHistory" -> Json.arr(
              Json.obj(
                "date" -> "2018-07-06T09:30:45Z",
                "description" -> "Remaining balance reduced due to amended return with coding out",
                "amount" -> 1500
              ),
              Json.obj(
                "date" -> "2018-07-06T09:30:45Z",
                "description" -> "Remaining balance reduced by taxpayer request with coding out",
                "amount" -> 1500
              )
            )
          ))
        }

        "there are charge details with coding out rejected" in {
         assertJsonEquals(chargeSummaryAuditFull(
            userType = Some(Agent),
            chargeItemWithCodingOutRejected,
            paymentBreakdown = paymentBreakdowns,
            chargeHistories = chargeHistory,
            paymentAllocations = paymentAllocation,
            agentReferenceNumber = Some("agentReferenceNumber"),
            isLateInterestCharge = false
          ).detail, commonAuditDetails(Agent) ++ Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetailWithCodingOutRejected.remainingToPay,
              "fullPaymentAmount" -> docDetailWithCodingOutRejected.originalAmount,
              "dueDate" -> chargeItemWithCodingOutRejected.dueDate,
              "chargeType" -> getChargeType(chargeItemWithCodingOutRejected, latePaymentCharge = false),
              "endTaxYear" -> taxYear,
              "overdue" -> chargeItemWithCodingOutRejected.isOverdue()
            ),
            "paymentBreakdown" -> Json.arr(
              Json.obj(
                "breakdownType" -> messages("chargeSummary.paymentBreakdown.incomeTax"),
                "total" -> 123.45,
                "chargeUnderReview" -> true,
                "interestLock" -> true
              ),
              Json.obj(
                "breakdownType" -> messages("chargeSummary.paymentBreakdown.nic2"),
                "total" -> 2345.67,
                "chargeUnderReview" -> false,
                "interestLock" -> true
              ),
              Json.obj(
                "breakdownType" -> messages("chargeSummary.paymentBreakdown.vcnic2"),
                "total" -> 3456.78,
                "chargeUnderReview" -> true,
                "interestLock" -> false
              ),
              Json.obj(
                "breakdownType" -> messages("chargeSummary.paymentBreakdown.cgt"),
                "total" -> 9876.54,
                "chargeUnderReview" -> false,
                "interestLock" -> false
              )
            ),
            "paymentAllocationsChargeHistory" -> Json.arr(
              Json.obj(
                "amount" -> 1500,
                "date" -> "2018-03-30",
                "description" -> "Cancelled PAYE Self Assessment (through your PAYE tax code)"),
              Json.obj(
                "amount" -> 1600,
                "date" -> "2018-03-31",
                "description" -> "Cancelled PAYE Self Assessment (through your PAYE tax code)")
            ),
            "chargeHistory" -> Json.arr(
              Json.obj(
                "date" -> "2018-07-06T09:30:45Z",
                "description" -> "Remaining balance reduced due to amended return with cancelledPayeSelfAssessment",
                "amount" -> 1500
              ),
              Json.obj(
                "date" -> "2018-07-06T09:30:45Z",
                "description" -> "Remaining balance reduced by taxpayer request with cancelledPayeSelfAssessment",
                "amount" -> 1500
              )
            ))
          )
        }

        "there are late payment charge details" in {

          chargeSummaryAuditFull(
            userType = Some(Agent),
            chargeItemWithInterest,
            paymentBreakdown = paymentBreakdowns,
            chargeHistories = chargeHistory,
            paymentAllocations = paymentAllocation,
            agentReferenceNumber = Some("agentReferenceNumber"),
            isLateInterestCharge = true
          ).detail mustBe commonAuditDetails(Agent) ++ Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetailWithInterest.interestRemainingToPay,
              "fullPaymentAmount" -> docDetailWithInterest.accruingInterestAmount,
              "dueDate" -> docDetailWithInterest.interestEndDate,
              "chargeType" -> getChargeType(chargeItemWithInterest, latePaymentCharge = true),
              "interestPeriod" -> "2021-10-06 to 2022-01-06",
              "endTaxYear" -> taxYear,
              "overdue" -> chargeItemWithNoInterest.isOverdue()
            ),
            "paymentBreakdown" -> Json.arr(),
            "paymentAllocationsChargeHistory" -> Json.arr(),
            "chargeHistory" -> Json.arr()
          )
        }

        "the charge summary audit has minimal details" in {

          assertJsonEquals(chargeSummaryAuditMin.detail, Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetail.remainingToPay,
              "fullPaymentAmount" -> docDetail.originalAmount,
              "dueDate" -> chargeItemWithNoInterest.dueDate,
              "chargeType" -> getChargeType(chargeItemWithNoInterest, latePaymentCharge = false),
              "endTaxYear" -> taxYear,
              "overdue" -> chargeItemWithNoInterest.isOverdue()),
            "nino" -> testNino,
            "paymentBreakdown" -> Json.arr(),
            "paymentAllocationsChargeHistory" -> Json.arr(),
            "chargeHistory" -> Json.arr(),
            "mtditid" -> testMtditid

          ))
        }
      }
    }
  }

  "PaymentSharedFunctions.getChargeType" should {
    "return the correct string" when {
      "passed various charges" in {
        val bcd = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = BalancingCharge)
        val poa1 = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = PoaOneDebit)
        val poa2 = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = PoaTwoDebit)
        val lpp1 = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = FirstLatePaymentPenalty)
        val lpp2 = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = SecondLatePaymentPenalty)
        val lsp = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = LateSubmissionPenalty)
        val poaRecon1 = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = PoaOneReconciliationDebit)
        val poaRecon2 = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = PoaTwoReconciliationDebit)
        val itsaAmendmentCharge = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = ITSAReturnAmendment)
        val mfaDebit = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = MfaDebitCharge)
        val nics2 = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = BalancingCharge, codedOutStatus = Some(Nics2))
        val codedBCD = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = BalancingCharge, codedOutStatus = Some(Accepted))
        val codedPOA1 = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = PoaOneDebit, codedOutStatus = Some(Accepted))
        val codedPOA2 = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = PoaTwoDebit, codedOutStatus = Some(Accepted))
        val cancelledCodedOut = chargeItemModel(interestOutstandingAmount = None, interestRate = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, transactionType = PoaTwoDebit, codedOutStatus = Some(Cancelled))

        getChargeType(bcd, false) shouldBe Some("Remaining balance")
        getChargeType(bcd, true) shouldBe Some("Late payment interest for remaining balance")

        getChargeType(poa1, false) shouldBe Some("First payment on account")
        getChargeType(poa1, true) shouldBe Some("Late payment interest on first payment on account")

        getChargeType(poa2, false) shouldBe Some("Second payment on account")
        getChargeType(poa2, true) shouldBe Some("Late payment interest on second payment on account")

        getChargeType(lpp1, false) shouldBe Some("First late payment penalty")
        getChargeType(lpp1, true) shouldBe Some("Late payment interest on first late payment penalty")

        getChargeType(lpp2, false) shouldBe Some("Second late payment penalty")
        getChargeType(lpp2, true) shouldBe Some("Late payment interest on second late payment penalty")

        getChargeType(lsp, false) shouldBe Some("Late submission penalty")
        getChargeType(lsp, true) shouldBe Some("Late payment interest on late submission penalty")

        getChargeType(poaRecon1, false) shouldBe Some("First payment on account: extra amount from your tax return")
        getChargeType(poaRecon1, true) shouldBe Some("Interest for first payment on account: extra amount")

        getChargeType(poaRecon2, false) shouldBe Some("Second payment on account: extra amount from your tax return")
        getChargeType(poaRecon2, true) shouldBe Some("Interest for second payment on account: extra amount")

        getChargeType(itsaAmendmentCharge, false) shouldBe Some("Balancing payment: extra amount due to amended return")
        getChargeType(itsaAmendmentCharge, true) shouldBe Some("Late payment interest on balancing payment: extra amount due to amended return")

        getChargeType(mfaDebit, false) shouldBe Some("MFADebit")

        getChargeType(nics2, false) shouldBe Some("Class 2 National Insurance")

        getChargeType(codedBCD, false) shouldBe Some("Balancing payment collected through PAYE tax code")

        getChargeType(codedPOA1, false) shouldBe Some("First payment on account collected through PAYE tax code")

        getChargeType(codedPOA2, false) shouldBe Some("Second payment on account collected through PAYE tax code")

        getChargeType(cancelledCodedOut, false) shouldBe Some("Cancelled PAYE Self Assessment (through your PAYE tax code)")
      }
    }
  }
}
