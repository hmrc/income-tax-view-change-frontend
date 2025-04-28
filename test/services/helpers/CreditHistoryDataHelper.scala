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
import models.creditDetailModel._
import models.financialDetails._
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import testConstants.BusinessDetailsTestConstants.fixedDate

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
    taxYear = 2022,
    transactionId = "testTransactionId1",
    documentDescription = Some("ITSA Cutover Credits"),
    documentText = None, outstandingAmount = 100.00, originalAmount = -120.00,
    documentDate = LocalDate.of(taxYear, 3, 29)
  )
  val documentDetailsWhichIsMfaCredit = DocumentDetail(taxYear = 2022,
    transactionId = "testTransactionId2",
    documentDescription = Some("TRM New Charge"), documentText = None,
    outstandingAmount = -150.00, originalAmount = -150.00,
    documentDate = LocalDate.of(2022, 3, 29))

  val documentDetailsWhichIsBCCredit = DocumentDetail(taxYear = 2022,
    transactionId = "testTransactionId3",
    documentDescription = Some("ITSA- Bal Charge"), documentText = None,
    outstandingAmount = 120.00, originalAmount = -150.00,
    documentDate = LocalDate.of(taxYear, 3, 29))


  val documentDetailsWhichIsRepaymentInterestCredit = DocumentDetail(taxYear = 2022,
    transactionId = "testTransactionId5",
    documentDescription = Some("SA Repayment Supplement Credit"), documentText = None,
    outstandingAmount = -250.00, originalAmount = -250.00,
    documentDate = LocalDate.of(taxYear, 3, 29))


  val taxYearFinancialDetails = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(
      documentDetailsWhichIsCutOverCredit,
      DocumentDetail(2022, "testTransactionId6", None, None, 0, 0, LocalDate.of(taxYear, 3, 29)),
      DocumentDetail(2022, "testTransactionId7", None, None, 0, 0, LocalDate.of(taxYear, 3, 31))
    ),
    financialDetails = List(
      FinancialDetail(taxYear = "2022", mainType = Some("ITSA Cutover Credits"), mainTransaction = Some("6110"), transactionId = Some(documentDetailsWhichIsCutOverCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25")))))),
      FinancialDetail(taxYear = "2022", mainType = None, mainTransaction = None, transactionId = Some("testTransactionId6"),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25")))))),
      FinancialDetail(taxYear = "2022", mainType = None, mainTransaction = None, transactionId = Some("testTransactionId7"),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25"))))))
    )
  )

  val taxYearFinancialDetailsTwoYears = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(
      documentDetailsWhichIsCutOverCredit,
      DocumentDetail(2022, "testTransactionId6", None, None, 0, 0, LocalDate.of(taxYear, 3, 29)),
      DocumentDetail(2022, "testTransactionId7", None, None, 0, 0, LocalDate.of(taxYear, 3, 31)),
      documentDetailsWhichIsMfaCredit,
      DocumentDetail(taxYear = 2022, transactionId = "testTransactionId8", documentDescription = None,
        documentText = None, outstandingAmount = 0, originalAmount = 0, documentDate = LocalDate.of(taxYear, 3, 29)),
      documentDetailsWhichIsBCCredit
    ),
    financialDetails = List(
      FinancialDetail(taxYear = "2022", mainType = Some("ITSA Cutover Credits"), mainTransaction = Some("6110"), transactionId = Some(documentDetailsWhichIsCutOverCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25")))))),
      FinancialDetail(taxYear = "2022", mainType = None, mainTransaction = None, transactionId = Some("testTransactionId6"),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25")))))),
      FinancialDetail(taxYear = "2022", mainType = None, mainTransaction = None, transactionId = Some("testTransactionId7"),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25")))))),
      FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"), mainTransaction = Some("4004"), transactionId = Some(documentDetailsWhichIsMfaCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some(fixedDate.plusDays(3)))))),
      FinancialDetail(taxYear = "2022", mainType = None, mainTransaction = None, transactionId = Some("testTransactionId8"),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some(fixedDate.plusDays(5)))))),
      FinancialDetail(taxYear = "2022", mainType = Some("SA Balancing Charge Credit"), mainTransaction = Some("4905"), transactionId = Some(documentDetailsWhichIsBCCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some(fixedDate.plusDays(5))))))
    )
  )

  val taxYearFinancialDetailsAllCredits = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(
      documentDetailsWhichIsRepaymentInterestCredit
    ),
    financialDetails = List(
     FinancialDetail(taxYear = "2022",
        mainType = Some("SA Repayment Supplement Credit"), mainTransaction = Some("6020"), transactionId = Some(documentDetailsWhichIsRepaymentInterestCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = Some(BigDecimal(-500)), outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25"))))))
    )
  )

  val taxYearFinancialDetailsAllCreditsPlusOneYear = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(
      documentDetailsWhichIsCutOverCredit,
      documentDetailsWhichIsMfaCredit,
      documentDetailsWhichIsBCCredit
    ),
    financialDetails = List(
      FinancialDetail(taxYear = "2022",
        mainType = Some("ITSA Cutover Credits"), mainTransaction = Some("6110"), transactionId = Some(documentDetailsWhichIsCutOverCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = Some(BigDecimal(-100)), outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25")))))),
      FinancialDetail(taxYear = "2022",
        mainType = Some("ITSA Overpayment Relief"), mainTransaction = Some("4004"), transactionId = Some(documentDetailsWhichIsMfaCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = Some(BigDecimal(-300)), outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25")))))),
      FinancialDetail(taxYear = "2022",
        mainType = Some("SA Balancing Charge Credit"), mainTransaction = Some("4905"), transactionId = Some(documentDetailsWhichIsBCCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = Some(BigDecimal(-200)), outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25"))))))
    )
  )

  val taxYearFinancialDetailsAllCreditsTwoYears = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(
      documentDetailsWhichIsRepaymentInterestCredit,
      documentDetailsWhichIsCutOverCredit,
      documentDetailsWhichIsMfaCredit,
      documentDetailsWhichIsBCCredit
    ),
    financialDetails = List(
      FinancialDetail(taxYear = "2022",
        mainType = Some("SA Repayment Supplement Credit"), mainTransaction = Some("6020"), transactionId = Some(documentDetailsWhichIsRepaymentInterestCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = Some(BigDecimal(-500)), outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25")))))),
      FinancialDetail(taxYear = "2022",
        mainType = Some("ITSA Cutover Credits"), mainTransaction = Some("6110"), transactionId = Some(documentDetailsWhichIsCutOverCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = Some(BigDecimal(-100)), outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25")))))),
      FinancialDetail(taxYear = "2022",
        mainType = Some("ITSA Overpayment Relief"), mainTransaction = Some("4004"), transactionId = Some(documentDetailsWhichIsMfaCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = Some(BigDecimal(-300)), outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25")))))),
      FinancialDetail(taxYear = "2022",
        mainType = Some("SA Balancing Charge Credit"), mainTransaction = Some("4905"), transactionId = Some(documentDetailsWhichIsBCCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = Some(BigDecimal(-200)), outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None,
        items = Some(Seq(SubItem(Some(LocalDate.parse("2022-08-25"))))))
    )
  )


  val expectedBalancedDetails = BalanceDetails(1.0,2.0,3.0,None,None,None,None,None)

  // TODO: tidy up mainType text !!!
  val financialDetailsForCutOver = List(
    FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
      mainTransaction = Some("6110"), transactionId = Some(documentDetailsWhichIsCutOverCredit.transactionId),
      transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
      clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
    )
  )
  val financialDetailsMfa = List(
    FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
      mainTransaction = Some("4004"), transactionId = Some(documentDetailsWhichIsMfaCredit.transactionId),
      transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
      clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
    )
  )

  val financialDetailsBcc = List(
    FinancialDetail(taxYear = "2022", mainType = Some("SA Balancing Charge Credit"),
      mainTransaction = Some("4905"), transactionId = Some(documentDetailsWhichIsBCCredit.transactionId),
      transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
      clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
    )
  )

  val financialDetailsRepayment = List(
    FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
      mainTransaction = Some("6020"), transactionId = Some(documentDetailsWhichIsRepaymentInterestCredit.transactionId),
      transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
      clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
    )
  )

  val newCutOverCharge: ChargeItem = ChargeItem.fromDocumentPair(
    documentDetailsWhichIsCutOverCredit,
    financialDetailsForCutOver
  )
  val newMfaCharge: ChargeItem = ChargeItem.fromDocumentPair(
    documentDetailsWhichIsMfaCredit,
    financialDetailsMfa
  )

  val newBccCharge: ChargeItem = ChargeItem.fromDocumentPair(
    documentDetailsWhichIsBCCredit,
    financialDetailsBcc
  )

  val newRepaymentCharge: ChargeItem = ChargeItem.fromDocumentPair(
    documentDetailsWhichIsRepaymentInterestCredit,
    financialDetailsRepayment
  )
  val creditDetailModelasCutOver = CreditDetailModel(
    date = LocalDate.parse("2022-08-25"),
    charge = newCutOverCharge, //documentDetailsWhichIsCutOverCredit,
    CutOverCreditType,
    availableCredit = expectedBalancedDetails.availableCredit
  )
  val creditDetailModelasMfa = CreditDetailModel(
    date = LocalDate.parse("2022-03-29"),
    charge = newMfaCharge, // documentDetailsWhichIsMfaCredit,
    MfaCreditType,
    availableCredit = expectedBalancedDetails.availableCredit
  )
  val creditDetailModelasBCC = CreditDetailModel(
    date = LocalDate.parse("2022-03-29"),
    charge = newBccCharge, // documentDetailsWhichIsBCCredit,
    BalancingChargeCreditType,
    availableCredit = expectedBalancedDetails.availableCredit
  )

  val creditDetailModelasSetInterest = CreditDetailModel(
    date = LocalDate.parse("2022-03-29"),
    charge = newRepaymentCharge, //§  = documentDetailsWhichIsRepaymentInterestCredit,
    RepaymentInterest,
    availableCredit = expectedBalancedDetails.availableCredit
  )

  val taxYearFinancialDetails_PlusOneYear = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(
      documentDetailsWhichIsMfaCredit,
      DocumentDetail(taxYear = 2022, transactionId = "testTransactionId8", documentDescription = None,
        documentText = None, outstandingAmount = 0, originalAmount = 0, documentDate = LocalDate.of(taxYear, 3, 29)),
      documentDetailsWhichIsBCCredit
    ),
    financialDetails = List(
      FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"), mainTransaction = Some("4004"), transactionId = Some(documentDetailsWhichIsMfaCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))),
      FinancialDetail(taxYear = "2022", mainType = None, mainTransaction = None, transactionId = Some("testTransactionId8"),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some(fixedDate.plusDays(5)))))),
      FinancialDetail(taxYear = "2022", mainType = Some("SA Balancing Charge Credit"), mainTransaction = Some("4905"), transactionId = Some(documentDetailsWhichIsBCCredit.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") )))))
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
    mainTransaction = Some("4920"),
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
        paymentMethod = Some("paymentMethod")
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
        paymentId = None
      )))
  )
  val cutOverCreditsAsFinancialDocumentA: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(
      DocumentDetail(
        taxYear = 2019,
        transactionId = "id",
        documentDescription = Some("documentDescription"),
        documentText = Some("documentText"),
        originalAmount = -300.00,
        outstandingAmount = -200.00,
        documentDate = LocalDate.of(2018, 3, 29)
      )
    ),
    financialDetails = List(
      financialDetail
    )
  )

  val cutOverCreditsAsFinancialDocumentB: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(
      DocumentDetail(
        taxYear = 2022,
        transactionId = "id",
        documentDescription = Some("documentDescription"),
        documentText = Some("documentText"),
        originalAmount = -300.00,
        outstandingAmount = -200.00,
        documentDate = LocalDate.of(2018, 3, 29)
      )
    ),
    financialDetails = List(
      financialDetail
    )
  )

}
