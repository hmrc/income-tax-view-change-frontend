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

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


class TaxYearId private(val firstYear: Int) extends AnyVal {
  def secondYear: Int = firstYear + 1 // Always
  def next: TaxYearId = new TaxYearId(firstYear + 1)
  def from: LocalDate = LocalDate.of(firstYear, 4, 6)
  def to: LocalDate = LocalDate.of(secondYear, 4, 6)

  // Will return taxYear in format: YYYY-YY
  def normalised: String = {
    val formatterFull = DateTimeFormatter.ofPattern("YYYY")
    val formatterShort = DateTimeFormatter.ofPattern("YY")
    s"${from.format(formatterFull)}-${to.format(formatterShort)}"
  }

  // Will return taxYear in format YYYY-YYYY
  def full: String = {
    val formatterFull = DateTimeFormatter.ofPattern("YYYY")
    s"${from.format(formatterFull)}-${to.format(formatterFull)}"
  }

  // in case we need to create next taxYear instance
  override def toString: String = this.full
}


object TaxYearId {

  // Enforce instance creation via "smart constructors"
  // Examples of "smart constructors" to create type instance

  def mkTaxYear(s: Int): Either[Throwable, TaxYearId] = Try {
    // Step 1: validate TaxYear before creating instance
    // Step 2: create taxYear string (TODO: add normalisation here / use fixed format, ie. 2023-2024     ?)
    new TaxYearId(s)
  }.toEither

  def mkTaxYear(in: String): Either[Throwable, TaxYearId] = Try {
    // Parse input string and validate if its comply with any taxYear formats we use
    new TaxYearId(2025) // temporary use of in param as a valida value
  }.toEither

  // create TaxYear based on the outcome of running external validator returning Future[Boolean]
  def mkTaxYear(s: Int, validationRule : TaxYearId => Future[Boolean])
               (implicit  ec : ExecutionContext): Future[Either[Throwable, TaxYearId]] = {
    for {
      internalTaxYear <- Future.successful(new TaxYearId(s))
      validationOutcome <- validationRule(internalTaxYear)
    } yield {
      if (validationOutcome)
        Right(internalTaxYear)
      else
        Left(new Error(s"Not valid taxYear: $internalTaxYear"))
    }
  }

}