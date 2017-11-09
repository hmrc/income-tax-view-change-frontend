/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.libs.json.{OFormat, Json}

sealed trait CalculationDataResponseModel

case class CalculationDataModel(
                                 incomeTaxYTD: BigDecimal,
                                 incomeTaxThisPeriod: BigDecimal,
                                 profitFromSelfEmployment: BigDecimal,
                                 profitFromUkLandAndProperty: BigDecimal,
                                 totalIncomeReceived: BigDecimal,
                                 proportionAllowance: BigDecimal,
                                 totalIncomeOnWhichTaxIsDue: BigDecimal,
                                 payPensionsProfitAtBRT: Option[BigDecimal],
                                 incomeTaxOnPayPensionsProfitAtBRT: BigDecimal,
                                 payPensionsProfitAtHRT: Option[BigDecimal],
                                 incomeTaxOnPayPensionsProfitAtHRT: BigDecimal,
                                 payPensionsProfitAtART: Option[BigDecimal],
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
  implicit val format: OFormat[CalculationDataModel] = Json.format[CalculationDataModel]
}


object CalculationDataErrorModel {
  implicit val format: OFormat[CalculationDataErrorModel] = Json.format[CalculationDataErrorModel]
}

object EoyEstimate {
  implicit val format: OFormat[EoyEstimate] = Json.format[EoyEstimate]
}