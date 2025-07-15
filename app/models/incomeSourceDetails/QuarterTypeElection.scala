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

sealed trait QuarterReportingType {
  val value: String
}

case object QuarterTypeStandard extends QuarterReportingType {
  val value: String = "STANDARD"
}

case object QuarterTypeCalendar extends QuarterReportingType {
  val value: String = "CALENDAR"
}

case class QuarterTypeElection(quarterReportingType: String, taxYearofElection: String) {

  val isStandardQuarterlyReporting: Option[QuarterReportingType] = {
    quarterReportingType match {
      case QuarterTypeStandard.value => Some(QuarterTypeStandard)
      case QuarterTypeCalendar.value => Some(QuarterTypeCalendar)
      case _ => None
    }
  }
}

object QuarterTypeElection {
  implicit val format: Format[QuarterTypeElection] = Json.format[QuarterTypeElection]
  implicit val orderingByTypeName: Ordering[Option[QuarterReportingType]] = Ordering.by(e => e.map(_.value))
}
