package helpers

import models.creditDetailModel.{BalancingChargeCreditType, CreditDetailModel, CutOverCreditType, MfaCreditType}
import models.financialDetails.DocumentDetail

import java.time.LocalDate

trait CreditsSummaryDataHelper {
  val chargesList: Seq[CreditDetailModel] = Seq(
    CreditDetailModel(
      date = LocalDate.of(2018, 3, 29),
      documentDetail = DocumentDetail(
        taxYear = "2023",
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
        taxYear = "2023",
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
        taxYear = "2023",
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
        taxYear = "2023",
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
        taxYear = "2023",
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
        taxYear = "2023",
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
        taxYear = "2023",
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
        taxYear = "2023",
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
        taxYear = "2023",
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
