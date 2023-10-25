/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.util.Try


class TaxYear private(val firstYear: Int) extends AnyVal {

  def secondYear: Int = firstYear + 1 // Always
  override def toString() = s"$firstYear-${secondYear}"
  // in case we need to create next taxYear instanse
  def next: TaxYear = new TaxYear(firstYear + 1)
}


object TaxYear {
  // Enforce instance creation via "smart constructors"
  // Examples of "smart constructors" to create type instance

  def mkTaxYear(s: Int): Either[Throwable, TaxYear] = Try {
    // Step 1: validate TaxYear before creating instance
    // Step 2: create taxYear string (TODO: add norlamisation here / use fixed format, ie. 2023-2024     ?)
    new TaxYear(s)
  }.toEither

  def mkTaxYear(in: String): Either[Throwable, TaxYear] = Try {
    // Parse input string and validate if its comply with any taxYear formats we use
    new TaxYear(2025) // temporary use of in param as a valida value
  }.toEither


}