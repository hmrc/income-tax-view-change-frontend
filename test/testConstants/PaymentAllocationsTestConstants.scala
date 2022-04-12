/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate
import models.paymentAllocations.{AllocationDetail, PaymentAllocations, PaymentAllocationsError}
import play.api.libs.json.{JsValue, Json}
import BaseTestConstants._
import models.financialDetails.{BalanceDetails, DocumentDetail, FinancialDetail, FinancialDetailsModel, SubItem}
import models.paymentAllocationCharges.{AllocationDetailWithClearingDate, FinancialDetailsWithDocumentDetailsModel, LatePaymentInterestPaymentAllocationDetails, PaymentAllocationViewModel}

object PaymentAllocationsTestConstants {

  val documentDetail: DocumentDetail = DocumentDetail(
    taxYear = "2018",
    transactionId = "id",
    documentDescription = Some("documentDescription"),
    documentText = Some("documentText"),
    originalAmount = Some(-300.00),
    outstandingAmount = Some(-200.00),
    documentDate = LocalDate.of(2018, 3, 29),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem")
  )

  val documentDetailNoPayment: DocumentDetail = DocumentDetail(
    taxYear = "2018",
    transactionId = "id",
    documentDescription = Some("documentDescription"),
    documentText = Some("documentText"),
    originalAmount = Some(-300.00),
    outstandingAmount = Some(-200.00),
    documentDate = LocalDate.of(2018, 3, 29)
  )

  val documentDetail2: DocumentDetail = DocumentDetail(
    taxYear = "2019",
    transactionId = "id2",
    documentDescription = Some("documentDescription2"),
    documentText = Some("documentText2"),
    originalAmount = Some(-100.00),
    outstandingAmount = Some(-50.00),
    documentDate = LocalDate.of(2018, 3, 29),
    paymentLot = Some("paymentLot2"),
    paymentLotItem = Some("paymentLotItem2")
  )

  val financialDetail: FinancialDetail = FinancialDetail(
    taxYear = "2018",
    transactionId = Some("transactionId"),
    transactionDate = Some("2018-03-29"),
    `type` = Some("type"),
    totalAmount = Some(BigDecimal("1000.00")),
    originalAmount = Some(BigDecimal(500.00)),
    outstandingAmount = Some(BigDecimal("500.00")),
    clearedAmount = Some(BigDecimal(500.00)),
    chargeType = Some("NIC4 Wales"),
    mainType = Some("SA Payment on Account 1"),
    items = Some(Seq(
      SubItem(
        subItemId = Some("1"),
        amount = Some(BigDecimal("100.00")),
        clearingDate = Some("2021-01-31"),
        clearingReason = None,
        outgoingPaymentMethod = Some("outgoingPaymentMethod"),
        paymentReference = Some("paymentReference"),
        paymentAmount = Some(BigDecimal("2000.00")),
        dueDate = Some("2021-01-31"),
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
        dueDate = Some("2021-01-31"),
        paymentMethod = Some("paymentMethod2"),
        paymentLot = Some("paymentLot2"),
        paymentLotItem = None,
        paymentId = None
      )))
  )

  val financialDetailNoPayment: FinancialDetail = FinancialDetail(
    taxYear = "2018",
    transactionId = Some("transactionId"),
    transactionDate = Some("2018-03-29"),
    `type` = Some("type"),
    totalAmount = Some(BigDecimal("1000.00")),
    originalAmount = Some(BigDecimal(500.00)),
    outstandingAmount = Some(BigDecimal("500.00")),
    clearedAmount = Some(BigDecimal(500.00)),
    chargeType = Some("NIC4 Wales"),
    mainType = Some("SA Payment on Account 1"),
    items = Some(Seq(
      SubItem(
        subItemId = Some("1"),
        amount = Some(BigDecimal("100.00")),
        clearingDate = Some("2021-01-31"),
        clearingReason = None,
        outgoingPaymentMethod = Some("outgoingPaymentMethod"),
        paymentReference = Some("paymentReference"),
        paymentAmount = Some(BigDecimal("2000.00")),
        dueDate = Some("2021-01-31"),
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
        dueDate = Some("2021-01-31"),
        paymentMethod = Some("paymentMethod2")
      )))
  )

