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

package models

import models.IncomeSourceIdHash.mkIncomeSourceIdHash


class IncomeSourceId private(val value: String) extends AnyVal {
  def toHash: IncomeSourceIdHash = mkIncomeSourceIdHash(this)

  // to support conversion to string when needed by context
  override def toString: String = s"IncomeSourceId: $value"
}


object IncomeSourceId {
  def mkIncomeSourceId(incomeSourceAsString: String): IncomeSourceId = {
    // validation can be added to verify incomeSourceId String value if needed
    new IncomeSourceId(incomeSourceAsString)
  }
}

// Hashing is not in the scope of MISUV-6471 // all code below
class IncomeSourceIdHash private(val hash: String) extends AnyVal {
  override def toString: String = s"IncomeSourceIdHash: $hash"
}


object IncomeSourceIdHash {
  // the only way to create incomeSourceId hash is to provide instance of incomeSourceId

  def mkIncomeSourceIdHash(id: IncomeSourceId): IncomeSourceIdHash = {
    // TODO: implement incomeSourceId value hashing here
    val hash = id.value.hashCode().abs.toString() // default hashing used as the moment
    new IncomeSourceIdHash(hash)
  }
}