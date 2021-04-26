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
                       totalIncomeReceived: Option[BigDecimal] = None,
                       totalTaxableIncome: Option[BigDecimal] = None,
                       incomeTaxNicAmount: Option[BigDecimal] = None,
                       timestamp: Option[String] = None,
                       crystallised: Boolean,
                       nationalRegime: Option[String] = None,
                       payPensionsProfit: PayPensionsProfit = PayPensionsProfit(),
                       savingsAndGains: SavingsAndGains = SavingsAndGains(),
                       reductionsAndCharges: ReductionsAndCharges = ReductionsAndCharges(),
                       dividends: Dividends = Dividends(),
                       allowancesAndDeductions: AllowancesAndDeductions = AllowancesAndDeductions(),
                       nic: Nic = Nic(),
                       taxDeductedAtSource: TaxDeductedAtSource = TaxDeductedAtSource(),
                       lumpSums: LumpSums = LumpSums(),
                       gainsOnLifePolicies: GainsOnLifePolicies = GainsOnLifePolicies()
                      ) extends CalculationResponseModel with CrystallisedViewModel

object Calculation {
  implicit val reads: Reads[Calculation] = (
    readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "totalIncomeTaxAndNicsDue") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "totalIncomeTaxNicsCharged") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "summary" \ "totalIncomeReceivedFromAllSources") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "summary" \ "totalTaxableIncome") and
      readNullable[BigDecimal](__ \ "endOfYearEstimate" \ "summary" \ "incomeTaxNicAmount") and
      readNullable[String](__ \ "metadata" \ "calculationTimestamp") and
      (__ \ "metadata" \ "crystallised").read[Boolean] and
      readNullable[String](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "taxRegime") and
      __.read[PayPensionsProfit] and
      __.read[SavingsAndGains] and
      __.read[ReductionsAndCharges] and
      __.read[Dividends] and
      __.read[AllowancesAndDeductions] and
      __.read[Nic] and
      __.read[TaxDeductedAtSource] and
      __.read[LumpSums] and
      __.read[GainsOnLifePolicies]
    ) (Calculation.apply _)
  implicit val writes: OWrites[Calculation] = Json.writes[Calculation]
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

  val isCalculation: Boolean = model match {
    case _: Calculation => true
    case _ => false
  }
}
