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

package returns.testConstants

import common.services.DateService
import common.testConstants.BaseTestConstants.{app, chargeReference, testErrorMessage, testErrorStatus}
import play.api.libs.json.{JsValue, Json}
import returns.models.*
import shared.enums.CodingOutType.*

import java.time.LocalDate


object FinancialDetailsTestConstants {

  val fixedDate: LocalDate = LocalDate.of(2023, 12, 15)

  implicit val dateService: DateService = app.injector.instanceOf[DateService]

  val id1040000123 = "1040000123"
  val id1040000124 = "1040000124"
  val id1040000125 = "1040000125"
  val id1040000126 = "1040000126"
  val id1040000127 = "1040000127"
  val codingout = "CODINGOUT01"



  val userNoPoaDetails: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List.empty,
    financialDetails = List.empty,
  )

  val userPOADetails2024: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(2024), genericDocumentDetailPOA2(2024)),
    financialDetails = List(genericFinancialDetailPOA1(2023, 150.00), genericFinancialDetailPOA2(2024, 250.00)),
  )

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

  def financialDetailsErrorModel(errorCode: Int = 404): FinancialDetailsErrorModel = FinancialDetailsErrorModel(errorCode, "There was an error...")

  def genericUserPoaDetails(taxYearEnd: Int, outstandingAmount: BigDecimal): FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(taxYearEnd, outstandingAmount = outstandingAmount), genericDocumentDetailPOA2(taxYearEnd, outstandingAmount = outstandingAmount)),
    financialDetails = List.empty,
  )

  def POA2MarkedAsCredit(taxYearEnd: Int, outstandingAmount: BigDecimal = 150.00) = DocumentDetail(
    taxYear = taxYearEnd,
    transactionId = "DOCID01",
    documentDescription = Some("ITSA- POA 2"),
    documentText = None,
    outstandingAmount = outstandingAmount,
    originalAmount = -150.00,
    documentDueDate = Some(LocalDate.of(taxYearEnd, 1, 31)),
    documentDate = LocalDate.of(taxYearEnd, 3, 29),
    interestOutstandingAmount = Some(150),
    interestRate = None,
    interestFromDate = None,
    interestEndDate = None,
    accruingInterestAmount = Some(150),
    latePaymentInterestId = None
  )

  def POA1MarkedAsCredit(taxYearEnd: Int, outstandingAmount: BigDecimal = 150.00) = DocumentDetail(
    taxYear = taxYearEnd,
    transactionId = "DOCID01",
    documentDescription = Some("ITSA- POA 1"),
    documentText = None,
    outstandingAmount = outstandingAmount,
    originalAmount = -150.00,
    documentDueDate = Some(LocalDate.of(taxYearEnd, 1, 31)),
    documentDate = LocalDate.of(taxYearEnd, 3, 29),
    interestOutstandingAmount = Some(150),
    interestRate = None,
    interestFromDate = None,
    interestEndDate = None,
    accruingInterestAmount = Some(150),
    latePaymentInterestId = None
  )

  def genericUserPoaDetailsWithPoaCredits(taxYearEnd: Int, outstandingAmount: BigDecimal): FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(genericDocumentDetailPOA1(taxYearEnd, outstandingAmount = outstandingAmount), genericDocumentDetailPOA2(taxYearEnd, outstandingAmount = outstandingAmount),
      POA1MarkedAsCredit(taxYearEnd, outstandingAmount), POA2MarkedAsCredit(taxYearEnd, outstandingAmount)),
    financialDetails = List.empty,
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
    documentDescription = Some("ITSA- POA 2"),
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

  def documentDetailModel(taxYear: Int = 2018,
                          documentDescription: Option[String] = Some("ITSA- POA 1"),
                          documentText: Option[String] = Some("documentText"),
                          outstandingAmount: BigDecimal = 1400.00,
                          originalAmount: BigDecimal = 1400.00,
                          documentDate: LocalDate = LocalDate.of(2018, 3, 29),
                          documentDueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                          paymentLotItem: Option[String] = Some("paymentLotItem"),
                          paymentLot: Option[String] = Some("paymentLot"),
                          accruingInterestAmount: Option[BigDecimal] = Some(100),
                          interestOutstandingAmount: Option[BigDecimal] = Some(80),
                          latePaymentInterestAmount: Option[BigDecimal] = Some(100),
                          transactionId: String = id1040000123,
                          lpiWithDunningLock: Option[BigDecimal] = Some(100),
                          effectiveDateOfPayment: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                          amountCodedOut: Option[BigDecimal] = None): DocumentDetail =
    DocumentDetail(
      taxYear = taxYear,
      transactionId = transactionId,
      documentDescription,
      documentText = documentText,
      outstandingAmount = outstandingAmount,
      originalAmount = originalAmount,
      documentDate = documentDate,
      documentDueDate = documentDueDate,
      interestOutstandingAmount = interestOutstandingAmount,
      interestRate = Some(100),
      interestFromDate = Some(LocalDate.of(2018, 3, 29)),
      interestEndDate = Some(LocalDate.of(2018, 6, 15)),
      accruingInterestAmount = accruingInterestAmount,
      latePaymentInterestAmount = latePaymentInterestAmount,
      lpiWithDunningLock = lpiWithDunningLock,
      paymentLotItem = paymentLotItem,
      paymentLot = paymentLot,
      effectiveDateOfPayment = effectiveDateOfPayment,
      amountCodedOut = amountCodedOut
    )

  def financialDetail(taxYear: Int = 2018,
                      mainType: String = "SA Payment on Account 1",
                      mainTransaction: String = "4920",
                      originalAmount: BigDecimal = 100,
                      chargeRef: Option[String] = Some(chargeReference),
                      dunningLock: Option[String] = None,
                      interestLock: Option[String] = None,
                      accruedInterest: Option[BigDecimal] = None,
                      additionalSubItems: Seq[SubItem] = Seq(),
                      dueDateValue: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                      transactionId: Option[String] = Some(id1040000123),
                      codedOutStatus: Option[String] = None
                     ): FinancialDetail = FinancialDetail.apply(
    taxYear = taxYear.toString,
    mainType = Some(mainType),
    mainTransaction = Some(mainTransaction),
    transactionId = transactionId,
    transactionDate = Some(LocalDate.parse("2022-08-16")),
    chargeReference = chargeRef,
    `type` = Some("type"),
    originalAmount = Some(originalAmount),
    outstandingAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = None,
    accruedInterest = accruedInterest,
    items =
      Some(Seq(
        SubItem(
          dueDate = dueDateValue,
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = dunningLock,
          interestLock = interestLock,
          clearingDate = Some(LocalDate.parse("2019-07-23")),
          clearingReason = Some("clearingReason"),
          codedOutStatus = codedOutStatus
        )
      ) ++ additionalSubItems)
  )


  def documentDetailWithDueDateModel(taxYear: Int = 2018,
                                     documentDescription: Option[String] = Some("ITSA- POA 1"),
                                     documentText: Option[String] = Some("documentText"),
                                     outstandingAmount: BigDecimal = 1400.00,
                                     originalAmount: BigDecimal = 1400.00,
                                     transactionId: String = id1040000123,
                                     accruingInterestAmount: Option[BigDecimal] = Some(100),
                                     paymentLot: Option[String] = Some("paymentLot"),
                                     paymentLotItem: Option[String] = None,
                                     dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                                     isMFADebit: Boolean = false): DocumentDetailWithDueDate =
    DocumentDetailWithDueDate(documentDetailModel(taxYear = taxYear, documentDescription = documentDescription, outstandingAmount = outstandingAmount,
      originalAmount = originalAmount, documentText = documentText, transactionId = transactionId, paymentLot = paymentLot, paymentLotItem = paymentLotItem,
      accruingInterestAmount = accruingInterestAmount, documentDueDate = dueDate), dueDate = dueDate, isMFADebit = isMFADebit)

  val balanceDetails: BalanceDetails = BalanceDetails(
    balanceDueWithin30Days = 1.00,
    overDueAmount = 2.00,
    balanceNotDuein30Days = 0.00,
    totalBalance = 3.00,
    totalCreditAvailableForRepayment = Some(100.00),
    None,
    None,
    totalCredit = Some(200.00),
    None,
    None,
    None
  )

  val documentDetailClass2Nic: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_CLASS2_NICS), paymentLot = None, accruingInterestAmount = None)
  val documentDetailPaye: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_ACCEPTED), paymentLot = None, accruingInterestAmount = None)
  val fullDocumentDetailModel: DocumentDetail = documentDetailModel()

  def financialDetails(balanceDetails: BalanceDetails = balanceDetails,
                       documentDetails: DocumentDetail = documentDetailModel(),
                       financialDetails: FinancialDetail = financialDetail()): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(documentDetails),
      financialDetails = List(financialDetails)
    )

  def financialDetailsModel(
                             taxYear: Int = 2018,
                             mainTransaction:String = "4920",
                             outstandingAmount: BigDecimal = 1400.0,
                             dunningLock: Option[String] = None,
                             lpiWithDunningLock: Option[BigDecimal] = Some(100),
                             dueDateValue: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                             accruingInterestAmount: Option[BigDecimal] = Some(100),
                             amountCodedOut: Option[BigDecimal] = None
                           ): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(documentDetailModel(taxYear, outstandingAmount = outstandingAmount, documentDueDate = dueDateValue, paymentLot = None,
        paymentLotItem = None, lpiWithDunningLock = lpiWithDunningLock, accruingInterestAmount = accruingInterestAmount, amountCodedOut = amountCodedOut)),
      financialDetails = List(financialDetail(taxYear, dunningLock = dunningLock, dueDateValue = dueDateValue, mainTransaction = mainTransaction)
      )
    )

  val testFinancialDetailsErrorModel: FinancialDetailsErrorModel = FinancialDetailsErrorModel(testErrorStatus, testErrorMessage)

  val penaltiesDocumentDetails: List[DocumentDetail] = List(
    documentDetailModel( transactionId = "LSP",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      latePaymentInterestAmount = None, interestOutstandingAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2100, 5, 15))),
    documentDetailModel(transactionId = "LPP1",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      latePaymentInterestAmount = None, interestOutstandingAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2100, 5, 15))),
    documentDetailModel(transactionId = "LPP2",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      latePaymentInterestAmount = None, interestOutstandingAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2100, 5, 15)))
  )

  val MFADebitsDocumentDetails: List[DocumentDetail] = List(
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFADEBIT01",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      latePaymentInterestAmount = None, interestOutstandingAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15))),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFADEBIT02",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      latePaymentInterestAmount = None, interestOutstandingAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15))),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFADEBIT03",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      latePaymentInterestAmount = None, interestOutstandingAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15)))
  )

  val MFADebitsFinancialDetails: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, Some(6.00), Some(3.00), Some(7.00), Some(1.00), Some(2.00), Some(4.00), None),
    documentDetails = MFADebitsDocumentDetails,
    financialDetails = List(
      FinancialDetail(taxYear = "2018", mainType = Some("ITSA PAYE Charge"), mainTransaction = Some("4000"), transactionId = Some("MFADEBIT01"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainType = Some("ITSA Calc Error Correction"), mainTransaction = Some("4001"), transactionId = Some("MFADEBIT02"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainType = Some("ITSA Manual Penalty Pre CY-4"), mainTransaction = Some("4002"), transactionId = Some("MFADEBIT03"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15))))))
    )
  )

  val ReviewAndReconcileDocumentDetailsNotDue: List[DocumentDetail] = List(
    documentDetailModel(documentDescription = Some("SA POA 1 Reconciliation Debit"), transactionId = "RARDEBIT01",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      latePaymentInterestAmount = None, interestOutstandingAmount = None, effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2100, 5, 15))),
    documentDetailModel(documentDescription = Some("SA POA 2 Reconciliation Debit"), transactionId = "RARDEBIT02",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00, latePaymentInterestAmount = None, interestOutstandingAmount = None,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2100, 5, 15)))
  )


  val financialDetailsWithReviewAndReconcileDebits: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, Some(6.00), Some(3.00), Some(7.00), Some(1.00), Some(2.00), Some(4.00), None),
    documentDetails = ReviewAndReconcileDocumentDetailsNotDue,
    financialDetails = List(
      FinancialDetail(taxYear = "2018", mainType = Some("SA POA 1 Reconciliation Debit"), mainTransaction = Some("4911"), transactionId = Some("RARDEBIT01"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainType = Some("SA POA 2 Reconciliation Debit"), mainTransaction = Some("4913"), transactionId = Some("RARDEBIT02"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15))))))
    )
  )


  val ReviewAndReconcileDocumentDetailsAccruingInterest: List[DocumentDetail] = List(
    documentDetailModel(documentDescription = Some("SA POA 1 Reconciliation Debit"), transactionId = "RARDEBIT01",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = Some(100),
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15))),
    documentDetailModel(documentDescription = Some("SA POA 2 Reconciliation Debit"), transactionId = "RARDEBIT02",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = Some(200),
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15)))
  )

  val financialDetailsWithAccruingInterestReviewAndReconcileDebits: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, Some(6.00), Some(3.00), Some(7.00), Some(1.00), Some(2.00), Some(4.00), None),
    documentDetails = ReviewAndReconcileDocumentDetailsAccruingInterest,
    financialDetails = List(
      FinancialDetail(taxYear = "2018", mainType = Some("SA POA 1 Reconciliation Debit"), mainTransaction = Some("4911"), transactionId = Some("RARDEBIT01"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainType = Some("SA POA 2 Reconciliation Debit"), mainTransaction = Some("4913"), transactionId = Some("RARDEBIT02"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15))))))
    )
  )

  val financialDetailsWithAllThreePenalties: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, Some(6.00), Some(3.00), Some(7.00), Some(1.00), Some(2.00), Some(4.00), None),
    documentDetails = penaltiesDocumentDetails,
    financialDetails = List(
      FinancialDetail(taxYear = "2018", mainTransaction = Some("4027"), transactionId = Some("LSP"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainTransaction = Some("4028"), transactionId = Some("LPP1"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainTransaction = Some("4029"), transactionId = Some("LPP2"), chargeReference = Some("chargeRef123"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15))))))
    )
  )

  val financialDetailsWithLPP2NoChargeRef: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, Some(6.00), Some(3.00), Some(7.00), Some(1.00), Some(2.00), Some(4.00), None),
    documentDetails = penaltiesDocumentDetails,
    financialDetails = List(
      FinancialDetail(taxYear = "2018", mainTransaction = Some("4029"), transactionId = Some("LPP2"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15))))))
    )
  )

  val MFADebitsDocumentsTransactionIds: List[String] = MFADebitsFinancialDetails.documentDetails.map(_.transactionId)

  val testValidFinancialDetailsModel: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(taxYear = 2019,
        transactionId = id1040000123,
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        outstandingAmount = 10.33,
        originalAmount = 10.33,
        documentDate = LocalDate.of(2018, 3, 29),
        interestOutstandingAmount = Some(100),
        interestRate = Some(100),
        latePaymentInterestId = Some("latePaymentInterestId1"),
        interestFromDate = Some(LocalDate.of(2018, 3, 29)),
        interestEndDate = Some(LocalDate.of(2018, 3, 29)),
        accruingInterestAmount = Some(100),
        lpiWithDunningLock = Some(100),
        paymentLotItem = Some("paymentLotItem"),
        paymentLot = Some("paymentLot")),
      DocumentDetail(taxYear = 2020,
        transactionId = id1040000124,
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        outstandingAmount = 10.34,
        originalAmount = 10.34,
        documentDate = LocalDate.of(2018, 3, 29),
        interestOutstandingAmount = Some(100),
        interestRate = Some(100),
        latePaymentInterestId = Some("latePaymentInterestId2"),
        interestFromDate = Some(LocalDate.of(2018, 3, 29)),
        interestEndDate = Some(LocalDate.of(2018, 3, 29)),
        accruingInterestAmount = Some(100),
        lpiWithDunningLock = Some(100),
        paymentLotItem = Some("paymentLotItem"),
        paymentLot = Some("paymentLot"))
    ),
    financialDetails = List(
      FinancialDetail(
        taxYear = "2019",
        mainType = Some("SA Balancing Charge"),
        mainTransaction = Some("4910"),
        transactionId = Some(id1040000123),
        transactionDate = Some(LocalDate.parse("2020-08-16")),
        `type` = Some("type"),
        totalAmount = Some(100),
        originalAmount = Some(100),
        outstandingAmount = Some(100),
        clearedAmount = Some(100),
        chargeType = Some("NIC4 Wales"),
        accruedInterest = Some(100),
        items = Some(Seq(SubItem(
          dueDate = Some(LocalDate.parse("2019-05-15")),
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = Some("Stand over order"),
          interestLock = Some("interestLock"),
          clearingDate = Some(LocalDate.parse("2020-08-16")),
          clearingReason = Some("clearingReason"),
          clearingSAPDocument = None,
          outgoingPaymentMethod = Some("outgoingPaymentMethod"),
          paymentReference = Some("paymentReference"),
          paymentAmount = Some(100),
          paymentMethod = Some("paymentMethod"),
          paymentLot = Some("paymentLot"),
          paymentLotItem = Some("paymentLotItem"),
          paymentId = Some("paymentLot-paymentLotItem"),
          codedOutStatus = Some("I"))))),
      FinancialDetail(
        taxYear = "2020",
        mainType = Some("SA Balancing Charge"),
        mainTransaction = Some("4910"),
        transactionId = Some(id1040000124),
        transactionDate = Some(LocalDate.parse("2020-08-16")),
        `type` = Some("type"),
        totalAmount = Some(100),
        originalAmount = Some(100),
        outstandingAmount = Some(100),
        clearedAmount = Some(100),
        chargeType = Some("NIC4 Wales"),
        accruedInterest = Some(100),
        items = Some(Seq(SubItem(
          dueDate = Some(LocalDate.parse("2019-05-15")),
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = None,
          interestLock = None,
          clearingDate = Some(LocalDate.parse("2020-08-16")),
          clearingReason = Some("clearingReason"),
          clearingSAPDocument = None,
          outgoingPaymentMethod = Some("outgoingPaymentMethod"),
          paymentReference = Some("paymentReference"),
          paymentAmount = Some(100),
          paymentMethod = Some("paymentMethod"),
          paymentLot = Some("paymentLot"),
          paymentLotItem = Some("paymentLotItem"),
          paymentId = Some("paymentLot-paymentLotItem")))))
    )
  )

  val testValidFinancialDetailsModelJsonReads: JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> 2019,
        "transactionId" -> id1040000123,
        "documentDescription" -> "TRM New Charge",
        "documentText" -> "documentText",
        "outstandingAmount" -> 10.33,
        "originalAmount" -> 10.33,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> 100,
        "interestRate" -> 100,
        "latePaymentInterestId" -> "latePaymentInterestId1",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> 100,
        "lpiWithDunningLock" -> 100,
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      ),
      Json.obj(
        "taxYear" -> 2020,
        "transactionId" -> id1040000124,
        "documentDescription" -> "TRM New Charge",
        "documentText" -> "documentText",
        "outstandingAmount" -> 10.34,
        "originalAmount" -> 10.34,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> 100,
        "interestRate" -> 100,
        "latePaymentInterestId" -> "latePaymentInterestId2",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> 100,
        "lpiWithDunningLock" -> 100,
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> id1040000123,
        "transactionDate" -> "2020-08-16",
        "type" -> "type",
        "totalAmount" -> 100,
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> "NIC4 Wales",
        "accruedInterest" -> 100,
        "items" -> Json.arr(
          Json.obj(
            "dueDate" -> LocalDate.parse("2019-05-15"),
            "subItemId" -> "1",
            "amount" -> 100,
            "dunningLock" -> "Stand over order",
            "interestLock" -> "interestLock",
            "clearingDate" -> "2020-08-16",
            "clearingReason" -> "clearingReason",
            "outgoingPaymentMethod" -> "outgoingPaymentMethod",
            "paymentReference" -> "paymentReference",
            "paymentAmount" -> 100,
            "paymentMethod" -> "paymentMethod",
            "paymentLot" -> "paymentLot",
            "paymentLotItem" -> "paymentLotItem",
            "paymentId" -> "paymentLot-paymentLotItem",
            "codedOutStatus" -> "I"
          )
        )
      ),
      Json.obj(
        "taxYear" -> "2020",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> id1040000124,
        "transactionDate" -> "2020-08-16",
        "type" -> "type",
        "totalAmount" -> 100,
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> "NIC4 Wales",
        "accruedInterest" -> 100,
        "items" -> Json.arr(
          Json.obj("dueDate" -> LocalDate.parse("2019-05-15"),
            "subItemId" -> "1",
            "amount" -> 100,
            "clearingDate" -> "2020-08-16",
            "clearingReason" -> "clearingReason",
            "outgoingPaymentMethod" -> "outgoingPaymentMethod",
            "paymentReference" -> "paymentReference",
            "paymentAmount" -> 100,
            "paymentMethod" -> "paymentMethod",
            "paymentLot" -> "paymentLot",
            "paymentLotItem" -> "paymentLotItem",
            "paymentId" -> "paymentLot-paymentLotItem"
          )
        )
      )
    )
  )

  val testInvalidFinancialDetailsJson: JsValue = Json.obj(
    "amount" -> "invalidAmount",
    "payMethod" -> "Payment by Card",
    "valDate" -> "2019-05-27"
  )

  val testFinancialDetailsErrorModelParsing: FinancialDetailsErrorModel = FinancialDetailsErrorModel(
    testErrorStatus, "Json Validation Error. Parsing FinancialDetails Data Response")

}