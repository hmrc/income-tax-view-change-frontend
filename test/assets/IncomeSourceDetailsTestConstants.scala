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

import assets.BaseTestConstants._
import assets.BusinessDetailsTestConstants._
import assets.PropertyDetailsTestConstants._
import models.calculation.BillsViewModel
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}

object IncomeSourceDetailsTestConstants {
  val businessesAndPropertyIncome = IncomeSourceDetailsModel(List(business1, business2), Some(propertyDetails))
  val singleBusinessIncome = IncomeSourceDetailsModel(List(business1), None)
  val businessIncome2018and2019 = IncomeSourceDetailsModel(List(business2018, business2019), None)
  val propertyIncomeOnly = IncomeSourceDetailsModel(List(), Some(propertyDetails))
  val businessAndPropertyAligned = IncomeSourceDetailsModel(List(alignedBusiness), Some(propertyDetails))
  val noIncomeDetails = IncomeSourceDetailsModel(List(), None)
  val errorResponse = IncomeSourceDetailsError(testErrorStatus, testErrorMessage)
}
