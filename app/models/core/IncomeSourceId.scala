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

package models.core

import models.core.IncomeSourceIdHash.mkIncomeSourceIdHash

class IncomeSourceId private(val value: String) extends AnyVal {
  def toHash: IncomeSourceIdHash = mkIncomeSourceIdHash(this)

  override def toString: String = s"IncomeSourceId: $value"
}

object IncomeSourceId {
  def apply(incomeSourceAsString: String): IncomeSourceId = {
    new IncomeSourceId(incomeSourceAsString)
  }
  def mkIncomeSourceId(incomeSourceAsString: String): IncomeSourceId = {

    new IncomeSourceId(incomeSourceAsString)

  }

  // For future validation implementation:
  // suggested valid pattern - val validPattern = "^[a-zA-Z0-9]{15}$".r
  // suggested method of comparison - validPattern.unapplySeq(incomeSourceAsString).isDefined

}
