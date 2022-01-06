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

package testConstants

import testConstants.BusinessDetailsTestConstants.{business2, obligationsDataSuccessModel}
import testConstants.NextUpdatesTestConstants.nextUpdatesDataPropertySuccessModel
import models.nextUpdates.ObligationsModel

object IncomeSourcesWithDeadlinesTestConstants {
  val businessAndPropertyIncomeWithDeadlines = ObligationsModel(Seq(obligationsDataSuccessModel,
    obligationsDataSuccessModel.copy(identification = business2.incomeSourceId.get), nextUpdatesDataPropertySuccessModel))
  val singleBusinessIncomeWithDeadlines = ObligationsModel(Seq(obligationsDataSuccessModel))
  val propertyIncomeOnlyWithDeadlines = ObligationsModel(Seq(nextUpdatesDataPropertySuccessModel))
  val businessAndPropertyAlignedWithDeadlines = ObligationsModel(Seq(obligationsDataSuccessModel, nextUpdatesDataPropertySuccessModel))
  val noIncomeDetailsWithNoDeadlines = ObligationsModel(Seq.empty)
}
