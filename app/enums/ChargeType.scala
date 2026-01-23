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

package enums

import scala.language.implicitConversions

enum ChargeType(val name: String):
  
  case ITSA_ENGLAND_AND_NI extends ChargeType("ITSA England & NI")
  case ITSA_NI extends ChargeType("ITSA NI")
  case ITSA_SCOTLAND extends ChargeType("ITSA Scotland")
  case ITSA_WALES extends ChargeType("ITSA Wales")
  case NIC4_GB extends ChargeType("NIC4_GB")
  case NIC4_SCOTLAND extends ChargeType("NIC4 Scotland")
  case NIC4_WALES extends ChargeType("NIC4 Wales")
  case NIC4_NI extends ChargeType("NIC4-NI")
  case ITSA_NIC2_INTEREST_GB extends ChargeType("ITSA NIC2 Interest GB")
  case ITSA_NIC4_INTEREST_GB extends ChargeType("ITSA NIC4 Interest GB")
  case NIC2_GB extends ChargeType("NIC2-GB")
  case NIC2_WALES extends ChargeType("NIC2 Wales")
  case VOLUNTARY_NIC2_GB extends ChargeType("Voluntary NIC2-GB")
  case VOLUNTARY_NIC2_NI extends ChargeType("Voluntary NIC2-NI")
  case CGT extends ChargeType("CGT")
  case SL extends ChargeType("SL")
  
  given Conversion[ChargeType, String] = chargeType => chargeType.name