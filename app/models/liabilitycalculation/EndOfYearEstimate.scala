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

package models.liabilitycalculation

import play.api.libs.json._

case class EndOfYearEstimate(
                              incomeSource: Option[List[IncomeSource]],
                              totalEstimatedIncome: Option[Int] = None,
                              totalTaxableIncome: Option[Int] = None,
                              incomeTaxAmount: Option[BigDecimal] = None,
                              nic2: Option[BigDecimal] = None,
                              nic4: Option[BigDecimal] = None,
                              totalNicAmount: Option[BigDecimal] = None,
                              totalTaxDeductedBeforeCodingOut: Option[BigDecimal] = None,
                              saUnderpaymentsCodedOut: Option[BigDecimal] = None,
                              totalStudentLoansRepaymentAmount: Option[BigDecimal] = None,
                              totalAnnuityPaymentsTaxCharged: Option[BigDecimal] = None,
                              totalRoyaltyPaymentsTaxCharged: Option[BigDecimal] = None,
                              totalTaxDeducted: Option[BigDecimal] = None,
                              incomeTaxNicAmount: Option[BigDecimal] = None,
                              cgtAmount: Option[BigDecimal] = None,
                              incomeTaxNicAndCgtAmount: Option[BigDecimal] = None
                            )

object EndOfYearEstimate {
  implicit val format: OFormat[EndOfYearEstimate] = Json.format[EndOfYearEstimate]
}

case class IncomeSource(
                         incomeSourceType: String,
                         incomeSourceName: Option[String],
                         taxableIncome: Int
                       )

object IncomeSource {
  implicit val format: OFormat[IncomeSource] = Json.format[IncomeSource]
}
