/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json._

import scala.math.BigDecimal.RoundingMode

case class ClaimToAdjustPoaRequest(
  nino: String,
  taxYear: String,
  amount: BigDecimal,
  poaAdjustmentReason: SelectYourReason)

object ClaimToAdjustPoaRequest {

  private val currencyWrites: Writes[BigDecimal] = new Writes[BigDecimal] {
    def writes(o: BigDecimal): JsValue = {
      JsNumber(o.setScale(2, RoundingMode.HALF_UP))
    }
  }

  implicit val claimToAdjustPoaRequestWrites: Writes[ClaimToAdjustPoaRequest] =
    Json.writes[ClaimToAdjustPoaRequest]

}
