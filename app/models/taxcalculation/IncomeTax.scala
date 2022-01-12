/*
 * Copyright 2022 HM Revenue & Customs
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

package models.taxcalculation

import play.api.libs.json.{Json, OFormat}

case class IncomeTax(
                      totalIncomeReceivedFromAllSources: Int,
                      totalAllowancesAndDeductions: Int,
                      totalTaxableIncome: Int,
                      payPensionsProfit: Option[PayPensionsProfit] = None,
                      savingsAndGains: Option[SavingsAndGains] = None,
                      dividends: Option[Dividends] = None,
                      lumpSums: Option[LumpSums] = None,
                      gainsOnLifePolicies: Option[GainsOnLifePolicies] = None,
                      totalReliefs: Option[BigDecimal] = None,
                      totalNotionalTax: Option[BigDecimal] = None,
                      incomeTaxDueAfterTaxReductions: Option[BigDecimal] = None,
                      totalPensionSavingsTaxCharges: Option[BigDecimal] = None,
                      statePensionLumpSumCharges: Option[BigDecimal] = None,
                      payeUnderpaymentsCodedOut: Option[BigDecimal] = None
                    )

object IncomeTax {
  implicit val format: OFormat[IncomeTax] = Json.format[IncomeTax]
}

case class TaxBands(
                     name: String,
                     rate: BigDecimal,
                     bandLimit: Int,
                     apportionedBandLimit: Int,
                     income: Int,
                     taxAmount: BigDecimal
                   )

object TaxBands {
  implicit val format: OFormat[TaxBands] = Json.format[TaxBands]
}

case class PayPensionsProfit(taxBands: Seq[TaxBands] = Seq())

object PayPensionsProfit {
  implicit val format: OFormat[PayPensionsProfit] = Json.format[PayPensionsProfit]
}

case class SavingsAndGains(taxableIncome: Int, taxBands: Seq[TaxBands] = Seq())

object SavingsAndGains {
  implicit val format: OFormat[SavingsAndGains] = Json.format[SavingsAndGains]
}

case class LumpSums(taxBands: Seq[TaxBands] = Seq())

object LumpSums {
  implicit val format: OFormat[LumpSums] = Json.format[LumpSums]
}

case class Dividends(taxableIncome: Int, taxBands: Seq[TaxBands] = Seq())

object Dividends {
  implicit val format: OFormat[Dividends] = Json.format[Dividends]
}

case class GainsOnLifePolicies(taxBands: Seq[TaxBands] = Seq())

object GainsOnLifePolicies {
  implicit val format: OFormat[GainsOnLifePolicies] = Json.format[GainsOnLifePolicies]
}
