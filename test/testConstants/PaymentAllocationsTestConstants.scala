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

package testConstants

import enums.ChargeType.{ITSA_NIC4_INTEREST_GB, NIC4_WALES}
import models.financialDetails._
import models.paymentAllocationCharges.{AllocationDetailWithClearingDate, FinancialDetailsWithDocumentDetailsModel, LatePaymentInterestPaymentAllocationDetails, PaymentAllocationViewModel}
import models.paymentAllocations.{AllocationDetail, PaymentAllocations, PaymentAllocationsError}
import play.api.libs.json.{JsValue, Json}
import testConstants.BaseTestConstants._

import java.time.LocalDate

object PaymentAllocationsTestConstants {

  val documentDetail: DocumentDetail = DocumentDetail(
    taxYear = 2018,
    transactionId = "id",
    documentDescription = Some("documentDescription"),
    documentText = Some("documentText"),
    originalAmount = -300.00,
    outstandingAmount = -200.00,
    documentDate = LocalDate.of(2018, 3, 29),
    paymentLot = Some("paymentLot"),
    effectiveDateOfPayment = Some(LocalDate.of(2021, 1, 31)),
    paymentLotItem = Some("paymentLotItem")
  )

  val documentDetailWithCredit: DocumentDetail = DocumentDetail(
    taxYear = 2018,
    transactionId = "id",
    documentDescription = Some("documentDescription"),
    documentText = Some("documentText"),
    originalAmount = -300.00,
    outstandingAmount = -200.00,
    documentDate = LocalDate.of(2018, 3, 29),
    paymentLot = None,
    paymentLotItem = None
  )

  val documentDetailNoPayment: DocumentDetail = DocumentDetail(
    taxYear = 2018,
    transactionId = "id",
    documentDescription = Some("documentDescription"),
    documentText = Some("documentText"),
    originalAmount = -300.00,
    outstandingAmount = -200.00,
    effectiveDateOfPayment = Some(LocalDate.of(2021, 1, 31)),
    documentDate = LocalDate.of(2018, 3, 29)
  )

  val documentDetailNoPaymentCredit: DocumentDetail = DocumentDetail(
    taxYear = 2018,
    transactionId = "id",
    documentDescription = Some("documentDescription"),
    documentText = Some("documentText"),
    originalAmount = -300.00,
    outstandingAmount = 0,
    effectiveDateOfPayment = Some(LocalDate.of(2021, 1, 31)),
    documentDate = LocalDate.of(2018, 3, 29)
  )

  val documentDetail2: DocumentDetail = DocumentDetail(
    taxYear = 2019,
    transactionId = "id2",
    documentDescription = Some("documentDescription2"),
    documentText = Some("documentText2"),
    originalAmount = -100.00,
    outstandingAmount = -50.00,
    documentDate = LocalDate.of(2018, 3, 29),
    paymentLot = Some("paymentLot2"),
    effectiveDateOfPayment = Some(LocalDate.of(2021, 1, 31)),
    paymentLotItem = Some("paymentLotItem2")
  )

