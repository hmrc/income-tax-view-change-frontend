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

package models.incomeSourceDetails.incomeSourceIds

import models.incomeSourceDetails.incomeSourceIds.IncomeSourceIdHash.mkFromIncomeSourceId

import scala.util.Try

class IncomeSourceIdHash private(val hash: String) extends AnyVal {
  override def toString: String = s"IncomeSourceIdHash: $hash"

  def oneOf(ids: List[IncomeSourceId]): Option[IncomeSourceId] = {
    ids.find(id => {
      val hashId = mkFromIncomeSourceId(id)
      hashId.hash == this.hash
    })
  }
}

object IncomeSourceIdHash {
  def mkFromIncomeSourceId(id: IncomeSourceId): IncomeSourceIdHash = {
    val hash = id.value.hashCode().abs.toString
    new IncomeSourceIdHash(hash)
  }

  def mkFromQueryString(id: String): Either[Throwable, IncomeSourceIdHash] = Try {
    val hash = id.hashCode().abs.toString
    new IncomeSourceIdHash(hash)
  }.toEither

}