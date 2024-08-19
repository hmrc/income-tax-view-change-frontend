/*
 * Copyright 2024 HM Revenue & Customs
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

package models.optin

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import play.api.libs.json.{Json, OFormat}

case class OptInContextData(currentTaxYear: String,
                            currentYearITSAStatus: String,
                            nextTaxYear: String,
                            nextYearITSAStatus: String) {

  def currentYearAsTaxYear(): Option[TaxYear] = asTaxYear(currentTaxYear)
  def nextTaxYearAsTaxYear(): Option[TaxYear] = asTaxYear(nextTaxYear)

  private def asTaxYear(taxYearValue: String): Option[TaxYear] = {
    TaxYear.getTaxYearModel(taxYearValue)
  }
}

object OptInContextData {

  implicit val format: OFormat[OptInContextData] = Json.format[OptInContextData]

  /* todo: unify with optout code */
  def statusToString(status: ITSAStatus): String =
    status match {
      case ITSAStatus.NoStatus  => "U"
      case ITSAStatus.Voluntary => "V"
      case ITSAStatus.Annual    => "A"
      case ITSAStatus.Mandated  => "M"
      // This will be validated earlier on in a future ticket
      case _ => throw new RuntimeException("Unexpected status")
    }

  def stringToStatus(status: String): ITSAStatus.Value =
    status match {
      case "U" => ITSAStatus.NoStatus
      case "V" => ITSAStatus.Voluntary
      case "A" => ITSAStatus.Annual
      case "M" => ITSAStatus.Mandated
      case _ => throw new RuntimeException("Unexpected status")
    }
}
