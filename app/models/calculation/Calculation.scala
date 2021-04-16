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

import models.{readNullable, readNullableList}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, OFormat, OWrites, Reads, _}

case class TaxBand(name: String, rate: BigDecimal, income: BigDecimal, taxAmount: BigDecimal)

object TaxBand {
  implicit val format: OFormat[TaxBand] = Json.format[TaxBand]
}

case class PayPensionsProfit(totalSelfEmploymentProfit: Option[BigDecimal] = None,
                             totalPropertyProfit: Option[BigDecimal] = None,
                             incomeTaxAmount: Option[BigDecimal] = None,
                             taxableIncome: Option[BigDecimal] = None,
                             bands: List[TaxBand] = Nil,
                             totalPayeEmploymentAndLumpSumIncome: Option[BigDecimal] = None,
                             totalBenefitsInKind: Option[BigDecimal] = None,
                             totalEmploymentExpenses: Option[BigDecimal] = None,
                             totalEmploymentIncome: Option[BigDecimal] = None,
                             totalOccupationalPensionIncome: Option[BigDecimal] = None,
                             totalStateBenefitsIncome: Option[BigDecimal] = None,
                             totalFHLPropertyProfit: Option[BigDecimal] = None,
                             totalForeignPropertyProfit: Option[BigDecimal] = None,
                             totalEeaFhlProfit: Option[BigDecimal] = None,
                             totalOverseasPensionsStateBenefitsRoyalties: Option[BigDecimal] = None,
                             totalAllOtherIncomeReceivedWhilstAbroad: Option[BigDecimal] = None,
                             totalOverseasIncomeAndGains: Option[BigDecimal] = None,
                             totalForeignBenefitsAndGifts: Option[BigDecimal] = None,
                             totalShareSchemesIncome: Option[BigDecimal] = None
                            )

object PayPensionsProfit {
  implicit val reads: Reads[PayPensionsProfit] = (
    readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalSelfEmploymentProfit") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalPropertyProfit") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "incomeTax" \ "payPensionsProfit" \ "incomeTaxAmount") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "taxableIncome") and
      readNullableList[TaxBand](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "incomeTax" \ "payPensionsProfit" \ "taxBands") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalPayeEmploymentAndLumpSumIncome") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalBenefitsInKind") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalEmploymentExpenses") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalEmploymentIncome") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalOccupationalPensionIncome") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalStateBenefitsIncome") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalFHLPropertyProfit") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalForeignPropertyProfit") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalEeaFhlProfit") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalOverseasPensionsStateBenefitsRoyalties") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalAllOtherIncomeReceivedWhilstAbroad") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalOverseasIncomeAndGains") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalForeignBenefitsAndGifts") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "payPensionsProfit" \ "totalShareSchemesIncome")
    ) (PayPensionsProfit.apply _)
  implicit val writes: OWrites[PayPensionsProfit] = Json.writes[PayPensionsProfit]
}

case class Dividends(incomeTaxAmount: Option[BigDecimal] = None,
                     taxableIncome: Option[BigDecimal] = None,
                     totalForeignDividends: Option[BigDecimal] = None,
                     bands: List[TaxBand] = Nil) {
  val dividendsAllowance: BigDecimal = bands.filter(_.rate == 0).map(_.income).sum
}

object Dividends {
  implicit val reads: Reads[Dividends] = (
    readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "incomeTax" \ "dividends" \ "incomeTaxAmount") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "dividends" \ "taxableIncome") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "dividends" \ "totalForeignDividends") and
      readNullableList[TaxBand](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "incomeTax" \ "dividends" \ "taxBands")
    ) (Dividends.apply _)
  implicit val writes: OWrites[Dividends] = Json.writes[Dividends]
}

case class SavingsAndGains(incomeTaxAmount: Option[BigDecimal] = None,
                           taxableIncome: Option[BigDecimal] = None,
                           totalForeignSavingsAndGainsIncome: Option[BigDecimal] = None,
                           totalOfAllGains: Option[BigDecimal] = None,
                           bands: List[TaxBand] = Nil) {
  val savingsAllowance: BigDecimal = bands.filter(_.rate == 0).map(_.income).sum
}

