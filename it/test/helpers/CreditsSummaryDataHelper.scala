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

package helpers

import models.creditDetailModel.{BalancingChargeCreditType, CreditDetailModel, CutOverCreditType, MfaCreditType}
import models.financialDetails.DocumentDetail

import java.time.LocalDate

trait CreditsSummaryDataHelper {
  val chargesList: Seq[CreditDetailModel] = Seq(
    CreditDetailModel(
      date = LocalDate.of(2018, 3, 29),
      documentDetail = DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = Some(BigDecimal("1400")),
        originalAmount = Some(BigDecimal("1400")),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
      creditType = MfaCreditType,
      balanceDetails = None
    ),
    CreditDetailModel(
      date = LocalDate.now().plusYears(1),
      documentDetail = DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = Some(BigDecimal("1400")),
        originalAmount = Some(BigDecimal("1400")),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
      creditType = CutOverCreditType,
      balanceDetails = None
    ),
    CreditDetailModel(
      date = LocalDate.now().plusYears(1),
      documentDetail = DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = Some(BigDecimal("1400")),
        originalAmount = Some(BigDecimal("1400")),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
      creditType = CutOverCreditType,
      balanceDetails = None
    ),
    CreditDetailModel(
      date = LocalDate.now().plusYears(1),
      documentDetail = DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = Some(BigDecimal("1400")),
        originalAmount = Some(BigDecimal("1400")),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
      creditType = CutOverCreditType,
      balanceDetails = None
    )
  )

  val chargesListV2: Seq[CreditDetailModel] = Seq(
    CreditDetailModel(
      date = LocalDate.of(2018, 3, 29),
      documentDetail = DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = Some(BigDecimal("1400")),
        originalAmount = Some(BigDecimal("1400")),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
      creditType = BalancingChargeCreditType,
      balanceDetails = None
    ),
    CreditDetailModel(
      date = LocalDate.of(2018, 3, 29),
      documentDetail = DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = Some(BigDecimal("1400")),
        originalAmount = Some(BigDecimal("1400")),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
      creditType = BalancingChargeCreditType,
      balanceDetails = None
    ),
    CreditDetailModel(
      date = LocalDate.now().plusYears(1),
      documentDetail = DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = Some(BigDecimal("1400")),
        originalAmount = Some(BigDecimal("1400")),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
      creditType = CutOverCreditType,
      balanceDetails = None
    ),
    CreditDetailModel(
      date = LocalDate.now().plusYears(1),
      documentDetail = DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = Some(BigDecimal("1400")),
        originalAmount = Some(BigDecimal("1400")),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
      creditType = CutOverCreditType,
      balanceDetails = None
    ),
    CreditDetailModel(
      date = LocalDate.now().plusYears(1),
      documentDetail = DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = Some(BigDecimal("1400")),
        originalAmount = Some(BigDecimal("1400")),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
      creditType = CutOverCreditType,
      balanceDetails = None
    )
  )
}
