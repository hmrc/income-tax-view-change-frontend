/*
 * Copyright 2024 HM Revenue & Customs
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

package testConstants.claimToAdjustPoa

import models.chargeHistory.ChargeHistoryModel
import models.claimToAdjustPoa.{PaymentOnAccountViewModel, WhatYouNeedToKnowViewModel}
import models.core.NormalMode
import models.financialDetails._
import models.incomeSourceDetails.TaxYear

import java.time.LocalDate

object ClaimToAdjustPoaTestConstants {

  def genericFinancialDetailPOA1(taxYearEnd: Int, outstandingAmount: BigDecimal = 0.0) = FinancialDetail(
    taxYear = taxYearEnd.toString,
    mainType = Some("SA Payment on Account 1"),
    mainTransaction = Some("4920"),
    outstandingAmount = Some(outstandingAmount),
    chargeReference = Some("ABCD1234"),
    items = None,
    transactionId = Some("DOCID01")
  )

  def genericFinancialDetailPOA2(taxYearEnd: Int, outstandingAmount: BigDecimal = 0.0) = FinancialDetail(
    taxYear = taxYearEnd.toString,
    mainType = Some("SA Payment on Account 2"),
    mainTransaction = Some("4930"),
    outstandingAmount = Some(outstandingAmount),
    chargeReference = Some("ABCD1234"),
    items = None,
    transactionId = Some("DOCID02")
  )

  def genericFinancialDetailPOA2NoTransactionID(taxYearEnd: Int, outstandingAmount: BigDecimal = 0.0) = FinancialDetail(
    taxYear = taxYearEnd.toString,
    mainType = Some("SA Payment on Account 2"),
    mainTransaction = Some("4930"),
    outstandingAmount = Some(outstandingAmount),
    chargeReference = Some("ABCD1234"),
    items = None
  )

  def genericDocumentDetailPOA1(taxYearEnd: Int, outstandingAmount: BigDecimal = 150.00) = DocumentDetail(
    taxYear = taxYearEnd,
    transactionId = "DOCID01",
    documentDescription = Some("ITSA- POA 1"),
    documentText = None,
    outstandingAmount = outstandingAmount,
    originalAmount = 150.00,
    poaRelevantAmount = Some(100.00),
    documentDueDate = Some(LocalDate.of(taxYearEnd, 1, 31)),
    documentDate = LocalDate.of(taxYearEnd, 3, 29),
    interestOutstandingAmount = Some(150),
    interestRate = None,
    interestFromDate = None,
    interestEndDate = None,
    accruingInterestAmount = Some(150),
    latePaymentInterestId = None
  )

  def genericDocumentDetailPOA2(taxYearEnd: Int, outstandingAmount: BigDecimal = 250.00) = DocumentDetail(
    taxYear = taxYearEnd,
    transactionId = "DOCID02",
    documentDescription = Some("ITSA - POA 2"),
    documentText = None,
    outstandingAmount = outstandingAmount,
    originalAmount = 250.00,
    poaRelevantAmount = Some(100.00),
    documentDueDate = Some(LocalDate.of(taxYearEnd, 7, 31)),
    documentDate = LocalDate.of(taxYearEnd, 3, 29),
    interestOutstandingAmount = Some(250),
    interestRate = None,
    interestFromDate = None,
    interestEndDate = None,
    accruingInterestAmount = Some(250),
    latePaymentInterestId = None
  )

  val userPOADetails2024: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(2024), genericDocumentDetailPOA2(2024)),
    financialDetails = List(genericFinancialDetailPOA1(2023, 150.00), genericFinancialDetailPOA2(2024, 250.00)),
  )

  val userPOADetails2023: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(2023), genericDocumentDetailPOA2(2023)),
    financialDetails = List(genericFinancialDetailPOA1(2023, 150.00),
      genericFinancialDetailPOA2(2023, 250.00))
  )

  def financialDetailsWithUnpaidPoas(taxYearEnd: Int): FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(taxYearEnd), genericDocumentDetailPOA2(taxYearEnd)),
    financialDetails = List(genericFinancialDetailPOA1(taxYearEnd, 150.00), genericFinancialDetailPOA2(taxYearEnd, 250.00))
  )

  def genericUserPoaDetails(taxYearEnd: Int, outstandingAmount: BigDecimal): FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(taxYearEnd, outstandingAmount = outstandingAmount), genericDocumentDetailPOA2(taxYearEnd, outstandingAmount = outstandingAmount)),
    financialDetails = List.empty,
  )

  def genericUserPoaDetailsPOA1Only(taxYearEnd: Int): FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(taxYearEnd)),
    financialDetails = List.empty,
  )

  def genericUserPoaDetailsPOA2Only(taxYearEnd: Int): FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(taxYearEnd)),
    financialDetails = List.empty,
  )

  val userPoaDetails2023OnlyPOA1: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(2023)),
    financialDetails = List.empty,
  )

  val userPoaDetails2023OnlyPOA2: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA2(2023)),
    financialDetails = List.empty,
  )

  val userNoPoaDetails: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List.empty,
    financialDetails = List.empty,
  )

  def financialDetailsErrorModel(errorCode: Int = 404): FinancialDetailsErrorModel = FinancialDetailsErrorModel(errorCode, "There was an error...")

  val testPoa1Maybe: Option[PaymentOnAccountViewModel] = Some(
    PaymentOnAccountViewModel(
      poaOneTransactionId = "poaOne-Id",
      poaTwoTransactionId = "poaTwo-Id",
      taxYear = TaxYear.makeTaxYearWithEndYear(2024),
      totalAmountOne= 5000.00,
      totalAmountTwo = 5000.00,
      relevantAmountOne = 5000.00,
      relevantAmountTwo = 5000.00,
      partiallyPaid = false,
      fullyPaid = false,
      previouslyAdjusted = None
    )
  )

  val fixedDate: LocalDate = LocalDate.of(2023, 12, 15)

  def whatYouNeedToKnowViewModel(isAgent: Boolean, showIncreaseAfterPaymentContent: Boolean): WhatYouNeedToKnowViewModel = WhatYouNeedToKnowViewModel(poaTaxYear = TaxYear(fixedDate.getYear, fixedDate.getYear + 1), showIncreaseAfterPaymentContent, controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent, NormalMode).url)

  def chargeHistoryModelNoPOA(taxYear: Int): ChargeHistoryModel = ChargeHistoryModel(s"${taxYear.toString}", "1040000124", LocalDate.of(taxYear, 7, 6), "documentDescription", 1500, LocalDate.of(2018, 7, 6), "amended return", None)

}
