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
import play.api.libs.json.{JsPath, Writes}
import play.api.libs.functional.syntax._


// To be used as a model to generate required Json only;
// Ine the rest of the cases please use TaxYearId type
case class TaxYearJson private(startYear: Int, endYear: Int)

object TaxYearJson {

  // decorate TaxYear case class creation with "smart constructor" to have a valid object
  def mkTaxYear(id: TaxYearId): TaxYearJson = new TaxYearJson(id.from.getYear, id.to.getYear)

  implicit val taxYearWrites: Writes[TaxYearJson] = (
    (JsPath \ "startYear").write[Int] and
      (JsPath \ "endYear").write[Int]
    )(unlift(TaxYearJson.unapply))

  /*
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

  def getTaxYearModel(years: String): Option[TaxYearJson] = {

    years.split('-') match {
      case Array(yearOne, yearTwo) if areValidYears(yearOne, yearTwo) =>
        Some(
          TaxYearJson(yearOne.toInt, yearTwo.toInt)
        )
      case _ => None
    }
  }

   */
}
