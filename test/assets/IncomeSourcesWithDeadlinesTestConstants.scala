/*
 * Copyright 2019 HM Revenue & Customs
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

package assets

import assets.BusinessDetailsTestConstants._
import assets.PropertyDetailsTestConstants._
import models.incomeSourcesWithDeadlines.IncomeSourcesWithDeadlinesModel

object IncomeSourcesWithDeadlinesTestConstants {
  val businessAndPropertyIncomeWithDeadlines = IncomeSourcesWithDeadlinesModel(List(businessIncomeModel, businessIncomeModel2), Some(propertyIncomeModel), None)
  val singleBusinessIncomeWithDeadlines = IncomeSourcesWithDeadlinesModel(List(businessIncomeModel), None, None)
  val businessIncome2018and2019WithDeadlines = IncomeSourcesWithDeadlinesModel(List(business2018IncomeModel, business2019IncomeModel), None, None)
  val propertyIncomeOnlyWithDeadlines = IncomeSourcesWithDeadlinesModel(List.empty, Some(propertyIncomeModel), None)
  val businessAndPropertyAlignedWithDeadlines =
    IncomeSourcesWithDeadlinesModel(List(businessIncomeModelAlignedTaxYear), Some(propertyIncomeModel), None)
  val noIncomeDetailsWithNoDeadlines = IncomeSourcesWithDeadlinesModel(List.empty, None, None)
}
