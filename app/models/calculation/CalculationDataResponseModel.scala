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

import models._
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Json, _}

sealed trait CalculationDataResponseModel

case class CalculationDataModel(
                                 nationalRegime: Option[String] = None,
                                 totalTaxableIncome: BigDecimal,
                                 totalIncomeTaxNicYtd: BigDecimal,
                                 personalAllowance: BigDecimal,
                                 taxReliefs: BigDecimal,
                                 totalIncomeAllowancesUsed: BigDecimal,
                                 incomeReceived: IncomeReceivedModel,
                                 savingsAndGains: SavingsAndGainsModel,
                                 dividends: DividendsModel,
                                 giftAid : GiftAidModel,
                                 nic: NicModel,
                                 eoyEstimate: Option[EoyEstimate] = None,
                                 payAndPensionsProfit: PayPensionsProfitModel = PayPensionsProfitModel(0, 0, Seq())
                               ) extends CalculationDataResponseModel {

  val taxableDividendIncome: BigDecimal = dividends.totalAmount
  val taxableSavingsIncome: BigDecimal = savingsAndGains.total
  val taxableIncomeTaxIncome: BigDecimal = totalTaxableIncome - taxableDividendIncome - taxableSavingsIncome
  val savingsAllowanceSummaryData: BigDecimal = savingsAndGains.startBand.taxableIncome + savingsAndGains.zeroBand.taxableIncome
  val additionalAllowances: BigDecimal = totalIncomeAllowancesUsed - personalAllowance - savingsAllowanceSummaryData
  val taxablePayPensionsProfit: BigDecimal = payAndPensionsProfit.payAndPensionsProfitBands.map(_.income).sum
  val taxableSavingsInterest : BigDecimal = savingsAndGains.startBand.taxableIncome + savingsAndGains.zeroBand.taxableIncome + savingsAndGains.basicBand.taxableIncome + savingsAndGains.higherBand.taxableIncome + savingsAndGains.additionalBand.taxableIncome

  def hasDividendsAtSpecifiedRate(taxAmount: BigDecimal): Boolean = taxAmount > 0

  val dividendsAllowance: BigDecimal = dividends.band.filter(_.rate == 0).map(_.income).sum

  def srtSiITCalc :BigDecimal = savingsAndGains.startBand.taxableIncome
  def srtSiITAmount : BigDecimal  = savingsAndGains.startBand.taxAmount
  def zrtSiITCalc :BigDecimal = savingsAndGains.zeroBand.taxableIncome
  def zrtSiITAmount : BigDecimal  = savingsAndGains.zeroBand.taxAmount
  def brtSiITCalc :BigDecimal = savingsAndGains.basicBand.taxableIncome
  def brtSiITAmount : BigDecimal  = savingsAndGains.basicBand.taxAmount
  def hrtSiITCalc : BigDecimal = savingsAndGains.higherBand.taxableIncome
  def hrtSiITAmount : BigDecimal = savingsAndGains.higherBand.taxAmount
  def artSiITCalc : BigDecimal = savingsAndGains.additionalBand.taxableIncome
  def artSiITAmount : BigDecimal = savingsAndGains.additionalBand.taxAmount
}

case class IncomeReceivedModel(selfEmployment: BigDecimal,
                               ukProperty: BigDecimal,
                               bankBuildingSocietyInterest: BigDecimal,
                               ukDividends: BigDecimal
                              ) {
  def estimateBuisnessProfit: BigDecimal = selfEmployment + ukProperty + bankBuildingSocietyInterest

  def buisnessProfit: BigDecimal = selfEmployment + ukProperty
}

case class PayPensionsProfitModel(totalAmount: BigDecimal,
                                  taxableIncome: BigDecimal,
                                  payAndPensionsProfitBands: Seq[TaxBandModel])

case class SavingsAndGainsModel(total: BigDecimal,
                                taxableIncome: BigDecimal,
                                bands: Seq[BandModel]) {
  val defaultBand = BandModel(0, 0, 0)

  val startBand: BandModel = bands.find(_.name == "SSR").getOrElse(defaultBand)
  val zeroBand: BandModel = bands.find(_.name == "ZRT").getOrElse(defaultBand)
  val basicBand: BandModel = bands.find(_.name == "BRT").getOrElse(defaultBand)
  val higherBand: BandModel = bands.find(_.name == "HRT").getOrElse(defaultBand)
  val additionalBand: BandModel = bands.find(_.name == "ART").getOrElse(defaultBand)
}


case class DividendsModel(totalAmount: BigDecimal,
                          taxableIncome: BigDecimal,
                          band: Seq[DividendsBandModel])

case class DividendsBandModel(name: String,
                              rate: BigDecimal,
                              threshold: Option[Int],
                              apportionedThreshold: Option[Int],
                              income: BigDecimal,
                              amount : BigDecimal)

case class GiftAidModel (paymentsMade: BigDecimal,
                         rate: BigDecimal,
                         taxableAmount: BigDecimal)

case class BandModel(taxableIncome: BigDecimal,
                     taxRate: BigDecimal,
                     taxAmount: BigDecimal,
                     name: String = "")

