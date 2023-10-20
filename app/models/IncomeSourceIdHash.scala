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


object IncomeSources {
  implicit class IncomeSourceId(val id: String) extends AnyVal {
    override def toString: String = s"IncomeSourceId: ${id}"

    def makeHash(): IncomeSourceHashId = {
      new IncomeSourceHashId(id.hashCode.toHexString)
    }
  }

  implicit class IncomeSourceHashId(val hash: String) extends AnyVal {
    override def toString: String = s"IncomeSourceID hash value: $hash"

    // check if hash value is accommodate with any incomceSourceId in the list
    def contains(ids: List[IncomeSourceId]): Option[IncomeSourceId] = ???
  }

  def makeId(incomeSourceAsString: String): IncomeSourceId = new IncomeSourceId(incomeSourceAsString)
}


