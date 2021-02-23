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
import assets.BaseTestConstants._
import enums.Estimate
import models.calculation._
import models.financialDetails.{Charge, ChargeModelWithYear, SubItem}
import models.financialTransactions.{SubItemModel, TransactionModel, TransactionModelWithYear}
import play.api.http.Status

object EstimatesTestConstants {

  val testYear = 2018
  val testYearPlusOne = 2019
  val testYearPlusTwo = 2020

  //Last Tax Calculations

  val lastTaxCalcSuccess = Calculation(crystallised = false, timestamp = Some(testTimeStampString), totalIncomeTaxAndNicsDue = Some(123) )

  val responseModel = CalcDisplayModel(testTimeStampString, 10.00, lastTaxCalcSuccess, Estimate)

  val lastTaxCalcCrystallisedSuccess = Calculation(crystallised = true, totalIncomeTaxAndNicsDue = Some(123))

  val lastTaxCalcError = CalculationErrorModel(testErrorStatus, testErrorMessage)
  val lastTaxCalcNotFound: CalculationResponseModel = CalculationErrorModel(Status.NOT_FOUND, "Not found")

  //Last Tax Calculation With Years (for sub pages)
  val lastTaxCalcSuccessWithYear = CalculationResponseModelWithYear(lastTaxCalcSuccess, testYear)
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
  val lastTaxCalcErrorWithYear = CalculationResponseModelWithYear(lastTaxCalcError, testYear)

  val lastThreeTaxYearFinancialTransactions = List(
    TransactionModelWithYear(transactionModelStatus(false, false), testYear),
    TransactionModelWithYear(transactionModelStatus(true, false), testYearPlusOne),
    TransactionModelWithYear(transactionModelStatus(false, true), testYearPlusTwo)
  )

  val lastThreeTaxYearFinancialCharges = List(
    ChargeModelWithYear(chargeModelStatus(false, false), testYear),
    ChargeModelWithYear(chargeModelStatus(true, false), testYearPlusOne),
    ChargeModelWithYear(chargeModelStatus(false, true), testYearPlusTwo)
  )

  def transactionModelStatus(paid: Boolean, overdue: Boolean): TransactionModel = {
    val outstandingAmount = if (paid) 0 else 1
    val dueDate = if (overdue) LocalDate.now().minusDays(1) else LocalDate.now().plusDays(1)
    TransactionModel(
      outstandingAmount = Some(outstandingAmount),
      items = Some(Seq(SubItemModel(dueDate = Some(dueDate))))
    )
  }

  def chargeModelStatus(paid: Boolean, overdue: Boolean): Charge = {
    val outstandingAmount = if (paid) 0 else 1
    val dueDate = if (overdue) LocalDate.now().minusDays(1).toString else LocalDate.now().plusDays(1).toString
    Charge(
			taxYear = "2019",
			transactionId = "id",
      outstandingAmount = Some(outstandingAmount),
      items = Some(Seq(SubItem(dueDate = Some(dueDate))))
    )
  }
}
