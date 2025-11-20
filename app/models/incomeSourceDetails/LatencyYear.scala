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

object LatencyYear {
  def apply(taxYear: String, reportingMethod: String): LatencyYear = {
    reportingMethod match {
      case "A" => AnnualReporting(taxYear)
      case "Q" => QuarterlyReporting(taxYear)
    }
  }

  def isValidLatencyYear(year: TaxYear, latencyDetails: LatencyDetails): Boolean = {
    val latencyYears = Set(latencyDetails.taxYear1, latencyDetails.taxYear2)
    latencyYears.contains(year.endYear.toString)
  }

}

sealed trait LatencyYear {
  val taxYear: String

  def isAnnualReporting: Boolean = this.isInstanceOf[AnnualReporting]
}

final case class AnnualReporting(taxYear: String) extends LatencyYear

final case class QuarterlyReporting(taxYear: String) extends LatencyYear