object SavingsAndGains {
  implicit val reads: Reads[SavingsAndGains] = (
    readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "incomeTax" \ "savingsAndGains" \ "incomeTaxAmount") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "savingsAndGains" \ "taxableIncome") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "savingsAndGains" \ "totalForeignSavingsAndGainsIncome") and
      readNullable[BigDecimal](__ \ "taxableIncome" \ "detail" \ "savingsAndGains" \ "totalOfAllGains") and
      readNullableList[TaxBand](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "incomeTax" \ "savingsAndGains" \ "taxBands")
    ) (SavingsAndGains.apply _)
  implicit val writes: OWrites[SavingsAndGains] = Json.writes[SavingsAndGains]
}

case class ReductionsAndCharges(giftAidTax: Option[BigDecimal] = None,
                                totalPensionSavingsTaxCharges: Option[BigDecimal] = None,
                                statePensionLumpSumCharges: Option[BigDecimal] = None,
                                totalStudentLoansRepaymentAmount: Option[BigDecimal] = None,
                                propertyFinanceRelief: Option[BigDecimal] = None,
                                totalForeignTaxCreditRelief: Option[BigDecimal] = None,
                                reliefsClaimed: Option[Seq[ReliefsClaimed]] = None,
                                totalNotionalTax: Option[BigDecimal] = None,
                                incomeTaxDueAfterTaxReductions: Option[BigDecimal] = None,
                                totalIncomeTaxDue: Option[BigDecimal] = None
                               ) {
  val reliefsClaimedMap: Map[String, Option[BigDecimal]] = reliefsClaimed.getOrElse(Seq()).map(relief => relief.`type` -> relief.amountUsed).toMap

}

object ReductionsAndCharges {
  implicit val reads: Reads[ReductionsAndCharges] = (
    readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "incomeTax" \ "giftAid" \ "giftAidTax") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "incomeTax" \ "totalPensionSavingsTaxCharges") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "incomeTax" \ "statePensionLumpSumCharges") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "totalStudentLoansRepaymentAmount") and
      readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "detail" \ "reliefs" \ "residentialFinanceCosts" \ "propertyFinanceRelief") and
      readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "detail" \ "reliefs" \ "foreignTaxCreditRelief" \ "totalForeignTaxCreditRelief") and
      readNullable[Seq[ReliefsClaimed]](__ \ "allowancesDeductionsAndReliefs" \ "detail" \ "reliefs" \ "reliefsClaimed") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "incomeTax" \ "totalNotionalTax") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "incomeTax" \ "incomeTaxDueAfterTaxReductions") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "incomeTax" \ "totalIncomeTaxDue")
    ) (ReductionsAndCharges.apply _)
  implicit val writes: OWrites[ReductionsAndCharges] = Json.writes[ReductionsAndCharges]
}

case class ReliefsClaimed(`type`: String, amountUsed: Option[BigDecimal])

object ReliefsClaimed {
  implicit val format: OFormat[ReliefsClaimed] = Json.format[ReliefsClaimed]
}


case class ResidentialFinanceCosts(rate: BigDecimal,
                                   propertyFinanceRelief: BigDecimal)

object ResidentialFinanceCosts {
  implicit val format: OFormat[ResidentialFinanceCosts] = Json.format[ResidentialFinanceCosts]
}


case class AllowancesAndDeductions(personalAllowance: Option[BigDecimal] = None,
                                   totalPensionContributions: Option[BigDecimal] = None,
                                   lossesAppliedToGeneralIncome: Option[BigDecimal] = None,
                                   giftOfInvestmentsAndPropertyToCharity: Option[BigDecimal] = None,
                                   totalAllowancesAndDeductions: Option[BigDecimal] = None,
                                   totalTaxableIncome: Option[BigDecimal] = None,
                                   totalReliefs: Option[BigDecimal] = None,
                                   grossAnnualPayments: Option[BigDecimal] = None,
                                   qualifyingLoanInterestFromInvestments: Option[BigDecimal] = None,
                                   postCessationTradeReceipts: Option[BigDecimal] = None,
                                   paymentsToTradeUnionsForDeathBenefits: Option[BigDecimal] = None
                                  ) {

  val totalAllowancesDeductionsReliefs: Option[BigDecimal] = (totalAllowancesAndDeductions ++ totalReliefs).reduceOption(_ + _)

}

