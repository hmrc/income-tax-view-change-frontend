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

package models.penalties

case class ActivePenaltyCount(activeLspCount: Int, activeLppCount: Int){
  val hasLSPs: Boolean = activeLspCount > 0
  val hasLPPs: Boolean = activeLppCount > 0
  val total: Int = activeLspCount + activeLppCount
}

object ActivePenaltyCount {
  def apply(activeLspCount: Int = 0, activeLppCount: Int = 0): ActivePenaltyCount = {
    new ActivePenaltyCount(
      activeLspCount = activeLspCount.max(0),
      activeLppCount = activeLppCount.max(0)
    )
  }

  def default: ActivePenaltyCount = ActivePenaltyCount(0, 0)
}
