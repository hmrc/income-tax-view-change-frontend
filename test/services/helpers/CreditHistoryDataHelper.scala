/*
 * Copyright 2023 HM Revenue & Customs
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

package services.helpers


import enums.ChargeType.NIC4_WALES
import models.financialDetails._
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import models.creditDetailModel._

import java.time.LocalDate

trait CreditHistoryDataHelper {


  val paymentsForTheGivenTaxYear: List[Payment] = List(Payment(reference = Some("reference"), amount = Some(100.00),
    outstandingAmount = Some(1.00), method = Some("method"), documentDescription = None, lot = Some("lot"), lotItem = Some("lotItem"),
    dueDate = Some(LocalDate.parse("2020-08-20")), documentDate = LocalDate.parse("2020-08-20"), Some("DOCID01")))


  val creditsForTheGivenTaxYear: List[Payment] = List(
    Payment(reference = Some("reference"), amount = Some(-100.00),
      outstandingAmount = Some(1.00), method = Some("method"), documentDescription = None, lot = None, lotItem = Some("lotItem"),

      dueDate = Some(LocalDate.parse("2020-08-20")), documentDate = LocalDate.parse("2020-08-20"), Some("DOCID01")),
    Payment(reference = Some("reference"), amount = Some(-100.00),
      outstandingAmount = Some(1.00), method = Some("method"), documentDescription = None, lot = None, lotItem = Some("lotItem"),
      dueDate = Some(LocalDate.parse("2020-08-20")), documentDate = LocalDate.parse("2020-08-20"), Some("DOCID02"))
  )

  val taxYear: Int = 2022
  val nino: String = "someNino"
  val documentIdA: String = "DOCID01"
  val documentIdB: String = "DOCID02"


  val documentDetailsWhichIsCutOverCredit = DocumentDetail(
    taxYear = "2022", transactionId = "testTransactionId1",
    documentDescription = Some("ITSA Cutover Credits"),
    documentText = None, outstandingAmount = Some(100.00), originalAmount = Some(-120.00),
    documentDate = LocalDate.of(taxYear, 3, 29)
  )
  val documentDetailsWhichIsMfaCredit = DocumentDetail(taxYear = "2022",
    transactionId = "testTransactionId1",
    documentDescription = Some("TRM New Charge"), documentText = None,
    outstandingAmount = Some(-150.00), originalAmount = Some(-150.00),
    documentDate = LocalDate.of(taxYear, 3, 29))

  val documentDetailsWhichIsBCCredit = DocumentDetail(taxYear = "2022",
    transactionId = "testTransactionId3",
    documentDescription = Some("ITSA- Bal Charge"), documentText = None,
    outstandingAmount = Some(120.00), originalAmount = Some(-150.00),
    documentDate = LocalDate.of(taxYear, 3, 29))


  val taxYearFinancialDetails = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    documentDetails = List(
      documentDetailsWhichIsCutOverCredit,
      DocumentDetail("2022", "testTransactionId2", None, None, None, None, LocalDate.of(taxYear, 3, 29)),
      DocumentDetail("2022", "testTransactionId3", None, None, None, None, LocalDate.of(taxYear, 3, 31))
    ),
    financialDetails = List(
      FinancialDetail(taxYear = "2022", mainType = Some("ITSA Cutover Credits"), transactionId = Some("testTransactionId1"),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25")))))),
      FinancialDetail(taxYear = "2022", mainType = None, transactionId = Some("testTransactionId2"),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25")))))),
      FinancialDetail(taxYear = "2022", mainType = None, transactionId = Some("testTransactionId3"),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25"))))))
    )
  )

  val expectedBalancedDetails = BalanceDetails(1.0,2.0,3.0,None,None,None,None)

  val creditDetailModelasCutOver = CreditDetailModel(
    date = LocalDate.parse("2022-08-25"),
    documentDetail = documentDetailsWhichIsCutOverCredit,
    CutOverCreditType,
    balanceDetails = Some(expectedBalancedDetails)
  )
  val creditDetailModelasMfa = CreditDetailModel(
    date = LocalDate.parse("2022-03-29"),
    documentDetail = documentDetailsWhichIsMfaCredit,
    MfaCreditType,
    balanceDetails = Some(expectedBalancedDetails)
  )
  val creditDetailModelasBCC = CreditDetailModel(
    date = LocalDate.parse("2022-03-29"),
    documentDetail = documentDetailsWhichIsBCCredit,
    BalancingChargeCreditType,
    balanceDetails = Some(expectedBalancedDetails)
  )

  val taxYearFinancialDetails_PlusOneYear = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    documentDetails = List(
      documentDetailsWhichIsMfaCredit,
      DocumentDetail(taxYear = "2022", transactionId = "testTransactionId2", documentDescription = None,
        documentText = None, outstandingAmount = None, originalAmount = None, documentDate = LocalDate.of(taxYear, 3, 29)),
      documentDetailsWhichIsBCCredit
    ),
    financialDetails = List(
      FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"), transactionId = Some("testTransactionId1"),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some(LocalDate.now.plusDays(3)))))),
      FinancialDetail(taxYear = "2022", mainType = None, transactionId = Some("testTransactionId2"),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some(LocalDate.now.plusDays(5)))))),
      FinancialDetail(taxYear = "2022", mainType = Some("SA Balancing Charge Credit"), transactionId = Some("testTransactionId3"),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some(LocalDate.now.plusDays(5))))))
    )
  )

  val financialDetail: FinancialDetail = FinancialDetail(
    taxYear = "2018",
    transactionId = Some("transactionId"),
    transactionDate = Some(LocalDate.parse("2018-03-29")),
    `type` = Some("type"),
    totalAmount = Some(BigDecimal("1000.00")),
    originalAmount = Some(BigDecimal(500.00)),
    outstandingAmount = Some(BigDecimal("500.00")),
    clearedAmount = Some(BigDecimal(500.00)),
    chargeType = Some(NIC4_WALES),
    mainType = Some("SA Payment on Account 1"),
    items = Some(Seq(
      SubItem(
        subItemId = Some("1"),
        amount = Some(BigDecimal("100.00")),
        clearingDate = Some(LocalDate.parse("2021-01-31")),
        clearingReason = None,
        outgoingPaymentMethod = Some("outgoingPaymentMethod"),
        paymentReference = Some("paymentReference"),
        paymentAmount = Some(BigDecimal("2000.00")),
        dueDate = Some(LocalDate.parse("2021-01-31")),
        paymentMethod = Some("paymentMethod"),
        paymentLot = Some("paymentLot"),
        paymentLotItem = Some("paymentLotItem"),
        paymentId = Some("paymentLot-paymentLotItem")
      ),
      SubItem(
        subItemId = Some("2"),
        amount = Some(BigDecimal("200.00")),
        clearingDate = None,
        clearingReason = None,
        outgoingPaymentMethod = Some("outgoingPaymentMethod2"),
        paymentReference = None,
        paymentAmount = Some(BigDecimal("3000.00")),
        dueDate = Some(LocalDate.parse("2021-01-31")),
        paymentMethod = Some("paymentMethod2"),
        paymentLot = Some("paymentLot2"),
        paymentLotItem = None,
        paymentId = None
      )))
  )
  val cutOverCreditsAsFinancialDocumentA: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(
      DocumentDetail(
        taxYear = "2019",
        transactionId = "id",
        documentDescription = Some("documentDescription"),
        documentText = Some("documentText"),
        originalAmount = Some(-300.00),
        outstandingAmount = Some(-200.00),
        documentDate = LocalDate.of(2018, 3, 29),
        paymentLot = Some("paymentLot"),
        paymentLotItem = Some("paymentLotItem")
      )
    ),
    financialDetails = List(
      financialDetail
    )
  )

  val cutOverCreditsAsFinancialDocumentB: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(
      DocumentDetail(
        taxYear = "2022",
        transactionId = "id",
        documentDescription = Some("documentDescription"),
        documentText = Some("documentText"),
        originalAmount = Some(-300.00),
        outstandingAmount = Some(-200.00),
        documentDate = LocalDate.of(2018, 3, 29),
        paymentLot = Some("paymentLot"),
        paymentLotItem = Some("paymentLotItem")
      )
    ),
    financialDetails = List(
      financialDetail
    )
  )

}
