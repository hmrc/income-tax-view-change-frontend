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
import models.financialDetails.Payment
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities.JsonUtil

case class PaymentHistoryResponseAuditModel(mtdItUser: MtdItUser[_],
                                            payments: Seq[Payment],
                                            CutOverCreditsEnabled: Boolean,
                                            MFACreditsEnabled: Boolean,
                                            ) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.PaymentHistoryResponse
  override val auditType: String = enums.AuditType.PaymentHistoryResponse

  private def getPayment(payment: Payment, desc: String): JsObject = Json.obj("description" -> desc) ++
    ("paymentDate", if (payment.validMFACreditType()) Some(payment.documentDate) else payment.dueDate) ++
    ("amount", payment.amount)

  private def paymentHistoryMapper(payment: Payment): Option[JsObject] = {
    val isCredit = payment.credit.isDefined
    val isMFA = payment.validMFACreditType()
    (isCredit, isMFA) match {
      case (true, true) =>
        if (MFACreditsEnabled) Some(getPayment(payment, "Credit from HMRC adjustment")) else None
      case (true, false) =>
        if (CutOverCreditsEnabled) Some(getPayment(payment, "Credit from an earlier tax year")) else None
      case (false, _) => Some(getPayment(payment, "Payment Made to HMRC"))
    }
  }


  private val paymentHistory: Seq[JsObject] = payments.flatMap(paymentHistoryMapper)

  override val detail: JsValue = userAuditDetails(mtdItUser) ++
    Json.obj("paymentHistory" -> paymentHistory)

}
