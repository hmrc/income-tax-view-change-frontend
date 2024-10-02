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

import auth.MtdItUser
import enums.ChargeType._
import enums.CodingOutType.{CODING_OUT_ACCEPTED, CODING_OUT_CANCELLED}
import models.chargeHistory.ChargeHistoryModel
import models.chargeSummary.{PaymentHistoryAllocation, PaymentHistoryAllocations}
import models.financialDetails._
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.DateService
import testConstants.BaseTestConstants._
import testConstants.FinancialDetailsTestConstants.financialDetail
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class ChargeSummaryAuditSpec extends AnyWordSpecLike with Matchers with AuditFunctions {

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
    latePaymentInterestAmount = Some(54.32),
    interestOutstandingAmount = Some(2),
    interestFromDate = Some(LocalDate.of(2021, 10, 6)),
    interestEndDate = Some(LocalDate.of(2022, 1, 6))
  )
  val paymentAllocation: List[PaymentHistoryAllocations] = List(
    paymentsWithCharge("SA Payment on Account 1", ITSA_NI, "2018-03-30", -1500.0),
    paymentsWithCharge("SA Payment on Account 1", NIC4_SCOTLAND, "2018-03-31", -1600.0)
  )
  val chargeHistoryModel: ChargeHistoryModel = ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 7, 6), "documentDescription", 1500, LocalDate.of(2018, 7, 6), "amended return", None)

  paymentAllocation.map(_.getPaymentAllocationTextInChargeSummary)
  val chargeHistoryModel2: ChargeHistoryModel = ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 7, 6), "documentDescription", 1500, LocalDate.of(2018, 7, 6), "Customer Request", None)
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
    transactionType = PaymentOnAccountOne,
    subTransactionType = None,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(fixedDate),
    originalAmount = 10.34,
    outstandingAmount = 0.0,
    interestOutstandingAmount = None,
    latePaymentInterestAmount = None,
    interestFromDate = None,
    interestEndDate = None,
    interestRate = None,
    lpiWithDunningLock = None,
    amountCodedOut = None,
    dunningLock = false
  )
  val chargeItemWithCodingOutAccepted: ChargeItem = chargeItemWithNoInterest.copy(
    transactionType = BalancingCharge,
    subTransactionType = Some(Accepted)
  )
  val chargeItemWithCodingOutRejected: ChargeItem =  chargeItemWithNoInterest.copy(
    transactionType = BalancingCharge,
    subTransactionType = Some(Cancelled)
  )
  val chargeItemWithInterest: ChargeItem = chargeItemWithNoInterest.copy(
    latePaymentInterestAmount = Some(54.32),
    interestOutstandingAmount = Some(2),
    interestFromDate = Some(LocalDate.of(2021, 10, 6)),
    interestEndDate = Some(LocalDate.of(2022, 1, 6))
  )



  val chargeSummaryAuditMin: ChargeSummaryAudit = ChargeSummaryAudit(
    mtdItUser = MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = None,
      incomeSources = IncomeSourceDetailsModel("nino", "mtditid", None, List.empty, List.empty),
      btaNavPartial = None,
      saUtr = None,
      credId = None,
      userType = None,
      arn = None
    ),
    chargeItem = chargeItemWithNoInterest,
    paymentBreakdown = List.empty,
    chargeHistories = List.empty,
    paymentAllocations = List.empty,
    isLatePaymentCharge = false,
    taxYear = taxYear
  )

  def paymentsWithCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal): PaymentHistoryAllocations =
    PaymentHistoryAllocations(
      allocations = List(
        PaymentHistoryAllocation(
          amount = Some(amount),
          dueDate = Some(LocalDate.parse(date)),
          clearingSAPDocument = None,
          clearingId = None
        )), chargeMainType = Some(mainType), chargeType = Some(chargeType))


  def chargeSummaryAuditFull(userType: Option[AffinityGroup] = Some(Agent),
                             chargeItem: ChargeItem, paymentBreakdown: List[FinancialDetail],
                             chargeHistories: List[ChargeHistoryModel], paymentAllocations: List[PaymentHistoryAllocations],
                             agentReferenceNumber: Option[String] = Some("agentReferenceNumber"), isLateInterestCharge: Boolean = true): ChargeSummaryAudit = ChargeSummaryAudit(
    mtdItUser = MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = Some(Name(Some("firstName"), Some("lastName"))),
      incomeSources = IncomeSourceDetailsModel("nino", "mtditid", None, Nil, Nil),
      btaNavPartial = None,
      saUtr = Some("saUtr"),
      credId = Some("credId"),
      userType = userType,
      arn = agentReferenceNumber
    ),
    chargeItem = chargeItem,
    paymentBreakdown = if (!isLateInterestCharge) paymentBreakdowns else List.empty,
    chargeHistories = if (!isLateInterestCharge) chargeHistory else List.empty,
    paymentAllocations = paymentAllocation,
    isLatePaymentCharge = isLateInterestCharge,
    taxYear = taxYear
  )

  "ChargeSummaryAudit(mtdItUser, charge, agentReferenceNumber)" should {

    s"have the correct transaction name of '$transactionName'" in {
      chargeSummaryAuditFull(None,
        chargeItemWithInterest,
        paymentBreakdown = paymentBreakdowns,
        chargeHistories = chargeHistory,
        paymentAllocations = paymentAllocation,
        agentReferenceNumber = Some("arn")
      ).transactionName mustBe transactionName
    }

    s"have the correct audit event type of '$auditType'" in {
      chargeSummaryAuditFull(None,
        chargeItemWithInterest,
        paymentBreakdown = paymentBreakdowns,
        chargeHistories = chargeHistory,
        paymentAllocations = paymentAllocation,
        agentReferenceNumber = Some("arn")
      ).auditType mustBe auditType
    }

    "have the correct details for the audit event" when {
      "the charge summary audit has all detail" when {
        "there are charge details" in {
          chargeSummaryAuditFull(
            userType = Some(Agent),
            chargeItemWithInterest
            .copy(latePaymentInterestAmount = None),
            paymentBreakdown = paymentBreakdowns,
            chargeHistories = chargeHistory,
            paymentAllocations = paymentAllocation,
            agentReferenceNumber = Some("agentReferenceNumber"),
            isLateInterestCharge = false
          ).detail mustBe Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetailWithInterest.remainingToPay,
              "fullPaymentAmount" -> docDetailWithInterest.originalAmount,
              "dueDate" -> chargeItemWithNoInterest.dueDate,
              "chargeType" -> getChargeType(chargeItemWithInterest, latePaymentCharge = false),
              "interestPeriod" -> "2021-10-06 to 2022-01-06",
              "endTaxYear" -> taxYear,
              "overdue" -> chargeItemWithNoInterest.isOverdue()
            ),
            "saUtr" -> "saUtr",
            "nino" -> "nino",
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
            "agentReferenceNumber" -> "agentReferenceNumber",
            "chargeHistory" -> Json.arr(
              Json.obj(
                "date" -> "2018-07-06",
                "description" -> messages("chargeSummary.chargeHistory.amend.paymentOnAccount1.text"),
                "amount" -> 1500
              ),
              Json.obj(
                "date" -> "2018-07-06",
                "description" -> messages("chargeSummary.chargeHistory.request.paymentOnAccount1.text"),
                "amount" -> 1500
              )
            ),
            "userType" -> "Agent",
            "credId" -> "credId",
            "mtditid" -> "mtditid"
          )
        }

        "there are charge details with coding out accepted" in {
          chargeSummaryAuditFull(
            userType = Some(Agent),
            chargeItemWithCodingOutAccepted,
            paymentBreakdown = paymentBreakdowns,
            chargeHistories = chargeHistory,
            paymentAllocations = paymentAllocation,
            agentReferenceNumber = Some("agentReferenceNumber"),
            isLateInterestCharge = false
          ).detail mustBe Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetailWithCodingOutAccepted.remainingToPay,
              "fullPaymentAmount" -> docDetailWithCodingOutRejected.originalAmount,
              "dueDate" -> chargeItemWithCodingOutAccepted.dueDate,
              "chargeType" -> getChargeType(chargeItemWithCodingOutAccepted, latePaymentCharge = false),
              "endTaxYear" -> taxYear,
              "overdue" -> chargeItemWithCodingOutAccepted.isOverdue()
            ),
            "saUtr" -> "saUtr",
            "nino" -> "nino",
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
            "agentReferenceNumber" -> "agentReferenceNumber",
            "chargeHistory" -> Json.arr(
              Json.obj(
                "date" -> "2018-07-06",
                "description" -> "Remaining balance reduced due to amended return",
                "amount" -> 1500
              ),
              Json.obj(
                "date" -> "2018-07-06",
                "description" -> "Remaining balance reduced by taxpayer request",
                "amount" -> 1500
              )
            ),
            "userType" -> "Agent",
            "credId" -> "credId",
            "mtditid" -> "mtditid"
          )
        }

        "there are charge details with coding out rejected" in {
          chargeSummaryAuditFull(
            userType = Some(Agent),
            chargeItemWithCodingOutRejected,
            paymentBreakdown = paymentBreakdowns,
            chargeHistories = chargeHistory,
            paymentAllocations = paymentAllocation,
            agentReferenceNumber = Some("agentReferenceNumber"),
            isLateInterestCharge = false
          ).detail mustBe Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetailWithCodingOutRejected.remainingToPay,
              "fullPaymentAmount" -> docDetailWithCodingOutRejected.originalAmount,
              "dueDate" -> chargeItemWithCodingOutRejected.dueDate,
              "chargeType" -> getChargeType(chargeItemWithCodingOutRejected, latePaymentCharge = false),
              "endTaxYear" -> taxYear,
              "overdue" -> chargeItemWithCodingOutRejected.isOverdue()
            ),
            "saUtr" -> "saUtr",
            "nino" -> "nino",
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
            "agentReferenceNumber" -> "agentReferenceNumber",
            "chargeHistory" -> Json.arr(
              Json.obj(
                "date" -> "2018-07-06",
                "description" -> "Remaining balance reduced due to amended return",
                "amount" -> 1500
              ),
              Json.obj(
                "date" -> "2018-07-06",
                "description" -> "Remaining balance reduced by taxpayer request",
                "amount" -> 1500
              )
            ),
            "userType" -> "Agent",
            "credId" -> "credId",
            "mtditid" -> "mtditid"
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
          ).detail mustBe Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetailWithInterest.interestRemainingToPay,
              "fullPaymentAmount" -> docDetailWithInterest.latePaymentInterestAmount,
              "dueDate" -> docDetailWithInterest.interestEndDate,
              "chargeType" -> getChargeType(chargeItemWithInterest, latePaymentCharge = true),
              "interestPeriod" -> "2021-10-06 to 2022-01-06",
              "endTaxYear" -> taxYear,
              "overdue" -> chargeItemWithNoInterest.isOverdue()
            ),
            "saUtr" -> "saUtr",
            "nino" -> "nino",
            "paymentBreakdown" -> Json.arr(),
            "paymentAllocationsChargeHistory" -> Json.arr(),
            "agentReferenceNumber" -> "agentReferenceNumber",
            "chargeHistory" -> Json.arr(),
            "userType" -> "Agent",
            "credId" -> "credId",
            "mtditid" -> "mtditid"
          )
        }

        "the charge summary audit has minimal details" in {

          chargeSummaryAuditMin.detail mustBe Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetail.remainingToPay,
              "fullPaymentAmount" -> docDetail.originalAmount,
              "dueDate" -> chargeItemWithNoInterest.dueDate,
              "chargeType" -> getChargeType(chargeItemWithNoInterest, latePaymentCharge = false),
              "endTaxYear" -> taxYear,
              "overdue" -> chargeItemWithNoInterest.isOverdue()),
            "nino" -> "nino",
            "paymentBreakdown" -> Json.arr(),
            "paymentAllocationsChargeHistory" -> Json.arr(),
            "chargeHistory" -> Json.arr(),
            "mtditid" -> "mtditid"

          )
        }
      }
    }
  }
}
