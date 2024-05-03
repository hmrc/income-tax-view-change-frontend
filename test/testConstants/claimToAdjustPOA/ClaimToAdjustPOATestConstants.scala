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

package testConstants.claimToAdjustPOA

import models.financialDetails.{BalanceDetails, DocumentDetail, FinancialDetailsErrorModel, FinancialDetailsModel}

import java.time.LocalDate

object ClaimToAdjustPOATestConstants {

  def genericDocumentDetailPOA1(taxYearEnd: Int) = DocumentDetail(
    taxYear = taxYearEnd,
    transactionId = "DOCID01",
    documentDescription = Some("ITSA- POA 1"),
    documentText = None,
    outstandingAmount = Some(150.00),
    originalAmount = Some(150.00),
    documentDueDate = Some(LocalDate.of(taxYearEnd, 1, 31)),
    documentDate = LocalDate.of(taxYearEnd, 3, 29),
    interestOutstandingAmount = Some(150),
    interestRate = None,
    interestFromDate = None,
    interestEndDate = None,
    latePaymentInterestAmount = Some(150),
    latePaymentInterestId = None
  )

  def genericDocumentDetailPOA2(taxYearEnd: Int) = DocumentDetail(
    taxYear = taxYearEnd,
    transactionId = "DOCID02",
    documentDescription = Some("ITSA - POA 2"),
    documentText = None,
    outstandingAmount = Some(250.00),
    originalAmount = Some(250.00),
    documentDueDate = Some(LocalDate.of(taxYearEnd, 7, 31)),
    documentDate = LocalDate.of(taxYearEnd, 3, 29),
    interestOutstandingAmount = Some(250),
    interestRate = None,
    interestFromDate = None,
    interestEndDate = None,
    latePaymentInterestAmount = Some(250),
    latePaymentInterestId = None
  )

  val userPOADetails2024: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(2024), genericDocumentDetailPOA2(2024)),
    financialDetails = List.empty,
  )

  def genericUserPOADetails(taxYearEnd: Int): FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(taxYearEnd), genericDocumentDetailPOA2(taxYearEnd)),
    financialDetails = List.empty,
  )

  def genericUserPOADetailsPOA1Only(taxYearEnd: Int): FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(taxYearEnd)),
    financialDetails = List.empty,
  )

  def genericUserPOADetailsPOA2Only(taxYearEnd: Int): FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(taxYearEnd)),
    financialDetails = List.empty,
  )

  val userPOADetails2023OnlyPOA1: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(2023)),
    financialDetails = List.empty,
  )

  val userPOADetails2023OnlyPOA2: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA2(2023)),
    financialDetails = List.empty,
  )

  val userNoPOADetails: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List.empty,
    financialDetails = List.empty,
  )

  def financialDetailsErrorModel(errorCode: Int = 404): FinancialDetailsErrorModel = FinancialDetailsErrorModel(errorCode, "There was an error...")

}
