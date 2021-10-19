/*
 * Copyright 2021 HM Revenue & Customs
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
import models.chargeHistory.ChargeHistoryModel
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetail, Payment, PaymentsWithChargeType}
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities._


case class ChargeSummaryAudit(mtdItUser: MtdItUser[_], docDateDetail: DocumentDetailWithDueDate,
                              paymentBreakdown: List[FinancialDetail], chargeHistories: List[ChargeHistoryModel],
                              paymentAllocations: List[PaymentsWithChargeType], agentReferenceNumber: Option[String],
                              txmEventsR6: Boolean, isLatePaymentCharge: Boolean) extends ExtendedAuditModel {

  private val userType: JsObject = mtdItUser.userType match {
    case Some("Agent") => Json.obj("userType" -> "Agent")
    case Some(_) => Json.obj("userType" -> "Individual")
    case None => Json.obj()
  }

  val getChargeType: String = docDateDetail.documentDetail.documentDescription match {
    case Some("ITSA- POA 1") => "Payment on account 1 of 2"
    case Some("ITSA - POA 2") => "Payment on account 2 of 2"
    case Some("TRM New Charge") | Some("TRM Amend Charge") => "Remaining balance"
    case error => {
      Logger("application").error(s"[Charge][getChargeTypeKey] Missing or non-matching charge type: $error found")
      "unknownCharge"
    }
  }

  private val interestPeriod: Option[String] = (docDateDetail.documentDetail.interestFromDate, docDateDetail.documentDetail.interestEndDate) match {
    case (Some(fromDate), Some(endDate)) => Some(fromDate + " to " + endDate)
    case _ => None
  }

  private val chargeHistory: Seq[JsObject] = chargeHistories.map(chargeHistoryJson)

  private def chargeHistoryJson(chargeHistory: ChargeHistoryModel): JsObject = Json.obj(
    "date" -> chargeHistory.documentDate,
    "description" -> chargeHistory.documentDescription,
    "amount" -> chargeHistory.totalAmount
  )

  private val paymentAllocationsChargeHistory: Seq[JsObject] =
    if(!isLatePaymentCharge) paymentAllocations.flatMap(paymentAllocationsChargeHistoryJSon)
    else Seq.empty

  private def paymentAllocationsChargeHistoryJSon(paymentAllocation: PaymentsWithChargeType): Seq[JsObject] =
    paymentAllocation.payments.map( payment => Json.obj()++
      ("date", payment.date)++
      ("description", payment.lotItem)++
      ("amount", payment.amount)
  )

  private val paymentBreakdowns: Seq[JsObject] = paymentBreakdown.map(paymentBreakdownsJson)

  private def paymentBreakdownsJson(paymentBreakdown: FinancialDetail): JsObject = Json.obj(
    "breakdownType" -> paymentBreakdown.messageKeyForChargeType,
    "total" -> paymentBreakdown.originalAmount,
    "chargeUnderReview" -> paymentBreakdown.dunningLockExists,
    "interestLock" -> paymentBreakdown.interestLockExists)

  private val chargeDetails: JsObject = Json.obj(
    "chargeType" -> getChargeType)++
    ("interestPeriod", interestPeriod) ++
    ("dueDate", docDateDetail.dueDate) ++
    ("fullPaymentAmount", docDateDetail.documentDetail.originalAmount) ++
    Json.obj("remainingToPay" -> docDateDetail.documentDetail.remainingToPay)

  def release6Update: JsObject = {
    if (txmEventsR6) {
      Json.obj("paymentBreakdown" -> paymentBreakdowns) ++
      Json.obj("chargeHistory" -> chargeHistory)++
      Json.obj("paymentAllocationsChargeHistory" -> paymentAllocationsChargeHistory)
    } else Json.obj()
  }

  override val transactionName: String = "charge-summary"
  override val detail: JsValue = {
    Json.obj("nationalInsuranceNumber" -> mtdItUser.nino,
      "mtditid" -> mtdItUser.mtditid) ++
      userType ++
      ("agentReferenceNumber", mtdItUser.arn) ++
      ("saUtr", mtdItUser.saUtr) ++
      ("credId", mtdItUser.credId) ++
      Json.obj("charge" -> chargeDetails) ++
      release6Update
  }
  override val auditType: String = "ChargeSummary"

}
