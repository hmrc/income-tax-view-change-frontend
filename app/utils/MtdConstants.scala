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

package utils

import services.DateService

import java.time.LocalDate

trait MtdConstants {
  val dateService: DateService

  private final val baseThreshold = "£50,000"
  private final val thresholdFrom2027 = "£30,000"
  private final val thresholdFrom2028 = "£20,000"

  def getMtdThreshold: String = {
    if (dateService.getCurrentDate.isBefore(LocalDate.of(2027, 4, 6))) {
      baseThreshold
    } else if (dateService.getCurrentDate.isBefore(LocalDate.of(2028, 4, 6))) {
      thresholdFrom2027
    } else {
      thresholdFrom2028
    }
  }
}
