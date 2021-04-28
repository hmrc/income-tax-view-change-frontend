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

import audit.Utilities.userAuditDetails
import auth.MtdItUser
import models.financialDetails.Payment
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities.JsonUtil

case class PaymentHistoryResponseAuditModel(mtdItUser: MtdItUser[_],
                                            payments: Seq[Payment]) extends ExtendedAuditModel {

  override val transactionName: String = "payment-history-response"
  override val auditType: String = "PaymentHistoryResponse"

  private def paymentHistoryDetail(payment: Payment): JsObject = Json.obj() ++
    ("paymentDate", payment.date) ++
    ("amount", payment.amount)

  private val paymentHistory: Seq[JsObject] = payments.map(paymentHistoryDetail)

  override val detail: JsValue = userAuditDetails(mtdItUser) ++
    Json.obj("paymentHistory" -> paymentHistory)

}
