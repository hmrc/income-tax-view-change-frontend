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

import models.incomeSourceDetails.incomeSourceIds.IncomeSourceIdHash.mkIncomeSourceIdHash
import play.api.{Logger, Logging}

class IncomeSourceId private(val value: String) extends AnyVal {
  def toHash: IncomeSourceIdHash = mkIncomeSourceIdHash(this)

  override def toString: String = s"IncomeSourceId: $value"
}


object IncomeSourceId {
  def mkIncomeSourceId(incomeSourceAsString: String): IncomeSourceId = {

    if (validateStringAsIncomeSourceId(incomeSourceAsString = incomeSourceAsString)) {
      new IncomeSourceId(incomeSourceAsString)
    } else {
      Logger("application").info(s"[IncomeSourceId][mkIncomeSourceId] incomeSourceId was not the correct length or contained special characters")
      new IncomeSourceId(incomeSourceAsString)
    }

  }

  def validateStringAsIncomeSourceId(incomeSourceAsString: String): Boolean = {
    val validPattern = "^[a-zA-Z0-9]{15}$".r

    validPattern.unapplySeq(incomeSourceAsString).isDefined
  }
}