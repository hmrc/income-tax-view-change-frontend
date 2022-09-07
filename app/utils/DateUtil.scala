/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.{LocalDate, LocalDateTime, Month, ZoneId, ZoneOffset}

object DateUtil {
  def getFirstQuaterStart: Int => LocalDate = (taxYear: Int) => LocalDate.of(taxYear - 1, Month.APRIL, 5)

  def getFirstQuaterEnd: Int => LocalDate = (taxYear: Int) => LocalDate.of(taxYear - 1, Month.JULY, 5)

  def getSecondQuaterEnd: Int => LocalDate = (taxYear: Int) => LocalDate.of(taxYear - 1, Month.OCTOBER, 5)

  def getThirdQuaterEnd: Int => LocalDate = (taxYear: Int) => LocalDate.of(taxYear, Month.JANUARY, 5)

  def getFourthQuaterEnd: Int => LocalDate = (taxYear: Int) => LocalDate.of(taxYear, Month.APRIL, 5)

  def getLocalDateFromTimestamp(timestamp: String): Option[LocalDate] = {
    val instant = try {
      Some(java.time.Instant.parse(timestamp))
    } catch {
      case _: Exception => None
    }
    instant.map(i => LocalDateTime.ofInstant(i, ZoneId.of(ZoneOffset.UTC.getId)).toLocalDate)
  }
}
