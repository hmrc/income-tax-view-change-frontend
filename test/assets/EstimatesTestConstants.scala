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
import models.calculation._
import play.api.http.Status

object EstimatesTestConstants {

  val testYear = 2018
  val testYearPlusOne = 2019
  val testYearPlusTwo = 2020

  //Last Tax Calculations
  val lastTaxCalcSuccess = CalculationModel(
    testTaxCalculationId,
    Some(543.21),
    Some(testTimeStampString),
    Some(false),
    Some(123.45),
    Some(987.65)
  )

  val lastTaxCalcCrystallisedSuccess = CalculationModel(
    testTaxCalculationId,
    Some(543.21),
    Some(testTimeStampString),
    Some(true),
    Some(123.45),
    Some(987.65),
    Some(CalculationDataModel(
      None, 0.0, 123.45, 0, 0, 0,
      IncomeReceivedModel(0, 0, 0, 0),
      SavingsAndGainsModel(0, List()),
      DividendsModel(0, List()),
      GiftAidModel(0, 0, 0),
      NicModel(0, 0),
      None, List()
    ))
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

  val lastTaxCalcError = CalculationErrorModel(testErrorStatus, testErrorMessage)
  val lastTaxCalcNotFound: CalculationResponseModel = CalculationErrorModel(Status.NOT_FOUND, "Not found")

  //Last Tax Calculation With Years (for sub pages)
  val lastTaxCalcSuccessWithYear = CalculationResponseModelWithYear(lastTaxCalcSuccess, testYear)
  val lastTaxCalcWithYearList = List(
    CalculationResponseModelWithYear(lastTaxCalcSuccess, testYear),
    CalculationResponseModelWithYear(lastTaxCalcSuccess, testYearPlusOne))
  val lastTaxCalcWithYearCrystallisedList = List(
    CalculationResponseModelWithYear(lastTaxCalcCrystallisedSuccess, testYear),
    CalculationResponseModelWithYear(lastTaxCalcCrystallisedSuccess, testYearPlusOne)
  )
  val lastTaxCalcWithYearListWithError = List(
    CalculationResponseModelWithYear(lastTaxCalcCrystallisedSuccess, testYear),
    CalculationResponseModelWithYear(lastTaxCalcError, testYearPlusOne)
  )
  val lastTaxCalcWithYearListWithCalcNotFound = List(
    CalculationResponseModelWithYear(lastTaxCalcCrystallisedSuccess, testYear),
    CalculationResponseModelWithYear(lastTaxCalcNotFound, testYearPlusOne)
  )
  val lastTaxCalcErrorWithYear = CalculationResponseModelWithYear(lastTaxCalcError, testYear)

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
