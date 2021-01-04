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

package models.calculation

import enums.{CalcStatus, Crystallised, Estimate}
import implicits.ImplicitCurrencyFormatter._
import play.api.libs.json.{Format, Json}

sealed trait CalcDisplayResponseModel extends CrystallisedViewModel

case class CalcDisplayModel(calcTimestamp: String,
                            calcAmount: BigDecimal,
                            calcDataModel: Calculation,
                            calcStatus: CalcStatus) extends CalcDisplayResponseModel with CrystallisedViewModel {

  val whatYouOwe : String = s"${calcAmount.toCurrency}"

  def crystallisedWithBBSInterest :Boolean = {
    calcStatus == Crystallised && calcDataModel.savingsAndGains.taxableIncome.exists(_ > 0)
  }

  def savingsAllowanceHeading: String = {
    calcStatus match {
      case Estimate     => ".pa-estimates-savings"
      case _            => ".pa-bills-savings"
    }
  }

  def estimatedWithBBSInterest : Boolean = {
    calcDataModel.savingsAndGains.taxableIncome.exists(_ > 0) && calcStatus == Estimate
  }
}

case object CalcDisplayError extends CalcDisplayResponseModel
case object CalcDisplayNoDataFound extends CalcDisplayResponseModel

object CalcDisplayModel {
  implicit val format: Format[CalcDisplayModel] = Json.format[CalcDisplayModel]
}
