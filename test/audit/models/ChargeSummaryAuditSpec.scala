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
import models.financialDetails._
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.Logger
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

class ChargeSummaryAuditSpec extends WordSpecLike with MustMatchers {

  implicit val dateService: DateService = app.injector.instanceOf[DateService]

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val transactionName: String = "charge-summary"
  val auditType: String = "ChargeSummary"

  val docDetail: DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some("ITSA- POA 1"),
    documentText = Some("documentText"),
    originalAmount = Some(10.34),
    outstandingAmount = Some(0),
    documentDate = LocalDate.of(2018, 3, 29)
  )

  val docDetailWithCodingOutAccepted: DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some("TRM Amend Charge"),
    documentText = Some(CODING_OUT_ACCEPTED.name),
    originalAmount = Some(10.34),
    outstandingAmount = Some(0),
    documentDate = LocalDate.of(2018, 3, 29)
  )

  val docDetailWithCodingOutRejected: DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some("TRM Amend Charge"),
    documentText = Some(CODING_OUT_CANCELLED.name),
    originalAmount = Some(10.34),
    outstandingAmount = Some(0),
    documentDate = LocalDate.of(2018, 3, 29)
  )

  val docDetailWithInterest: DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some("ITSA- POA 1"),
    documentText = Some("documentText"),
    originalAmount = Some(10.34),
    outstandingAmount = Some(0),
    documentDate = LocalDate.of(2018, 3, 29),
    latePaymentInterestAmount = Some(54.32),
    interestOutstandingAmount = Some(2),
    interestFromDate = Some(LocalDate.of(2021, 10, 6)),
    interestEndDate = Some(LocalDate.of(2022, 1, 6))
  )
  val paymentAllocation: List[PaymentsWithChargeType] = List(
    paymentsWithCharge("SA Payment on Account 1", ITSA_NI, "2018-03-30", -1500.0),
    paymentsWithCharge("SA Payment on Account 1", NIC4_SCOTLAND, "2018-03-31", -1600.0)
  )
  val chargeHistoryModel: ChargeHistoryModel = ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 7, 6), "documentDescription", 1500, LocalDate.of(2018, 7, 6), "amended return")

  paymentAllocation.map(_.getPaymentAllocationTextInChargeSummary)
  val chargeHistoryModel2: ChargeHistoryModel = ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 7, 6), "documentDescription", 1500, LocalDate.of(2018, 7, 6), "Customer Request")
  val chargeHistory: List[ChargeHistoryModel] = List(
    chargeHistoryModel,
    chargeHistoryModel2)
  val paymentBreakdowns: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, dunningLock = Some("Stand over order"), interestLock = Some("Manual RPI Signal")),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, interestLock = Some("Breathing Space Moratorium Act")),
    financialDetail(originalAmount = 3456.78, chargeType = VOLUNTARY_NIC2_NI, dunningLock = Some("Stand over order")),
    financialDetail(originalAmount = 9876.54, chargeType = CGT))
  val docDateDetail: DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetail,
    dueDate = Some(LocalDate.now())
  )
  val docDateDetailWithCodingOutAccepted: DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetailWithCodingOutAccepted,
    dueDate = Some(LocalDate.now())
  )
  val docDateDetailWithCodingOutRejected: DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetailWithCodingOutRejected,
    dueDate = Some(LocalDate.now())
  )
  val docDateDetailWithInterest: DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetailWithInterest,
    dueDate = Some(LocalDate.now())
  )
  val chargeSummaryAuditMin: ChargeSummaryAudit = ChargeSummaryAudit(
    mtdItUser = MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = None,
      incomeSources = IncomeSourceDetailsModel("mtditid", None, List.empty, List.empty),
      btaNavPartial = None,
      saUtr = None,
      credId = None,
      userType = None,
      arn = None
    ),
    docDateDetail = docDateDetail,
    paymentBreakdown = List.empty,
    chargeHistories = List.empty,
    paymentAllocations = List.empty,
    isLatePaymentCharge = false,
    taxYear = taxYear
  )

  def paymentsWithCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal): PaymentsWithChargeType =
    PaymentsWithChargeType(
      payments = List(Payment(reference = Some("reference"), amount = Some(amount), outstandingAmount = None, method = Some("method"),
        documentDescription = None, lot = Some("lot"), lotItem = Some("lotItem"), dueDate = Some(LocalDate.parse(date)),
        documentDate = LocalDate.parse(date), transactionId = None)), mainType = Some(mainType), chargeType = Some(chargeType))

  def getChargeType(latePayment: Boolean, documentDetail: DocumentDetail): String =
    (documentDetail.documentDescription, documentDetail.documentText) match {
    case (_, Some(text)) if text.contains("Cancelled PAYE Self Assessment") =>
      "Cancelled PAYE Self Assessment (through your PAYE tax code)"
    case (_, Some(text)) if text.contains("Balancing payment collected through PAYE tax code") =>
      "Balancing payment collected through PAYE tax code"
    case (Some("ITSA- POA 1"), _) =>
      if (latePayment) "Late Payment Interest on payment on account 1 of 2" else "Payment on account 1 of 2"
    case (Some("ITSA - POA 2"), _) =>
      if (latePayment) "Late Payment Interest on payment on account 2 of 2" else "Payment on account 2 of 2"
    case (Some("TRM New Charge"), _) | (Some("TRM Amend Charge"), _) =>
      if (latePayment) "Late Payment Interest on remaining balance" else "Remaining balance"
    case error => {
      Logger("application")
        .error(s"[Charge][getChargeTypeKey] Missing or non-matching charge type: $error found")
      "unknownCharge"
    }
  }

  def chargeSummaryAuditFull(userType: Option[AffinityGroup] = Some(Agent),
                             docDateDetails: DocumentDetailWithDueDate, paymentBreakdown: List[FinancialDetail],
                             chargeHistories: List[ChargeHistoryModel], paymentAllocations: List[PaymentsWithChargeType],
                             agentReferenceNumber: Option[String] = Some("agentReferenceNumber"), isLateInterestCharge: Boolean = true): ChargeSummaryAudit = ChargeSummaryAudit(
    mtdItUser = MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = Some(Name(Some("firstName"), Some("lastName"))),
      incomeSources = IncomeSourceDetailsModel("mtditid", None, Nil, Nil),
      btaNavPartial = None,
      saUtr = Some("saUtr"),
      credId = Some("credId"),
      userType = userType,
      arn = agentReferenceNumber
    ),
    docDateDetail = docDateDetails,
    paymentBreakdown = if (!isLateInterestCharge) paymentBreakdowns else List.empty,
    chargeHistories = if (!isLateInterestCharge) chargeHistory else List.empty,
    paymentAllocations = paymentAllocation,
    isLatePaymentCharge = isLateInterestCharge,
    taxYear = taxYear
  )

  "ChargeSummaryAudit(mtdItUser, charge, agentReferenceNumber)" should {

    s"have the correct transaction name of '$transactionName'" in {
      chargeSummaryAuditFull(None,
        docDateDetailWithInterest,
        paymentBreakdown = paymentBreakdowns,
        chargeHistories = chargeHistory,
        paymentAllocations = paymentAllocation,
        agentReferenceNumber = Some("arn")
      ).transactionName mustBe transactionName
    }

    s"have the correct audit event type of '$auditType'" in {
      chargeSummaryAuditFull(None,
        docDateDetailWithInterest,
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
            docDateDetailWithInterest,
            paymentBreakdown = paymentBreakdowns,
            chargeHistories = chargeHistory,
            paymentAllocations = paymentAllocation,
            agentReferenceNumber = Some("agentReferenceNumber"),
            isLateInterestCharge = false
          ).detail mustBe Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetailWithInterest.remainingToPay,
              "fullPaymentAmount" -> docDetailWithInterest.originalAmount,
              "dueDate" -> docDateDetail.dueDate,
              "chargeType" -> getChargeType(false, docDetailWithInterest),
              "interestPeriod" -> "2021-10-06 to 2022-01-06",
              "endTaxYear" -> taxYear,
              "overdue" -> docDateDetail.isOverdue
            ),
            "saUtr" -> "saUtr",
            "nationalInsuranceNumber" -> "nino",
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
            docDateDetailWithCodingOutAccepted,
            paymentBreakdown = paymentBreakdowns,
            chargeHistories = chargeHistory,
            paymentAllocations = paymentAllocation,
            agentReferenceNumber = Some("agentReferenceNumber"),
            isLateInterestCharge = false
          ).detail mustBe Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetailWithCodingOutAccepted.remainingToPay,
              "fullPaymentAmount" -> docDetailWithCodingOutRejected.originalAmount,
              "dueDate" -> docDateDetailWithCodingOutAccepted.dueDate,
              "chargeType" -> getChargeType(latePayment = false, docDetailWithCodingOutAccepted),
              "endTaxYear" -> taxYear,
              "overdue" -> docDateDetailWithCodingOutAccepted.isOverdue
            ),
            "saUtr" -> "saUtr",
            "nationalInsuranceNumber" -> "nino",
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
            docDateDetailWithCodingOutRejected,
            paymentBreakdown = paymentBreakdowns,
            chargeHistories = chargeHistory,
            paymentAllocations = paymentAllocation,
            agentReferenceNumber = Some("agentReferenceNumber"),
            isLateInterestCharge = false
          ).detail mustBe Json.obj(
            "charge" -> Json.obj(
              "remainingToPay" -> docDetailWithCodingOutRejected.remainingToPay,
              "fullPaymentAmount" -> docDetailWithCodingOutRejected.originalAmount,
              "dueDate" -> docDateDetailWithCodingOutRejected.dueDate,
              "chargeType" -> getChargeType(latePayment = false, docDetailWithCodingOutRejected),
              "endTaxYear" -> taxYear,
              "overdue" -> docDateDetailWithCodingOutRejected.isOverdue
            ),
            "saUtr" -> "saUtr",
            "nationalInsuranceNumber" -> "nino",
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
            docDateDetailWithInterest,
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
              "chargeType" -> getChargeType(latePayment = true, docDetailWithInterest),
              "interestPeriod" -> "2021-10-06 to 2022-01-06",
              "endTaxYear" -> taxYear,
              "overdue" -> docDateDetail.isOverdue
            ),
            "saUtr" -> "saUtr",
            "nationalInsuranceNumber" -> "nino",
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
              "dueDate" -> docDateDetail.dueDate,
              "chargeType" -> getChargeType(latePayment = false, docDetail),
              "endTaxYear" -> taxYear,
              "overdue" -> docDateDetail.isOverdue),
            "nationalInsuranceNumber" -> "nino",
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
