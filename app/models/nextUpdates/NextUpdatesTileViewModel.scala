/*
 * Copyright 2024 HM Revenue & Customs
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

package models.nextUpdates

import java.time.LocalDate

case class NextUpdatesTileViewModel(dueDates: Seq[LocalDate]) {
  def getLatestDeadline: LocalDate = {
    dueDates.sortWith(_ isBefore _).head
  }

  def getOverdueObligations(currentDate: LocalDate): Seq[LocalDate] = {
    dueDates.filter(_.isBefore(currentDate))
  }

  def showNextUpdatesTileContent: Boolean = dueDates.nonEmpty

}
