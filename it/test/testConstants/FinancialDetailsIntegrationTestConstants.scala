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

package testConstants

import enums.ChargeType.NIC4_WALES
import helpers.ComponentSpecBase
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import play.api.libs.json.{JsValue, Json}
import services.DateServiceInterface
import testConstants.BaseIntegrationTestConstants.{testErrorMessage, testErrorNotFoundStatus, testErrorStatus}
import testConstants.IncomeSourceIntegrationTestConstants.{id1040000123, noDunningLock, noInterestLock}

import java.time.LocalDate

object FinancialDetailsIntegrationTestConstants  extends ComponentSpecBase {

  val currentDate: LocalDate = LocalDate.of(2023, 4, 5)

  def documentDetailModel(taxYear: Int = 2018,
                          documentDescription: Option[String] = Some("ITSA- POA 1"),
                          outstandingAmount: BigDecimal = 1400.00,
                          originalAmount: BigDecimal = 1400.00): DocumentDetail =
    DocumentDetail(
      taxYear = taxYear,
      transactionId = "1040000123",
      documentDescription,
      documentText = Some("documentText"),
      outstandingAmount = outstandingAmount,
      originalAmount = originalAmount,
      documentDate = LocalDate.of(2018, 3, 29),
      interestOutstandingAmount = Some(100),
      interestRate = Some(100),
      interestFromDate = Some(LocalDate.of(2018, 3, 29)),
      interestEndDate = Some(LocalDate.of(2018, 3, 29)),
      accruingInterestAmount = Some(100),
      paymentLotItem = Some("paymentLotItem"),
      paymentLot = Some("paymentLot")
    )

  def financialDetail(taxYear: Int = 2018, mainType: Option[String] = Some("ITSA- POA 1"), mainTransaction: Option[String] = None): FinancialDetail = FinancialDetail(
    taxYear = taxYear.toString,
    mainType = mainType,
    mainTransaction = mainTransaction,
    transactionId = Some("transactionId"),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    outstandingAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    items =
      Some(Seq(
        SubItem(
          dueDate = Some(LocalDate.of(2019, 5, 15)),
          subItemId = Some("1"),
          amount = Some(100),
          clearingDate = Some(LocalDate.parse("2020-08-16")),
          clearingReason = Some("clearingReason"),
          outgoingPaymentMethod = Some("outgoingPaymentMethod"),
          paymentReference = Some("paymentReference"),
          paymentAmount = Some(100),
          paymentMethod = Some("paymentMethod"),
          paymentLot = Some("paymentLot"),
          paymentLotItem = Some("paymentLotItem"),
          paymentId = Some("paymentId")
        )
      ))
  )


