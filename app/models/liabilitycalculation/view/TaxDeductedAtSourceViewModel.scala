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

package models.liabilitycalculation.view

import models.liabilitycalculation.TaxDeductedAtSource

case class TaxDeductedAtSourceViewModel(
                                         payeEmployments: Option[BigDecimal] = None,
                                         ukPensions: Option[BigDecimal] = None,
                                         stateBenefits: Option[BigDecimal] = None,
                                         cis: Option[BigDecimal] = None,
                                         ukLandAndProperty: Option[BigDecimal] = None,
                                         specialWithholdingTax: Option[BigDecimal] = None,
                                         voidISAs: Option[BigDecimal] = None,
                                         savings: Option[BigDecimal] = None,
                                         inYearAdjustmentCodedInLaterTaxYear: Option[BigDecimal] = None,
                                         total: Option[BigDecimal] = None,
                                         totalIncomeTaxAndNicsDue: Option[BigDecimal] = None
                                       ) {
  val allFields: Seq[(String, BigDecimal)] = Seq(
    "inYearAdjustment" -> inYearAdjustmentCodedInLaterTaxYear,
    "payeEmployments" -> payeEmployments,
    "ukPensions" -> ukPensions,
    "stateBenefits" -> stateBenefits,
    "cis" -> cis,
    "ukLandAndProperty" -> ukLandAndProperty,
    "specialWithholdingTax" -> specialWithholdingTax,
    "voidISAs" -> voidISAs,
    "savings" -> savings
  ).collect {
    case (key, Some(amount)) => key -> amount
  }
  val nonEmpty: Boolean = allFields.nonEmpty
}

object TaxDeductedAtSourceViewModel {
  def apply(taxDeductedAtSource: Option[TaxDeductedAtSource]): TaxDeductedAtSourceViewModel = {
    taxDeductedAtSource match {
      case Some(tds) =>
        TaxDeductedAtSourceViewModel(
          ukLandAndProperty = tds.ukLandAndProperty,
          cis = tds.cis,
          voidISAs = tds.voidedIsa,
          payeEmployments = tds.payeEmployments,
          ukPensions = tds.occupationalPensions,
          stateBenefits = tds.stateBenefits,
          specialWithholdingTax = tds.specialWithholdingTaxOrUkTaxPaid,
          inYearAdjustmentCodedInLaterTaxYear = tds.inYearAdjustmentCodedInLaterTaxYear
        )
      case None => TaxDeductedAtSourceViewModel()
    }
  }
}