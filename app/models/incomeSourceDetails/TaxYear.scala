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

package models.incomeSourceDetails

import play.api.libs.json.{Format, JsError, JsNumber, Reads}
import services.DateServiceInterface

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

case class TaxYear(startYear: Int, endYear: Int) {

  override def toString: String = s"$startYear-$endYear"

  def addYears(delta: Int): TaxYear = {
    TaxYear(startYear = startYear + delta, endYear = endYear + delta)
  }

  def formatAsShortYearRange: String = {
    s"${startYear.toString.takeRight(2)}-${endYear.toString.takeRight(2)}"
  }

  def isFutureTaxYear(implicit dateService: DateServiceInterface): Boolean = {
    val currentTaxYearEnd = dateService.getCurrentTaxYearEnd
    endYear >= currentTaxYearEnd
  }

  def previousYear: TaxYear = addYears(-1)

  def nextYear: TaxYear = addYears(+1)

  def isSameAs(taxYear: TaxYear): Boolean = this.startYear == taxYear.startYear && this.endYear == taxYear.endYear

  def isAfter(taxYear: TaxYear): Boolean = this.startYear > taxYear.startYear

  def isBefore(taxYear: TaxYear): Boolean = this.startYear < taxYear.startYear

  private val April = 4
  private val Sixth = 6
  private val Fifth = 5

  def toFinancialYearStart: LocalDate = {
    LocalDate.of(this.startYear, April, Sixth)
  }

  def toFinancialYearEnd: LocalDate = {
    LocalDate.of(this.endYear, April, Fifth)
  }

  def financialYearStartString: String = {
    toFinancialYearStart.format(DateTimeFormatter.ISO_DATE)
  }

  def financialYearEndString: String = {
    toFinancialYearEnd.format(DateTimeFormatter.ISO_DATE)
  }

  def shortenTaxYearEnd: String = {
    s"$startYear-${endYear.toString.toSeq.drop(2)}"
  }

  def `taxYearYY-YY`: String = {
    s"${startYear.toString.toSeq.drop(2)}-${endYear.toString.toSeq.drop(2)}"
  }

}

object TaxYear {

  implicit val format: Format[TaxYear] = Format(
    fjs = Reads[TaxYear](
      _.validate[Int]
        .filter(JsError("Could not parse tax year"))(v => TaxYear.isValidYear(s"$v"))
        .map(TaxYear.forYearEnd)
    ),
    tjs = (o: TaxYear) => JsNumber(o.endYear))


  private def isValidYear(year: String): Boolean =
    year.length == 4 &&
      year.forall(_.isDigit) &&
      Try(year.toInt).toOption.isDefined

  private def areValidYears(yearOne: String, yearTwo: String): Boolean = {
    def differenceIsOne(yearOne: String, yearTwo: String): Boolean =
      yearOne.toInt + 1 == yearTwo.toInt

    isValidYear(yearOne) &&
      isValidYear(yearTwo) &&
      differenceIsOne(yearOne, yearTwo)
  }

  def getTaxYearModel(years: String): Option[TaxYear] = {

    years.split('-') match {
      case Array(yearOne, yearTwo) if areValidYears(yearOne, yearTwo) =>
        Some(
          TaxYear(yearOne.toInt, yearTwo.toInt)
        )
      case _ => None
    }
  }

  def getTaxYear(localDate: LocalDate): TaxYear = {
    if (localDate.isBefore(LocalDate.of(localDate.getYear, 4, 6))) {
      TaxYear.forYearEnd(localDate.getYear)
    } else {
      TaxYear.forYearEnd(localDate.getYear + 1)
    }
  }

  def makeTaxYearWithEndYear(endYear: Int): TaxYear = {
    TaxYear(startYear = (endYear - 1), endYear = endYear)
  }

  def forYearEnd(endYear: Int): TaxYear = {
    require(isValidYear(endYear.toString), "invalid year")
    TaxYear(startYear = endYear - 1, endYear = endYear)
  }

  def `fromStringYYYY-YYYY`(year: String): Option[TaxYear] = {
    year.split("-") match {
      case Array(startYear, endYear) => Some(TaxYear(startYear.toInt, endYear.toInt))
      case _ => None
    }
  }

}

