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

import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class LatencyDetails(
                           latencyEndDate: LocalDate,
                           taxYear1: String,
                           latencyIndicator1: String,
                           taxYear2: String,
                           latencyIndicator2: String
                         )


case class LatencyYearsQuarterly(firstYear: Option[Boolean], secondYear: Option[Boolean])

case class LatencyYearsAnnual(firstYear: Option[Boolean], secondYear: Option[Boolean])

case class LatencyYearsQuarterlyAndAnnualStatus(latencyYearsQuarterly: LatencyYearsQuarterly, latencyYearsAnnual: LatencyYearsAnnual)

case class LatencyYearsCrystallised(firstYear: Option[Boolean], secondYear: Option[Boolean])

object LatencyDetails {
  implicit val format: Format[LatencyDetails] = Json.format[LatencyDetails]
}