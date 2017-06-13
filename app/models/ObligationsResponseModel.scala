/*
 * Copyright 2017 HM Revenue & Customs
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

package models

import java.time.LocalDate

import play.api.libs.json.Json

sealed trait ObligationsResponseModel
sealed trait ObligationsDisplayModel

case class ObligationsModel(obligations: List[ObligationModel]) extends ObligationsResponseModel

case class ObligationModel(start: LocalDate,
                           end: LocalDate,
                           due: LocalDate,
                           met: Boolean
                          ) {
  def currentTime(): LocalDate = LocalDate.now()

  def getObligationStatus: ObligationStatus = (met, due) match {
      case (true, _)                                          => Received
      case (false, date) if currentTime().isBefore(date)      => Open(date)
      case (false, _)                                         => Overdue
    }
}

case class ObligationsErrorModel(code: Int, message: String) extends ObligationsResponseModel

object ObligationModel {
  implicit val format = Json.format[ObligationModel]
}

object ObligationsModel {
  implicit val format = Json.format[ObligationsModel]
}

object ObligationsErrorModel {
  implicit val format = Json.format[ObligationsErrorModel]
}

