/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate

import PropertyDetailsTestConstants.propertyDetails
import BaseTestConstants._
import BusinessDetailsTestConstants._
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}

object IncomeSourceDetailsTestConstants {
  val businessesAndPropertyIncome = IncomeSourceDetailsModel(testMtdItId, None, List(business1, business2), Some(propertyDetails))
  val singleBusinessIncome = IncomeSourceDetailsModel(testMtdItId, Some("2017"), List(business1), None)
  val singleBusinessIncomeWithCurrentYear = IncomeSourceDetailsModel(testMtdItId, Some(LocalDate.now().getYear.toString), List(business1), None)
  val businessIncome2018and2019 = IncomeSourceDetailsModel(testMtdItId, None, List(business2018, business2019), None)
  val propertyIncomeOnly = IncomeSourceDetailsModel(testMtdItId, None, List(), Some(propertyDetails))
  val businessAndPropertyAligned = IncomeSourceDetailsModel(testMtdItId, None, List(alignedBusiness), Some(propertyDetails))
  val singleBusinessAndPropertyMigrat2019 = IncomeSourceDetailsModel(testMtdItId, Some(testMigrationYear2019), List(alignedBusiness), Some(propertyDetails))
  val noIncomeDetails = IncomeSourceDetailsModel(testMtdItId, None, List(), None)
  val errorResponse = IncomeSourceDetailsError(testErrorStatus, testErrorMessage)
  val businessIncome2018and2019AndProp = IncomeSourceDetailsModel(testMtdItId, None, List(business2018, business2019), Some(propertyDetails))
  val oldUserDetails = IncomeSourceDetailsModel(testMtdItId, None, List(oldUseralignedBusiness), Some(propertyDetails))
}
