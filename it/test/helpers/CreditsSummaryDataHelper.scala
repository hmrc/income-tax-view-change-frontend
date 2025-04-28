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

import models.creditDetailModel.CreditDetailModel
import models.financialDetails._

import java.time.LocalDate

trait CreditsSummaryDataHelper {
  lazy val fixedDate : LocalDate = LocalDate.of(2022, 1, 7)
  val chargesList: Seq[CreditDetailModel] = Seq(
    CreditDetailModel(
      date = LocalDate.of(2018, 3, 29),
      charge = ChargeItem.fromDocumentPair(
        DocumentDetail(taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = BigDecimal("1400"),
        originalAmount = BigDecimal("1400"),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
        List(
          FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
          mainTransaction = Some("4004"), transactionId = Some("transId"),
          transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
          clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
        )
        )
      ),
      creditType = MfaCreditType,
      availableCredit = None
    ),
    CreditDetailModel(
      date = fixedDate.plusYears(1),
      charge =
        ChargeItem.fromDocumentPair( DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = BigDecimal("1400"),
        originalAmount = BigDecimal("1400"),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
          List(
            FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
              mainTransaction = Some("4004"), transactionId = Some("transId"),
              transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
              clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
            )
          )
        ),
      creditType = CutOverCreditType,
      availableCredit = None
    ),
    CreditDetailModel(
      date = fixedDate.plusYears(1),
      charge =
        ChargeItem.fromDocumentPair(
          DocumentDetail(
            taxYear = 2023,
            transactionId = "transId",
            documentDescription = Some("docId"),
            documentText = Some("text"),
            outstandingAmount = BigDecimal("1400"),
            originalAmount = BigDecimal("1400"),
          documentDate = LocalDate.of(2023, 12, 23)
          ),
          List(
            FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
              mainTransaction = Some("4004"), transactionId = Some("transId"),
              transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
              clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
            )
          )
        ),
      creditType = CutOverCreditType,
      availableCredit = None
    ),
    CreditDetailModel(
      date = fixedDate.plusYears(1),
      charge =
        ChargeItem.fromDocumentPair(
          DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = BigDecimal("1400"),
        originalAmount = BigDecimal("1400"),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
          List(
            FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
              mainTransaction = Some("4004"), transactionId = Some("transId"),
              transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
              clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
            )
          )
        ),
      creditType = CutOverCreditType,
      availableCredit = None
    )
  )

  val chargesListV2: Seq[CreditDetailModel] = Seq(
    CreditDetailModel(
      date = LocalDate.of(2018, 3, 29),
      charge =
        ChargeItem.fromDocumentPair( DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = BigDecimal("1400"),
        originalAmount = BigDecimal("1400"),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
          List(
            FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
              mainTransaction = Some("4004"), transactionId = Some("transId"),
              transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
              clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
            )
          )
        ),
      creditType = BalancingChargeCreditType,
      availableCredit = None
    ),
    CreditDetailModel(
      date = LocalDate.of(2018, 3, 29),
      charge =
        ChargeItem.fromDocumentPair( DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = BigDecimal("1400"),
        originalAmount = BigDecimal("1400"),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
          List(
            FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
              mainTransaction = Some("4004"), transactionId = Some("transId"),
              transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
              clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
            )
          )
        ),
      creditType = BalancingChargeCreditType,
      availableCredit = None
    ),
    CreditDetailModel(
      date = fixedDate.plusYears(1),
      charge = ChargeItem.fromDocumentPair( DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = BigDecimal("1400"),
        originalAmount = BigDecimal("1400"),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
        List(
          FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
            mainTransaction = Some("4004"), transactionId = Some("transId"),
            transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
            clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
          )
        )
      ),
      creditType = CutOverCreditType,
      availableCredit = None
    ),
    CreditDetailModel(
      date = fixedDate.plusYears(1),
      charge = ChargeItem.fromDocumentPair( DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = BigDecimal("1400"),
        originalAmount = BigDecimal("1400"),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
        List(
          FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
            mainTransaction = Some("4004"), transactionId = Some("transId"),
            transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
            clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
          )
        )
      ),
      creditType = CutOverCreditType,
      availableCredit = None
    ),
    CreditDetailModel(
      date = fixedDate.plusYears(1),
      charge = ChargeItem.fromDocumentPair(
        DocumentDetail(
        taxYear = 2023,
        transactionId = "transId",
        documentDescription = Some("docId"),
        documentText = Some("text"),
        outstandingAmount = BigDecimal("1400"),
        originalAmount = BigDecimal("1400"),
        documentDate = LocalDate.of(2023, 12, 23)
      ),
        List(
          FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
            mainTransaction = Some("4004"), transactionId = Some("transId"),
            transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
            clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
          )
        )
      ),
      creditType = CutOverCreditType,
      availableCredit = None
    )
  )
}
