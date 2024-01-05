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

import scala.util.Try

case class TaxYear(startYear: Int, endYear: Int) {
  override def toString: String = s"$startYear-$endYear"
}

object TaxYear {

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

  def getTaxYearModel(years: String): Option[TaxYear] = {

    years.split('-') match {
      case Array(yearOne, yearTwo) if areValidYears(yearOne, yearTwo) =>
        Some(
          TaxYear(yearOne.toInt, yearTwo.toInt)
        )
      case _ => None
    }
  }

  def isValidLatencyYear(year: TaxYear, latencyDetails: LatencyDetails): Boolean = {
    (latencyDetails, year) match {
      case (details, year) =>
        val latencyYears = Set(details.taxYear1, details.taxYear2)
        latencyYears.contains(year.endYear.toString)
      case _ => false
    }
  }

}

