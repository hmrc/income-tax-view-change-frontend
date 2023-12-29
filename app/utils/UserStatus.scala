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

package utils

sealed trait UserStatus {
  val isAgent: Boolean
  val isChanged: Boolean
}

object UserStatus {
  def apply(isAgent: Boolean, isChanged: Boolean): UserStatus = (isAgent, isChanged) match {
    case (true, false)  => AgentNormal
    case (true, true)   => AgentChange
    case (false, false) => IndividualNormal
    case (false, true)  => IndividualChange
  }
}

case object AgentChange extends UserStatus {
  override val isAgent: Boolean = true
  override val isChanged: Boolean = true
}

case object AgentNormal extends UserStatus {
  override val isAgent: Boolean = true
  override val isChanged: Boolean = false
}

case object IndividualChange extends UserStatus {
  override val isAgent: Boolean = false
  override val isChanged: Boolean = true
}

case object IndividualNormal extends UserStatus {
  override val isAgent: Boolean = false
  override val isChanged: Boolean = false
}