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
import enums.AuditType.ChargeSummary
import enums.{Poa1Charge, Poa2Charge, TRMAmmendCharge, TRMNewCharge}
import models.chargeHistory.ChargeHistoryModel
import models.chargeSummary.PaymentHistoryAllocations
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetail}
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.Utilities._


case class ChargeSummaryAudit(mtdItUser: MtdItUser[_], docDateDetail: DocumentDetailWithDueDate,
                              paymentBreakdown: List[FinancialDetail], chargeHistories: List[ChargeHistoryModel],
                              paymentAllocations: List[PaymentHistoryAllocations], isLatePaymentCharge: Boolean,
                              isMFADebit: Boolean = false, taxYear: Int) extends ExtendedAuditModel {

  private val userType: JsObject = mtdItUser.userType match {
    case Some(Agent) => Json.obj("userType" -> "Agent")
    case Some(_) => Json.obj("userType" -> "Individual")
    case None => Json.obj()
  }

  val getChargeType: String = (docDateDetail.documentDetail.getDocType, docDateDetail.documentDetail.documentText) match {
    case _ if isMFADebit => "MFADebit"
    case (_, Some(text)) if text.contains("Cancelled PAYE Self Assessment") => "Cancelled PAYE Self Assessment (through your PAYE tax code)"
    case (_, Some(text)) if text.contains("Balancing payment collected through PAYE tax code") => "Balancing payment collected through PAYE tax code"
    case (Poa1Charge, _) => if (isLatePaymentCharge) "Late Payment Interest on payment on account 1 of 2" else "Payment on account 1 of 2"
    case (Poa2Charge, _) => if (isLatePaymentCharge) "Late Payment Interest on payment on account 2 of 2" else "Payment on account 2 of 2"
    case (TRMNewCharge, _) | (TRMAmmendCharge, _) =>
      if (isLatePaymentCharge) "Late Payment Interest on remaining balance" else "Remaining balance"
    case error => {
      Logger("application").error(s"Missing or non-matching charge type: $error found")
      "unknownCharge"
    }
  }

  private def getAllocationDescriptionFromKey(key: Option[String]): String = {
    key match {
      case Some("chargeSummary.paymentAllocations.mfaDebit") => "Payment put towards HMRC adjustment"
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
      case Some("codingOut.cancelled") => "Cancelled PAYE Self Assessment (through your PAYE tax code)"
      case Some("codingOut.accepted") =>
        s"Amount collected through your PAYE tax code for ${taxYear + 1} to ${taxYear + 2} tax year"
      case _ => s"Some unexpected message key: $key"
    }
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
    case Some("chargeSummary.chargeHistory.adjustment.paymentOnAccount1.text") => "You updated your first payment on account"
    case Some("chargeSummary.chargeHistory.adjustment.paymentOnAccount2.text") => "You updated your second payment on account"
    case _ => s"Some unexpected message key: $key"
  }

  private val interestPeriod: Option[String] = (docDateDetail.documentDetail.interestFromDate, docDateDetail.documentDetail.interestEndDate) match {
    case (Some(fromDate), Some(endDate)) => Some(s"$fromDate to $endDate")
    case _ => None
  }

  private val chargeHistory: Seq[JsObject] = chargeHistories.map(chargeHistoryJson)

  private def chargeHistoryJson(chargeHistory: ChargeHistoryModel): JsObject = Json.obj(
    "date" -> chargeHistory.reversalDate,
    "description" -> getChargeTypeFromKey(Some(s"chargeSummary.chargeHistory.${chargeHistory.reasonCode}.${docDateDetail.documentDetail.getChargeTypeKey()}")),
    "amount" -> chargeHistory.totalAmount
  )

  private val paymentAllocationsChargeHistory: Seq[JsObject] = {
    if (!isLatePaymentCharge) paymentAllocations.flatMap(paymentAllocationsChargeHistoryJSon)
    else Seq.empty
  }

  private def paymentAllocationsChargeHistoryJSon(paymentAllocation: PaymentHistoryAllocations): Seq[JsObject] = {
    val description = if(docDateDetail.documentDetail.isPayeSelfAssessment){
     Some(getAllocationDescriptionFromKey(Some("codingOut.accepted")))
   } else if (docDateDetail.documentDetail.isCancelledPayeSelfAssessment) {
     Some(getAllocationDescriptionFromKey(Some("codingOut.cancelled")))
   } else {
     Some(getAllocationDescriptionFromKey(paymentAllocation.getPaymentAllocationTextInChargeSummary))
   }

    paymentAllocation.allocations.map(payment => Json.obj() ++
      ("date", payment.dueDate) ++
      ("description", description) ++
      ("amount", payment.amount.map(_.abs))
    )
  }

  private val paymentBreakdowns: Seq[JsObject] = paymentBreakdown.map(paymentBreakdownsJson)

  private def paymentBreakdownsJson(paymentBreakdown: FinancialDetail): JsObject = Json.obj(
    "breakdownType" -> getBreakdownTypeFromKey(paymentBreakdown.messageKeyForChargeType),
    "total" -> paymentBreakdown.originalAmount,
    "chargeUnderReview" -> paymentBreakdown.dunningLockExists,
    "interestLock" -> paymentBreakdown.interestLockExists)

  private val fullPaymentAmount = if (isLatePaymentCharge)
    docDateDetail.documentDetail.latePaymentInterestAmount else Option(docDateDetail.documentDetail.originalAmount)

  private val remainingToPay = if (isLatePaymentCharge)
    docDateDetail.documentDetail.interestRemainingToPay else docDateDetail.documentDetail.remainingToPay

  private val dueDate = if (isLatePaymentCharge) docDateDetail.documentDetail.interestEndDate else docDateDetail.dueDate

  private val chargeDetails: JsObject = Json.obj(
    "chargeType" -> getChargeType) ++
    ("interestPeriod", interestPeriod) ++
    ("dueDate", dueDate) ++
    ("fullPaymentAmount", fullPaymentAmount) ++
    Json.obj( "endTaxYear" -> taxYear) ++
    Json.obj("overdue"-> docDateDetail.isOverdue) ++
    Json.obj("remainingToPay" -> remainingToPay)

  def release6Update: JsObject = {
    Json.obj("paymentBreakdown" -> paymentBreakdowns) ++
      Json.obj("chargeHistory" -> chargeHistory) ++
      Json.obj("paymentAllocationsChargeHistory" -> paymentAllocationsChargeHistory)
  }

  override val transactionName: String = enums.TransactionName.ChargeSummary
  override val detail: JsValue = {
    Json.obj("nino" -> mtdItUser.nino,
      "mtditid" -> mtdItUser.mtditid) ++
      userType ++
      ("agentReferenceNumber", mtdItUser.arn) ++
      ("saUtr", mtdItUser.saUtr) ++
      ("credId", mtdItUser.credId) ++
      Json.obj("charge" -> chargeDetails) ++
      release6Update
  }
  override val auditType: String = ChargeSummary

}
