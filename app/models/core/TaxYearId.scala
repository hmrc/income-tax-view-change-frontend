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

import models.core.Quarter.mkQuarter
import models.core.TaxYearId.{FIFTHS_DAY_OF_MONTH, SIXTH_DAY_OF_MONTH, formatterFull, formatterShort}
import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.TaxYear._

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, Month}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


class TaxYearId private(val firstYear: Int) extends AnyVal {
  def secondYear: Int = firstYear + 1 // Always

  def next: TaxYearId = new TaxYearId(firstYear + 1)

  def prev: TaxYearId = new TaxYearId(firstYear - 1)

  def quarters : Seq[Quarter] = {
    for {
      number <- 1 to 4
    } yield mkQuarter(number).toOption.get // Always expect Quarter to be evaluate under this range
  }

  def toModel: TaxYear = mkTaxYear(this)

  def from: LocalDate = LocalDate.of(firstYear, Month.APRIL, SIXTH_DAY_OF_MONTH)

  def to: LocalDate = LocalDate.of(secondYear, Month.APRIL, FIFTHS_DAY_OF_MONTH)

  def contains(date: LocalDate): Boolean = {
    // TODO: verify this with relevant unit test as there can be edge cases
    date.toEpochDay >= from.toEpochDay && date.toEpochDay <= to.toEpochDay
  }

  // Only quarter available within taxYear can be created
  def mkQuater(date: LocalDate): Either[Throwable, Quarter] = {
    if (contains(date)) {
      Right(Quarter.mkQuarter(date))
    } else {
      Left(new Error(s"TaxYear: $firstYear-$secondYear does not contain date: $date"))
    }
  }

  // tax year in format: YYYY-YY
  def normalised: String = {
    s"${from.format(formatterFull)}-${to.format(formatterShort)}"
  }

  // tax year in format: YYYY-YYYY
  def full: String = {
    s"${from.format(formatterFull)}-${to.format(formatterFull)}"
  }

  // in case we need to create next taxYear instance
  override def toString: String = this.full
}

object TaxYearId {
  private val SIXTH_DAY_OF_MONTH: Int = 6
  private val FIFTHS_DAY_OF_MONTH: Int = 5

  private val formatterFull = DateTimeFormatter.ofPattern("YYYY")
  private val formatterShort = DateTimeFormatter.ofPattern("YY")

  // Enforce instance creation via "smart constructors"
  // Examples of "smart constructors" to create type instance

  def mkTaxYearId(s: Int): TaxYearId = {
    new TaxYearId(s)
  }

  // TODO: refactor this method(s) to be more granular/flexible
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

  def mkTaxYearId(years: String): Either[Throwable, TaxYearId] = {
    // Parse input string and validate if its comply with any taxYear formats we use
    years.split('-') match {
      case Array(yearOne, yearTwo) if areValidYears(yearOne, yearTwo) =>
        Try {
          new TaxYearId(yearOne.toInt)
        }.toEither
      case _ =>
        Left(new Error(s"Unable to parse input taxYear: $years"))
    }
  }

  // create TaxYear based on the outcome of running external validator returning Future[Boolean]
  def mkTaxYearId(s: Int, validationRule: TaxYearId => Future[Boolean])
                 (implicit ec: ExecutionContext): Future[Either[Throwable, TaxYearId]] = {
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

  // Expect to convert LocalDate type to TaxYearId instance
  def mkTaxYear(date: LocalDate) : TaxYearId = {
    // We simply expect TaxYear to be one of these:
    Seq( mkTaxYearId( date.getYear), mkTaxYearId( date.getYear + 1) )
      .find(_.contains(date)).get
  }

}