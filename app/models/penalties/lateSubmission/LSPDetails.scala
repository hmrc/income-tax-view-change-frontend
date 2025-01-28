/*
 * Copyright 2025 HM Revenue & Customs
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

package models.penalties.lateSubmission

import models.penalties.appealInformation.AppealInformationType
import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class LSPDetails(penaltyNumber: String,
                      penaltyOrder: Option[String],
                      penaltyCategory: Option[LSPPenaltyCategoryEnum.Value],
                      penaltyStatus: LSPPenaltyStatusEnum.Value,
                      FAPIndicator: Option[String],
                      penaltyCreationDate: LocalDate,
                      triggeringProcess: String,
                      penaltyExpiryDate: LocalDate,
                      expiryReason: Option[String],
                      communicationsDate: Option[LocalDate],
                      lateSubmissions: Option[Seq[LateSubmission]],
                      appealInformation: Option[Seq[AppealInformationType]],
                      chargeReference: Option[String],
                      chargeAmount: Option[BigDecimal],
                      chargeOutstandingAmount: Option[BigDecimal],
                      chargeDueDate: Option[LocalDate]
                     )

object LSPDetails {
  implicit val format: Format[LSPDetails] = Json.format[LSPDetails]
}
