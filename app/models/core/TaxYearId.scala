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

package models.core

import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.TaxYear._

import java.time.{LocalDate, Month}
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class TaxYearId private(val firstYear: Int) extends AnyVal {
  def secondYear: Int = firstYear + 1 // Always

  def next: TaxYearId = new TaxYearId(firstYear + 1)
  def prev: TaxYearId = new TaxYearId(firstYear - 1)

  def toModel : TaxYear =  mkTaxYear(this)

  def from: LocalDate = LocalDate.of(firstYear, Month.APRIL, 6)
  def to: LocalDate = LocalDate.of(secondYear, Month.APRIL, 5)

  // tax year in format: YYYY-YY
  def normalised: String = {
    val formatterFull = DateTimeFormatter.ofPattern("YYYY")
    val formatterShort = DateTimeFormatter.ofPattern("YY")
    s"${from.format(formatterFull)}-${to.format(formatterShort)}"
  }

  // tax year in format: YYYY-YYYY
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

  def mkTaxYearId(s: Int): TaxYearId =  {
    new TaxYearId(s)
  }

  private def areValidYears(yearOne: String, yearTwo: String): Boolean = {

    def isValidYear(year: String): Boolean =
      year.length == 4 &&
        year.forall(_.isDigit) &&
        Try(year.toInt).toOption.isDefined

    def differenceIsOne(yearOne: String, yearTwo: String): Boolean =
      yearOne.toInt + 1 == yearTwo.toInt

    isValidYear(yearOne) &&
      isValidYear(yearTwo) &&
      differenceIsOne(yearOne, yearTwo)
  }

  def mkTaxYearId(years: String): Either[Throwable, TaxYearId] =  {
    // Parse input string and validate if its comply with any taxYear formats we use
    years.split('-') match {
      case Array(yearOne, yearTwo) if areValidYears(yearOne, yearTwo) =>
        Try { new TaxYearId(yearOne.toInt) }.toEither
      case _ =>
        Left( new Error(s"Unable to parse input taxYear: $years"))
    }
  }

  // create TaxYear based on the outcome of running external validator returning Future[Boolean]
  def mkTaxYearId(s: Int, validationRule : TaxYearId => Future[Boolean])
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