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

package models.calculation

import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait CalculationResponseModel

case class CalculationErrorModel(code: Int,
                                 message: String) extends CalculationResponseModel

object CalculationErrorModel {
  implicit val format: Format[CalculationErrorModel] =
    Json.format[CalculationErrorModel]
}

case class CalculationModel(calcId: String,
                            calcAmount: Option[BigDecimal],
                            calcTimestamp: Option[String],
                            crystallised: Option[Boolean],
                            incomeTaxNicYtd: Option[BigDecimal],
                            incomeTaxNicAmount: Option[BigDecimal]) extends CalculationResponseModel {

  val displayAmount: Option[BigDecimal] = (calcAmount, incomeTaxNicYtd) match {
    case (_, Some(result))    => Some(result)
    case (Some(result), None) => Some(result)
    case (None, None)         => None
  }

  val isBill: Boolean = crystallised.isDefined
}

object CalculationModel {
  implicit val writes: Writes[CalculationModel] =
    Json.writes[CalculationModel]

  implicit val reads: Reads[CalculationModel] = (
    (JsPath \\ "calcID").read[String] and
    (JsPath \\ "calcAmount").readNullable[BigDecimal] and
    (JsPath \\ "calcTimestamp").readNullable[String] and
    (JsPath \\ "crystallised").readNullable[Boolean] and
    (JsPath \\ "incomeTaxNicYtd").readNullable[BigDecimal] and
    (JsPath \\ "incomeTaxNicAmount").readNullable[BigDecimal]
  ) (CalculationModel.apply _)
}




