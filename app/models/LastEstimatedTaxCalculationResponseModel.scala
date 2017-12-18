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
import enums.{CalcStatus, Estimate}

import scala.concurrent.Future

sealed trait LastTaxCalculationResponseModel

case class LastTaxCalculation(calcID: String,
                              calcTimestamp: String,
                              calcAmount: BigDecimal,
                              calcStatus: Option[CalcStatus] = None) extends LastTaxCalculationResponseModel

case class LastTaxCalculationWithYear(calculation: LastTaxCalculationResponseModel,
                                      taxYear: Int) {
  val matchesStatus: CalcStatus => Boolean = status => calculation match {
    case model: LastTaxCalculation if model.calcStatus.isDefined => if(model.calcStatus.get == status) true else false
    case _ => false
  }
}

case class LastTaxCalculationError(status: Int, message: String) extends LastTaxCalculationResponseModel
case object NoLastTaxCalculation extends LastTaxCalculationResponseModel


object LastTaxCalculation {
  implicit val format: OFormat[LastTaxCalculation] = Json.format[LastTaxCalculation]
}

object LastTaxCalculationError {
  implicit val format: OFormat[LastTaxCalculationError] = Json.format[LastTaxCalculationError]
}