case class TaxBandModel(name: String, rate: BigDecimal, income: BigDecimal, taxAmount: BigDecimal)

object TaxBandModel {
  implicit val format: Format[TaxBandModel] = Json.format[TaxBandModel]
}

case class NicModel(class2: BigDecimal,
                    class4: BigDecimal)

case class EoyEstimate(totalNicAmount: BigDecimal)


object CalculationDataModel {
  val defaultZero: JsPath => Reads[BigDecimal] = _.read[BigDecimal].orElse(Reads.pure[BigDecimal](0.00))
  val logFieldError: JsPath => Reads[Option[String]] = _.readNullable[String] map {
    case None => Logger.error(s"[CalculationDataResponseModel][CalculationDataModel] - National Regime field is missing from json");None
    case data => data
  }

  implicit val reads: Reads[CalculationDataModel] = (
      logFieldError(__ \ "calcOutput" \ "calcResult" \ "nationalRegime") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "totalTaxableIncome") and
      (__ \ "calcOutput" \ "calcResult" \ "incomeTaxNicYtd").read[BigDecimal] and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "annualAllowances" \ "personalAllowance") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "totalAllowancesAndReliefs") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "taxableIncome" \ "totalIncomeAllowancesUsed") and
      __.read[IncomeReceivedModel] and
      __.read[SavingsAndGainsModel] and
      __.read[DividendsModel] and
      __.read[GiftAidModel] and
      __.read[NicModel] and
        (__ \ "calcOutput" \ "calcResult" \ "eoyEstimate").readNullable[EoyEstimate].orElse(Reads.pure(None)) and
      __.read[PayPensionsProfitModel]
    ) (CalculationDataModel.apply _)

  implicit val writes: Writes[CalculationDataModel] = (
    (__ \ "nationalRegime").writeNullable[String] and
    (__ \ "totalTaxableIncome").write[BigDecimal] and
      (__ \ "totalIncomeTaxNicYtd").write[BigDecimal] and
      (__ \ "personalAllowance").write[BigDecimal] and
      (__ \ "taxReliefs").write[BigDecimal] and
      (__ \ "totalIncomeAllowancesUsed").write[BigDecimal] and
      (__ \ "incomeReceived").write[IncomeReceivedModel] and
      (__ \ "incomeTax" \ "savingsAndGains").write[SavingsAndGainsModel] and
      (__ \ "incomeTax" \ "dividends").write[DividendsModel] and
      (__ \ "giftAid").write[GiftAidModel] and
      (__ \ "nic").write[NicModel] and
      (__ \ "eoyEstimate").writeNullable[EoyEstimate] and
      (__ \ "incomeTax" \ "payPensionsProfit").write[PayPensionsProfitModel]
    )(unlift(CalculationDataModel.unapply))
}

object GiftAidModel {
  implicit val reads: Reads[GiftAidModel] = (
    defaultZero(__ \  "calcOutput" \ "calcResult" \ "incomeTax" \ "giftAid" \ "paymentsMade") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "giftAid" \ "rate") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "giftAid" \ "taxableIncome")
    ) (GiftAidModel.apply _)
  implicit val writes: Writes[GiftAidModel] = Json.writes[GiftAidModel]
}

object IncomeReceivedModel {
  implicit val reads: Reads[IncomeReceivedModel] = (
    defaultZero(__ \ "calcOutput" \ "calcResult" \ "taxableIncome" \ "incomeReceived" \ "selfEmploymentIncome") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "taxableIncome" \ "incomeReceived" \ "ukPropertyIncome") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "taxableIncome" \ "incomeReceived" \ "bbsiIncome") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "taxableIncome" \ "incomeReceived" \ "ukDividendIncome")
    ) (IncomeReceivedModel.apply _)
  implicit val writes: Writes[IncomeReceivedModel] = Json.writes[IncomeReceivedModel]
}

object SavingsAndGainsModel {
  implicit val reads: Reads[SavingsAndGainsModel] = (
  defaultZero(__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "savingsAndGains" \ "totalAmount") and
    defaultZero(__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "savingsAndGains" \ "taxableIncome") and
    (__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "savingsAndGains" \ "band").read[Seq[BandModel]].orElse(Reads.pure(Seq.empty[BandModel]))
    ) (SavingsAndGainsModel.apply _)
  implicit val writes: Writes[SavingsAndGainsModel] = Json.writes[SavingsAndGainsModel]
}

object PayPensionsProfitModel {
  implicit val reads: Reads[PayPensionsProfitModel] = (
    defaultZero(__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "payPensionsProfit" \ "totalAmount") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "payPensionsProfit" \ "taxableIncome") and
      (__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "payPensionsProfit" \ "band").read[Seq[TaxBandModel]].orElse(Reads.pure(Seq.empty[TaxBandModel]))
    ) (PayPensionsProfitModel.apply _)
  implicit val writes: Writes[PayPensionsProfitModel] = Json.writes[PayPensionsProfitModel]
}

