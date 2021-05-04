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

import assets.BaseTestConstants._
import models.calculation._
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}
import models.financialTransactions.{SubItemModel, TransactionModel}
import play.api.http.Status

import java.time.LocalDate

object EstimatesTestConstants {

  val testYear = 2018
  val testYearPlusOne = 2019
  val testYearPlusTwo = 2020

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

  def transactionModelStatus(paid: Boolean, overdue: Boolean): TransactionModel = {
    val outstandingAmount = if (paid) 0 else 1
    val dueDate = if (overdue) LocalDate.now().minusDays(1) else LocalDate.now().plusDays(1)
    TransactionModel(
      outstandingAmount = Some(outstandingAmount),
      items = Some(Seq(SubItemModel(dueDate = Some(dueDate))))
    )
  }

  def chargeModelStatus(paid: Boolean, overdue: Boolean): DocumentDetailWithDueDate = {
    val outstandingAmount = if (paid) 0 else 1
    val dueDate = if (overdue) LocalDate.now().minusDays(1) else LocalDate.now().plusDays(1)
    DocumentDetailWithDueDate(
      DocumentDetail(
        taxYear = "2019",
        transactionId = "id",
        documentDescription = None,
        outstandingAmount = Some(outstandingAmount),
        originalAmount = None
      ),
      Some(dueDate)
    )
  }
}
