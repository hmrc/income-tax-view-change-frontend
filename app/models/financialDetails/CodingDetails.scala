/*
 * Copyright 2025 HM Revenue & Customs
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

package models.financialDetails

import models.incomeSourceDetails.TaxYear
import play.api.libs.json.{Json, Reads, Writes}

import java.time.LocalDate

case class CodingDetails(totalLiabilityAmount: Option[BigDecimal],
                         taxYearReturn: Option[String]) {

  def toCodingOutDetails: Option[CodingOutDetails] = {
    (totalLiabilityAmount, taxYearReturn) match {
      case (Some(amount), Some(year)) => Some(CodingOutDetails(amount, TaxYear.makeTaxYearWithEndYear(year.toInt)))
      case _ => None
    }
  }
}

object CodingDetails {
  implicit val writes: Writes[CodingDetails] = Json.writes[CodingDetails]
  implicit val reads: Reads[CodingDetails] = Json.reads[CodingDetails]
}
