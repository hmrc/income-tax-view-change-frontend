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

package testConstants

import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}
import testConstants.BaseTestConstants.{testErrorMessage, testErrorStatus, testMigrationYear2019, testMtditid, testMtditid2}
import testConstants.BusinessDetailsTestConstants._
import testConstants.PropertyDetailsTestConstants._

import java.time.LocalDate

object IncomeSourceDetailsTestConstants {
  val businessesAndPropertyIncome = IncomeSourceDetailsModel(testMtdItId, Some("2018"), List(business1, business2), List(propertyDetails))
  val businessesAndPropertyIncomeCeased = IncomeSourceDetailsModel(testMtdItId, Some("2018"), List(ceasedBusiness), List(ceasedPropertyDetails))
  val singleBusinessIncome = IncomeSourceDetailsModel(testMtdItId, Some("2017"), List(business1), Nil)
  val singleBusinessIncome2023 = IncomeSourceDetailsModel(testMtdItId, Some("2023"), List(businessWithLatency1), Nil)
  val singleBusinessIncome2024 = IncomeSourceDetailsModel(testMtdItId, Some("2024"), List(businessWithLatency2), Nil)
  val singleBusinessIncomeNotMigrated = IncomeSourceDetailsModel(testMtdItId, None, List(business1), Nil)
  val singleBusinessIncomeWithCurrentYear = IncomeSourceDetailsModel(testMtdItId, Some(LocalDate.now().getYear.toString), List(business1), Nil)
  val businessIncome2018and2019 = IncomeSourceDetailsModel(testMtdItId, None, List(business2018, business2019), Nil)
  val propertyIncomeOnly = IncomeSourceDetailsModel(testMtdItId, None, List(), List(propertyDetails))
  val businessAndPropertyAligned = IncomeSourceDetailsModel(testMtdItId, Some(getCurrentTaxYearEnd.minusYears(1).getYear.toString),
    List(alignedBusiness), List(propertyDetails))
  val singleBusinessAndPropertyMigrat2019 = IncomeSourceDetailsModel(testMtdItId, Some(testMigrationYear2019), List(alignedBusiness), List(propertyDetails))
  val noIncomeDetails = IncomeSourceDetailsModel(testMtdItId, None, List(), Nil)
  val errorResponse = IncomeSourceDetailsError(testErrorStatus, testErrorMessage)
  val businessIncome2018and2019AndProp = IncomeSourceDetailsModel(testMtdItId, None, List(business2018, business2019), List(propertyDetails))
  val oldUserDetails = IncomeSourceDetailsModel(testMtdItId, Some(getCurrentTaxYearEnd.minusYears(1).getYear.toString),
    List(oldUseralignedBusiness), List(propertyDetails))
  val preSanitised = IncomeSourceDetailsModel(testMtdItId, Some((LocalDate.now.getYear - 1).toString), List(business2018, alignedBusiness), List(propertyDetails))

  val businessIncome = IncomeSourceDetailsModel(testMtditid, Some("2018"), List(soleTraderBusiness), List())
  val businessIncome2 = IncomeSourceDetailsModel(testMtditid2, Some("2018"), List(soleTraderBusiness2), List())
  val businessIncome3 = IncomeSourceDetailsModel(testMtditid2, Some("2018"), List(soleTraderBusiness, soleTraderBusiness2), List())
  val ukPropertyIncome = IncomeSourceDetailsModel(testMtdItId, Some("2018"), List(), List(ukPropertyDetails))
  val ukPropertyWithSoleTraderBusiness = IncomeSourceDetailsModel(testMtdItId, None, List(business2018), List(ukPropertyDetails))
  val ukPlusForeignPropertyWithSoleTraderIncomeSource = IncomeSourceDetailsModel(testMtdItId, Some("2018"), List(soleTraderBusiness), List(ukPropertyDetails, foreignPropertyDetails))
  val ukPropertyAndSoleTraderBusinessIncome = IncomeSourceDetailsModel(testMtdItId, Some("2018"), List(soleTraderBusiness), List(ukPropertyDetails))
  val ukPlusForeignPropertyAndSoleTraderPlusCeasedBusinessIncome = IncomeSourceDetailsModel(testMtdItId, Some("2018"), List(soleTraderBusiness, ceasedBusiness), List(ukPropertyDetails, foreignPropertyDetails))
  val ukPropertyAndSoleTraderBusinessIncomeNoTradingName = IncomeSourceDetailsModel(testMtdItId, Some("2018"), List(soleTraderBusinessNoTradingName), List(ukPropertyDetails))
  val ukPropertyAndSoleTraderBusinessIncomeNoTradingStartDate = IncomeSourceDetailsModel(testMtdItId, Some("2018"), List(soleTraderBusinessNoTradingStartDate), List(ukPropertyDetails))


  val foreignPropertyAndCeasedBusinessIncome = IncomeSourceDetailsModel(testMtdItId, Some("2018"), List(ceasedBusiness, ceasedBusiness2), List(foreignPropertyDetails))
  val foreignPropertyAndCeasedBusinessIncomeNoStartDate = IncomeSourceDetailsModel(testMtdItId, Some("2018"), List(ceasedBusiness, ceasedBusiness2), List(foreignPropertyDetailsNoStartDate))


  val foreignPropertyIncome = IncomeSourceDetailsModel(testMtdItId, Some("2018"), Nil, List(foreignPropertyDetails))


  def getCurrentTaxEndYear(currentDate: LocalDate): Int = {
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }
}
