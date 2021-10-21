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

package models.chargeHistory

import play.api.Logger
import play.api.libs.json.{Format, Json}

import java.time.LocalDate


case class ChargeHistoryModel(taxYear: String,
															 documentId: String,
                               documentDate: String,
                               documentDescription: String,
                               totalAmount: BigDecimal,
                               reversalDate: LocalDate,
                               reversalReason: String) {

	val reasonCode: String = reversalReason match {
		case "amended return" => "amend"
		case "Customer Request" => "request"
		case error =>
			Logger("application").error(s"[ChargeHistoryModel][reasonCode] Missing or non-matching history reason: $error found")
			"unrecognisedReason"
	}
}

  object ChargeHistoryModel {
    implicit val format: Format[ChargeHistoryModel] = Json.format[ChargeHistoryModel]
}
