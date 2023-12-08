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

sealed trait IncomeSourceTypeKey {
  val key: String
}

case object SelfEmploymentKey extends IncomeSourceTypeKey {
  override val key: String = "SE"
}

case object UkPropertyKey extends IncomeSourceTypeKey {
  override val key: String = "UK"
}

case object ForeignPropertyKey extends IncomeSourceTypeKey {
  override val key: String = "FP"
}

object IncomeSourceTypeKey {
  implicit def incomeSourceTypeKeyToString(in: IncomeSourceTypeKey): String = {
    in match {
      case SelfEmploymentKey => SelfEmploymentKey.key
      case UkPropertyKey => UkPropertyKey.key
      case ForeignPropertyKey => ForeignPropertyKey.key
    }
  }
}