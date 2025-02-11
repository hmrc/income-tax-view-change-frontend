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
import play.api.libs.json.{Format, JsResult, JsValue}
import utils.JsonUtils

import java.time.LocalDate

case class LSPDetails(penaltyNumber: String,
                      penaltyOrder: Option[String],
                      penaltyCategory: Option[LSPPenaltyCategoryEnum],
                      penaltyStatus: LSPPenaltyStatusEnum,
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

object LSPDetails extends JsonUtils {
  implicit val format: Format[LSPDetails] = new Format[LSPDetails] {
    override def reads(json: JsValue): JsResult[LSPDetails] = {
      for {
        penaltyNumber <- (json \ "penaltyNumber").validate[String]
        penaltyOrder <- (json \ "penaltyOrder").validateOpt[String]
        penaltyCategory <- (json \ "penaltyCategory").validateOpt[LSPPenaltyCategoryEnum](LSPPenaltyCategoryEnum.format)
        penaltyStatus <- (json \ "penaltyStatus").validate[LSPPenaltyStatusEnum](LSPPenaltyStatusEnum.format)
        fapIndicator <- (json \ "FAPIndicator").validateOpt[String]
        penaltyCreationDate <- (json \ "penaltyCreationDate").validate[LocalDate]
        triggeringProcess <- (json \ "triggeringProcess").validate[String]
        penaltyExpiryDate <- (json \ "penaltyExpiryDate").validate[LocalDate]
        expiryReason <- (json \ "expiryReason").validateOpt[String]
        communicationsDate <- (json \ "communicationsDate").validateOpt[LocalDate]
        lateSubmissions <- (json \ "lateSubmissions").validateOpt[Seq[LateSubmission]]
        appealInformation <- (json \ "appealInformation").validateOpt[Seq[AppealInformationType]]
        chargeReference <- (json \ "chargeReference").validateOpt[String]
        chargeAmount <- (json \ "chargeAmount").validateOpt[BigDecimal]
        chargeOutstandingAmount <- (json \ "chargeOutstandingAmount").validateOpt[BigDecimal]
        chargeDueDate <- (json \ "chargeDueDate").validateOpt[LocalDate]
      } yield LSPDetails(penaltyNumber, penaltyOrder, penaltyCategory, penaltyStatus, fapIndicator, penaltyCreationDate,
        triggeringProcess, penaltyExpiryDate, expiryReason, communicationsDate, lateSubmissions, appealInformation,
        chargeReference, chargeAmount, chargeOutstandingAmount, chargeDueDate
      )
    }

    override def writes(o: LSPDetails): JsValue =
      jsonObjNoNulls(
        "penaltyNumber" -> o.penaltyNumber,
        "penaltyOrder" -> o.penaltyOrder,
        "penaltyCategory" -> o.penaltyCategory,
        "penaltyStatus" -> o.penaltyStatus,
        "FAPIndicator" -> o.FAPIndicator,
        "penaltyCreationDate" -> o.penaltyCreationDate,
        "triggeringProcess" -> o.triggeringProcess,
        "penaltyExpiryDate" -> o.penaltyExpiryDate,
        "expiryReason" -> o.expiryReason,
        "communicationsDate" -> o.communicationsDate,
        "lateSubmissions" -> o.lateSubmissions,
        "appealInformation" -> o.appealInformation,
        "chargeReference" -> o.chargeReference,
        "chargeAmount" -> o.chargeAmount,
        "chargeOutstandingAmount" -> o.chargeOutstandingAmount,
        "chargeDueDate" -> o.chargeDueDate
      )
  }
}
