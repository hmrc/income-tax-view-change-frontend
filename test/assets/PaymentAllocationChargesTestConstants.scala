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

package assets

import java.time.LocalDate

import models.financialDetails.{DocumentDetail, FinancialDetail, SubItem}
import models.paymentAllocationCharges._
import play.api.libs.json.{JsValue, Json}

object PaymentAllocationChargesTestConstants {


 val documentDetail: DocumentDetail = DocumentDetail(
  taxYear = "2018",
  transactionId = "id",
  documentDescription = Some("documentDescription"),
  originalAmount = Some(-300.00),
  outstandingAmount = Some(-200.00),
  documentDate = LocalDate.of(2018, 3, 29),
  paymentLot = Some("paymentLot"),
  paymentLotItem = Some("paymentLotItem")
 )

 val documentDetail2: DocumentDetail = DocumentDetail(
  taxYear = "2019",
  transactionId = "id2",
  documentDescription = Some("documentDescription2"),
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

 val paymentAllocationChargesModel: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(List(documentDetail), List(financialDetail))

 val paymentAllocationChargesModelMultiplePayments: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(List(documentDetail, documentDetail2),
  List(financialDetail, financialDetail2))

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

 val validPaymentAllocationChargesJson: JsValue = Json.parse(
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
    |        }
    |    ]
    |}
		|""".stripMargin)

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
    |            "transactionDate": "transactionDate2",
    |            "transactionId": "transactionId2",
    |            "type": "type2"
    |        }
    |    ]
    |}
		|""".stripMargin)
}
