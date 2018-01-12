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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Json, OFormat, Reads, _}

sealed trait CalculationDataResponseModel

case class CalculationDataModel(
                                 totalTaxableIncome: BigDecimal,
                                 totalIncomeTaxNicYtd: BigDecimal,
                                 personalAllowance: BigDecimal,
                                 incomeReceived: IncomeReceivedModel,
                                 payPensionsProfit: PayPensionsProfitModel,
                                 savingsAndGains: SavingsAndGainsModel,
                                 dividends: DividendsModel,
                                 nic: NicModel,
                                 eoyEstimate: Option[EoyEstimate] = None) extends CalculationDataResponseModel

case class IncomeReceivedModel(selfEmployment: BigDecimal,
                               ukProperty: BigDecimal,
                               bankBuildingSocietyInterest: BigDecimal,
                               ukDividends: BigDecimal)

case class PayPensionsProfitModel(basicBand: BandModel,
                                  higherBand: BandModel,
                                  additionalBand: BandModel)

case class SavingsAndGainsModel(startBand: BandModel,
                                zeroBand: BandModel,
                                basicBand: BandModel,
                                higherBand: BandModel,
                                additionalBand: BandModel)

case class DividendsModel(allowance: BigDecimal,
                          basicBand: BandModel,
                          higherBand: BandModel,
                          additionalBand: BandModel)

case class BandModel(taxableIncome: BigDecimal,
                     taxRate: BigDecimal,
                     taxAmount: BigDecimal)

case class NicModel(class2: BigDecimal,
                    class4: BigDecimal)

case class EoyEstimate(incomeTaxNicAmount: BigDecimal)


object CalculationDataModel {
  val defaultZero: JsPath => Reads[BigDecimal] = _.read[BigDecimal].orElse(Reads.pure[BigDecimal](BigDecimal(0)))
  implicit val reads: Reads[CalculationDataModel] = (
    defaultZero(__ \ "totalIncomeOnWhichTaxIsDue") and
      (__ \ "incomeTaxYTD").read[BigDecimal] and
      defaultZero(__ \ "proportionAllowance") and
      __.read[IncomeReceivedModel] and
      __.read[PayPensionsProfitModel] and
      __.read[SavingsAndGainsModel] and
      __.read[DividendsModel] and
      __.read[NicModel] and
      (__ \ "eoyEstimate").readNullable[EoyEstimate]
    ) (CalculationDataModel.apply _)
  implicit val writes: Writes[CalculationDataModel] = Json.writes[CalculationDataModel]
}

object IncomeReceivedModel {
  implicit val reads: Reads[IncomeReceivedModel] = (
    defaultZero(__ \ "profitFromSelfEmployment") and
      defaultZero(__ \ "profitFromUkLandAndProperty") and
      defaultZero(__ \ "interestReceivedFromUkBanksAndBuildingSocieties") and
      defaultZero(__ \ "dividendsFromUkCompanies")
    )(IncomeReceivedModel.apply _)
  implicit val writes: Writes[IncomeReceivedModel] = Json.writes[IncomeReceivedModel]
}

object PayPensionsProfitModel {
  implicit val reads: Reads[PayPensionsProfitModel] = (
    __.read[BandModel](BandModel.payPensionsProfitReadsBRT) and
      __.read[BandModel](BandModel.payPensionsProfitReadsHRT) and
      __.read[BandModel](BandModel.payPensionsProfitReadsART)
    )(PayPensionsProfitModel.apply _)
  implicit val writes: Writes[PayPensionsProfitModel] = Json.writes[PayPensionsProfitModel]
}

object SavingsAndGainsModel {
  implicit val reads: Reads[SavingsAndGainsModel] = (
    __.read[BandModel](BandModel.interestReadsStartingRate) and
      __.read[BandModel](BandModel.interestReadsZeroRate) and
      __.read[BandModel](BandModel.interestReadsBRT) and
      __.read[BandModel](BandModel.interestReadsHRT) and
      __.read[BandModel](BandModel.interestReadsART)
    )(SavingsAndGainsModel.apply _)
  implicit val writes: Writes[SavingsAndGainsModel] = Json.writes[SavingsAndGainsModel]
}

