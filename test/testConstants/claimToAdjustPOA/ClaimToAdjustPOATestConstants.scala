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

import models.financialDetails.{BalanceDetails, DocumentDetail, FinancialDetailsModel}

import java.time.LocalDate

object ClaimToAdjustPOATestConstants {

  val documentDetailPOA1 = DocumentDetail(
    taxYear = 2024,
    transactionId = "DOCID01",
    documentDescription = Some("ITSA- POA 1"),
    documentText = None,
    outstandingAmount = Some(100.00),
    originalAmount = Some(100.00),
    documentDate = LocalDate.of(2024, 3, 29),
    interestOutstandingAmount = Some(100),
    interestRate = None,
    interestFromDate = None,
    interestEndDate = None,
    latePaymentInterestAmount = Some(100),
    latePaymentInterestId = None
  )

  val documentDetailPOA2 = DocumentDetail(
    taxYear = 2024,
    transactionId = "DOCID01",
    documentDescription = Some("ITSA - POA 2"),
    documentText = None,
    outstandingAmount = Some(100.00),
    originalAmount = Some(100.00),
    documentDate = LocalDate.of(2024, 3, 29),
    interestOutstandingAmount = Some(100),
    interestRate = None,
    interestFromDate = None,
    interestEndDate = None,
    latePaymentInterestAmount = Some(100),
    latePaymentInterestId = None
  )

  val userPOADetails2024: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(documentDetailPOA1, documentDetailPOA2),
    financialDetails = List.empty,
  )

  val empty1553Response: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List.empty,
    financialDetails = List.empty,
  )

}
