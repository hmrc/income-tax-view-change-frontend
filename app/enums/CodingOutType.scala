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

sealed trait CodingOutType {
  val name: String

  implicit def codingOutTypeToString(codingOutType: CodingOutType): String = {
    codingOutType.name
  }
}

object CodingOutType {
  case object CODING_OUT_ACCEPTED extends CodingOutType {
    override val name: String = "Balancing payment collected through PAYE tax code"
  }

  case object CODING_OUT_CANCELLED extends CodingOutType {
    override val name: String = "Cancelled PAYE Self Assessment"
  }

  case object CODING_OUT_CLASS2_NICS extends CodingOutType {
    override val name: String = "Class 2 National Insurance"
  }
}