object DividendsModel {
  implicit val reads: Reads[DividendsModel] = (
    defaultZero(__ \ "dividendAllowance") and
      __.read[BandModel](BandModel.dividendsReadsBRT) and
      __.read[BandModel](BandModel.dividendsReadsHRT) and
      __.read[BandModel](BandModel.dividendsReadsART)
    )(DividendsModel.apply _)
  implicit val writes: Writes[DividendsModel] = Json.writes[DividendsModel]
}

object NicModel {
  implicit val reads: Reads[NicModel] = (
    defaultZero(__ \ "nationalInsuranceClass2Amount") and
      defaultZero(__ \ "totalClass4Charge")
    )(NicModel.apply _)
  implicit val writes: Writes[NicModel] = Json.writes[NicModel]
}

object BandModel {

  // SE Business and Property Reads
  val payPensionsProfitReadsBRT: Reads[BandModel] = (
    defaultZero(__ \ "payPensionsProfitAtBRT") and defaultZero(__ \ "rateBRT") and defaultZero(__ \ "incomeTaxOnPayPensionsProfitAtBRT")
    )(BandModel.apply _)
  val payPensionsProfitReadsHRT: Reads[BandModel] = (
    defaultZero(__ \ "payPensionsProfitAtHRT") and defaultZero(__ \ "rateHRT") and defaultZero(__ \ "incomeTaxOnPayPensionsProfitAtHRT")
    )(BandModel.apply _)
  val payPensionsProfitReadsART: Reads[BandModel] = (
    defaultZero(__ \ "payPensionsProfitAtART") and defaultZero(__ \ "rateART") and defaultZero(__ \ "incomeTaxOnPayPensionsProfitAtART")
    )(BandModel.apply _)

  // Bank and Building Society Interest Reads
  val interestReadsStartingRate: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtStartingRate") and Reads.pure[BigDecimal](0) and defaultZero(__ \ "incomeTaxOnInterestReceivedAtStartingRate")
    )(BandModel.apply _)
  val interestReadsZeroRate: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtZeroRate") and Reads.pure[BigDecimal](0) and defaultZero(__ \ "incomeTaxOnInterestReceivedAtZeroRate")
    )(BandModel.apply _)
  val interestReadsBRT: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtBRT") and defaultZero(__ \ "rateBRT") and defaultZero(__ \ "incomeTaxOnInterestReceivedAtBRT")
    )(BandModel.apply _)
  val interestReadsHRT: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtHRT") and defaultZero(__ \ "rateHRT") and defaultZero(__ \ "incomeTaxOnInterestReceivedAtHRT")
    )(BandModel.apply _)
  val interestReadsART: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtART") and defaultZero(__ \ "rateART") and defaultZero(__ \ "incomeTaxOnInterestReceivedAtART")
    )(BandModel.apply _)

  // Dividends Reads
  val dividendsReadsBRT: Reads[BandModel] = (
    defaultZero(__ \ "dividendsAtBRT") and defaultZero(__ \ "dividendBRT") and defaultZero(__ \ "incomeTaxOnDividendsAtBRT")
    )(BandModel.apply _)
  val dividendsReadsHRT: Reads[BandModel] = (
    defaultZero(__ \ "dividendsAtHRT") and defaultZero(__ \ "dividendHRT") and defaultZero(__ \ "incomeTaxOnDividendsAtHRT")
    )(BandModel.apply _)
  val dividendsReadsART: Reads[BandModel] = (
    defaultZero(__ \ "dividendsAtART") and defaultZero(__ \ "dividendART") and defaultZero(__ \ "incomeTaxOnDividendsAtART")
    )(BandModel.apply _)

  // Implicit Writes
  implicit val writes: Writes[BandModel] = Json.writes[BandModel]
}

object EoyEstimate {
  implicit val format: OFormat[EoyEstimate] = Json.format[EoyEstimate]
}

case class CalculationDataErrorModel(code: Int, message: String) extends CalculationDataResponseModel

object CalculationDataErrorModel {
  implicit val format: OFormat[CalculationDataErrorModel] = Json.format[CalculationDataErrorModel]
}