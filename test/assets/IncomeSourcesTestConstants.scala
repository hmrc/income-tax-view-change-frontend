/*
 * Copyright 2018 HM Revenue & Customs
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
import models.incomeSourcesWithDeadlines.IncomeSourcesModel

object IncomeSourcesTestConstants {
  val bothIncomeSourceSuccessMisalignedTaxYear = IncomeSourcesModel(List(businessIncomeModel, businessIncomeModel2), Some(propertyIncomeModel))
  val businessIncomeSourceSuccess = IncomeSourcesModel(List(businessIncomeModel), None)
  val business2018IncomeSourceSuccess = IncomeSourcesModel(List(business2018IncomeModel), None)
  val business2018And19IncomeSourceSuccess = IncomeSourcesModel(List(business2018IncomeModel, business2019IncomeModel), None)
  val propertyIncomeSourceSuccess = IncomeSourcesModel(List.empty, Some(propertyIncomeModel))
  val noIncomeSourceSuccess = IncomeSourcesModel(List.empty, None)
  val bothIncomeSourcesSuccessBusinessAligned =
    IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), Some(propertyIncomeModel))
}
