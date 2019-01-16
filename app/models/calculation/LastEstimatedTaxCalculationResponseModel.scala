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

import enums.{CalcStatus, Estimate}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Format, Json, Reads, _}

sealed trait LastTaxCalculationResponseModel

case class LastTaxCalculation(calcID: String,
                              calcTimestamp: String,
                              calcAmount: BigDecimal,
                              calcStatus: CalcStatus) extends LastTaxCalculationResponseModel

case class LastTaxCalculationWithYear(calculation: LastTaxCalculationResponseModel,
                                      taxYear: Int) {

  lazy val matchesStatus: CalcStatus => Boolean = status => calculation match {
    case model: LastTaxCalculation => model.calcStatus == status
    case _ => false
  }

  lazy val isErrored: Boolean = calculation match {
    case _: LastTaxCalculationError => true
    case _ => false
  }
}

case class LastTaxCalculationError(status: Int, message: String) extends LastTaxCalculationResponseModel
case object NoLastTaxCalculation extends LastTaxCalculationResponseModel


object LastTaxCalculation {
  implicit val reads: Reads[LastTaxCalculation] = (
    (__ \ "calcID").read[String] and
      (__ \ "calcTimestamp").read[String] and
      (__ \ "calcAmount").read[BigDecimal] and
      ((__ \ "calcStatus").read[CalcStatus] or Reads.pure[CalcStatus](Estimate))
    ) (LastTaxCalculation.apply _)
  implicit val writes: Writes[LastTaxCalculation] = Json.writes[LastTaxCalculation]
}

object LastTaxCalculationError {
  implicit val format: Format[LastTaxCalculationError] = Json.format[LastTaxCalculationError]
}
