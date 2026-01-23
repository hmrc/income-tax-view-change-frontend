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

enum CodingOutType(val name: String, val code:String):
  
  case CODING_OUT_ACCEPTED extends CodingOutType("Balancing payment collected through PAYE tax code", "I")
  case CODING_OUT_CANCELLED extends CodingOutType("Cancelled PAYE Self Assessment", "C")
  case CODING_OUT_CLASS2_NICS extends CodingOutType("Class 2 National Insurance", "N/A" )
  case CODING_OUT_FULLY_COLLECTED extends CodingOutType("Fully Collected", "F")
  case CODING_OUT_PARTLY_COLLECTED extends CodingOutType("Partly Collected", "P")
  case CODING_OUT_NOT_COLLECTED extends CodingOutType("Not Collected", "N")

  given Conversion[CodingOutType, String] = codingOutType => codingOutType.name

/*given codingOutTypeToString: Conversion[CodingOutType, String] with {
  def apply(codingOutType: CodingOutType): String = codingOutType.name 
}*/