object DividendsModel {
  implicit val reads: Reads[DividendsModel] = (
    defaultZero(__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "dividends" \ "totalAmount") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "dividends" \ "taxableIncome") and
      (__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "dividends" \ "band").read[Seq[DividendsBandModel]].orElse(Reads.pure(Nil))
    ) (DividendsModel.apply _)
  implicit val writes: Writes[DividendsModel] = Json.writes[DividendsModel]
}

object NicModel {
  implicit val reads: Reads[NicModel] = (
    defaultZero(__ \ "calcOutput" \ "calcResult" \ "nic" \ "class2" \ "amount") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "nic" \ "class4" \ "totalAmount")
    ) (NicModel.apply _)
  implicit val writes: Writes[NicModel] = Json.writes[NicModel]
}

object BandModel {

  implicit val genericBandReads: Reads[BandModel] = (
    defaultZero(__ \ "income") and defaultZero(__ \ "rate") and defaultZero(__ \ "taxAmount") and (__ \ "name").read[String]
    ) (BandModel.apply _)

  // SE Business and Property Reads
  val payPensionsProfitReadsBRT: Reads[BandModel] = (
    defaultZero(__ \ "payPensionsProfitAtBRT") and defaultZero(__ \ "rateBRT") and defaultZero(__ \ "incomeTaxOnPayPensionsProfitAtBRT") and Reads.pure("BRT")
    ) (BandModel.apply _)
  val payPensionsProfitReadsHRT: Reads[BandModel] = (
    defaultZero(__ \ "payPensionsProfitAtHRT") and defaultZero(__ \ "rateHRT") and defaultZero(__ \ "incomeTaxOnPayPensionsProfitAtHRT") and Reads.pure("HRT")
    ) (BandModel.apply _)
  val payPensionsProfitReadsART: Reads[BandModel] = (
    defaultZero(__ \ "payPensionsProfitAtART") and defaultZero(__ \ "rateART") and defaultZero(__ \ "incomeTaxOnPayPensionsProfitAtART") and Reads.pure("ART")
    ) (BandModel.apply _)

  // Bank and Building Society Interest Reads
  val interestReadsStartingRate: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtStartingRate") and Reads.pure[BigDecimal](0) and
      defaultZero(__ \ "incomeTaxOnInterestReceivedAtStartingRate") and Reads.pure("SSR")
    ) (BandModel.apply _)
  val interestReadsZeroRate: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtZeroRate") and Reads.pure[BigDecimal](0) and
      defaultZero(__ \ "incomeTaxOnInterestReceivedAtZeroRate") and Reads.pure("ZRT")
    ) (BandModel.apply _)
  val interestReadsBRT: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtBRT") and defaultZero(__ \ "rateBRT") and defaultZero(__ \ "incomeTaxOnInterestReceivedAtBRT") and Reads.pure("BRT")
    ) (BandModel.apply _)
  val interestReadsHRT: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtHRT") and defaultZero(__ \ "rateHRT") and defaultZero(__ \ "incomeTaxOnInterestReceivedAtHRT") and Reads.pure("HRT")
    ) (BandModel.apply _)
  val interestReadsART: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtART") and defaultZero(__ \ "rateART") and defaultZero(__ \ "incomeTaxOnInterestReceivedAtART") and Reads.pure("ART")
    ) (BandModel.apply _)

  // Dividends Reads
  val dividendsReadsBRT: Reads[BandModel] = (
    defaultZero(__ \ "dividendsAtBRT") and defaultZero(__ \ "dividendBRT") and defaultZero(__ \ "incomeTaxOnDividendsAtBRT") and Reads.pure("BRT")
    ) (BandModel.apply _)
  val dividendsReadsHRT: Reads[BandModel] = (
    defaultZero(__ \ "dividendsAtHRT") and defaultZero(__ \ "dividendHRT") and defaultZero(__ \ "incomeTaxOnDividendsAtHRT") and Reads.pure("HRT")
    ) (BandModel.apply _)
  val dividendsReadsART: Reads[BandModel] = (
    defaultZero(__ \ "dividendsAtART") and defaultZero(__ \ "dividendART") and defaultZero(__ \ "incomeTaxOnDividendsAtART") and Reads.pure("ART")
    ) (BandModel.apply _)

  // Implicit Writes
  implicit val writes: Writes[BandModel] = Json.writes[BandModel]
}

object EoyEstimate {
  implicit val format: Format[EoyEstimate] = Json.format[EoyEstimate]
}

case class CalculationDataErrorModel(code: Int, message: String) extends CalculationDataResponseModel

object CalculationDataErrorModel {
  implicit val format: Format[CalculationDataErrorModel] = Json.format[CalculationDataErrorModel]
}

object DividendsBandModel {
  implicit val writes: Writes[DividendsBandModel] = Json.writes[DividendsBandModel]

  implicit val reads: Reads[DividendsBandModel] = (
    (__ \ "name").read[String] and
      (__ \ "rate").read[BigDecimal] and
      (__ \ "threshold").readNullable[Int] and
      (__ \ "apportionedThreshold").readNullable[Int] and
      (__ \ "income").read[BigDecimal] and
      (__ \ "taxAmount").read[BigDecimal]
    ) (DividendsBandModel.apply _)

}
