/*
 * Copyright 2026 HM Revenue & Customs
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

package models.newHomePage

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum MaturityLevel(val tagColour: String):
  
  case Overdue extends MaturityLevel("red")
  case DueToday extends MaturityLevel("pink")
  case DueEarly extends MaturityLevel("yellow") // <= 30 days
  case Upcoming extends MaturityLevel("green") // > 30 days

object MaturityLevel {
  def get(dueDate: Option[LocalDate]): Option[MaturityLevel] =
    dueDate.map { date =>
      val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), date)
      if (daysUntil < 0) MaturityLevel.Overdue
      else if (daysUntil == 0) MaturityLevel.DueToday
      else if (daysUntil <= 30) MaturityLevel.DueEarly
      else MaturityLevel.Upcoming
    }
}