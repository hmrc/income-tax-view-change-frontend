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

import assets.BusinessDetailsIntegrationTestConstants._
import assets.PropertyDetailsIntegrationTestConstants._
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.libs.json.{JsValue, Json}

object IncomeSourceIntegrationTestConstants {

  val singleBusinessResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(business1),
    property = None
  )

  val misalignedBusinessWithPropertyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(business2),
    property = Some(property)
  )

  val multipleBusinessesResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(
      business1,
      business2
    ),
    property = None
  )

  val businessAndPropertyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(business1),
    property = Some(property)
  )

  val multipleBusinessesAndPropertyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(
      business1,
      business2
    ),
    property = Some(property)
  )

  val propertyOnlyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(),
    property = Some(property)
  )

  val noPropertyOrBusinessResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    List(), None
  )

  val errorResponse: IncomeSourceDetailsError = IncomeSourceDetailsError(500,"ISE")

  val testEmptyFinancialDetailsModelJson: JsValue = Json.obj("financialDetails" -> Json.arr())

  def testValidFinancialDetailsModelJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018"): JsValue = Json.obj(
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000123",
        "transactionDate" -> "2019-05-15",
        "type" -> "Balancing Charge Debit",
        "chargeType" -> "Balancing Charge debit",
        "totalAmount" -> 3400,
        "originalAmount" -> originalAmount,
        "outstandingAmount" -> outstandingAmount,
        "items" -> Json.arr(
          Json.obj(
            "subItemId" -> "001",
            "amount" ->  100,
            "clearingDate" -> "2019-05-15",
            "clearingReason" -> "01",
            "outgoingPaymentMethod" -> "A",
            "outgoingPaymentMethod" -> "A",
            "paymentAmount" -> 2000,
            "dueDate" -> s"2018-02-14",
            "paymentMethod" -> "A",
            "paymentId" -> "081203010024-000001"
          ),
          Json.obj(
            "subItemId" -> "002",
            "amount" ->  101,
            "clearingDate" -> "2019-05-16",
            "clearingReason" -> "02",
            "outgoingPaymentMethod" -> "B",
            "paymentAmount" -> 3000,
            "dueDate" -> "2018-02-14",
            "paymentMethod" -> "B",
            "paymentId" -> "081203010025-000002"
          )
        )
      )
    )
  )

  def chargeJson(originalAmount: Option[BigDecimal], outstandingAmount: Option[BigDecimal],
                 totalAmount: Option[BigDecimal], taxYear: String = "2018"): JsValue = Json.obj(
    "taxYear" -> taxYear,
    "transactionId" -> "1040000123",
    "transactionDate" -> "2019-05-15",
    "type" -> "Balancing Charge Debit",
    "totalAmount" -> totalAmount,
    "originalAmount" -> originalAmount,
    "outstandingAmount" -> outstandingAmount,
    "items" -> Json.arr(
      Json.obj(
        "subItemId" -> "001",
        "amount" ->  100,
        "clearingDate" -> "2019-05-15",
        "clearingReason" -> "01",
        "outgoingPaymentMethod" -> "A",
        "paymentReference" -> "A",
        "paymentAmount" -> 2000,
        "dueDate" -> s"$taxYear-02-14",
        "paymentMethod" -> "A",
        "paymentId" -> "081203010024-000001"
      ),
      Json.obj(
        "subItemId" -> "002",
        "amount" ->  101,
        "clearingDate" -> "2019-05-16",
        "clearingReason" -> "02",
        "outgoingPaymentMethod" -> "B",
        "paymentReference" -> "B",
        "paymentAmount" -> 3000,
        "dueDate" -> s"$taxYear-02-14",
        "paymentMethod" -> "B",
        "paymentId" -> "081203010025-000002"
      )
    )
  )

  def testFinancialDetailsErrorModelJson(status: String = "500"): JsValue = Json.obj(
    "code" -> status,
    "message" -> "ERROR MESSAGE"
  )
}
