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

import models._
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Json, _}

sealed trait CalculationDataResponseModel

case class CalculationDataModel(
                                 nationalRegime: Option[String] = None, // calculation.nationalRegime
                                 totalTaxableIncome: BigDecimal, // calculation.totalTaxableIncome
                                 totalIncomeTaxNicYtd: BigDecimal, // calculation.totalIncomeTaxNicsCharged
                                 annualAllowances: AnnualAllowances, // see within
                                 taxReliefs: BigDecimal, // calculation.allowancesAndDeductions.totalAllowancesDeductionsReliefs
                                 totalIncomeAllowancesUsed: BigDecimal, // not used anymore
                                 giftOfInvestmentsAndPropertyToCharity: BigDecimal, // calculation.allowancesAndDeductions.giftOfInvestmentsAndPropertyToCharity
                                 incomeReceived: IncomeReceivedModel, // see within
                                 savingsAndGains: SavingsAndGainsModel, // see within
                                 dividends: DividendsModel, // see within
                                 giftAid: GiftAidModel, // see within
                                 nic: NicModel, // see within
                                 eoyEstimate: Option[EoyEstimate] = None, // see within
                                 payAndPensionsProfit: PayPensionsProfitModel = PayPensionsProfitModel(0, 0, Seq()) // see within
                               ) extends CalculationDataResponseModel

case class IncomeReceivedModel(selfEmployment: BigDecimal, // calculation.payPensionsProfit.totalSelfEmploymentProfit
                               ukProperty: BigDecimal, // calculation.payPensionsProfit.totalPropertyProfit
                               bankBuildingSocietyInterest: BigDecimal, // calculation.savingsAndGains.taxableIncome
                               ukDividends: BigDecimal // calculation.dividends.taxableIncome
                              )

case class PayPensionsProfitModel(
                                   totalAmount: BigDecimal, // calculation.payPensionsProfit.incomeTaxAmount
                                   taxableIncome: BigDecimal, // calculation.payPensionsProfit.taxableIncome
                                   payAndPensionsProfitBands: Seq[TaxBandModel] // calculation.payPensionsProfit.bands
                                 )

case class SavingsAndGainsModel(
                                 total: BigDecimal, // calculation.savingsAndGains.incomeTaxAmount
                                 taxableIncome: BigDecimal, // calculation.savingsAndGains.taxableIncome
                                 bands: Seq[BandModel] // calculation.savingsAndGains.bands
                               ) {
  val savingsAllowance: BigDecimal = bands.filter(_.taxRate == 0).map(_.taxableIncome).sum
}


case class DividendsModel(
                           totalAmount: BigDecimal, // calculation.dividends.incomeTaxAmount
                           taxableIncome: BigDecimal, // calculation.dividends.taxableIncome
                           band: Seq[DividendsBandModel] // calculation.dividends.bands
                         ) {
  val dividendsAllowance: BigDecimal = band.filter(_.rate == 0).map(_.income).sum
}

// TaxBand model
case class DividendsBandModel(name: String,
                              rate: BigDecimal,
                              threshold: Option[Int], // not required
                              apportionedThreshold: Option[Int], // not required
                              income: BigDecimal,
                              amount: BigDecimal)

case class GiftAidModel(paymentsMade: BigDecimal, // calculation.giftAid.payments
                        rate: BigDecimal, // calculation.giftAid.rate
                        taxableAmount: BigDecimal) // calculation.giftAid.giftAidTax


// TaxBand model
case class BandModel(taxableIncome: BigDecimal,
                     taxRate: BigDecimal,
                     taxAmount: BigDecimal,
                     name: String = "")


// TaxBand model
case class TaxBandModel(name: String, rate: BigDecimal, income: BigDecimal, taxAmount: BigDecimal)

object TaxBandModel {
  implicit val format: Format[TaxBandModel] = Json.format[TaxBandModel]
}

case class AnnualAllowances(
                             personalAllowance: BigDecimal, // calculation.allowancesAndDeductions.personalAllowance
                             giftAidExtender: BigDecimal // no longer exists, TODO: WHAT DO?
                           )


case class NicModel(class2: BigDecimal, // calculation.nic.class2
                    class4: BigDecimal) // calculation.nic.class4


case class EoyEstimate(totalNicAmount: BigDecimal) // calculation.nic.totalNic


object CalculationDataModel {
  val defaultZero: JsPath => Reads[BigDecimal] = _.read[BigDecimal].orElse(Reads.pure[BigDecimal](0.00))
  val logFieldError: JsPath => Reads[Option[String]] = _.readNullable[String] map {
    case None => Logger.error(s"[CalculationDataResponseModel][CalculationDataModel] - National Regime field is missing from json"); None
    case data => data
  }

  implicit val reads: Reads[CalculationDataModel] = (
    logFieldError(__ \ "calcOutput" \ "calcResult" \ "nationalRegime") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "totalTaxableIncome") and
      (__ \ "calcOutput" \ "calcResult" \ "incomeTaxNicYtd").read[BigDecimal] and
      __.read[AnnualAllowances] and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "totalAllowancesAndReliefs") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "taxableIncome" \ "totalIncomeAllowancesUsed") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "taxableIncome" \ "allowancesAndDeductions" \ "giftOfInvestmentsAndPropertyToCharity") and
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
      (__ \ "annualAllowances").write[AnnualAllowances] and
      (__ \ "taxReliefs").write[BigDecimal] and
      (__ \ "totalIncomeAllowancesUsed").write[BigDecimal] and
      (__ \ "giftOfInvestmentsAndPropertyToCharity").write[BigDecimal] and
      (__ \ "incomeReceived").write[IncomeReceivedModel] and
      (__ \ "incomeTax" \ "savingsAndGains").write[SavingsAndGainsModel] and
      (__ \ "incomeTax" \ "dividends").write[DividendsModel] and
      (__ \ "giftAid").write[GiftAidModel] and
      (__ \ "nic").write[NicModel] and
      (__ \ "eoyEstimate").writeNullable[EoyEstimate] and
      (__ \ "incomeTax" \ "payPensionsProfit").write[PayPensionsProfitModel]
    ) (unlift(CalculationDataModel.unapply))
}

object AnnualAllowances {
  implicit val reads: Reads[AnnualAllowances] = (
    defaultZero(__ \ "calcOutput" \ "calcResult" \ "annualAllowances" \ "personalAllowance") and
      defaultZero(__ \ "calcOutput" \ "calcResult" \ "annualAllowances" \ "giftAidExtender")
    ) (AnnualAllowances.apply _)
  implicit val writes: Writes[AnnualAllowances] = Json.writes[AnnualAllowances]
}


object GiftAidModel {
  implicit val reads: Reads[GiftAidModel] = (
    defaultZero(__ \ "calcOutput" \ "calcResult" \ "incomeTax" \ "giftAid" \ "paymentsMade") and
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
