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

import models.TaxYearId

import scala.util.Try
import play.api.libs.json.{JsPath, Writes}
import play.api.libs.functional.syntax._


case class TaxYear private (startYear: Int, endYear: Int)

object TaxYear {

  // decorate TaxYear case class creation with "smart constructor" to have a valid object
  def mkTaxYear(id: TaxYearId): TaxYear = new TaxYear(id.from.getYear, id.to.getYear)

  implicit val taxYearWrites: Writes[TaxYear] = (
    (JsPath \ "startYear").write[Int] and
      (JsPath \ "endYear").write[Int]
    )(unlift(TaxYear.unapply))

  // TODO: review these method below / if we really need these ???
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
}
