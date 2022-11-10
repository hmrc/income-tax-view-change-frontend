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

package enums

sealed trait ChargeType {
  val name: String

  implicit def chargeTypeToString(chargeType: ChargeType): String = {
    chargeType.name
  }
}

object ChargeType {
  case object ITSA_ENGLAND_AND_NI extends ChargeType {
    override val name: String = "ITSA England & NI"
  }

  case object ITSA_NI extends ChargeType {
    override val name: String = "ITSA NI"
  }

  case object ITSA_SCOTLAND extends ChargeType {
    override val name: String = "ITSA Scotland"
  }

  case object ITSA_WALES extends ChargeType {
    override val name: String = "ITSA Wales"
  }

  case object NIC4_GB extends ChargeType {
    override val name: String = "NIC4-GB"
  }

  case object NIC4_SCOTLAND extends ChargeType {
    override val name: String = "NIC4 Scotland"
  }

  case object NIC4_WALES extends ChargeType {
    override val name: String = "NIC4 Wales"
  }

  case object NIC4_NI extends ChargeType {
    override val name: String = "NIC4-NI"
  }

  case object ITSA_NIC4_INTEREST_GB extends ChargeType {
    override val name: String = "ITSA NIC4 Interest GB"
  }

  case object NIC2_GB extends ChargeType {
    override val name: String = "NIC2-GB"
  }

  case object VOLUNTARY_NIC2_NI extends ChargeType {
    override val name: String = "Voluntary NIC2-NI"
  }

  case object CGT extends ChargeType {
    override val name: String = "CGT"
  }
}