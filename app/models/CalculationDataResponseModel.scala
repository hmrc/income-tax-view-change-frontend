/*
 * Copyright 2018 HM Revenue & Customs
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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Json, OFormat, Reads, _}

sealed trait CalculationDataResponseModel

case class CalculationDataModel(
                                 incomeTaxYTD: BigDecimal,
                                 incomeTaxThisPeriod: BigDecimal,
                                 profitFromSelfEmployment: BigDecimal,
                                 profitFromUkLandAndProperty: BigDecimal,
                                 totalIncomeReceived: BigDecimal,
                                 proportionAllowance: BigDecimal,
                                 totalIncomeOnWhichTaxIsDue: BigDecimal,
                                 payPensionsProfitAtBRT: BigDecimal,
                                 incomeTaxOnPayPensionsProfitAtBRT: BigDecimal,
                                 payPensionsProfitAtHRT: BigDecimal,
                                 incomeTaxOnPayPensionsProfitAtHRT: BigDecimal,
                                 payPensionsProfitAtART: BigDecimal,
                                 incomeTaxOnPayPensionsProfitAtART: BigDecimal,
                                 incomeTaxDue: BigDecimal,
                                 totalClass4Charge: BigDecimal,
                                 nationalInsuranceClass2Amount: BigDecimal,
                                 rateBRT: BigDecimal,
                                 rateHRT: BigDecimal,
                                 rateART: BigDecimal,
                                 eoyEstimate: Option[EoyEstimate] = None
                               ) extends CalculationDataResponseModel

case class CalculationDataErrorModel(code: Int, message: String) extends CalculationDataResponseModel

case class EoyEstimate(incomeTaxNicAmount: BigDecimal)

object CalculationDataModel {
  val defaultZero: JsPath => Reads[BigDecimal] = _.read[BigDecimal].orElse(Reads.pure[BigDecimal](BigDecimal(0)))
  implicit val reads: Reads[CalculationDataModel] = (
    (__ \ "incomeTaxYTD").read[BigDecimal] and
      (__ \ "incomeTaxThisPeriod").read[BigDecimal] and
      defaultZero(__ \ "profitFromSelfEmployment") and
      defaultZero(__ \ "profitFromUkLandAndProperty") and
      defaultZero(__ \ "totalIncomeReceived") and
      defaultZero(__ \ "proportionAllowance") and
      defaultZero(__ \ "totalIncomeOnWhichTaxIsDue") and
      defaultZero(__ \ "payPensionsProfitAtBRT") and
      defaultZero(__ \ "incomeTaxOnPayPensionsProfitAtBRT") and
      defaultZero(__ \ "payPensionsProfitAtHRT") and
      defaultZero(__ \ "incomeTaxOnPayPensionsProfitAtHRT") and
      defaultZero(__ \ "payPensionsProfitAtART") and
      defaultZero(__ \ "incomeTaxOnPayPensionsProfitAtART") and
      defaultZero(__ \ "incomeTaxDue") and
      defaultZero(__ \ "totalClass4Charge") and
      defaultZero(__ \ "nationalInsuranceClass2Amount") and
      defaultZero(__ \ "rateBRT") and
      defaultZero(__ \ "rateHRT") and
      defaultZero(__ \ "rateART") and
      (__ \ "eoyEstimate").readNullable[EoyEstimate]
    ) (CalculationDataModel.apply _)
  implicit val writes: Writes[CalculationDataModel] = Json.writes[CalculationDataModel]
}


object CalculationDataErrorModel {
  implicit val format: OFormat[CalculationDataErrorModel] = Json.format[CalculationDataErrorModel]
}

object EoyEstimate {
  implicit val format: OFormat[EoyEstimate] = Json.format[EoyEstimate]
}

case class ApiCalculationResponse(
                                    incomeTaxYTD: BigDecimal,
                                    incomeTaxThisPeriod: BigDecimal,
                                    profitFromSelfEmployment: Option[BigDecimal],
                                    profitFromUkLandAndProperty: Option[BigDecimal],
                                    totalIncomeReceived: Option[BigDecimal],
                                    proportionAllowance: Option[BigDecimal],
                                    totalIncomeOnWhichTaxIsDue: Option[BigDecimal],
                                    payPensionsProfitAtBRT: Option[BigDecimal],
                                    incomeTaxOnPayPensionsProfitAtBRT: Option[BigDecimal],
                                    payPensionsProfitAtHRT: Option[BigDecimal],
                                    incomeTaxOnPayPensionsProfitAtHRT: Option[BigDecimal],
                                    payPensionsProfitAtART: Option[BigDecimal],
                                    incomeTaxOnPayPensionsProfitAtART: Option[BigDecimal],
                                    incomeTaxDue: Option[BigDecimal],
                                    totalClass4Charge: Option[BigDecimal],
                                    nationalInsuranceClass2Amount: Option[BigDecimal],
                                    rateBRT: Option[BigDecimal],
                                    rateHRT: Option[BigDecimal],
                                    rateART: Option[BigDecimal],
                                    eoyEstimate: Option[EoyEstimate]
                                  )

object ApiCalculationResponse {
  implicit val format: Format[ApiCalculationResponse] = Json.format[ApiCalculationResponse]
}
