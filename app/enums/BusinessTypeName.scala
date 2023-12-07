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

sealed trait BusinessTypeName {
  val name: String
}

case object UkPropertyBusinessName extends BusinessTypeName {
  override val name: String = "UKPROPERTY"
}

case object ForeignPropertyBusinessName extends BusinessTypeName {
  override val name: String = "FOREIGNPROPERTY"
}

case object SelfEmploymentBusinessName extends BusinessTypeName {
  override val name: String = "SE"
}

object BusinessTypeName {
  implicit def businessTypeToString(in : BusinessTypeName) : String = in match {
    case UkPropertyBusinessName =>
      UkPropertyBusinessName.name
    case ForeignPropertyBusinessName =>
      ForeignPropertyBusinessName.name
    case SelfEmploymentBusinessName =>
      SelfEmploymentBusinessName.name
  }
}