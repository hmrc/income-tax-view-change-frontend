/*
 * Copyright 2019 HM Revenue & Customs
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

package models.calculation

import auth.MtdItUser
import enums.{CalcStatus, Crystallised, Estimate}
import play.api.libs.json.{Format, Json}

sealed trait CalcDisplayResponseModel extends CrystallisedViewModel

case class CalcDisplayModel(calcTimestamp: String,
                            calcAmount: BigDecimal,
                            calcDataModel: Option[CalculationDataModel],
                            calcStatus: CalcStatus) extends CalcDisplayResponseModel with CrystallisedViewModel {

  val breakdownNonEmpty: Boolean = calcDataModel.nonEmpty
  val hasEoyEstimate: Boolean = calcDataModel.fold(false)(_.eoyEstimate.nonEmpty)

  val hasBRTSection: Boolean = calcDataModel.fold(false)(_.payPensionsProfit.basicBand.taxableIncome > 0)
  val hasHRTSection: Boolean = calcDataModel.fold(false)(_.payPensionsProfit.higherBand.taxableIncome > 0)
  val hasARTSection: Boolean = calcDataModel.fold(false)(_.payPensionsProfit.additionalBand.taxableIncome > 0)

  val hasNic2Amount: Boolean = calcDataModel.fold(false)(_.nic.class2 > 0)
  val hasNic4Amount: Boolean = calcDataModel.fold(false)(_.nic.class4 > 0)
  val hasNISection: Boolean = hasNic2Amount || hasNic4Amount

  val hasTaxReliefs: Boolean = calcDataModel.fold(false)(_.taxReliefs > 0)

  def crystallisedWithBBSInterest :Boolean = {
    calcStatus == Crystallised && calcDataModel.get.incomeReceived.bankBuildingSocietyInterest > 0
  }

  def personalAllowanceHeading: String = {
    (calcStatus, calcDataModel.get.incomeReceived.bankBuildingSocietyInterest > 0) match {
      case (Estimate, false) => ".pa-estimates"
      case (Estimate, _)     => ".pa-estimates-savings"
      case (_, false)        => ".pa-bills"
      case (_, _)            => ".pa-bills-savings"
    }
  }

  def estimatedWithBBSInterest : Boolean = {
    if(calcDataModel.get.incomeReceived.bankBuildingSocietyInterest > 0 && calcStatus == Estimate) true else false
  }
}

case object CalcDisplayError extends CalcDisplayResponseModel
case object CalcDisplayNoDataFound extends CalcDisplayResponseModel

object CalcDisplayModel {
  implicit val format: Format[CalcDisplayModel] = Json.format[CalcDisplayModel]

  def selfEmployedIncomeOrReceived[A](implicit user: MtdItUser[A], taxYear: Int, breakdown: CalculationDataModel): Boolean = {
    if((user.incomeSources.hasBusinessIncome && user.incomeSources.businesses.exists(_.accountingPeriod.determineTaxYear == taxYear)) || breakdown.incomeReceived.selfEmployment > 0) true else false
  }

  def propertyIncomeOrReceived[A](implicit user: MtdItUser[A], taxYear: Int, breakdown: CalculationDataModel): Boolean = {
    if((user.incomeSources.hasPropertyIncome && user.incomeSources.property.get.accountingPeriod.determineTaxYear == taxYear) || breakdown.incomeReceived.ukProperty > 0) true else false
  }



}
