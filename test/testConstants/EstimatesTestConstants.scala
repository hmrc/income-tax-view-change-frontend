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

package testConstants

import testConstants.BaseTestConstants.{testErrorMessage, testErrorStatus, testTimeStampString}
import models.calculation._
import play.api.http.Status

object EstimatesTestConstants {

	val testYear = 2018
	val testYearPlusOne = 2019
	val testYearPlusTwo = 2020
	val testYearPlusThree = 2021
	val testYearPlusFour = 2022

	//Last Tax Calculations

	val lastTaxCalcSuccess: Calculation = Calculation(crystallised = false, timestamp = Some(testTimeStampString), totalIncomeTaxAndNicsDue = Some(123))

	val lastTaxCalcCrystallisedSuccess: Calculation = Calculation(crystallised = true, totalIncomeTaxAndNicsDue = Some(123))

	val lastTaxCalcError: CalculationErrorModel = CalculationErrorModel(testErrorStatus, testErrorMessage)
	val lastTaxCalcNotFound: CalculationResponseModel = CalculationErrorModel(Status.NOT_FOUND, "Not found")

	val lastTaxCalcWithYearList = List(
		CalculationResponseModelWithYear(lastTaxCalcSuccess, testYear),
		CalculationResponseModelWithYear(lastTaxCalcSuccess, testYearPlusOne)
	)
	val lastThreeTaxCalcWithYear = List(
		CalculationResponseModelWithYear(lastTaxCalcSuccess, testYear),
		CalculationResponseModelWithYear(lastTaxCalcSuccess, testYearPlusOne),
		CalculationResponseModelWithYear(lastTaxCalcSuccess, testYearPlusTwo)
	)
	val lastTaxCalcWithYearListOneNotFound = List(
		CalculationResponseModelWithYear(lastTaxCalcSuccess, testYear),
		CalculationResponseModelWithYear(lastTaxCalcNotFound, testYearPlusOne)
	)
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

}
