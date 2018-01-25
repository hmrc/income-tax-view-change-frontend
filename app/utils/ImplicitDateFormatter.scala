/*
 * Copyright 2018 HM Revenue & Customs
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

package utils

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle._
import java.util.Locale._

trait ImplicitDateFormatter {

  implicit class localDate(s: String) {
    def toLocalDate: LocalDate = LocalDate.parse(s, DateTimeFormatter.ofPattern("uuuu-M-d"))
    def toLocalDateTime: LocalDateTime = LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME)
    def toZonedDateTime: ZonedDateTime = ZonedDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME)
  }

  implicit class longDate(d: LocalDate) {
    def toLongDate: String = d.getDayOfMonth + " " + d.getMonth.getDisplayName(FULL, UK) + " " + d.getYear
  }

  implicit class longDateTime(dt: LocalDateTime) {
    def toLongDateTime: String = dt.getDayOfMonth + " " + dt.getMonth.getDisplayName(FULL, UK) + " " + dt.getYear
  }

  implicit def toLocalDate(s: String): LocalDate = localDate(s).toLocalDate

}

object ImplicitDateFormatter extends ImplicitDateFormatter {
  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)
}
