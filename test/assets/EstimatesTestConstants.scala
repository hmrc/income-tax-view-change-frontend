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

import assets.BaseTestConstants._
import enums.{Crystallised, Estimate}
import models.calculation._

object EstimatesTestConstants {

  val testYear = 2018
  val testYearPlusOne = 2019
  val testYearPlusTwo = 2020
  val testCalcType = "it"

  //Last Tax Calculations
  val lastTaxCalcSuccess = LastTaxCalculation(
    calcID = testTaxCalculationId,
    calcTimestamp = testTimeStampString,
    calcAmount = 543.21,
    calcStatus = Estimate
  )
  val lastTaxCalcCrystallisedSuccess = LastTaxCalculation(
    calcID = testTaxCalculationId,
    calcTimestamp = testTimeStampString,
    calcAmount = 543.21,
    calcStatus = Crystallised
  )

  val lastCalcSuccessEstimate = CalculationModel(
    calcID = testTaxCalculationId,
    calcAmount = Some(543.21),
    calcTimestamp = Some(testTimeStampString),
    crystallised = None,
    incomeTaxNicYtd = None,
    incomeTaxNicAmount = None
  )

  val lastCalcSuccessBill = CalculationModel(
    calcID = testTaxCalculationId,
    calcAmount = Some(543.21),
    calcTimestamp = Some(testTimeStampString),
    crystallised = Some(true),
    incomeTaxNicYtd = None,
    incomeTaxNicAmount = None
  )

  val lastTaxCalcError = LastTaxCalculationError(testErrorStatus, testErrorMessage)
  val lastTaxCalcNotFound: LastTaxCalculationResponseModel = NoLastTaxCalculation

  //Last Tax Calculation With Years (for sub pages)
  val lastTaxCalcSuccessWithYear = LastTaxCalculationWithYear(lastTaxCalcSuccess, testYear)
  val lastTaxCalcWithYearList = List(
    LastTaxCalculationWithYear(lastTaxCalcSuccess, testYear),
    LastTaxCalculationWithYear(lastTaxCalcSuccess, testYearPlusOne))
  val lastTaxCalcWithYearCrystallisedList = List(
    LastTaxCalculationWithYear(lastTaxCalcCrystallisedSuccess, testYear),
    LastTaxCalculationWithYear(lastTaxCalcCrystallisedSuccess, testYearPlusOne)
  )
  val lastTaxCalcWithYearListWithError = List(
    LastTaxCalculationWithYear(lastTaxCalcCrystallisedSuccess, testYear),
    LastTaxCalculationWithYear(lastTaxCalcError, testYearPlusOne)
  )
  val lastTaxCalcWithYearListWithCalcNotFound = List(
    LastTaxCalculationWithYear(lastTaxCalcCrystallisedSuccess, testYear),
    LastTaxCalculationWithYear(lastTaxCalcNotFound, testYearPlusOne)
  )
  val lastTaxCalcErrorWithYear = LastTaxCalculationWithYear(lastTaxCalcError, testYear)

  val fullEstimateViewModel: EstimatesViewModel =
    EstimatesViewModel(
      timestamp = testTimeStampString,
      currentEstimate = 123.45,
      taxYear = 2018,
      annualEstimate = Some(543.21)
    )

  val minEstimateViewModel: EstimatesViewModel =
    EstimatesViewModel(
      timestamp = testTimeStampString,
      currentEstimate = 123.45,
      taxYear = 2018,
      annualEstimate = None
    )
}
