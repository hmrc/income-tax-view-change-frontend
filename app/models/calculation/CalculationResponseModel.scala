/*
 * Copyright 2020 HM Revenue & Customs
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
import models.readNullable
import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait CalculationResponseModel

case class CalculationErrorModel(code: Int, message: String) extends CalculationResponseModel

object CalculationErrorModel {
  implicit val format: Format[CalculationErrorModel] = Json.format[CalculationErrorModel]
}

case class Calculation(totalIncomeTaxAndNicsDue: Option[BigDecimal] = None,
                       totalIncomeTaxNicsCharged: Option[BigDecimal] = None,
                       totalTaxableIncome: Option[BigDecimal] = None,
                       incomeTaxNicAmount: Option[BigDecimal] = None,
                       timestamp: Option[String] = None,
                       crystallised: Boolean,
                       nationalRegime: Option[String] = None,
                       payPensionsProfit: PayPensionsProfit = PayPensionsProfit(),
                       savingsAndGains: SavingsAndGains = SavingsAndGains(),
                       dividends: Dividends = Dividends(),
                       allowancesAndDeductions: AllowancesAndDeductions = AllowancesAndDeductions(),
                       nic: Nic = Nic(),
                       giftAid: GiftAid = GiftAid()) extends CalculationResponseModel with CrystallisedViewModel

object Calculation {
  implicit val reads: Reads[Calculation] = (
    readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "totalIncomeTaxAndNicsDue") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "totalIncomeTaxNicsCharged") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "summary" \ "totalTaxableIncome") and
      readNullable[BigDecimal](__ \ "endOfYearEstimate" \ "summary" \ "incomeTaxNicAmount") and
      readNullable[String](__ \ "metadata" \ "calculationTimestamp") and
      (__ \ "metadata" \ "crystallised").read[Boolean] and
      readNullable[String](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "taxRegime") and
      __.read[PayPensionsProfit] and
      __.read[SavingsAndGains] and
      __.read[Dividends] and
      __.read[AllowancesAndDeductions] and
      __.read[Nic] and
      __.read[GiftAid]
    ) (Calculation.apply _)
  implicit val writes: OWrites[Calculation] = Json.writes[Calculation]
}

case class CalculationModel(calcID: String, // not in new model
                            calcAmount: Option[BigDecimal], // calculation.totalIncomeTaxAndNicsDue
                            calcTimestamp: Option[String], // calculation.timestamp
                            crystallised: Option[Boolean], // calculation.crystallised
                            incomeTaxNicYtd: Option[BigDecimal], // calculation.totalIncomeTaxNicsCharged
                            incomeTaxNicAmount: Option[BigDecimal], // calculation.eoyEstimate.incomeTaxNicAmount
                            calculationDataModel: Option[CalculationDataModel] = None // see within
                           ) extends CalculationResponseModel with CrystallisedViewModel {

  val displayAmount: Option[BigDecimal] = (calcAmount, incomeTaxNicYtd) match {
    case (_, Some(result)) => Some(result)
    case (Some(result), None) => Some(result)
    case (None, None) => None
  }

  val isBill: Boolean = crystallised.getOrElse(false)
  val status: CalcStatus = if (isBill) Crystallised else Estimate
}

case class CalculationResponseModelWithYear(model: CalculationResponseModel, year: Int) {

  val isError: Boolean = model match {
    case CalculationErrorModel(status, _) if status >= 500 => true
    case _ => false
  }

  val notCrystallised: Boolean = model match {
    case model: Calculation => !model.crystallised
    case _ => false
  }

  val isCrystallised: Boolean = model match {
    case model: Calculation => model.crystallised
    case _ => false
  }
}

object CalculationModel {
  implicit val writes: Writes[CalculationModel] = Json.writes[CalculationModel]

  implicit val reads: Reads[CalculationModel] = (
    (JsPath \\ "calcOutput" \ "calcID").read[String] and
      (JsPath \\ "calcOutput" \ "calcAmount").readNullable[BigDecimal] and
      (JsPath \\ "calcOutput" \ "calcTimestamp").readNullable[String] and
      (JsPath \\ "calcOutput" \ "crystallised").readNullable[Boolean] and
      (JsPath \\ "calcOutput" \ "calcResult" \ "incomeTaxNicYtd").readNullable[BigDecimal].orElse(Reads.pure(None)) and
      (JsPath \\ "calcOutput" \ "calcResult" \ "eoyEstimate" \ "incomeTaxNicAmount").readNullable[BigDecimal].orElse(Reads.pure(None)) and
      JsPath.read[CalculationDataModel].map(x => Option(x)).orElse(Reads.pure(None))
    ) (CalculationModel.apply _)
}
