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

package testConstants

import BusinessDetailsIntegrationTestConstants._
import PaymentHistoryTestConstraints.oldBusiness1
import PropertyDetailsIntegrationTestConstants._
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.libs.json.{JsValue, Json}
import testConstants.BaseIntegrationTestConstants.getCurrentTaxYearEnd

object IncomeSourceIntegrationTestConstants {

  val singleBusinessResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(business1),
    property = None,
    yearOfMigration = Some("2018")
  )

  val singleBusinessResponseWoMigration: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(business1),
    property = None,
    yearOfMigration = None
  )

  val misalignedBusinessWithPropertyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(business2),
    property = Some(property),
    yearOfMigration = Some("2018")
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
    yearOfMigration = Some("2018")
  )

  val businessAndPropertyResponseWoMigration: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(business1),
    property = Some(property),
    yearOfMigration = None
  )

  val paymentHistoryBusinessAndPropertyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testMtdItId,
    Some(getCurrentTaxYearEnd.minusYears(1).getYear.toString),
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
    yearOfMigration = Some("2018")
  )

  val multipleBusinessesAndPropertyResponseWoMigration: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
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
  val testEmptyFinancialDetailsModelJson: JsValue = Json.obj("balanceDetails" -> Json.obj(
    "balanceDueWithin30Days" -> 0.00,
    "overDueAmount" -> 0.00,
    "totalBalance" -> 0.00
  ), "documentDetails" -> Json.arr(), "financialDetails" -> Json.arr())

  def propertyOnlyResponseWithMigrationData(year: Int,
                                            yearOfMigration: Option[String]): IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testMtdItId,
    businesses = List(),
    property = Some(propertyWithCurrentYear(year)),
    yearOfMigration = yearOfMigration
  )

  def testValidFinancialDetailsModelJsonSingleCharge(originalAmount: BigDecimal, outstandingAmount: BigDecimal,
                                                     taxYear: String = "2018", dueDate: String = "2018-02-14"): JsValue = Json.obj(
    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
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
            "dueDate" -> dueDate,
          )
        )
      )
    )
  )

  val noDunningLock: List[String] = List("dunningLock", "dunningLock")
  val oneDunningLock: List[String] = List("Stand over order", "dunningLock")
  val twoDunningLocks: List[String] = List("Stand over order", "Stand over order")
  val noInterestLock: List[String] = List("Interest lock", "Interest lock")
  val twoInterestLocks: List[String] = List("Breathing Space Moratorium Act", "Manual RPI Signal")

  val id1040000123 = "1040000123"

  def testValidFinancialDetailsModelJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                         dueDate: String = "2018-02-14", dunningLock: List[String] = noDunningLock,
                                         interestLocks: List[String] = noInterestLock): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "totalBalance" -> 3.00
    ),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "latePaymentInterestAmount" -> 100.0,
        "interestOutstandingAmount" -> 80.0
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000124",
        "documentDescription" -> "ITSA- POA 1",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29"
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000125",
        "documentDescription" -> "ITSA - POA 2",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29"
      ),
      Json.obj(
        "taxYear" -> "9999",
        "transactionId" -> "PAYID01",
        "documentDescription" -> "TRM Amend Charge",
        "outstandingAmount" -> -outstandingAmount,
        "originalAmount" -> -originalAmount,
        "documentDate" -> "2018-03-29",
        "paymentLot" -> "081203010024",
        "paymentLotItem" -> "000001",
        "latePaymentInterestId" -> "latePaymentInterestId"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> "1040000123",
        "chargeType" -> "ITSA NI",
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "transactionId" -> "1040000124",
        "chargeType" -> "ITSA NI",
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 9000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"),
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "transactionId" -> "1040000125",
        "chargeType" -> "ITSA NI",
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"),
          Json.obj(
            "interestLock" -> interestLocks(1),
            "dunningLock" -> dunningLock(1)
          ))
      )
    )
  )

  def testValidFinancialDetailsModelJsonAccruingInterest(originalAmount: BigDecimal, outstandingAmount: BigDecimal,
                                                         taxYear: String = "2018", dueDate: String = "2018-04-14",
                                                         latePaymentInterestAmount: BigDecimal = 0): JsValue = Json.obj(
    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-04-14",
        "interestEndDate" -> "2019-01-01",
        "latePaymentInterestAmount" -> latePaymentInterestAmount
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000124",
        "documentDescription" -> "ITSA- POA 1",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> "2019-01-01",
        "latePaymentInterestAmount" -> latePaymentInterestAmount
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000125",
        "documentDescription" -> "ITSA - POA 2",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> "2019-01-01",
        "latePaymentInterestAmount" -> latePaymentInterestAmount
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> "1040000123",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "transactionId" -> "1040000124",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "transactionId" -> "1040000125",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      )
    )
  )

  def testValidFinancialDetailsModelJsonLPI(originalAmount: BigDecimal, outstandingAmount: BigDecimal,
                                            taxYear: String = "2018", dueDate: String = "2018-04-14",
                                            latePaymentInterestAmount: BigDecimal = 0, interestDueDate: String = "2019-01-01"): JsValue = Json.obj(
    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-04-14",
        "interestEndDate" -> interestDueDate,
        "latePaymentInterestAmount" -> latePaymentInterestAmount
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000124",
        "documentDescription" -> "ITSA- POA 1",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> interestDueDate,
        "latePaymentInterestAmount" -> latePaymentInterestAmount
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000125",
        "documentDescription" -> "ITSA - POA 2",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> interestDueDate,
        "latePaymentInterestAmount" -> latePaymentInterestAmount
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> "1040000123",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "transactionId" -> "1040000124",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "transactionId" -> "1040000125",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      )
    )
  )

  def testValidFinancialDetailsModelJsonCodingOut(originalAmount: BigDecimal, outstandingAmount: BigDecimal,
                                                  taxYear: String = "2018", dueDate: String = "2018-04-14",
                                                  latePaymentInterestAmount: BigDecimal = 0,
                                                  payeSaTaxYear: String = "2018", amountCodedOut: BigDecimal = 0): JsValue = Json.obj(
    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
    "codingDetails" -> Json.arr(
      Json.obj(
        "taxYearReturn" -> payeSaTaxYear,
        "taxYearCoding" -> (payeSaTaxYear.toInt - 1).toString,
        "amountCodedOut" -> amountCodedOut
      )
    ),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "documentText" -> "Class 2 National Insurance",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29"
      ),
      Json.obj(
        "taxYear" -> payeSaTaxYear,
        "transactionId" -> "1040000124",
        "documentDescription" -> "TRM New Charge",
        "documentText" -> "PAYE Self Assessment",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29"
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000125",
        "documentDescription" -> "TRM Amend Charge",
        "documentText" -> "Cancelled PAYE Self Assessment",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29"
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000126",
        "documentDescription" -> "ITSA - POA 2",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> "2019-01-01",
        "latePaymentInterestAmount" -> latePaymentInterestAmount
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> "1040000123",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> "1040000124",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001",
            "dunningLock" -> "Coded Out"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> "1040000125",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001",
            "dunningLock" -> "Coded Out"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "transactionId" -> "1040000126",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      )
    )
  )

  def documentDetailJson(originalAmount: BigDecimal,
                         outstandingAmount: BigDecimal,
                         taxYear: String = "2018",
                         documentDescription: String = "TRM New Charge",
                         transactionId: String = "1040000123"): JsValue = Json.obj(
    "taxYear" -> taxYear,
    "transactionId" -> transactionId,
    "documentDescription" -> documentDescription,
    "outstandingAmount" -> outstandingAmount,
    "originalAmount" -> originalAmount,
    "documentDate" -> "2018-03-29"
  )

  def financialDetailJson(taxYear: String = "2018",
                          mainType: String = "SA Balancing Charge",
                          dueDate: String = "2018-02-14",
                          transactionId: String = "1040000123"): JsValue = Json.obj(
    "taxYear" -> taxYear,
    "mainType" -> mainType,
    "transactionId" -> transactionId,
    "items" -> Json.arr(
      Json.obj("dueDate" -> dueDate)
    )
  )

  def testAuditFinancialDetailsModelJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                         dueDate: String = "2018-04-14", dunningLock: List[String] = noDunningLock,
                                         interestLocks: List[String] = noInterestLock, totalAmount: BigDecimal = 100): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "totalBalance" -> 3.00
    ),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestFromDate" -> "2018-04-14",
        "interestEndDate" -> "2019-01-01",
        "latePaymentInterestAmount" -> 100.0,
        "interestOutstandingAmount" -> 80.0
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000124",
        "documentDescription" -> "ITSA- POA 1",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29"
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000125",
        "documentDescription" -> "ITSA - POA 2",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29"
      ),
      Json.obj(
        "taxYear" -> "9999",
        "transactionId" -> "PAYID01",
        "documentDescription" -> "Payment On Account",
        "outstandingAmount" -> -outstandingAmount,
        "originalAmount" -> -originalAmount,
        "documentDate" -> "2018-03-29",
        "paymentLot" -> "081203010024",
        "paymentLotItem" -> "000001"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> "1040000123",
        "chargeType" -> "ITSA NI",
        "originalAmount" -> originalAmount,
        "totalAmount" -> totalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "transactionId" -> "1040000123",
        "chargeType" -> "NIC4 Scotland",
        "originalAmount" -> originalAmount,
        "totalAmount" -> totalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 9000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"),
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "transactionId" -> "1040000123",
        "chargeType" -> "NIC4 Scotland",
        "originalAmount" -> originalAmount,
        "totalAmount" -> totalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"),
          Json.obj(
            "interestLock" -> interestLocks(1),
            "dunningLock" -> dunningLock(1)
          ))
      )
    )
  )

  def testFinancialDetailsErrorModelJson(status: String = "500"): JsValue = Json.obj(
    "code" -> status,
    "message" -> "ERROR MESSAGE"
  )

  def testChargeHistoryJson(mtdBsa: String, documentId: String, amount: BigDecimal): JsValue = Json.obj(
    "idType" -> "MTDBSA",
    "idValue" -> mtdBsa,
    "regimeType" -> "ITSA",
    "chargeHistoryDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "documentId" -> documentId,
        "documentDate" -> "2018-02-14",
        "documentDescription" -> "ITSA- POA 1",
        "totalAmount" -> amount,
        "reversalDate" -> "2019-02-14",
        "reversalReason" -> "Customer Request"
      )
    )
  )
}
