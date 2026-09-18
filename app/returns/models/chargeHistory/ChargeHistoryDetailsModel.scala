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

package returns.models.chargeHistory

import financials.utils.JsonUtils
import play.api.libs.json.{Format, Json}
import shared.enums.ChargeClassificationType

import java.time.{LocalDate, LocalDateTime}


case class ChargeHistoryDetailsModel(taxYear: String,
                                     documentId: String,
                                     documentDate: LocalDate,
                                     documentDescription: String,
                                     totalAmount: BigDecimal,
                                     reversalDate: LocalDateTime,
                                     reversalReason: String,
                                     poaAdjustmentReason: Option[String],
                                     chargeClassificationType: Option[ChargeClassificationType]) 

object ChargeHistoryDetailsModel extends JsonUtils {
  implicit val format: Format[ChargeHistoryDetailsModel] = Json.format[ChargeHistoryDetailsModel]
}
