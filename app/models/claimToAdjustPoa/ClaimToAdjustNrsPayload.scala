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

package models.claimToAdjustPoa

import play.api.libs.json.{Json, OWrites}

case class ClaimToAdjustNrsPayload(
  credId: Option[String],
  saUtr:  Option[String],
  nino:   String,

  // Required data items (audit mapping)
  clientIP:                        Option[String],
  deviceCookie:                    Option[String],  // if available
  sessionId:                       Option[String],  // X-Session-ID
  userType:                        Option[String],  // Individual / Agent
  generatedAt:                     String,          // ISO8601 instant
  sessionCookie:                   Option[String],  // if available
  isDecreased:                     Boolean,
  previousPaymentOnAccountAmount:  BigDecimal,
  requestedPaymentOnAccountAmount: BigDecimal,
  adjustmentReasonCode:            String,
  adjustmentReasonDescription:     String,
  mtditId:                         String
)

object ClaimToAdjustNrsPayload {
  implicit val writes: OWrites[ClaimToAdjustNrsPayload] = Json.writes[ClaimToAdjustNrsPayload]
}
