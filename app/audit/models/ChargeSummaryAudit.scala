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

import auth.MtdItUser
import implicits.ImplicitCurrencyFormatter.CurrencyFormatter
import models.chargeHistory.ChargeHistoryModel
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetail, PaymentsWithChargeType}
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities._


case class ChargeSummaryAudit(mtdItUser: MtdItUser[_], docDateDetail: DocumentDetailWithDueDate,
                              paymentBreakdown: List[FinancialDetail], chargeHistories: List[ChargeHistoryModel],
                              paymentAllocations: List[PaymentsWithChargeType], isLatePaymentCharge: Boolean) extends ExtendedAuditModel {

  private val userType: JsObject = mtdItUser.userType match {
    case Some("Agent") => Json.obj("userType" -> "Agent")
    case Some(_) => Json.obj("userType" -> "Individual")
    case None => Json.obj()
  }

  val getChargeType: String = docDateDetail.documentDetail.documentDescription match {
    case Some("ITSA- POA 1") => if (isLatePaymentCharge) "Late Payment Interest on payment on account 1 of 2" else "Payment on account 1 of 2"
    case Some("ITSA - POA 2") => if (isLatePaymentCharge) "Late Payment Interest on payment on account 2 of 2" else "Payment on account 2 of 2"
    case Some("TRM New Charge") | Some("TRM Amend Charge") =>
      if (isLatePaymentCharge) "Late Payment Interest on remaining balance" else "Remaining balance"
    case error => {
      Logger("application").error(s"[Charge][getChargeTypeKey] Missing or non-matching charge type: $error found")
      "unknownCharge"
    }
  }

  private def getAllocationDescriptionFromKey(key: Option[String]): String = key match {
    case Some("chargeSummary.paymentAllocations.poa1.incomeTax") => "Income Tax for payment on account 1 of 2"
    case Some("chargeSummary.paymentAllocations.poa1.nic4") => "Class 4 National Insurance for payment on account 1 of 2"
    case Some("chargeSummary.paymentAllocations.poa2.incomeTax") => "Income Tax for payment on account 2 of 2"
    case Some("chargeSummary.paymentAllocations.poa2.nic4") => "Class 4 National Insurance for payment on account 2 of 2"
    case Some("chargeSummary.paymentAllocations.bcd.incomeTax") => "Income Tax for remaining balance"
    case Some("chargeSummary.paymentAllocations.bcd.nic2") => "Class 2 National Insurance for remaining balance"
    case Some("chargeSummary.paymentAllocations.bcd.vcnic2") => "Voluntary Class 2 National Insurance for remaining balance"
    case Some("chargeSummary.paymentAllocations.bcd.nic4") => "Class 4 National Insurance for remaining balance"
    case Some("chargeSummary.paymentAllocations.bcd.sl") => "Student Loans for remaining balance"
    case Some("chargeSummary.paymentAllocations.bcd.cgt") => "Capital Gains Tax for remaining balance"
    case Some("paymentOnAccount1.text") => "Late payment interest for payment on account 1 of 2"
    case Some("paymentOnAccount2.text") => "Late payment interest for payment on account 2 of 2"
    case Some("balancingCharge.text") => "Late payment interest for remaining balance"
    case _ => s"Some unexpected message key: $key"
  }

  private def getBreakdownTypeFromKey(key: Option[String]): String = key match {
    case Some("incomeTax") => "Income Tax"
    case Some("nic2") => "Class 2 National Insurance"
    case Some("vcnic2") => "Voluntary Class 2 National Insurance"
    case Some("nic4") => "Class 4 National Insurance"
    case Some("cgt") => "Capital Gains Tax"
    case Some("sl") => "Student Loans"
    case _ => s"Some unexpected key: $key"
  }

  private def getChargeTypeFromKey(key: Option[String]): String = key match {
    case Some("chargeSummary.chargeHistory.created.paymentOnAccount1.text") => "Payment on account 1 of 2 created"
    case Some("chargeSummary.chargeHistory.created.paymentOnAccount2.text") => "Payment on account 2 of 2 created"
    case Some("chargeSummary.chargeHistory.created.balancingCharge.text") => "Remaining balance created"
    case Some("chargeSummary.chargeHistory.request.paymentOnAccount1.text") => "Payment on account 1 of 2 reduced by taxpayer request"
    case Some("chargeSummary.chargeHistory.request.paymentOnAccount2.text") => "Payment on account 2 of 2 reduced by taxpayer request"
    case Some("chargeSummary.chargeHistory.request.balancingCharge.text") => "Remaining balance reduced by taxpayer request"
    case Some("chargeSummary.chargeHistory.amend.paymentOnAccount1.text") => "Payment on account 1 of 2 reduced due to amended return"
    case Some("chargeSummary.chargeHistory.amend.paymentOnAccount2.text") => "Payment on account 2 of 2 reduced due to amended return"
    case Some("chargeSummary.chargeHistory.amend.balancingCharge.text") => "Remaining balance reduced due to amended return"
    case _ => s"Some unexpected message key: $key"
  }

  private val interestPeriod: Option[String] = (docDateDetail.documentDetail.interestFromDate, docDateDetail.documentDetail.interestEndDate) match {
    case (Some(fromDate), Some(endDate)) => Some(fromDate + " to " + endDate)
    case _ => None
  }

  private val chargeHistory: Seq[JsObject] = chargeHistories.map(chargeHistoryJson)

  private def chargeHistoryJson(chargeHistory: ChargeHistoryModel): JsObject = Json.obj(
    "date" -> chargeHistory.reversalDate,
    "description" -> getChargeTypeFromKey(Some(s"chargeSummary.chargeHistory.${chargeHistory.reasonCode}.${docDateDetail.documentDetail.getChargeTypeKey()}")),
    "amount" -> chargeHistory.totalAmount
  )

  private val paymentAllocationsChargeHistory: Seq[JsObject] =
    if (!isLatePaymentCharge) paymentAllocations.flatMap(paymentAllocationsChargeHistoryJSon)
    else Seq.empty

  private def paymentAllocationsChargeHistoryJSon(paymentAllocation: PaymentsWithChargeType): Seq[JsObject] =
    paymentAllocation.payments.map(payment => Json.obj() ++
      ("date", payment.date) ++
      ("description", Some(getAllocationDescriptionFromKey(paymentAllocation.getPaymentAllocationTextInChargeSummary))) ++
      ("amount", payment.amount.map(_.abs))
    )

  private val paymentBreakdowns: Seq[JsObject] = paymentBreakdown.map(paymentBreakdownsJson)

  private def paymentBreakdownsJson(paymentBreakdown: FinancialDetail): JsObject = Json.obj(
    "breakdownType" -> getBreakdownTypeFromKey(paymentBreakdown.messageKeyForChargeType),
    "total" -> paymentBreakdown.originalAmount,
    "chargeUnderReview" -> paymentBreakdown.dunningLockExists,
    "interestLock" -> paymentBreakdown.interestLockExists)

  private val fullPaymentAmount = if (isLatePaymentCharge)
    docDateDetail.documentDetail.latePaymentInterestAmount else docDateDetail.documentDetail.originalAmount

  private val remainingToPay = if (isLatePaymentCharge)
    docDateDetail.documentDetail.interestRemainingToPay else docDateDetail.documentDetail.remainingToPay

  private val dueDate = if (isLatePaymentCharge) docDateDetail.documentDetail.interestEndDate else docDateDetail.dueDate

  private val chargeDetails: JsObject = Json.obj(
    "chargeType" -> getChargeType) ++
    ("interestPeriod", interestPeriod) ++
    ("dueDate", dueDate) ++
    ("fullPaymentAmount", fullPaymentAmount) ++
    Json.obj("remainingToPay" -> remainingToPay)

  def release6Update: JsObject = {
    Json.obj("paymentBreakdown" -> paymentBreakdowns) ++
      Json.obj("chargeHistory" -> chargeHistory) ++
      Json.obj("paymentAllocationsChargeHistory" -> paymentAllocationsChargeHistory)
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
