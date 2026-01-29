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

import audit.Utilities
import auth.MtdItUser
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.language.implicitConversions

case class AdjustPaymentsOnAccountAuditModel(isSuccessful: Boolean,
                                             previousPaymentOnAccountAmount: BigDecimal,
                                             requestedPaymentOnAccountAmount: BigDecimal,
                                             adjustmentReasonCode: String,
                                             adjustmentReasonDescription: String,
                                             isDecreased: Boolean
                                            )(implicit user: MtdItUser[_]) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.TransactionName.AdjustPaymentsOnAccount

  override val auditType: String = enums.AuditType.AuditType.AdjustPaymentsOnAccount

  private val successOutcome: JsObject = Json.obj(
    "outcome" ->
      Json.obj("isSuccessful" -> isSuccessful)
  )

  private val failureOutcome: JsObject = Json.obj(
    "outcome" ->
      Json.obj(
        "isSuccessful" -> isSuccessful,
        "failureCategory" -> "API_FAILURE",
        "failureReason" -> "API 1773 returned errors - not able to update payments on account"
      )
  )

  val outcome: JsObject = if (isSuccessful) successOutcome else failureOutcome

  override val detail: JsValue =
    Utilities.userAuditDetails(user) ++ outcome ++
      Json.obj(
        "previousPaymentOnAccountAmount" -> previousPaymentOnAccountAmount,
        "requestedPaymentOnAccountAmount" -> requestedPaymentOnAccountAmount,
        "adjustmentReasonCode" -> adjustmentReasonCode,
        "adjustmentReasonDescription" -> adjustmentReasonDescription,
        "isDecreased" -> isDecreased
      )
}
