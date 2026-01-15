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

package enums.TriggeredMigration

sealed trait Channel

object Channel {
  val CustomerLedValue = "1"
  val UnconfirmedValue = "2"
  val ConfirmedValue = "3"

  val confirmedUsers: Set[String] = Set(
    CustomerLedValue,
    ConfirmedValue
  )
}

case object CustomerLed extends Channel {
  override val toString: String = Channel.CustomerLedValue
}

case object Unconfirmed extends Channel {
  override val toString: String = Channel.UnconfirmedValue
}

case object Confirmed extends Channel {
  override val toString: String = Channel.ConfirmedValue
}
