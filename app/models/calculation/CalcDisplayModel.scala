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

import config.FrontendAppConfig
import enums.CalcStatus
import auth.MtdItUser
import enums.{CalcStatus, Crystallised, Estimate}
import play.api.libs.json.{Format, Json}

import implicits.ImplicitCurrencyFormatter._

sealed trait CalcDisplayResponseModel extends CrystallisedViewModel

case class CalcDisplayModel(calcTimestamp: String,
                            calcAmount: BigDecimal,
                            calcDataModel: Option[CalculationDataModel],
                            calcStatus: CalcStatus) extends CalcDisplayResponseModel with CrystallisedViewModel {

  val breakdownNonEmpty: Boolean = calcDataModel.nonEmpty
  val hasEoyEstimate: Boolean = calcDataModel.fold(false)(_.eoyEstimate.nonEmpty)
  val hasSRTSiSection: Boolean = calcDataModel.fold(false)(_.savingsAndGains.startBand.taxableIncome > 0)
  val hasZRTSiSection: Boolean = calcDataModel.fold(false)(_.savingsAndGains.zeroBand.taxableIncome > 0)
  val hasBRTSiSection: Boolean = calcDataModel.fold(false)(_.savingsAndGains.basicBand.taxableIncome > 0)
  val hasHRTSiSection: Boolean = calcDataModel.fold(false)(_.savingsAndGains.higherBand.taxableIncome > 0)
  val hasARTSiSection: Boolean = calcDataModel.fold(false)(_.savingsAndGains.additionalBand.taxableIncome > 0)

  val hasNic2Amount: Boolean = calcDataModel.fold(false)(_.nic.class2 > 0)
  val hasNic4Amount: Boolean = calcDataModel.fold(false)(_.nic.class4 > 0)
  val hasNISection: Boolean = hasNic2Amount || hasNic4Amount

  val hasTaxReliefs: Boolean = calcDataModel.fold(false)(_.taxReliefs > 0)
  val whatYouOwe : String = s"${calcDataModel.fold(calcAmount.toCurrency)(_.totalIncomeTaxNicYtd.toCurrency)}"

  def displayCalcBreakdown(appConfig: FrontendAppConfig): Boolean = {
    breakdownNonEmpty && appConfig.features.calcBreakdownEnabled()
  }
  def crystallisedWithBBSInterest :Boolean = {
    calcStatus == Crystallised && calcDataModel.get.incomeReceived.bankBuildingSocietyInterest > 0
  }

  def savingsAllowanceHeading: String = {
    (calcStatus) match {
      case (Estimate)     => ".pa-estimates-savings"
      case (_)            => ".pa-bills-savings"
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
    (user.incomeSources.hasBusinessIncome && user.incomeSources.businesses.exists(_.accountingPeriod.determineTaxYear == taxYear)) || breakdown.incomeReceived.selfEmployment > 0
  }

  def propertyIncomeOrReceived[A](implicit user: MtdItUser[A], taxYear: Int, breakdown: CalculationDataModel): Boolean = {
    (user.incomeSources.hasPropertyIncome && user.incomeSources.property.get.accountingPeriod.determineTaxYear == taxYear) || breakdown.incomeReceived.ukProperty > 0
  }



}