object AllowancesAndDeductions {
  implicit val reads: Reads[AllowancesAndDeductions] = (
    readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "detail" \ "allowancesAndDeductions" \ "personalAllowance") and
      readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "detail" \ "allowancesAndDeductions" \ "pensionContributions" \ "totalPensionContributions") and
      readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "detail" \ "allowancesAndDeductions" \ "lossesAppliedToGeneralIncome") and
      readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "detail" \ "allowancesAndDeductions" \ "giftOfInvestmentsAndPropertyToCharity") and
      readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "summary" \ "totalAllowancesAndDeductions") and
      readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "summary" \ "totalTaxableIncome") and
      readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "summary" \ "totalReliefs") and
      readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "detail" \ "allowancesAndDeductions" \ "annualPayments" \ "grossAnnualPayments") and
      readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "detail" \ "allowancesAndDeductions" \ "qualifyingLoanInterestFromInvestments") and
      readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "detail" \ "allowancesAndDeductions" \ "postCessationTradeReceipts") and
      readNullable[BigDecimal](__ \ "allowancesDeductionsAndReliefs" \ "detail" \ "allowancesAndDeductions" \ "paymentsToTradeUnionsForDeathBenefits")
    ) (AllowancesAndDeductions.apply _)
  implicit val writes: OWrites[AllowancesAndDeductions] = Json.writes[AllowancesAndDeductions]
}


case class Nic(class2: Option[BigDecimal] = None,
               class4: Option[BigDecimal] = None,
               class4Bands: Option[Seq[NicBand]] = None,
               totalNic: Option[BigDecimal] = None)

object Nic {
  implicit val reads: Reads[Nic] = (
    readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "nics" \ "class2NicsAmount") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "nics" \ "class4NicsAmount") and
      readNullable[Seq[NicBand]](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "nics" \ "class4Nics" \ "class4NicBands") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "nics" \ "totalNic")
    ) (Nic.apply _)
  implicit val writes: OWrites[Nic] = Json.writes[Nic]
}

case class NicBand(name: String,
                   income: BigDecimal,
                   rate: BigDecimal,
                   amount: BigDecimal)

object NicBand {
  implicit val format: OFormat[NicBand] = Json.format[NicBand]
}

case class TaxDeductedAtSource(payeEmployments: Option[BigDecimal] = None,
                               ukPensions: Option[BigDecimal] = None,
                               stateBenefits: Option[BigDecimal] = None,
                               cis: Option[BigDecimal] = None,
                               ukLandAndProperty: Option[BigDecimal] = None,
                               savings: Option[BigDecimal] = None,
                               total: Option[BigDecimal] = None
                              ) {
  val allFields: Seq[(String, BigDecimal)] = Seq(
    "payeEmployments" -> payeEmployments,
    "ukPensions" -> ukPensions,
    "stateBenefits" -> stateBenefits,
    "cis" -> cis,
    "ukLandAndProperty" -> ukLandAndProperty,
    "savings" -> savings
  ).collect {
    case (key, Some(amount)) => key -> amount
  }
  val nonEmpty: Boolean = allFields.nonEmpty
}

object TaxDeductedAtSource {
  implicit val reads: Reads[TaxDeductedAtSource] = (
    readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "taxDeductedAtSource" \ "payeEmployments") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "taxDeductedAtSource" \ "occupationalPensions") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "taxDeductedAtSource" \ "stateBenefits") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "taxDeductedAtSource" \ "cis") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "taxDeductedAtSource" \ "ukLandAndProperty") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "detail" \ "taxDeductedAtSource" \ "savings") and
      readNullable[BigDecimal](__ \ "incomeTaxAndNicsCalculated" \ "summary" \ "totalTaxDeducted")
    ) (TaxDeductedAtSource.apply _)
  implicit val writes: Writes[TaxDeductedAtSource] = Json.writes[TaxDeductedAtSource]
}