  val documentDetail3: DocumentDetail = DocumentDetail(
    taxYear = 2022,
    transactionId = "chargeReference3",
    documentDescription = Some("documentDescription2"),
    documentText = Some("documentText2"),
    originalAmount = -300,
    outstandingAmount = 0.00,
    documentDate = LocalDate.of(2022, 4, 6),
    paymentLot = Some("paymentLot3"),
    effectiveDateOfPayment = Some(LocalDate.of(2021, 1, 31)),
    paymentLotItem = Some("paymentLotItem3")
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

  val financialDetailNoPayment: FinancialDetail = FinancialDetail(
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
        paymentMethod = Some("paymentMethod"),
        paymentId = None
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
        paymentMethod = Some("paymentMethod2")
      )))
  )

  val financialDetailNoPaymentCredit: FinancialDetail = FinancialDetail(
    taxYear = "2018",
    transactionId = Some("transactionId"),
    transactionDate = Some(LocalDate.parse("2018-03-29")),
    `type` = Some("type"),
    totalAmount = Some(BigDecimal("1000.00")),
    originalAmount = Some(BigDecimal(-500.00)),
    outstandingAmount = Some(BigDecimal("0.00")),
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
        paymentMethod = Some("paymentMethod"),
        paymentId = None
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
        paymentMethod = Some("paymentMethod2")
      )))
  )

  val financialDetail2: FinancialDetail = FinancialDetail(
    taxYear = "2019",
    transactionId = Some("transactionId2"),
    transactionDate = Some(LocalDate.parse("2021-01-31")),
    `type` = Some("type2"),
    totalAmount = Some(BigDecimal("2000.00")),
    originalAmount = Some(BigDecimal(500.00)),
    outstandingAmount = Some(BigDecimal("200.00")),
    clearedAmount = Some(BigDecimal(500.00)),
    chargeType = Some(NIC4_WALES),
    mainType = Some("SA Payment on Account 1"),
    mainTransaction = Some("4920"),
    items = Some(Seq(
      SubItem(
        subItemId = Some("2"),
        amount = Some(BigDecimal("200.00")),
        clearingDate = None,
        clearingReason = None,
        outgoingPaymentMethod = Some("outgoingPaymentMethod2"),
        paymentReference = Some("paymentReference2"),
        paymentAmount = Some(BigDecimal("3000.00")),
        dueDate = Some(LocalDate.parse("2021-01-31")),
        paymentMethod = Some("paymentMethod2"),
        paymentLot = Some("paymentLot2"),
        paymentLotItem = Some("paymentLotItem2"),
        paymentId = Some("paymentLot2-paymentLotItem2")
      )))
  )

  val financialDetail3: FinancialDetail = FinancialDetail(
    taxYear = "2022",
    transactionId = Some("transactionId3"),
    transactionDate = Some(LocalDate.parse("2022-04-06")),
    `type` = Some("type3"),
    totalAmount = Some(BigDecimal("300.00")),
    originalAmount = Some(BigDecimal(300.00)),
    outstandingAmount = Some(BigDecimal("00.00")),
    clearedAmount = Some(BigDecimal(300.00)),
    chargeType = Some("Test"),
    mainType = Some("ITSA Misc Charge"),
    mainTransaction = Some("4003"),
    items = Some(Seq(
      SubItem(
        subItemId = Some("001"),
        amount = Some(BigDecimal("300.00")),
        clearingDate = None,
        clearingReason = None,
        outgoingPaymentMethod = Some("outgoingPaymentMethod3"),
        paymentReference = Some("paymentReference3"),
        paymentAmount = Some(BigDecimal("300.00")),
        dueDate = Some(LocalDate.parse("2021-01-31")),
        paymentMethod = Some("paymentMethod3"),
        paymentLot = Some("paymentLot3"),
        paymentLotItem = Some("paymentLotItem3"),
        paymentId = Some("paymentLot3-paymentLotItem3")
      )))
  )

  val testValidPaymentAllocationsModelJson: JsValue = Json.obj(
    "amount" -> 110.10,
    "method" -> "Payment by Card",
    "transactionDate" -> "2019-05-27",
    "reference" -> "reference",
    "allocations" -> Json.arr(
      Json.obj(
        "transactionId" -> "1040000872",
        "from" -> "2019-06-27",
        "to" -> "2019-08-27",
        "chargeType" -> NIC4_WALES,
        "mainType" -> "SA Payment on Account 1",
        "amount" -> 10.10,
        "clearedAmount" -> 5.50,
        "chargeReference" -> "chargeReference1"
      ),
      Json.obj(
        "transactionId" -> "1040000873",
        "from" -> "2019-07-28",
        "to" -> "2019-09-28",
        "chargeType" -> NIC4_WALES,
        "mainType" -> "SA Payment on Account 1",
        "amount" -> 10.90,
        "clearedAmount" -> 5.90,
        "chargeReference" -> "chargeReference2"
      )
    )
  )

  val testValidPaymentAllocationsModel: PaymentAllocations =
    PaymentAllocations(
      Some(110.10), Some("Payment by Card"),
      Some(LocalDate.parse("2019-05-27")),
      Some("reference"),
      Seq(
        AllocationDetail(Some("1040000872"), Some(LocalDate.parse("2019-06-27")), Some(LocalDate.parse("2019-08-27")), Some(NIC4_WALES), Some("SA Payment on Account 1"), Some(10.10), Some(5.50), Some("chargeReference1")),
        AllocationDetail(Some("1040000873"), Some(LocalDate.parse("2019-07-28")), Some(LocalDate.parse("2019-09-28")), Some(NIC4_WALES), Some("SA Payment on Account 1"), Some(10.90), Some(5.90), Some("chargeReference2"))
      )
    )

  val testValidLpiPaymentAllocationsModel: PaymentAllocations = PaymentAllocations(
    Some(110.10), Some("Payment by Card"), Some(LocalDate.parse("2019-05-27")), Some("reference"),
    Seq(
      AllocationDetail(Some("1040000872"), Some(LocalDate.parse("2019-06-27")), Some(LocalDate.parse("2019-08-27")), Some(ITSA_NIC4_INTEREST_GB), Some("SA Late Payment Interest"), Some(10.10), Some(5.50), Some("latePaymentInterestId")),
      AllocationDetail(Some("1040000873"), Some(LocalDate.parse("2019-07-28")), Some(LocalDate.parse("2019-09-28")), Some(ITSA_NIC4_INTEREST_GB), Some("SA Late Payment Interest"), Some(10.90), Some(5.90), Some("latePaymentInterestId"))
    )
  )

  val testInvalidPaymentAllocationsModelJson: JsValue = Json.obj(
    "amount" -> "invalidAmount",
    "payMethod" -> "Payment by Card",
    "valDate" -> "2019-05-27"
  )

  val testPaymentAllocationsErrorModelParsing: PaymentAllocationsError = PaymentAllocationsError(
    testErrorStatus, "Json Validation Error. Parsing Payment Allocations Data Response")

  val testPaymentAllocationsErrorModel: PaymentAllocationsError = PaymentAllocationsError(testErrorStatus, testErrorMessage)
  val testPaymentAllocationsErrorModelJson: JsValue = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )

  val paymentAllocationChargesModel: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(List(documentDetail), List(financialDetail))
  val paymentAllocationChargesModelNoOutstandingAmount: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(List(documentDetailNoPaymentCredit), List(financialDetailNoPaymentCredit))
  val paymentAllocationChargesModelWithCredit: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(List(documentDetailWithCredit), List(financialDetail))
  val paymentAllocationChargesModelNoPayment: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(List(documentDetailNoPayment), List(financialDetailNoPayment))

  val paymentAllocationViewModel: PaymentAllocationViewModel = PaymentAllocationViewModel(paymentAllocationChargesModel,
    Seq(
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("1040000872"), Some(LocalDate.parse("2019-06-27")), Some(LocalDate.parse("2019-08-27")), Some("NIC4 Wales"), Some("SA Payment on Account 1"), Some(10.10), Some(5.50), Some("chargeReference1"))),
        Some(LocalDate.parse("2019-05-27"))),
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("1040000873"), Some(LocalDate.parse("2019-07-28")), Some(LocalDate.parse("2019-09-28")), Some("NIC4 Wales"), Some("SA Payment on Account 1"), Some(10.90), Some(5.90), Some("chargeReference2"))),
        Some(LocalDate.parse("2019-05-27"))
      )
    ))

  val paymentAllocationViewModelNoOutstandingAmount: PaymentAllocationViewModel = PaymentAllocationViewModel(paymentAllocationChargesModelNoOutstandingAmount,
    Seq(
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("1040000872"), Some(LocalDate.parse("2019-06-27")), Some(LocalDate.parse("2019-08-27")), Some("NIC4 Wales"), Some("SA Payment on Account 1"), Some(10.10), Some(5.50), Some("chargeReference1"))),
        Some(LocalDate.parse("2019-05-27"))),
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("1040000873"), Some(LocalDate.parse("2019-07-28")), Some(LocalDate.parse("2019-09-28")), Some("NIC4 Wales"), Some("SA Payment on Account 1"), Some(10.90), Some(5.90), Some("chargeReference2"))),
        Some(LocalDate.parse("2019-05-27"))
      )
    ))

  val paymentAllocationViewModelNoPayment: PaymentAllocationViewModel = PaymentAllocationViewModel(paymentAllocationChargesModelNoPayment)


  val financialDetailsWithCreditZeroOutstanding: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetailNoPaymentCredit),
    List(financialDetailNoPaymentCredit)
  )

  val financialDetailsHmrcAdjustment: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail3),
    List(financialDetail3)
  )

  val paymentAllocationViewModelWithCreditZeroOutstanding: PaymentAllocationViewModel = PaymentAllocationViewModel(financialDetailsWithCreditZeroOutstanding,
    Seq(
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("1040000872"), Some(LocalDate.parse("2019-06-27")), Some(LocalDate.parse("2019-08-27")), Some(NIC4_WALES), Some("SA Payment on Account 1"), Some(10.10), Some(5.50), Some("chargeReference1"))),
        Some(LocalDate.parse("2021-01-31"))),
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("1040000873"), Some(LocalDate.parse("2019-07-28")), Some(LocalDate.parse("2019-09-28")), Some(NIC4_WALES), Some("SA Payment on Account 1"), Some(10.90), Some(5.90), Some("chargeReference2"))),
        Some(LocalDate.parse("2021-01-31"))
      )
    ))

  val paymentAllocationViewModelWithNoClearingAmount: PaymentAllocationViewModel = PaymentAllocationViewModel(paymentAllocationChargesModel,
    Seq(
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("1040000872"), Some(LocalDate.parse("2019-06-27")), Some(LocalDate.parse("2019-08-27")), Some(NIC4_WALES), Some("SA Payment on Account 1"), Some(10.10), Some(5.50), Some("chargeReference1"))),
        None)
    ))

  val paymentAllocationViewModelHmrcAdjustment: PaymentAllocationViewModel = PaymentAllocationViewModel(financialDetailsHmrcAdjustment,
    Seq(
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("chargeReference3"), Some(LocalDate.parse("2021-04-06")), Some(LocalDate.parse("2022-04-05")), Some("Test"), Some("ITSA Misc Charge"), Some(300.00), Some(300.00), Some("chargeReference3"))),
        Some(LocalDate.parse("2021-01-31")))
    ))

  val lpiParentChargeDocumentDetail = DocumentDetail(
    taxYear = 2020,
    transactionId = "transactionId",
    documentDescription = Some("TRM New Charge"),
    documentText = Some("documentText"),
    outstandingAmount = 100.00,
    originalAmount = 100.00,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = Some(100),
    interestRate = Some(100),
    interestFromDate = Some(LocalDate.of(2018, 3, 29)),
    interestEndDate = Some(LocalDate.of(2018, 3, 29)),
    accruingInterestAmount = Some(100),
    latePaymentInterestId = Some("latePaymentInterestId")
  )

  val lpiPaymentsDocumentDetail = DocumentDetail(
    taxYear = 2020,
    transactionId = "transactionId",
    documentDescription = Some("TRM New Charge"),
    documentText = Some("documentText"),
    outstandingAmount = 100.00,
    originalAmount = -300.00,
    documentDate = LocalDate.of(2018, 3, 29),
    paymentLotItem = Some("paymentLotItem"),
    effectiveDateOfPayment = Some(LocalDate.of(2021, 1, 31)),
    paymentLot = Some("paymentLot")
  )

  val lpiPaymentAllocationParentChargesModel: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    documentDetails = List(lpiPaymentsDocumentDetail), List(financialDetail))

  val paymentAllocationViewModelLpi: PaymentAllocationViewModel = PaymentAllocationViewModel(lpiPaymentAllocationParentChargesModel,
    Seq(),
    Some(LatePaymentInterestPaymentAllocationDetails(lpiParentChargeDocumentDetail, -300.00)),
    true
  )

  val lpiFinancialDetailsModel: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(lpiParentChargeDocumentDetail),
    financialDetails = List(financialDetail)
  )

  val singleTestPaymentAllocationChargeWithOutstandingAmountZero: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail.copy(outstandingAmount = 0)),
    List(financialDetail)
  )

  val validWrittenPaymentAllocationChargesJson: JsValue = Json.parse(
    """{
			|    "documentDetails": [
			|        {
			|            "documentDate": "2018-03-29",
			|            "documentDescription": "documentDescription",
			|            "documentText": "documentText",
			|            "originalAmount": -300.0,
			|            "outstandingAmount": -200.0,
			|            "taxYear": 2018,
			|            "transactionId": "id",
			|            "paymentLot": "paymentLot",
			|            "effectiveDateOfPayment": "2021-01-31",
			|            "paymentLotItem": "paymentLotItem"
			|        }
			|    ],
			|    "financialDetails": [
			|        {
			|            "chargeType": "NIC4 Wales",
			|            "clearedAmount": 500.0,
			|            "items": [
			|                {
			|                    "subItemId": "1",
			|                    "amount": 100.00,
			|                    "clearingDate": "2021-01-31",
			|                    "dueDate": "2021-01-31",
			|                    "outgoingPaymentMethod": "outgoingPaymentMethod",
			|                    "paymentAmount": 2000.00,
			|                    "paymentId": "paymentLot-paymentLotItem",
			|                    "paymentLot": "paymentLot",
			|                    "paymentLotItem": "paymentLotItem",
			|                    "paymentMethod": "paymentMethod",
			|                    "paymentReference": "paymentReference"
			|                },
			|                {
			|                    "subItemId": "2",
			|                    "amount": 200.00,
			|                    "dueDate": "2021-01-31",
			|                    "outgoingPaymentMethod": "outgoingPaymentMethod2",
			|                    "paymentAmount": 3000.00,
			|                    "paymentLot": "paymentLot2",
			|                    "paymentMethod": "paymentMethod2"
			|                }
			|            ],
			|            "mainType": "SA Payment on Account 1",
			|            "mainTransaction": "4920",
			|            "originalAmount": 500.0,
			|            "outstandingAmount": 500.00,
			|            "taxYear": "2018",
			|            "totalAmount": 1000.00,
			|            "transactionDate": "2018-03-29",
			|            "transactionId": "transactionId",
			|            "type": "type"
			|        }
			|    ]
			|}
			|""".stripMargin)

  val validPaymentAllocationChargesJson: JsValue = Json.parse(
    """{
			|    "documentDetails": [
			|        {
			|            "documentDate": "2018-03-29",
			|            "documentDescription": "documentDescription",
			|            "documentText": "documentText",
			|            "originalAmount": -300.0,
			|            "outstandingAmount": -200.0,
			|            "taxYear": 2018,
			|            "transactionId": "id",
			|            "paymentLot": "paymentLot",
			|            "effectiveDateOfPayment": "2021-01-31",
			|            "paymentLotItem": "paymentLotItem"
			|        }
			|    ],
			|    "financialDetails": [
			|        {
			|            "chargeType": "NIC4 Wales",
			|            "clearedAmount": 500.0,
			|            "items": [
			|                {
			|                    "subItemId": "1",
			|                    "amount": 100.00,
			|                    "clearingDate": "2021-01-31",
			|                    "dueDate": "2021-01-31",
			|                    "outgoingPaymentMethod": "outgoingPaymentMethod",
			|                    "paymentAmount": 2000.00,
			|                    "paymentId": "paymentLot-paymentLotItem",
			|                    "paymentLot": "paymentLot",
			|                    "paymentLotItem": "paymentLotItem",
			|                    "paymentMethod": "paymentMethod",
			|                    "paymentReference": "paymentReference"
			|                },
			|                {
			|                    "subItemId": "2",
			|                    "amount": 200.00,
			|                    "dueDate": "2021-01-31",
			|                    "outgoingPaymentMethod": "outgoingPaymentMethod2",
			|                    "paymentAmount": 3000.00,
			|                    "paymentId": "paymentLot2-paymentLotItem2",
			|                    "paymentLot": "paymentLot2",
			|                    "paymentMethod": "paymentMethod2"
			|                }
			|            ],
			|            "mainType": "SA Payment on Account 1",
|            "mainTransaction": "4920",
			|            "originalAmount": 500.0,
			|            "outstandingAmount": 500.00,
			|            "taxYear": "2018",
			|            "totalAmount": 1000.00,
			|            "transactionDate": "2018-03-29",
			|            "transactionId": "transactionId",
			|            "type": "type"
			|        }
			|    ]
			|}
			|""".stripMargin)

  val variedFinancialDetailsJson: JsValue = Json.parse(
    """{
			|    "documentDetails": [
			|        {
			|            "documentDate": "2018-03-29",
			|            "documentDescription": "documentDescription",
			|            "documentText": "documentText",
			|            "originalAmount": -300.0,
			|            "outstandingAmount": -200.0,
			|            "taxYear": "2018",
			|            "transactionId": "id",
			|            "paymentLot": "paymentLot",
			|            "paymentLotItem": "paymentLotItem"
			|        }
			|    ],
			|    "financialDetails": [
			|        {
			|            "chargeType": "NIC4 Wales",
			|            "clearedAmount": 500.0,
			|            "items": [
			|                {
			|                    "subItemId": "1",
			|                    "amount": 100.00,
			|                    "clearingDate": "2021-01-31",
|                          "clearingSAPDocument": "012345678912",
			|                    "dueDate": "2021-01-31",
			|                    "outgoingPaymentMethod": "outgoingPaymentMethod",
			|                    "paymentAmount": 2000.00,

			|                    "paymentMethod": "paymentMethod",
			|                    "paymentReference": "paymentReference"
			|                },
			|                {
			|                    "subItemId": "2",
			|                    "amount": 200.00,
			|                    "dueDate": "2021-01-31",
			|                    "outgoingPaymentMethod": "outgoingPaymentMethod2",
			|                    "paymentAmount": 3000.00,
			|
			|                    "paymentMethod": "paymentMethod2"
			|                }
			|            ],
			|            "mainType": "SA Payment on Account 1",
|            "mainTransaction": "4920",
			|            "originalAmount": 500.0,
			|            "outstandingAmount": 500.00,
			|            "taxYear": "2018",
			|            "totalAmount": 1000.00,
			|            "transactionDate": "2018-03-29",
			|            "transactionId": "transactionId",
			|            "type": "type"
			|        },
			|        {
			|            "chargeType": "",
			|            "clearedAmount": 500.0,
			|            "items": [
			|                {
			|                    "subItemId": "1",
			|                    "amount": 100.00,
			|                    "clearingDate": "2021-01-31",
			|                    "dueDate": "2021-01-31",
			|                    "outgoingPaymentMethod": "outgoingPaymentMethod",
			|                    "paymentAmount": 2000.00,
|                    "clearingSAPDocument": "012345678912",
			|                    "paymentId": "paymentLot-paymentLotItem",
			|                    "paymentLot": "paymentLot",
			|                    "paymentLotItem": "paymentLotItem",
			|                    "paymentMethod": "paymentMethod",
			|                    "paymentReference": "paymentReference"
			|                }
			|            ],
			|            "mainType": "SA Payment on Account 1",
|            "mainTransaction": "4920",
			|            "originalAmount": 500.0,
			|            "outstandingAmount": 500.00,
			|            "taxYear": "2018",
			|            "totalAmount": 1000.00,
			|            "transactionDate": "2018-03-29",
			|            "transactionId": "transactionId",
			|            "type": "type"
			|        }
			|    ]
			|}
			|""".stripMargin)

  val validMultiplePaymentAllocationChargesJson: JsValue = Json.parse(
    """{
			|    "documentDetails": [
			|        {
			|            "documentDate": "2018-03-29",
			|            "documentDescription": "documentDescription",
			|            "documentText": "documentText",
			|            "originalAmount": -300.0,
			|            "outstandingAmount": -200.0,
			|            "taxYear": 2018,
			|            "transactionId": "id",
			|            "paymentLot": "paymentLot",
			|            "effectiveDateOfPayment": "2021-01-31",
			|            "paymentLotItem": "paymentLotItem"
			|        },
			|        {
			|            "documentDate": "2018-03-29",
			|            "documentDescription": "documentDescription2",
			|            "documentText": "documentText2",
			|            "originalAmount": -100.0,
			|            "outstandingAmount": -50.0,
			|            "taxYear": 2019,
			|            "transactionId": "id2",
			|            "paymentLot": "paymentLot2",
			|            "effectiveDateOfPayment": "2021-01-31",
			|            "paymentLotItem": "paymentLotItem2"
			|        }
			|    ],
			|    "financialDetails": [
			|        {
			|            "chargeType": "NIC4 Wales",
			|            "clearedAmount": 500.0,
			|            "items": [
			|                {
			|                    "subItemId": "1",
			|                    "amount": 100.00,
			|                    "clearingDate": "2021-01-31",
			|                    "dueDate": "2021-01-31",
			|                    "outgoingPaymentMethod": "outgoingPaymentMethod",
			|                    "paymentAmount": 2000.00,
			|                    "paymentId": "paymentLot-paymentLotItem",
			|                    "paymentLot": "paymentLot",
			|                    "paymentLotItem": "paymentLotItem",
			|                    "paymentMethod": "paymentMethod",
			|                    "paymentReference": "paymentReference"
			|                },
			|                {
			|                    "subItemId": "2",
			|                    "amount": 200.00,
			|                    "dueDate": "2021-01-31",
			|                    "outgoingPaymentMethod": "outgoingPaymentMethod2",
			|                    "paymentAmount": 3000.00,
			|                    "paymentId": "paymentLot2-paymentLotItem2",
			|                    "paymentLot": "paymentLot2",
			|                    "paymentMethod": "paymentMethod2"
			|                }
			|            ],
			|            "mainType": "SA Payment on Account 1",
|            "mainTransaction": "4920",
			|            "originalAmount": 500.0,
			|            "outstandingAmount": 500.00,
			|            "taxYear": "2018",
			|            "totalAmount": 1000.00,
			|            "transactionDate": "2018-03-29",
			|            "transactionId": "transactionId",
			|            "type": "type"
			|        },
			|        {
			|            "chargeType": "NIC4 Wales",
			|            "clearedAmount": 500.0,
			|            "items": [
			|                {
			|                    "subItemId": "2",
			|                    "amount": 200.00,
			|                    "dueDate": "2021-01-31",
			|                    "outgoingPaymentMethod": "outgoingPaymentMethod2",
			|                    "paymentAmount": 3000.00,
			|                    "paymentId": "paymentLot2-paymentLotItem2",
			|                    "paymentLot": "paymentLot2",
			|                    "paymentLotItem": "paymentLotItem2",
			|                    "paymentMethod": "paymentMethod2",
			|                    "paymentReference": "paymentReference2"
			|                }
			|            ],
			|            "mainType": "SA Payment on Account 1",
|            "mainTransaction": "4920",
			|            "originalAmount": 500.0,
			|            "outstandingAmount": 200.00,
			|            "taxYear": "2019",
			|            "totalAmount": 2000.00,
			|            "transactionDate": "2021-01-31",
			|            "transactionId": "transactionId2",
			|            "type": "type2"
			|        }
			|    ]
			|}
			|""".stripMargin)

  val paymentAllocationChargesModelMultiplePayments: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(List(documentDetail, documentDetail2),
    List(financialDetail, financialDetail2))
}
