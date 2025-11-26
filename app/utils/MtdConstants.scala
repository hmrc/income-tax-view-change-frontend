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

import config.FrontendAppConfig
import services.DateServiceInterface

import java.time.LocalDate

trait MtdConstants {

  val dateService: DateServiceInterface

  def getMtdThreshold()(implicit appConfig: FrontendAppConfig): String = {
    val dateThreshold2027 = LocalDate.of(2027, 4, 6)
    val dateThreshold2028 = LocalDate.of(2028, 4, 6)

    if (dateService.getCurrentDate.isBefore(dateThreshold2027)) {
      appConfig.preThreshold2027
    } else if (dateService.getCurrentDate.isBefore(dateThreshold2028)) {
      appConfig.threshold2027
    } else {
      appConfig.threshold2028
    }
  }
}
