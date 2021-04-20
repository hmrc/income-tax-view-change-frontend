/*
 * Copyright 2017 HM Revenue & Customs
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

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

import assets.BaseIntegrationTestConstants.testMtditid
import play.api.libs.json.{JsObject, JsValue, Json}

object FinancialTransactionsIntegrationTestConstants {

  val testIdType: String = "MTDBSA"
  val testIdNumber: String = testMtditid
  val testRegimeType: String = "ITSA"
  val testProcessingDate: ZonedDateTime = ZonedDateTime.parse(s"2017-03-07T22:55:56.987Z", DateTimeFormatter.ISO_ZONED_DATE_TIME)
  val testJsonFromDate: LocalDate = LocalDate.of(2017,4,6)
  val testJsonToDate: LocalDate = LocalDate.of(2018,4,5)

  def financialTransactionsSingleErrorJson(status: String = "500"): JsValue =
    Json.obj(
      "code" -> status,
      "message" -> "ERROR MESSAGE"
    )

  val financialTransactionsMultiErrorJson: JsValue =
    Json.obj(
      "failures" -> Json.arr(
        Json.obj(
          "code" -> "500",
          "message" -> "ERROR MESSAGE 1"
        ),
        Json.obj(
          "code" -> "500",
          "message" -> "ERROR MESSAGE 2"
        )
      )
    )

  def financialTransactionsJson(outstandingAmount: BigDecimal, from: String = "2017-04-06", to: String ="2018-04-05"): JsObject =
    Json.obj(
      "idType" -> testIdType,
      "idNumber" -> testMtditid,
      "regimeType" -> testRegimeType,
      "processingDate" -> testProcessingDate,
      "financialTransactions" -> Json.arr(
        Json.obj(
          "chargeType" -> "PAYE",
          "mainType" -> "2100",
          "periodKey" -> "13RL",
          "periodKeyDescription" -> "abcde",
          "taxPeriodFrom" -> LocalDate.parse(from, DateTimeFormatter.ofPattern("uuuu-M-d")),
          "taxPeriodTo" -> LocalDate.parse(to, DateTimeFormatter.ofPattern("uuuu-M-d")),
          "businessPartner" -> "6622334455",
          "contractAccountCategory" -> "02",
          "contractAccount" -> "X",
          "contractObjectType" -> "ABCD",
          "contractObject" -> "00000003000000002757",
          "sapDocumentNumber" -> "1040000872",
          "sapDocumentNumberItem" -> "XM00",
          "chargeReference" -> "XM002610011594",
          "mainTransaction" -> "1234",
          "subTransaction" -> "5678",
          "originalAmount" -> 3400,
          "outstandingAmount" -> outstandingAmount,
          "clearedAmount" -> 2000,
          "accruedInterest" -> 0.23,
          "items" -> Json.arr(
            Json.obj(
              "subItem" -> "000",
              "dueDate" -> "2018-02-14",
              "amount" -> 3400,
              "clearingDate" -> "2018-02-17",
              "clearingReason" -> "A",
              "outgoingPaymentMethod" -> "B",
              "paymentLock" -> "C",
              "clearingLock" -> "D",
              "interestLock" -> "E",
              "dunningLock" -> "1",
              "returnFlag" -> false,
              "paymentReference" -> "F",
              "paymentAmount" -> 2000,
              "paymentMethod" -> "G",
              "paymentLot" -> "H",
              "paymentLotItem" -> "112",
              "clearingSAPDocument" -> "3350000253",
              "statisticalDocument" -> "I",
              "returnReason" -> "J",
              "promiseToPay" -> "K"
            )
          )
        )
      )
    )

  def transactionJson(outstandingAmount: Option[BigDecimal], originalAmount: Option[BigDecimal], from: String = "2017-04-06", to: String ="2018-04-05"): JsObject = Json.obj(
    "chargeType" -> "PAYE",
    "mainType" -> "2100",
    "periodKey" -> "13RL",
    "periodKeyDescription" -> "abcde",
    "taxPeriodFrom" -> LocalDate.parse(from, DateTimeFormatter.ofPattern("uuuu-M-d")),
    "taxPeriodTo" -> LocalDate.parse(to, DateTimeFormatter.ofPattern("uuuu-M-d")),
    "businessPartner" -> "6622334455",
    "contractAccountCategory" -> "02",
    "contractAccount" -> "X",
    "contractObjectType" -> "ABCD",
    "contractObject" -> "00000003000000002757",
    "sapDocumentNumber" -> "1040000872",
    "sapDocumentNumberItem" -> "XM00",
    "chargeReference" -> "XM002610011594",
    "mainTransaction" -> "1234",
    "subTransaction" -> "5678",
    "clearedAmount" -> 2000,
    "accruedInterest" -> 0.23,
    "items" -> Json.arr(
      Json.obj(
        "subItem" -> "000",
        "dueDate" -> "2018-02-14",
        "amount" -> 3400,
        "clearingDate" -> "2018-02-17",
        "clearingReason" -> "A",
        "outgoingPaymentMethod" -> "B",
        "paymentLock" -> "C",
        "clearingLock" -> "D",
        "interestLock" -> "E",
        "dunningLock" -> "1",
        "returnFlag" -> false,
        "paymentReference" -> "F",
        "paymentAmount" -> 2000,
        "paymentMethod" -> "G",
        "paymentLot" -> "H",
        "paymentLotItem" -> "112",
        "clearingSAPDocument" -> "3350000253",
        "statisticalDocument" -> "I",
        "returnReason" -> "J",
        "promiseToPay" -> "K"
      )
    )
  ) ++ outstandingAmount.fold(Json.obj())(amount => Json.obj("outstandingAmount" -> amount)) ++
    originalAmount.fold(Json.obj())(amount => Json.obj("originalAmount" -> amount))


  def financialTransactionWithoutDueDatesJson(outstandingAmount: BigDecimal): JsValue =
    Json.obj(
      "idType" -> testIdType,
      "idNumber" -> testMtditid,
      "regimeType" -> testRegimeType,
      "processingDate" -> testProcessingDate,
      "financialTransactions" -> Json.arr(
        Json.obj(
          "chargeType" -> "PAYE",
          "mainType" -> "2100",
          "periodKey" -> "13RL",
          "periodKeyDescription" -> "abcde",
          "taxPeriodFrom" -> "2017-04-06",
          "taxPeriodTo" -> "2018-04-05",
          "businessPartner" -> "6622334455",
          "contractAccountCategory" -> "02",
          "contractAccount" -> "X",
          "contractObjectType" -> "ABCD",
          "contractObject" -> "00000003000000002757",
          "sapDocumentNumber" -> "1040000872",
          "sapDocumentNumberItem" -> "XM00",
          "chargeReference" -> "XM002610011594",
          "mainTransaction" -> "1234",
          "subTransaction" -> "5678",
          "originalAmount" -> 3400,
          "outstandingAmount" -> outstandingAmount,
          "clearedAmount" -> 2000,
          "accruedInterest" -> 0.23,
          "items" -> Json.arr(
            Json.obj(
              "subItem" -> "000",
              "amount" -> 3400,
              "clearingDate" -> "2018-02-17",
              "clearingReason" -> "A",
              "outgoingPaymentMethod" -> "B",
              "paymentLock" -> "C",
              "clearingLock" -> "D",
              "interestLock" -> "E",
              "dunningLock" -> "1",
              "returnFlag" -> false,
              "paymentReference" -> "F",
              "paymentAmount" -> 2000,
              "paymentMethod" -> "G",
              "paymentLot" -> "H",
              "paymentLotItem" -> "112",
              "clearingSAPDocument" -> "3350000253",
              "statisticalDocument" -> "I",
              "returnReason" -> "J",
              "promiseToPay" -> "K"
            )
          )
        )
      )
    )

  val emptyFinancialTransaction: JsValue =
    Json.obj(
      "idType" -> testIdType,
      "idNumber" -> testMtditid,
      "regimeType" -> testRegimeType,
      "processingDate" -> testProcessingDate)

}

