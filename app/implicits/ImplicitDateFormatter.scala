/*
 * Copyright 2021 HM Revenue & Customs
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

package implicits

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZonedDateTime}

import javax.inject.{Inject, Singleton}
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils


@Singleton
class ImplicitDateFormatterImpl @Inject()(val languageUtils: LanguageUtils) extends ImplicitDateFormatter

trait ImplicitDateFormatter {

  implicit val languageUtils: LanguageUtils

  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  implicit class localDate(s: String) {
    def toLocalDate: LocalDate = LocalDate.parse(s, DateTimeFormatter.ofPattern("uuuu-M-d"))
    def toLocalDateTime: LocalDateTime = LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME)
    def toZonedDateTime: ZonedDateTime = ZonedDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME)
  }

  implicit class shortDate(date: LocalDate) {
    def toShortDate: String = date.format(DateTimeFormatter.ofPattern("d/MM/uuuu"))
  }

  implicit class longDate(d: LocalDate)(implicit messages: Messages) {

    def toLongDateNoYear: String = {
      val dt = languageUtils.Dates.formatDate(org.joda.time.LocalDate.parse(d.toString))(messages)
      dt.split(" ")(0) + " " + dt.split(" ")(1)
    }

    def toLongDateShort: String = {
      languageUtils.Dates.formatDateAbbrMonth(org.joda.time.LocalDate.parse(d.toString))(messages)
    }

    def toLongDate: String = {
      languageUtils.Dates.formatDate(org.joda.time.LocalDate.parse(d.toString))(messages)
    }
  }

  implicit class longDateTime(dt: LocalDateTime)(implicit messages: Messages) {
    def toLongDateTime:String = {
      languageUtils.Dates.formatDate(org.joda.time.LocalDate.parse(dt.toLocalDate.toString))(messages)
    }
  }

  implicit def toLocalDate(s: String): LocalDate = localDate(s).toLocalDate

}
