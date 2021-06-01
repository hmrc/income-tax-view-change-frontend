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
import assets.PaymentHistoryTestConstraints.oldBusiness1
import assets.PropertyDetailsIntegrationTestConstants._
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.libs.json.{JsValue, Json}

object IncomeSourceIntegrationTestConstants {

  val singleBusinessResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(business1),
    property = None,
    yearOfMigration = None
  )

  val misalignedBusinessWithPropertyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(business2),
    property = Some(property),
    yearOfMigration = None
  )

  val multipleBusinessesResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(
      business1,
      business2
    ),
    property = None,
    yearOfMigration = Some("2019")
  )

  val businessAndPropertyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(business1),
    property = Some(property),
    yearOfMigration = None
  )

  val paymentHistoryBusinessAndPropertyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testMtdItId,
    None,
    businesses = List(oldBusiness1),
    property = Some(oldProperty)
  )

  val multipleBusinessesAndPropertyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(
      business1,
      business2
    ),
    property = Some(property),
    yearOfMigration = None
  )

  val propertyOnlyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(),
    property = Some(property),
    yearOfMigration = Some("2018")
  )

  val noPropertyOrBusinessResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId, None,
    List(), None
  )
  val errorResponse: IncomeSourceDetailsError = IncomeSourceDetailsError(500, "ISE")
  val testEmptyFinancialDetailsModelJson: JsValue = Json.obj("documentDetails" -> Json.arr(), "financialDetails" -> Json.arr())

  def propertyOnlyResponseWithMigrationData(year: Int,
                                            yearOfMigration: Option[String]): IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(),
    property = Some(propertyWithCurrentYear(year)),
    yearOfMigration = yearOfMigration
  )

  def testValidFinancialDetailsModelJsonSingleCharge(originalAmount: BigDecimal, outstandingAmount: BigDecimal,
                                                     taxYear: String = "2018", dueDate: String = "2018-02-14"): JsValue = Json.obj(
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000123",
        "documentDescription" -> "ITSA- POA 1",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "items" -> Json.arr(
          Json.obj(
            "dueDate" -> dueDate
          )
        )
      )
    )
  )

  def testValidFinancialDetailsModelJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal,
                                         taxYear: String = "2018", dueDate: String = "2018-02-14"): JsValue = Json.obj(
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000123",
        "documentDescription" -> "ITSA- Bal Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29"
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000123",
        "documentDescription" -> "ITSA- POA 1",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29"
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000123",
        "documentDescription" -> "ITSA - POA 2",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "items" -> Json.arr(Json.obj("dueDate" -> dueDate))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "items" -> Json.arr(Json.obj("dueDate" -> dueDate))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "items" -> Json.arr(Json.obj("dueDate" -> dueDate))
      )
    )
  )

  def documentDetailJson(originalAmount: BigDecimal,
                         outstandingAmount: BigDecimal,
                         taxYear: String = "2018",
                         documentDescription: String = "ITSA- Bal Charge"): JsValue = Json.obj(
    "taxYear" -> taxYear,
    "transactionId" -> "1040000123",
    "documentDescription" -> documentDescription,
    "outstandingAmount" -> outstandingAmount,
    "originalAmount" -> originalAmount,
    "documentDate" -> "2018-03-29"
  )

  def financialDetailJson(taxYear: String = "2018",
                          mainType: String = "SA Balancing Charge",
                          dueDate: String = "2018-02-14"): JsValue = Json.obj(
    "taxYear" -> taxYear,
    "mainType" -> mainType,
    "items" -> Json.arr(
      Json.obj("dueDate" -> dueDate)
    )
  )

  def testFinancialDetailsErrorModelJson(status: String = "500"): JsValue = Json.obj(
    "code" -> status,
    "message" -> "ERROR MESSAGE"
  )

}
