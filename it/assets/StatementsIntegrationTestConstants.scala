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

import implicits.ImplicitDateFormatter._
import models.financialTransactions.{FinancialTransactionsModel, SubItemModel, TransactionModel}
import play.api.libs.json.{JsValue, Json}

object StatementsIntegrationTestConstants {

  def emptyResponse(): JsValue = Json.obj()

  def failureResponse(code: String, reason: String): JsValue = Json.obj(
    "code" -> code,
    "reason" -> reason
  )

  val charge2018: SubItemModel = SubItemModel(
    subItem = Some("000"),
    dueDate = Some("2019-1-31"),
    amount = Some(1000.00)
  )

  val charge2019: SubItemModel = SubItemModel(
    subItem = Some("000"),
    dueDate = Some("2020-1-31"),
    amount = Some(7500.00)
  )

  val payment2019: SubItemModel = SubItemModel(
    subItem = Some("001"),
    clearingDate = Some("2019-8-6"),
    paymentReference = Some("aPaymentReference"),
    paymentAmount = Some(500.00)
  )

  val otherPayment2019: SubItemModel = SubItemModel(
    subItem = Some("002"),
    clearingDate = Some("2019-8-7"),
    paymentReference = Some("aPaymentReference"),
    paymentAmount = Some(250.00)
  )

  val singleChargeTransactionModel = TransactionModel(
    chargeType = Some(""),
    mainType = Some(""),
    periodKey = Some(""),
    periodKeyDescription = Some(""),
    taxPeriodFrom = Some("2017-4-6"),
    taxPeriodTo = Some("2018-4-5"),
    businessPartner = Some(""),
    contractAccountCategory = Some(""),
    contractAccount = Some(""),
    contractObjectType = Some(""),
    contractObject = Some(""),
    sapDocumentNumber = Some(""),
    sapDocumentNumberItem = Some(""),
    chargeReference = Some(""),
    mainTransaction = Some(""),
    subTransaction = Some(""),
    originalAmount = Some(1000.00),
    outstandingAmount = Some(1000.00),
    clearedAmount = Some(0.00),
    accruedInterest = Some(0.00),
    items = Some(Seq(charge2018))
  )

  val singleCharge2PaymentsTransactionModel = TransactionModel(
    chargeType = Some(""),
    mainType = Some(""),
    periodKey = Some(""),
    periodKeyDescription = Some(""),
    taxPeriodFrom = Some("2018-4-6"),
    taxPeriodTo = Some("2019-4-5"),
    businessPartner = Some(""),
    contractAccountCategory = Some(""),
    contractAccount = Some(""),
    contractObjectType = Some(""),
    contractObject = Some(""),
    sapDocumentNumber = Some(""),
    sapDocumentNumberItem = Some(""),
    chargeReference = Some(""),
    mainTransaction = Some(""),
    subTransaction = Some(""),
    originalAmount = Some(7500.00),
    outstandingAmount = Some(6750.00),
    clearedAmount = Some(750.00),
    accruedInterest = Some(0.00),
    items = Some(Seq(charge2019, payment2019, otherPayment2019))
  )

  val singleFinancialTransactionsModel = FinancialTransactionsModel(
    idType = None,
    idNumber = None,
    regimeType = None,
    processingDate = "2017-03-07T09:30:00.000Z".toZonedDateTime,
    financialTransactions = Some(Seq(singleChargeTransactionModel))
  )

  val singleFTModel1charge2payments = FinancialTransactionsModel(
    idType = None,
    idNumber = None,
    regimeType = None,
    processingDate = "2017-03-07T09:30:00.000Z".toZonedDateTime,
    financialTransactions = Some(Seq(singleChargeTransactionModel, singleCharge2PaymentsTransactionModel))
  )

  val emptyFTModel = FinancialTransactionsModel(
    idType = None,
    idNumber = None,
    regimeType = None,
    processingDate = "2017-03-07T09:30:00.000Z".toZonedDateTime,
    financialTransactions = None
  )

  val emptyStatementResponse: JsValue = Json.toJson(emptyFTModel)

  val singleChargeStatementResponse: JsValue = Json.toJson(singleFinancialTransactionsModel)

  val twoChargeStatementResponse: JsValue = Json.toJson(singleFTModel1charge2payments)
}