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

sealed trait CodingOutType {
  val name: String
  val code: String

  implicit def codingOutTypeToString(codingOutType: CodingOutType): String = {
    codingOutType.name
  }
}

object CodingOutType {
  case object CODING_OUT_ACCEPTED extends CodingOutType {
    override val name: String = "Balancing payment collected through PAYE tax code"
    override val code: String = "I"
  }

  case object CODING_OUT_CANCELLED extends CodingOutType {
    override val name: String = "Cancelled PAYE Self Assessment"
    override val code: String = "C"
  }

  case object CODING_OUT_CLASS2_NICS extends CodingOutType {
    override val name: String = "Class 2 National Insurance"
    override val code: String = "N/A"
  }

  case object CODING_OUT_FULLY_COLLECTED extends CodingOutType {
    override val name: String = "Fully Collected"
    override val code: String = "F"
  }
}