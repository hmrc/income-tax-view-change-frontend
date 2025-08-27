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

package testConstants

import enums.ChargeType.{ITSA_ENGLAND_AND_NI, ITSA_NIC2_INTEREST_GB, ITSA_NIC4_INTEREST_GB, NIC4_WALES}
import models.financialDetails.{DocumentDetail, FinancialDetail, SubItem}
import models.paymentAllocationCharges.{AllocationDetailWithClearingDate, FinancialDetailsWithDocumentDetailsModel, LatePaymentInterestPaymentAllocationDetails, PaymentAllocationViewModel}
import models.paymentAllocations.{AllocationDetail, PaymentAllocations}
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

object PaymentAllocationIntegrationTestConstants {


  val documentDetail: DocumentDetail = DocumentDetail(
    taxYear = 2018,
    transactionId = "id",
    documentDescription = Some("documentDescription"),
    documentText = Some("documentText"),
    originalAmount = -300.00,
    outstandingAmount = -200.00,
    documentDate = LocalDate.of(2018, 3, 29),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem")
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
    paymentLotItem = Some("paymentLotItem2")
  )

  val documentDetail3: DocumentDetail = DocumentDetail(
    taxYear = 2022,
    transactionId = "MA999991A202202",
    documentDescription = Some("Payment"),
    documentText = Some("documentText"),
    originalAmount = 800,
    outstandingAmount = 0.00,
    documentDate = LocalDate.of(2022, 4, 6),
    paymentLot = Some("MA999991A"),
    paymentLotItem = Some("5")
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

  val financialDetail2: FinancialDetail = FinancialDetail(
    taxYear = "2019",
    transactionId = Some("transactionId2"),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type2"),
    totalAmount = Some(BigDecimal("2000.00")),
    originalAmount = Some(BigDecimal(500.00)),
    outstandingAmount = Some(BigDecimal("200.00")),
    clearedAmount = Some(BigDecimal(500.00)),
    chargeType = Some(NIC4_WALES),
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
        dueDate = Some(LocalDate.parse("2021-01-31")),
        paymentMethod = Some("paymentMethod2"),
        paymentLot = Some("paymentLot2"),
        paymentLotItem = Some("paymentLotItem2"),
        paymentId = Some("paymentLot2-paymentLotItem2")
      )))
  )

  val financialDetail3: FinancialDetail = FinancialDetail(
    taxYear = "2022",
    transactionId = Some("MA999991A202202"),
    transactionDate = Some(LocalDate.parse("2022-04-06")),
    `type` = Some("type3"),
    totalAmount = Some(BigDecimal("800.00")),
    originalAmount = Some(BigDecimal(800.00)),
    outstandingAmount = Some(BigDecimal("00.00")),
    clearedAmount = Some(BigDecimal(800.00)),
    chargeType = Some(ITSA_ENGLAND_AND_NI),
    mainType = Some("ITSA Misc Charge"),
    items = Some(Seq(
      SubItem(
        subItemId = Some("001"),
        amount = Some(BigDecimal("800.00")),
        clearingDate = Some(LocalDate.parse("2021-01-28")),
        paymentReference = Some("GF235687"),
        paymentAmount = Some(BigDecimal("800.00")),
        dueDate = Some(LocalDate.parse("2021-01-28")),
        paymentMethod = Some("Payment"),
        paymentLot = Some("MA999991A"),
        paymentLotItem = Some("5"),
      )))
  )


  val paymentAllocationChargesModel: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(List(documentDetail), List(financialDetail))

  val paymentAllocationChargesModelMultiplePayments: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(List(documentDetail, documentDetail2),
    List(financialDetail, financialDetail2))

  val financialDetailsHmrcAdjustment: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail3),
    List(financialDetail3)
  )

  val variedFinancialDetailsJson: JsValue = Json.parse(
    """{
			|    "documentDetails": [
			|        {
			|            "documentDate": "2018-03-29",
			|            "documentDescription": "documentDescription",
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
        "clearedAmount" -> 5.50
      ),
      Json.obj(
        "transactionId" -> "1040000873",
        "from" -> "2019-07-28",
        "to" -> "2019-09-28",
        "chargeType" -> NIC4_WALES,
        "mainType" -> "SA Payment on Account 1",
        "amount" -> 10.90,
        "clearedAmount" -> 5.90
      )
    )
  )

  val testValidPaymentAllocationsModel: PaymentAllocations = PaymentAllocations(
    Some(110.10), Some("Payment by Card"), Some(LocalDate.parse("2019-05-27")), Some("reference"),
    Seq(
      AllocationDetail(Some("1040000872"), Some(LocalDate.parse("2019-06-27")), Some(LocalDate.parse("2019-08-27")), Some(NIC4_WALES), Some("SA Payment on Account 1"), Some(10.10), Some(5.50), Some("chargeReference1")),
      AllocationDetail(Some("1040000873"), Some(LocalDate.parse("2019-07-28")), Some(LocalDate.parse("2019-09-28")), Some(NIC4_WALES), Some("SA Payment on Account 1"), Some(10.90), Some(5.90), Some("chargeReference2"))
    )
  )

  val testValidLpiPaymentAllocationsModel: PaymentAllocations = PaymentAllocations(
    Some(110.10), Some("Payment by Card"), Some(LocalDate.parse("2019-05-27")), Some("reference"),
    Seq(
      AllocationDetail(Some("1040000872"), Some(LocalDate.parse("2019-06-27")), Some(LocalDate.parse("2019-08-27")), Some(ITSA_NIC4_INTEREST_GB), Some("SA Late Payment Interest"), Some(10.10), Some(5.50), Some("latePaymentInterestId")),
      AllocationDetail(Some("1040000873"), Some(LocalDate.parse("2019-07-28")), Some(LocalDate.parse("2019-09-28")), Some(ITSA_NIC2_INTEREST_GB), Some("SA Late Payment Interest"), Some(10.90), Some(5.90), Some("latePaymentInterestId"))
    )
  )

  val testValidNoLpiPaymentAllocationHmrcAdjustment: PaymentAllocations = PaymentAllocations(
    amount = Some(-800.00), method = Some("Payment"), transactionDate = Some(LocalDate.parse("2021-01-31")), reference = Some("GF235688"),
    allocations = Seq(
      AllocationDetail(Some("MA999991A202202"),
        Some(LocalDate.parse("2021-04-06")),
        Some(LocalDate.parse("2022-04-05")),
        Some(ITSA_ENGLAND_AND_NI), Some("ITSA Misc Charge"),
        Some(800.00), Some(800.00),
        Some("MA999991A202202")))
  )

  val paymentAllocationViewModel: PaymentAllocationViewModel = PaymentAllocationViewModel(paymentAllocationChargesModel,
    Seq(
      AllocationDetailWithClearingDate(Some(AllocationDetail(Some("1040000872"), Some(LocalDate.parse("2019-06-27")), Some(LocalDate.parse("2019-08-27")), Some(NIC4_WALES), Some("SA Payment on Account 1"), Some(10.10), Some(5.50), Some("chargeReference1"))),
        Some(LocalDate.parse("2019-05-27"))),
      AllocationDetailWithClearingDate(Some(AllocationDetail(Some("1040000873"), Some(LocalDate.parse("2019-07-28")), Some(LocalDate.parse("2019-09-28")), Some(NIC4_WALES), Some("SA Payment on Account 1"), Some(10.90), Some(5.90), Some("chargeReference2"))),
        Some(LocalDate.parse("2019-05-27")))
    ))

  val paymentAllocationViewModelHmrcAdjustment: PaymentAllocationViewModel = PaymentAllocationViewModel(financialDetailsHmrcAdjustment,
    Seq(
      AllocationDetailWithClearingDate(
        Some(AllocationDetail(Some("MA999991A202202"), Some(LocalDate.parse("2021-04-06")), Some(LocalDate.parse("2022-04-05")), Some(ITSA_ENGLAND_AND_NI), Some("ITSA Misc Charge"), Some(800.00), Some(800.00), Some("XM002610011594"))),
        Some(LocalDate.parse("2021-01-31")))
    ))

  val lpiDocumentDetail = DocumentDetail(
    taxYear = 9999,
    transactionId = "1040000125",
    documentDescription = Some("TRM Amend Charge"),
    documentText = Some("documentText"),
    originalAmount = 10.34,
    outstandingAmount = 1.2,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = Some(42.50),
    interestRate = Some(3),
    interestFromDate = Some(LocalDate.of(2018, 2, 14)),
    interestEndDate = Some(LocalDate.of(2019, 1, 1)),
    accruingInterestAmount = Some(0)
  )

  val lpiPaymentAllocationViewModel: PaymentAllocationViewModel = PaymentAllocationViewModel(paymentAllocationChargesModel,
    Seq(), Some(LatePaymentInterestPaymentAllocationDetails(lpiDocumentDetail, -300.00)), true)

  val validPaymentAllocationChargesJson: JsValue = Json.parse(
    """{
			|    "documentDetails": [
			|        {
			|            "documentDate": "2018-03-29",
			|            "documentDescription": "documentDescription",
			|            "originalAmount": -300.0,
			|            "outstandingAmount": -200.0,
			|            "taxYear": 2018,
			|            "transactionId": "id",
			|            "paymentLot": "paymentLot",
			|            "paymentLotItem": "paymentLotItem",
			|            "effectiveDateOfPayment": "2021-01-31",
			|            "totalAmount": 100.00
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

  val validPaymentAllocationChargesHmrcAdjustmentJson: JsValue = Json.parse(
    """{
      |		"documentDetails" : [
      |			{
      |				"transactionId":"MA999991A202202",
      |				"outstandingAmount": 0,
      |				"originalAmount": 800,
      |				"taxYear": 2022,
      |				"documentId": "MA999991A202202",
      |				"documentDate": "2022-04-06",
      |				"documentDescription": "Payment",
      |				"documentText": "documentText",
      |				"formBundleNumber": "88888888",
      |				"totalAmount": 800,
      |				"documentOutstandingAmount": 0,
      |				"paymentLot": "MA999991A",
      |				"paymentLotItem": "5",
      |				"effectiveDateOfPayment": "2021-01-31",
      |				"statisticalFlag": false
      |			}
      |		],
      |		"financialDetails": [
      |			{
      |				"taxYear": "2022",
      |				"documentId": "MA999991A202202",
      |				"chargeType": "ITSA England & NI",
      |				"mainType": "ITSA Misc Charge",
      |				"sapDocumentNumber": "1040000872",
      |				"sapDocumentNumberItem": "XM00",
      |				"chargeReference": "XM002610011594",
      |				"originalAmount": 800.0,
      |				"outstandingAmount": 0.0,
      |				"clearedAmount": 800.0,
      |				"items": [
      |					{
      |						"subItem": "001",
      |						"dueDate": "2021-01-28",
      |						"clearingDate": "2021-01-28",
      |						"amount": 800,
      |						"paymentReference": "GF235687",
      |						"paymentAmount": 800,
      |						"paymentMethod": "Payment",
      |						"paymentLot": "MA999991A",
      |						"paymentLotItem": "5"
      |					}
      |				]
      |			}
      |		]
      |	}
      |""".stripMargin
  )

  val validWrittenPaymentAllocationChargesJson: JsValue = Json.parse(
    """{
			|    "documentDetails": [
			|        {
			|            "documentDate": "2018-03-29",
			|            "documentDescription": "documentDescription",
			|            "originalAmount": -300.0,
			|            "outstandingAmount": -200.0,
			|            "taxYear": "2018",
			|            "transactionId": "id",
			|            "paymentLot": "paymentLot",
			|            "paymentLotItem": "paymentLotItem"
			|        }
			|    ],
			|    "paymentDetails": [
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

  val validMultiplePaymentAllocationChargesJson: JsValue = Json.parse(
    """{
			|    "documentDetails": [
			|        {
			|            "documentDate": "2018-03-29",
			|            "documentDescription": "documentDescription",
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
			|            "transactionDate": "2020-08-16",
			|            "transactionId": "transactionId2",
			|            "type": "type2"
			|        }
			|    ]
			|}
			|""".stripMargin)
}
