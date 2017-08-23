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

import play.api.libs.json.{Json, OFormat}

sealed trait CalcDisplayResponseModel
case class CalcDisplayModel(calcTimestamp: String,
                            calcAmount: BigDecimal,
                            calcDataModel: Option[CalculationDataModel]) extends CalcDisplayResponseModel {
  val breakdownNonEmpty: Boolean = calcDataModel.nonEmpty

  val hasBRTSection: Boolean = {
    if (calcDataModel.nonEmpty)
      calcDataModel.get.payPensionsProfitAtBRT.getOrElse[BigDecimal](0) > 0 || calcDataModel.get.incomeTaxOnPayPensionsProfitAtBRT > 0
    else false
  }
  val hasHRTSection: Boolean = {
    if (calcDataModel.nonEmpty)
      calcDataModel.get.payPensionsProfitAtHRT.getOrElse[BigDecimal](0) > 0 || calcDataModel.get.incomeTaxOnPayPensionsProfitAtHRT > 0
    else false
  }
  val hasARTSection: Boolean = {
    if (calcDataModel.nonEmpty)
      calcDataModel.get.payPensionsProfitAtART.getOrElse[BigDecimal](0) > 0 || calcDataModel.get.incomeTaxOnPayPensionsProfitAtART > 0
    else false
  }
  val hasNISection: Boolean = {
    if(calcDataModel.nonEmpty)
      calcDataModel.get.nationalInsuranceClass2Amount > 0 || calcDataModel.get.totalClass4Charge > 0
    else false
  }
  val hasNic2Amount: Boolean = {
    if(calcDataModel.nonEmpty)
      calcDataModel.get.nationalInsuranceClass2Amount > 0
    else false
  }
  val hasNic4Amount: Boolean = {
    if(calcDataModel.nonEmpty)
      calcDataModel.get.totalClass4Charge > 0
    else false
  }
}
case object CalcDisplayError extends CalcDisplayResponseModel
case object CalcDisplayNoDataFound extends CalcDisplayResponseModel

object CalcDisplayModel {
  implicit val format: OFormat[CalcDisplayModel] = Json.format[CalcDisplayModel]
}