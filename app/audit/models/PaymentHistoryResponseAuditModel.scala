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

import audit.Utilities.userAuditDetails
import auth.MtdItUser
import models.financialDetails._
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities.JsonUtil

case class PaymentHistoryResponseAuditModel(mtdItUser: MtdItUser[_],
                                            payments: Seq[Payment]) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.PaymentHistoryResponse
  override val auditType: String = enums.AuditType.PaymentHistoryResponse

  private def getPayment(payment: Payment, desc: String): JsObject = Json.obj("description" -> desc) ++
    ("paymentDate", if (payment.creditType.contains(MfaCreditType)) Some(payment.documentDate) else payment.dueDate) ++
    ("amount", payment.amount)

  private def paymentHistoryMapper(payment: Payment): Option[JsObject] = {
    val hasCredit = payment.credit.isDefined
    val hasLot    = payment.lot.isDefined && payment.lotItem.isDefined
    payment.creditType match {
      case Some(MfaCreditType)             if hasCredit                           => Some(getPayment(payment, "Credit from HMRC adjustment"))
      case Some(CutOverCreditType)         if hasCredit                           => Some(getPayment(payment, "Credit from an earlier tax year"))
      case Some(BalancingChargeCreditType) if hasCredit                           => Some(getPayment(payment, "Balancing charge credit"))
      case Some(PoaOneReconciliationCredit)if hasCredit                           =>Some(getPayment(payment, "First payment on account: credit from your tax return"))
      case Some(PoaTwoReconciliationCredit)if hasCredit                           =>Some(getPayment(payment, "Second payment on account: credit from your tax return"))
      case Some(ITSAReturnAmendmentCredit) if hasCredit                           =>Some(getPayment(payment, "Credit from your amended tax return"))
      case Some(RepaymentInterest)         if hasCredit                           => Some(getPayment(payment, "Interest on set off charge"))
      case Some(PaymentType)               if !hasCredit && hasLot                => Some(getPayment(payment, "Payment Made to HMRC"))
      case _ => None
    }
  }

  private val paymentHistory: Seq[JsObject] = payments.flatMap(paymentHistoryMapper)

  override val detail: JsValue = userAuditDetails(mtdItUser) ++
    Json.obj("paymentHistory" -> paymentHistory)

}