  def documentDetailWithDueDateModel(taxYear: Int = 2018,
                                     documentDescription: Option[String] = Some("ITSA- POA 1"),
                                     outstandingAmount: BigDecimal = 1400.00,
                                     originalAmount: BigDecimal = 1400.00,
                                     dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)))
                                    (implicit dateService: DateServiceInterface): DocumentDetailWithDueDate =
    DocumentDetailWithDueDate(documentDetailModel(taxYear, documentDescription, outstandingAmount, originalAmount), dueDate)

  val fullDocumentDetailModel: DocumentDetail = documentDetailModel()
  val fullFinancialDetailModel: FinancialDetail = financialDetail()

  def fullDocumentDetailWithDueDateModel(implicit dateService: DateServiceInterface): DocumentDetailWithDueDate = DocumentDetailWithDueDate(fullDocumentDetailModel, Some(LocalDate.of(2019, 5, 15)))

  def financialDetailsModel(taxYear: Int = 2018, outstandingAmount: BigDecimal = 1400.0): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(documentDetailModel(taxYear, outstandingAmount = outstandingAmount)),
      financialDetails = List(financialDetail(taxYear))
    )

  def documentDetailWithDueDateFinancialDetailListModel(taxYear: Int = 2018,
                                                        outstandingAmount: BigDecimal = -1400.0,
                                                        dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                                                        originalAmount: BigDecimal = 1400.00,
                                                        mainType: Option[String] = Some("SA Payment on Account 1"),
                                                        mainTransaction: Option[String] = Some("4920")
                                                       ):
  (DocumentDetailWithDueDate, FinancialDetail) = {
    (documentDetailWithDueDateModel(
      taxYear = taxYear,
      outstandingAmount = outstandingAmount,
      dueDate = dueDate,
      originalAmount = originalAmount),
      financialDetail(mainType = mainType, mainTransaction = mainTransaction))
  }

  def financialDetailModelPartial(taxYear: Int = 2018,
                                  mainType: String = "SA Payment on Account 1",
                                  chargeType: String = NIC4_WALES,
                                  originalAmount: BigDecimal = 100,
                                  dunningLock: Option[String] = None,
                                  interestLock: Option[String] = None,
                                  accruedInterest: Option[BigDecimal] = None,
                                  additionalSubItems: Seq[SubItem] = Seq()): FinancialDetail = FinancialDetail.apply(
    taxYear = taxYear.toString,
    mainType = Some(mainType),
    transactionId = Some(id1040000123),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(originalAmount),
    outstandingAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(chargeType),
    accruedInterest = accruedInterest,
    items =
      Some(Seq(
        SubItem(
          dueDate = Some(LocalDate.of(2019, 5, 15)),
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = dunningLock,
          interestLock = interestLock,
          clearingDate = Some(LocalDate.parse("2019-07-23")),
          clearingReason = Some("clearingReason")
        )
      ) ++ additionalSubItems)
  )

  def testFinancialDetailsModel(documentDescription: List[Option[String]],
                                mainType: List[Option[String]],
                                mainTransaction: List[Option[String]],
                                transactionIds: List[Option[String]],
                                transactionDate: Option[LocalDate],
                                `type`: Option[String],
                                totalAmount: Option[BigDecimal],
                                originalAmount: Option[BigDecimal],
                                clearedAmount: Option[BigDecimal],
                                chargeType: Option[String],
                                dueDate: List[Option[LocalDate]],
                                dunningLock: List[String] = noDunningLock,
                                interestLock: List[String] = noInterestLock,
                                subItemId: Option[String],
                                amount: Option[BigDecimal],
                                clearingDate: Option[LocalDate],
                                clearingReason: Option[String],
                                outgoingPaymentMethod: Option[String],
                                paymentReference: Option[String],
                                paymentAmount: Option[BigDecimal],
                                paymentMethod: Option[String],
                                paymentLot: Option[String],
                                paymentLotItem: Option[String],
                                paymentId: Option[String],
                                outstandingAmount: List[BigDecimal],
                                taxYear: String,
                                accruingInterestAmount: List[Option[BigDecimal]] = List(Some(100), Some(100))
                               ): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, transactionIds(0).get, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId1"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), accruingInterestAmount(0), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate.head),
        DocumentDetail(taxYear.toInt, transactionIds(1).get, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId2"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), accruingInterestAmount(1), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, transactionIds(0), Some(LocalDate.parse("2020-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate.head, dunningLock = Some(dunningLock.head), interestLock = Some(interestLock.head))))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), transactionIds(1), Some(LocalDate.parse("2020-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate(1), dunningLock = Some(dunningLock(1)), interestLock = Some(interestLock(1))))))
      )
    )


  def testFinancialDetailsModelWithChargesOfSameType(documentDescription: List[Option[String]],
                                                     mainType: List[Option[String]],
                                                     mainTransaction: List[Option[String]],
                                                     transactionIds: List[Option[String]],
                                                     transactionDate: Option[LocalDate],
                                                     `type`: Option[String],
                                                     totalAmount: Option[BigDecimal],
                                                     originalAmount: Option[BigDecimal],
                                                     clearedAmount: Option[BigDecimal],
                                                     chargeType: Option[String],
                                                     dueDate: List[Option[LocalDate]],
                                                     subItemId: Option[String],
                                                     amount: Option[BigDecimal],
                                                     clearingDate: Option[LocalDate],
                                                     clearingReason: Option[String],
                                                     outgoingPaymentMethod: Option[String],
                                                     paymentReference: Option[String],
                                                     paymentAmount: Option[BigDecimal],
                                                     paymentMethod: Option[String],
                                                     paymentLot: Option[String],
                                                     paymentLotItem: Option[String],
                                                     paymentId: Option[String],
                                                     outstandingAmount: List[BigDecimal],
                                                     taxYear: String): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, transactionIds(0).get, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId1"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), Some(100), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate.head),
        DocumentDetail(taxYear.toInt, transactionIds(1).get, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId2"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), Some(100), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, transactionIds(0), Some(LocalDate.parse("2020-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), transactionIds(1), Some(LocalDate.parse("2020-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate(1)))))
      )
    )

  def outstandingChargesModel(dueDate: LocalDate): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456789012345.67, 1234), OutstandingChargeModel("ACI", None, 12.67, 1234)))

  def outstandingChargesEmptyBCDModel(dueDate: LocalDate): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("LATE", Some(dueDate), 123456789012345.67, 1234)))

  val outstandingChargesEmptyBCDModel: OutstandingChargesModel = outstandingChargesEmptyBCDModel(currentDate.plusDays(30))

  val outstandingChargesDueInMoreThan30Days: OutstandingChargesModel = outstandingChargesModel(currentDate.plusDays(35))

  val outstandingChargesOverdueData: OutstandingChargesModel = outstandingChargesModel(currentDate.minusYears(1).minusMonths(1))

  val outstandingChargesDueIn30Days: OutstandingChargesModel = outstandingChargesModel(currentDate.plusDays(30))

  val financialDetailsDueInMoreThan30Days: FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainTransaction = List(None, None),
    transactionIds = List(Some("transId1"), Some("transId2")),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    dueDate = List(Some(currentDate.plusDays(45)), Some(currentDate.plusDays(50))),
    noDunningLock,
    noInterestLock,
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some(LocalDate.parse("2020-08-16")),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    outstandingAmount = List(50, 75),
    taxYear = currentDate.getYear.toString
  )

  val financialDetailsDueIn30Days: FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainTransaction = List(None, None),
    transactionIds = List(Some("transId1"), Some("transId2")),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    dueDate = List(Some(currentDate), Some(currentDate)),
    noDunningLock,
    noInterestLock,
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some(LocalDate.parse("2020-08-16")),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    outstandingAmount = List(2000, 2000),
    taxYear = currentDate.getYear.toString,
    accruingInterestAmount = List(None, None)
  )

  def financialDetailsOverdueData(dunningLock: List[String] = noDunningLock, interestLock: List[String] = noInterestLock): FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainTransaction = List(None, None),
    transactionIds = List(Some("transId1"), Some("transId2")),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    dueDate = List(Some(currentDate.minusDays(15)), Some(currentDate.minusDays(15))),
    dunningLock,
    interestLock,
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some(LocalDate.parse("2020-08-16")),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    outstandingAmount = List(2000, 2000),
    taxYear = currentDate.getYear.toString
  )

  val financialDetailsWithMixedData1: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainTransaction = List(None, None),
    transactionIds = List(Some("transId1"), Some("transId2")),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    dueDate = List(Some(currentDate.plusDays(35)), Some(currentDate.minusDays(1))),
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some(LocalDate.parse("2020-08-16")),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    outstandingAmount = List(50, 75),
    taxYear = currentDate.getYear.toString
  )

  val financialDetailsWithMixedData2: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainTransaction = List(None, None),
    transactionIds = List(Some("transId1"), Some("transId2")),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    dueDate = List(Some(currentDate.plusDays(30)), Some(currentDate.minusDays(1))),
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some(LocalDate.parse("2020-08-16")),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    outstandingAmount = List(25, 50),
    taxYear = currentDate.getYear.toString
  )

  val financialDetailsDueIn30DaysWithAZeroOutstandingAmount: FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainTransaction = List(None, None),
    transactionIds = List(Some("transId1"), Some("transId2")),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    dueDate = List(Some(currentDate.plusDays(1)), Some(currentDate)),
    noDunningLock,
    noInterestLock,
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some(LocalDate.parse("2020-08-16")),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    outstandingAmount = List(100, 0),
    taxYear = currentDate.getYear.toString
  )

  val financialDetailsWithMFADebits: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(
        taxYear = currentDate.getYear,
        transactionId = "testMFA1",
        documentDescription = Some("ITSA PAYE Charge"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 1234.00,
        outstandingAmount = 0,
        interestOutstandingAmount = None,
        interestEndDate = None,
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23))
      ),
      DocumentDetail(
        taxYear = currentDate.getYear,
        transactionId = "testMFA2",
        documentDescription = Some("ITSA Calc Error Correction"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 2234.00,
        outstandingAmount = 0,
        interestOutstandingAmount = None,
        interestEndDate = None,
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 22))
      )),
    financialDetails = List(
      FinancialDetail(
        taxYear = currentDate.getYear.toString,
        transactionId = Some("testMFA1"),
        mainType = Some("ITSA PAYE Charge"),
        mainTransaction = Some("4000"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)), amount = Some(12), transactionId = Some("testMFA1"))))
      ),
      FinancialDetail(
        taxYear = currentDate.getYear.toString,
        transactionId = Some("testMFA2"),
        mainType = Some("ITSA Calc Error Correction"),
        mainTransaction = Some("4001"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 22)), amount = Some(12), transactionId = Some("testMFA2"))))
      )
    )
  )

  val staticDateOutstandingChargesOverdueData: OutstandingChargesModel = outstandingChargesModel(LocalDate.parse("2022-01-01"))


  val whatYouOweOutstandingChargesOnly: WhatYouOweChargesList = WhatYouOweChargesList(balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    outstandingChargesModel = Some(outstandingChargesOverdueData))

  val whatYouOweEmptyMFA: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.0, 2.0, 3.0, None, None, None, None, None), List(), Some(OutstandingChargesModel(List())), None)

  val whatYouOweNoChargeList: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None, None, None), List.empty)

  val whatYouOweFinancialDetailsEmptyBCDCharge: WhatYouOweChargesList = WhatYouOweChargesList(balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    outstandingChargesModel = Some(outstandingChargesEmptyBCDModel))

  val testInvalidFinancialDetailsJson: JsValue = Json.obj(
    "amount" -> "invalidAmount",
    "payMethod" -> "Payment by Card",
    "valDate" -> "2019-05-27"
  )

  val testFinancialDetailsErrorModelParsing: FinancialDetailsErrorModel = FinancialDetailsErrorModel(
    testErrorStatus, "Json Validation Error. Parsing FinancialDetails Data Response")

  val testFinancialDetailsErrorModel: FinancialDetailsErrorModel = FinancialDetailsErrorModel(testErrorStatus, testErrorMessage)

  val testFinancialDetailsNotFoundErrorModel: FinancialDetailsErrorModel = FinancialDetailsErrorModel(testErrorNotFoundStatus, testErrorMessage)

}
