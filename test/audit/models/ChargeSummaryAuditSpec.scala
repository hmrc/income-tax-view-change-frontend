/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate

import testConstants.BaseTestConstants._
import testConstants.FinancialDetailsTestConstants.financialDetail
import auth.MtdItUser
import models.chargeHistory.ChargeHistoryModel
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate, FinancialDetail, Payment, PaymentsWithChargeType}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.retrieve.Name

class ChargeSummaryAuditSpec extends WordSpecLike with MustMatchers {

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

  def paymentsWithCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal): PaymentsWithChargeType =
    PaymentsWithChargeType(
      payments = List(Payment(reference = Some("reference"), amount = Some(amount), method = Some("method"),
        lot = Some("lot"), lotItem = Some("lotItem"), date = Some(date), transactionId = None)),
      mainType = Some(mainType) , chargeType = Some(chargeType))

  val paymentAllocation: List[PaymentsWithChargeType] = List(
    paymentsWithCharge("SA Payment on Account 1", "ITSA NI", "2018-03-30", -1500.0),
    paymentsWithCharge("SA Payment on Account 1", "NIC4 Scotland", "2018-03-31", -1600.0)
  )

  val chargeHistoryModel: ChargeHistoryModel = ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 7, 6).toString, "documentDescription", 1500, LocalDate.of(2018, 7, 6), "amended return")
  val chargeHistoryModel2: ChargeHistoryModel = ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 7, 6).toString, "documentDescription", 1500, LocalDate.of(2018, 7, 6), "Customer Request")

  val chargeHistory: List[ChargeHistoryModel] = List(
    chargeHistoryModel,
    chargeHistoryModel2)

  val paymentBreakdowns: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = "ITSA England & NI", dunningLock = Some("Stand over order"), interestLock = Some("Manual RPI Signal")),
    financialDetail(originalAmount = 2345.67, chargeType = "NIC2-GB", interestLock = Some("Breathing Space Moratorium Act")),
    financialDetail(originalAmount = 3456.78, chargeType = "Voluntary NIC2-NI", dunningLock = Some("Stand over order")),
    financialDetail(originalAmount = 9876.54, chargeType = "CGT"))

  val docDateDetail: DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetail,
    dueDate = Some(LocalDate.now())
  )

  val docDateDetailWithInterest: DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetailWithInterest,
    dueDate = Some(LocalDate.now())
  )

  def getChargeType(latePayment: Boolean): String = docDetail.documentDescription match {
    case Some("ITSA- POA 1") => if(latePayment)"Late Payment Interest on payment on account 1 of 2" else "Payment on account 1 of 2"
    case Some("ITSA - POA 2") => if(latePayment)"Late Payment Interest on payment on account 2 of 2" else "Payment on account 2 of 2"
    case Some("TRM New Charge") | Some("TRM Amend Charge") =>
      if(latePayment)"Late Payment Interest on remaining balance" else "Remaining balance"
    case error =>
      Logger("application").error(s"[Charge][getChargeTypeKey] Missing or non-matching charge type: $error found")
      "unknownCharge"
  }

  def chargeSummaryAuditFull(userType: Option[String] = Some("Agent"),
                             docDateDetails: DocumentDetailWithDueDate, paymentBreakdown: List[FinancialDetail],
                             chargeHistories: List[ChargeHistoryModel], paymentAllocations: List[PaymentsWithChargeType],
                             agentReferenceNumber: Option[String] = Some("agentReferenceNumber"), isLateInterestCharge:Boolean = true): ChargeSummaryAudit = ChargeSummaryAudit(
    mtdItUser = MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = Some(Name(Some("firstName"), Some("lastName"))),
      incomeSources = IncomeSourceDetailsModel("mtditid", None, Nil, None),
      saUtr = Some("saUtr"),
      credId = Some("credId"),
      userType = userType,
      arn = agentReferenceNumber
    ),
    docDateDetail = docDateDetailWithInterest,
    paymentBreakdown = if(!isLateInterestCharge) paymentBreakdowns else List.empty,
    chargeHistories = if(!isLateInterestCharge) chargeHistory else List.empty,
    paymentAllocations = paymentAllocation,
    agentReferenceNumber = Some("agentReferenceNumber"),
    txmEventsR6 = true,
    isLatePaymentCharge = isLateInterestCharge
  )

  val chargeSummaryAuditMin: ChargeSummaryAudit = ChargeSummaryAudit(
    mtdItUser = MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = None,
      incomeSources = IncomeSourceDetailsModel("mtditid", None, List.empty, None),
      saUtr = None,
      credId = None,
      userType = None,
      arn = None
    ),
    docDateDetail = docDateDetail,
    paymentBreakdown = List.empty,
    chargeHistories = List.empty,
    paymentAllocations = List.empty,
    agentReferenceNumber = None,
    txmEventsR6 = true,
    isLatePaymentCharge = false
  )

  "ChargeSummaryAudit(mtdItUser, charge, agentReferenceNumber)" should {

    s"have the correct transaction name of '$transactionName'" in {
      chargeSummaryAuditFull(None,
        docDateDetail,
        paymentBreakdown = paymentBreakdowns,
        chargeHistories = chargeHistory,
        paymentAllocations = paymentAllocation,
        agentReferenceNumber = Some("arn")
      ).transactionName mustBe transactionName
    }

    s"have the correct audit event type of '$auditType'" in {
      chargeSummaryAuditFull(None,
        docDateDetail,
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
            userType = Some("Agent"),
            docDateDetail,
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
              "chargeType" -> getChargeType(false),
              "interestPeriod" -> "2021-10-06 to 2022-01-06"
            ),
            "saUtr" -> "saUtr",
            "nationalInsuranceNumber" -> "nino",
            "paymentBreakdown" -> Json.arr(
              Json.obj(
                "breakdownType" -> "Income Tax",
                "total" -> 123.45,
                "chargeUnderReview" -> true,
                "interestLock" -> true
              ),
              Json.obj(
                "breakdownType" -> "Class 2 National Insurance",
                "total" -> 2345.67,
                "chargeUnderReview" -> false,
                "interestLock" -> true
              ),
              Json.obj(
                "breakdownType"->"Voluntary Class 2 National Insurance",
                "total" -> 3456.78,
                "chargeUnderReview" -> true,
                "interestLock"-> false
              ),
              Json.obj(
                "breakdownType" -> "Capital Gains Tax",
                "total" -> 9876.54,
                "chargeUnderReview" -> false,
                "interestLock" -> false
              )
            ),
            "paymentAllocationsChargeHistory" -> Json.arr(
              Json.obj(
                "amount" -> 1500,
                "date" -> "2018-03-30",
                "description" -> "Income Tax for payment on account 1 of 2"),
              Json.obj(
                "amount" -> 1600,
                "date" -> "2018-03-31",
                "description"-> "Class 4 National Insurance for payment on account 1 of 2")
            ),
            "agentReferenceNumber" -> "agentReferenceNumber",
            "chargeHistory" -> Json.arr(
                Json.obj(
                  "date" -> "2018-07-06",
                  "description" -> "Payment on account 1 of 2 reduced due to amended return",
                  "amount" -> 1500
                ),
                Json.obj(
                  "date" -> "2018-07-06",
                  "description" -> "Payment on account 1 of 2 reduced by taxpayer request",
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
            userType = Some("Agent"),
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
              "chargeType" -> getChargeType(true),
              "interestPeriod" -> "2021-10-06 to 2022-01-06"
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
              "chargeType" -> getChargeType(false)),
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