  val financialDetail2: FinancialDetail = FinancialDetail(
    taxYear = "2019",
    transactionId = Some("transactionId2"),
    transactionDate = Some("transactionDate2"),
    `type` = Some("type2"),
    totalAmount = Some(BigDecimal("2000.00")),
    originalAmount = Some(BigDecimal(500.00)),
    outstandingAmount = Some(BigDecimal("200.00")),
    clearedAmount = Some(BigDecimal(500.00)),
    chargeType = Some("NIC4 Wales"),
    mainType = Some("SA Payment on Account 1"),
    items = Some(Seq(
      SubItem(
        subItemId = Some("2"),
        amount = Some(BigDecimal("200.00")),
        clearingDate = None,
        clearingReason = None,
        outgoingPaymentMethod = Some("outgoingPaymentMethod2"),
        paymentReference = Some("paymentReference2"),
        paymentAmount = Some(BigDecimal("3000.00")),
        dueDate = Some("2021-01-31"),
        paymentMethod = Some("paymentMethod2"),
        paymentLot = Some("paymentLot2"),
        paymentLotItem = Some("paymentLotItem2"),
        paymentId = Some("paymentLot2-paymentLotItem2")
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
        "chargeType" -> "NIC4 Wales",
        "mainType" -> "SA Payment on Account 1",
        "amount" -> 10.10,
        "clearedAmount" -> 5.50,
        "chargeReference" -> "chargeReference1"
      ),
      Json.obj(
        "transactionId" -> "1040000873",
        "from" -> "2019-07-28",
        "to" -> "2019-09-28",
        "chargeType" -> "NIC4 Wales",
        "mainType" -> "SA Payment on Account 1",
        "amount" -> 10.90,
        "clearedAmount" -> 5.90,
        "chargeReference" -> "chargeReference2"
      )
    )
  )

  val testValidPaymentAllocationsModel: PaymentAllocations = PaymentAllocations(
    Some(110.10), Some("Payment by Card"), Some("2019-05-27"), Some("reference"),
    Seq(
      AllocationDetail(Some("1040000872"), Some("2019-06-27"), Some("2019-08-27"), Some("NIC4 Wales"), Some("SA Payment on Account 1"), Some(10.10), Some(5.50), Some("chargeReference1")),
      AllocationDetail(Some("1040000873"), Some("2019-07-28"), Some("2019-09-28"), Some("NIC4 Wales"), Some("SA Payment on Account 1"), Some(10.90), Some(5.90), Some("chargeReference2"))
    )
  )

  val testValidLpiPaymentAllocationsModel: PaymentAllocations = PaymentAllocations(
    Some(110.10), Some("Payment by Card"), Some("2019-05-27"), Some("reference"),
    Seq(
      AllocationDetail(Some("1040000872"), Some("2019-06-27"), Some("2019-08-27"), Some("ITSA NIC4 Interest GB"), Some("SA Late Payment Interest"), Some(10.10), Some(5.50), Some("latePaymentInterestId")),
      AllocationDetail(Some("1040000873"), Some("2019-07-28"), Some("2019-09-28"), Some("ITSA NIC4 Interest GB"), Some("SA Late Payment Interest"), Some(10.90), Some(5.90), Some("latePaymentInterestId"))
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
  val paymentAllocationChargesModelNoPayment: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(List(documentDetailNoPayment), List(financialDetailNoPayment))


  val paymentAllocationViewModel: PaymentAllocationViewModel = PaymentAllocationViewModel(paymentAllocationChargesModel,
    Seq(
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("1040000872"), Some("2019-06-27"), Some("2019-08-27"), Some("NIC4 Wales"), Some("SA Payment on Account 1"), Some(10.10), Some(5.50), Some("chargeReference1"))),
        Some("2021-01-31")),
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("1040000873"), Some("2019-07-28"), Some("2019-09-28"), Some("NIC4 Wales"), Some("SA Payment on Account 1"), Some(10.90), Some(5.90), Some("chargeReference2"))),
        Some("2021-01-31")
      )
    ))

  val paymentAllocationViewModelNoPayment: PaymentAllocationViewModel = PaymentAllocationViewModel(paymentAllocationChargesModelNoPayment)

  val financialDetailsWithNoOriginalAmount: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail.copy(originalAmount = None)),
    List(financialDetail)
  )

  val paymentAllocationViewModelWithNoOriginalAmount: PaymentAllocationViewModel = PaymentAllocationViewModel(financialDetailsWithNoOriginalAmount,
    Seq(
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("1040000872"), Some("2019-06-27"), Some("2019-08-27"), Some("NIC4 Wales"), Some("SA Payment on Account 1"), Some(10.10), Some(5.50), Some("chargeReference1"))),
        Some("2021-01-31")),
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("1040000873"), Some("2019-07-28"), Some("2019-09-28"), Some("NIC4 Wales"), Some("SA Payment on Account 1"), Some(10.90), Some(5.90), Some("chargeReference2"))),
        Some("2021-01-31")
      )
    ))

  val paymentAllocationViewModelWithNoClearingAmount: PaymentAllocationViewModel = PaymentAllocationViewModel(paymentAllocationChargesModel,
    Seq(
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("1040000872"), Some("2019-06-27"), Some("2019-08-27"), Some("NIC4 Wales"), Some("SA Payment on Account 1"), Some(10.10), Some(5.50), Some("chargeReference1"))),
        None)
    ))

  val lpiParentChargeDocumentDetail = DocumentDetail(
    taxYear = "2020",
    transactionId = "transactionId",
    documentDescription = Some("TRM New Charge"),
    documentText = Some("documentText"),
    outstandingAmount = Some(100.00),
    originalAmount = Some(100.00),
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = Some(100),
    interestRate = Some(100),
    interestFromDate = Some(LocalDate.of(2018, 3, 29)),
    interestEndDate = Some(LocalDate.of(2018, 3, 29)),
    latePaymentInterestAmount = Some(100),
    latePaymentInterestId = Some("latePaymentInterestId")
  )

  val lpiPaymentsDocumentDetail = DocumentDetail(
    taxYear = "2020",
    transactionId = "transactionId",
    documentDescription = Some("TRM New Charge"),
    documentText = Some("documentText"),
    outstandingAmount = Some(100.00),
    originalAmount = Some(-300.00),
    documentDate = LocalDate.of(2018, 3, 29),
    paymentLotItem = Some("paymentLotItem"),
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
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    codingDetails = None,
    documentDetails = List(lpiParentChargeDocumentDetail),
    financialDetails = List(financialDetail)
  )

  val singleTestPaymentAllocationChargeWithOutstandingAmountZero: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail.copy(outstandingAmount = Some(0))),
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
			|                    "paymentId": "paymentLot-paymentLotItem",
			|                    "paymentLot": "paymentLot",
			|                    "paymentLotItem": "paymentLotItem",
			|                    "paymentMethod": "paymentMethod",
			|                    "paymentReference": "paymentReference"
			|                }
			|            ],
			|            "mainType": "SA Payment on Account 1",
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
			|            "taxYear": "2018",
			|            "transactionId": "id",
			|            "paymentLot": "paymentLot",
			|            "paymentLotItem": "paymentLotItem"
			|        },
			|        {
			|            "documentDate": "2018-03-29",
			|            "documentDescription": "documentDescription2",
			|            "documentText": "documentText2",
			|            "originalAmount": -100.0,
			|            "outstandingAmount": -50.0,
			|            "taxYear": "2019",
			|            "transactionId": "id2",
			|            "paymentLot": "paymentLot2",
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
			|            "originalAmount": 500.0,
			|            "outstandingAmount": 200.00,
			|            "taxYear": "2019",
			|            "totalAmount": 2000.00,
			|            "transactionDate": "transactionDate2",
			|            "transactionId": "transactionId2",
			|            "type": "type2"
			|        }
			|    ]
			|}
			|""".stripMargin)

  val paymentAllocationChargesModelMultiplePayments: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(List(documentDetail, documentDetail2),
    List(financialDetail, financialDetail2))
}
