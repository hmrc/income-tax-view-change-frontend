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

import models.paymentAllocationCharges.{DocumentDetail, FinancialDetail, PaymentAllocationChargesModel, SubItem}
import play.api.libs.json.{JsValue, Json}

object PaymentAllocationChargesTestConstants {


 val documentDetail: DocumentDetail = DocumentDetail(
  taxYear = "2018",
  transactionId = "id",
  documentDescription = Some("documentDescription"),
  originalAmount = Some(300.00),
  outstandingAmount = Some(200.00),
  documentDate = "2018-03-29"
 )

 val documentDetail2: DocumentDetail = DocumentDetail(
  taxYear = "2019",
  transactionId = "id2",
  documentDescription = Some("documentDescription2"),
  originalAmount = Some(100.00),
  outstandingAmount = Some(50.00),
  documentDate = "2018-03-29"
 )


 val financialDetail: FinancialDetail = FinancialDetail(
  taxYear = "2018",
  transactionId = "transactionId",
  transactionDate = Some("transactionDate"),
  `type` = Some("type"),
  totalAmount = Some(BigDecimal("1000.00")),
  originalAmount = Some(BigDecimal(500.00)),
  outstandingAmount = Some(BigDecimal("500.00")),
  clearedAmount = Some(BigDecimal(500.00)),
  chargeType = Some("POA1"),
  mainType = Some("4920"),
  items = Some(Seq(
   SubItem(
    subItemId = Some("1"),
    amount = Some(BigDecimal("100.00")),
    clearingDate = Some("clearingDate"),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(BigDecimal("2000.00")),
    dueDate = Some("dueDate"),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentLot-paymentLotItem")
   )))
 )

 val financialDetail2: FinancialDetail = FinancialDetail(
  taxYear = "2019",
  transactionId = "transactionId2",
  transactionDate = Some("transactionDate2"),
  `type` = Some("type2"),
  totalAmount = Some(BigDecimal("2000.00")),
  originalAmount = Some(BigDecimal(500.00)),
  outstandingAmount = Some(BigDecimal("200.00")),
  clearedAmount = Some(BigDecimal(500.00)),
  chargeType = Some("POA1"),
  mainType = Some("4920"),
  items = Some(Seq(
   SubItem(
    subItemId = Some("2"),
    amount = Some(BigDecimal("200.00")),
    clearingDate = Some("clearingDate2"),
    clearingReason = Some("clearingReason2"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod2"),
    paymentReference = Some("paymentReference2"),
    paymentAmount = Some(BigDecimal("3000.00")),
    dueDate = Some("dueDate2"),
    paymentMethod = Some("paymentMethod2"),
    paymentLot = Some("paymentLot2"),
    paymentLotItem = Some("paymentLotItem2"),
    paymentId = Some("paymentLot2-paymentLotItem2")
   )))
 )


 val paymentAllocationChargesModel = PaymentAllocationChargesModel(List(documentDetail), List(financialDetail))

 val paymentAllocationChargesModelMultiplePayments = PaymentAllocationChargesModel(List(documentDetail, documentDetail2),
  List(financialDetail, financialDetail2))

 val validPaymentAllocationChargesJson: JsValue = Json.parse(
  """{
    |    "documentDetails": [
    |        {
    |            "documentDate": "2018-03-29",
    |            "documentDescription": "documentDescription",
    |            "originalAmount": 300.0,
    |            "outstandingAmount": 200.0,
    |            "taxYear": "2018",
    |            "transactionId": "id"
    |        }
    |    ],
    |    "financialDetails": [
    |        {
    |            "chargeType": "POA1",
    |            "clearedAmount": 500.0,
    |            "items": [
    |                {
    |                    "subItemId": "1",
    |                    "amount": 100.00,
    |                    "clearingDate": "clearingDate",
    |                    "clearingReason": "clearingReason",
    |                    "dueDate": "dueDate",
    |                    "outgoingPaymentMethod": "outgoingPaymentMethod",
    |                    "paymentAmount": 2000.00,
    |                    "paymentId": "paymentLot-paymentLotItem",
    |                    "paymentLot": "paymentLot",
    |                    "paymentLotItem": "paymentLotItem",
    |                    "paymentMethod": "paymentMethod",
    |                    "paymentReference": "paymentReference"
    |                }
    |            ],
    |            "mainType": "4920",
    |            "originalAmount": 500.0,
    |            "outstandingAmount": 500.00,
    |            "taxYear": "2018",
    |            "totalAmount": 1000.00,
    |            "transactionDate": "transactionDate",
    |            "transactionId": "transactionId",
    |            "type": "type"
    |        }
    |    ]
    |}
		|""".stripMargin)



}
