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

package models.penalties.latePayment

import models.penalties.appealInformation.AppealInformationType
import play.api.libs.json.{Format, JsResult, JsValue}
import utils.JsonUtils

import java.time.LocalDate

case class LPPDetails(
                       principalChargeReference: String,
                       penaltyCategory: LPPPenaltyCategoryEnum,
                       penaltyStatus: LPPPenaltyStatusEnum,
                       penaltyAmountAccruing: BigDecimal,
                       penaltyAmountPosted: BigDecimal,
                       penaltyAmountPaid: Option[BigDecimal],
                       penaltyAmountOutstanding: Option[BigDecimal],
                       LPP1LRCalculationAmount: Option[BigDecimal],
                       LPP1LRDays: Option[String],
                       LPP1LRPercentage: Option[BigDecimal],
                       LPP1HRCalculationAmount: Option[BigDecimal],
                       LPP1HRDays: Option[String],
                       LPP1HRPercentage: Option[BigDecimal],
                       LPP2Days: Option[String],
                       LPP2Percentage: Option[BigDecimal],
                       penaltyChargeCreationDate: Option[LocalDate],
                       communicationsDate: Option[LocalDate],
                       penaltyChargeReference: Option[String],
                       penaltyChargeDueDate: Option[LocalDate],
                       appealInformation: Option[Seq[AppealInformationType]],
                       principalChargeDocNumber: String,
                       principalChargeMainTransaction: String,
                       principalChargeSubTransaction: String,
                       principalChargeBillingFrom: LocalDate,
                       principalChargeBillingTo: LocalDate,
                       principalChargeDueDate: LocalDate,
                       principalChargeLatestClearing: Option[LocalDate],
                       timeToPay: Option[Seq[TimeToPay]]
                     )

object LPPDetails extends JsonUtils {
  implicit val format: Format[LPPDetails] = new Format[LPPDetails] {
    override def reads(json: JsValue): JsResult[LPPDetails] = {
      for {
        principleChargeReference <- (json \ "principalChargeReference").validate[String]
        penaltyCategory <- (json \ "penaltyCategory").validate[LPPPenaltyCategoryEnum](LPPPenaltyCategoryEnum.reads)
        penaltyStatus <- (json \ "penaltyStatus").validate[LPPPenaltyStatusEnum](LPPPenaltyStatusEnum.format)
        penaltyAmountAccruing <- (json \ "penaltyAmountAccruing").validate[BigDecimal]
        penaltyAmountPosted <- (json \ "penaltyAmountPosted").validate[BigDecimal]
        penaltyAmountPaid <- (json \ "penaltyAmountPaid").validateOpt[BigDecimal]
        penaltyAmountOutstanding <- (json \ "penaltyAmountOutstanding").validateOpt[BigDecimal]
        lpp1LRCalculationAmount <- (json \ "LPP1LRCalculationAmount").validateOpt[BigDecimal]
        lpp1LRDays <- (json \ "LPP1LRDays").validateOpt[String]
        lpp1LRPercentage <- (json \ "LPP1LRPercentage").validateOpt[BigDecimal]
        lpp1HRCalculationAmount <- (json \ "LPP1HRCalculationAmount").validateOpt[BigDecimal]
        lpp1HRDays <- (json \ "LPP1HRDays").validateOpt[String]
        lpp1HRPercentage <- (json \ "LPP1HRPercentage").validateOpt[BigDecimal]
        lpp2Days <- (json \ "LPP2Days").validateOpt[String]
        lpp2Percentage <- (json \ "LPP2Percentage").validateOpt[BigDecimal]
        penaltyChargeCreationDate <- (json \ "penaltyChargeCreationDate").validateOpt[LocalDate]
        communicationsDate <- (json \ "communicationsDate").validateOpt[LocalDate]
        penaltyChargeReference <- (json \ "penaltyChargeReference").validateOpt[String]
        penaltyChargeDueDate <- (json \ "penaltyChargeDueDate").validateOpt[LocalDate]
        appealInformation <- (json \ "appealInformation").validateOpt[Seq[AppealInformationType]]
        principalChargeDocNumber <- (json \ "principalChargeDocNumber").validate[String]
        principalChargeMainTransaction <- (json \ "principalChargeMainTransaction").validate[String]
        principalChargeSubTransaction <- (json \ "principalChargeSubTransaction").validate[String]
        principalChargeBillingFrom <- (json \ "principalChargeBillingFrom").validate[LocalDate]
        principalChargeBillingTo <- (json \ "principalChargeBillingTo").validate[LocalDate]
        principalChargeDueDate <- (json \ "principalChargeDueDate").validate[LocalDate]
        principalChargeLatestClearing <- (json \ "principalChargeLatestClearing").validateOpt[LocalDate]
        timeToPay <- (json \ "timeToPay").validateOpt[Seq[TimeToPay]]
      } yield LPPDetails(principleChargeReference, penaltyCategory, penaltyStatus, penaltyAmountAccruing, penaltyAmountPosted,
        penaltyAmountPaid, penaltyAmountOutstanding, lpp1LRCalculationAmount, lpp1LRDays, lpp1LRPercentage, lpp1HRCalculationAmount,
        lpp1HRDays, lpp1HRPercentage, lpp2Days, lpp2Percentage, penaltyChargeCreationDate, communicationsDate, penaltyChargeReference,
        penaltyChargeDueDate, appealInformation, principalChargeDocNumber, principalChargeMainTransaction, principalChargeSubTransaction,
        principalChargeBillingFrom, principalChargeBillingTo, principalChargeDueDate, principalChargeLatestClearing, timeToPay)
    }

    override def writes(o: LPPDetails): JsValue =
      jsonObjNoNulls(
        "principalChargeReference" -> o.principalChargeReference,
        "penaltyCategory" -> o.penaltyCategory,
        "penaltyStatus" -> o.penaltyStatus,
        "penaltyAmountAccruing" -> o.penaltyAmountAccruing,
        "penaltyAmountPosted" -> o.penaltyAmountPosted,
        "penaltyAmountPaid" -> o.penaltyAmountPaid,
        "penaltyAmountOutstanding" -> o.penaltyAmountOutstanding,
        "LPP1LRCalculationAmount" -> o.LPP1LRCalculationAmount,
        "LPP1LRDays" -> o.LPP1LRDays,
        "LPP1LRPercentage" -> o.LPP1LRPercentage,
        "LPP1HRCalculationAmount" -> o.LPP1HRCalculationAmount,
        "LPP1HRDays" -> o.LPP1HRDays,
        "LPP1HRPercentage" -> o.LPP1HRPercentage,
        "LPP2Days" -> o.LPP2Days,
        "LPP2Percentage" -> o.LPP2Percentage,
        "penaltyChargeCreationDate" -> o.penaltyChargeCreationDate,
        "communicationsDate" -> o.communicationsDate,
        "penaltyChargeReference" -> o.penaltyChargeReference,
        "penaltyChargeDueDate" -> o.penaltyChargeDueDate,
        "appealInformation" -> o.appealInformation,
        "principalChargeDocNumber" -> o.principalChargeDocNumber,
        "principalChargeMainTransaction" -> o.principalChargeMainTransaction,
        "principalChargeSubTransaction" -> o.principalChargeSubTransaction,
        "principalChargeBillingFrom" -> o.principalChargeBillingFrom,
        "principalChargeBillingTo" -> o.principalChargeBillingTo,
        "principalChargeDueDate" -> o.principalChargeDueDate,
        "principalChargeLatestClearing" -> o.principalChargeLatestClearing,
        "timeToPay" -> o.timeToPay
      )
  }
}