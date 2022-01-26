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

package models.liabilitycalculation.taxcalculation

import play.api.libs.json._

case class CapitalGainsTax(
                            totalCapitalGainsIncome: BigDecimal,
                            adjustments: Option[BigDecimal] = None,
                            foreignTaxCreditRelief: Option[BigDecimal] = None,
                            taxOnGainsAlreadyPaid: Option[BigDecimal] = None,
                            capitalGainsTaxDue: BigDecimal,
                            capitalGainsOverpaid: Option[BigDecimal] = None,
                            residentialPropertyAndCarriedInterest: Option[ResidentialPropertyAndCarriedInterest] = None,
                            otherGains: Option[OtherGains] = None,
                            businessAssetsDisposalsAndInvestorsRel: Option[BusinessAssetsDisposalsAndInvestorsRel] = None
                          )

object CapitalGainsTax {
  implicit val format: OFormat[CapitalGainsTax] = Json.format[CapitalGainsTax]
}

case class ResidentialPropertyAndCarriedInterest(
                                                  cgtTaxBands: Seq[CgtTaxBands] = Seq()
                                                )

object ResidentialPropertyAndCarriedInterest {
  implicit val format: OFormat[ResidentialPropertyAndCarriedInterest] = Json.format[ResidentialPropertyAndCarriedInterest]
}

case class OtherGains(
                       cgtTaxBands: Seq[CgtTaxBands] = Seq()
                     )

object OtherGains {
  implicit val format: OFormat[OtherGains] = Json.format[OtherGains]
}

case class CgtTaxBands(
                        name: String,
                        rate: BigDecimal,
                        income: BigDecimal,
                        taxAmount: BigDecimal
                      )

object CgtTaxBands {
  implicit val format: OFormat[CgtTaxBands] = Json.format[CgtTaxBands]
}

case class BusinessAssetsDisposalsAndInvestorsRel(
                                                   taxableGains: Option[BigDecimal] = None,
                                                   rate: Option[BigDecimal] = None,
                                                   taxAmount: Option[BigDecimal] = None
                                                 )

object BusinessAssetsDisposalsAndInvestorsRel {
  implicit val format: OFormat[BusinessAssetsDisposalsAndInvestorsRel] = Json.format[BusinessAssetsDisposalsAndInvestorsRel]
}
