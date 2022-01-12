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

case class TaxDeductedAtSource(
                                bbsi: Option[BigDecimal] = None,
                                ukLandAndProperty: Option[BigDecimal] = None,
                                cis: Option[BigDecimal] = None,
                                voidedIsa: Option[BigDecimal] = None,
                                payeEmployments: Option[BigDecimal] = None,
                                occupationalPensions: Option[BigDecimal] = None,
                                stateBenefits: Option[BigDecimal] = None,
                                specialWithholdingTaxOrUkTaxPaid: Option[BigDecimal] = None,
                                inYearAdjustmentCodedInLaterTaxYear: Option[BigDecimal] = None
                              )

object TaxDeductedAtSource {
  implicit val format: OFormat[TaxDeductedAtSource] = Json.format[TaxDeductedAtSource]
}
