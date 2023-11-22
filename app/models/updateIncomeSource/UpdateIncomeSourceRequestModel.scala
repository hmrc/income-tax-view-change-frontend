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

package models.updateIncomeSource

import models.incomeSourceDetails.incomeSourceIds.IncomeSourceId
import play.api.libs.json.{Format, Json}

import java.time.LocalDate

sealed trait UpdateIncomeSourceRequest

case class TaxYearSpecific(taxYear: String, latencyIndicator: Boolean)

object TaxYearSpecific {
  implicit val format: Format[TaxYearSpecific] = Json.format
}

case class Cessation(cessationIndicator: Boolean, cessationDate: Option[LocalDate])

object Cessation {
  implicit val format: Format[Cessation] = Json.format
}

case class UpdateIncomeSourceRequestModel(nino: String,
                                          incomeSourceID: IncomeSourceId,
                                          cessation: Option[Cessation] = None,
                                          taxYearSpecific: Option[TaxYearSpecific] = None) extends UpdateIncomeSourceRequest

object UpdateIncomeSourceRequestModel {
  implicit val format: Format[UpdateIncomeSourceRequestModel] = Json.format
}

case class UpdateIncomeSourceRequestError(reason: String) extends UpdateIncomeSourceRequest

object UpdateIncomeSourceRequestError {
  implicit val format: Format[UpdateIncomeSourceRequestError] = Json.format
}
