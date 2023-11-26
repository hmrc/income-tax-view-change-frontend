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

package controllers.helpers

import auth.MtdItUser
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.IncomeSourceIdHash.mkFromQueryString
import models.core.{IncomeSourceId, IncomeSourceIdHash}

trait IncomeSourceIdHelper {

  def mkIncomeSourceHashMaybe(id: Option[String]): Option[IncomeSourceIdHash] = {
    id match {
      case Some(_) => id.flatMap(mkFromQueryString)
      case None => None
    }
  }

  def compare(incomeSourceIdHash: Option[IncomeSourceIdHash])(implicit user: MtdItUser[_]): Option[IncomeSourceId] = {
    val xs = user.incomeSources.businesses.map(m => mkIncomeSourceId(m.incomeSourceId))

    incomeSourceIdHash.flatMap(_.oneOf(xs))
  }

}
