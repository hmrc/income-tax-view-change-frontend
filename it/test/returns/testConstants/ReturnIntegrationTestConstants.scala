/*
 * Copyright 2026 HM Revenue & Customs
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

package returns.testConstants

import common.models.incomeSourceDetails.{IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import common.testConstants.BaseIntegrationTestConstants.*
import shared.enums.CodingOutType.CODING_OUT_CLASS2_NICS
import play.api.libs.json.{JsValue, Json}

object ReturnIntegrationTestConstants {

  private val poa1Description: String = "ITSA- POA 1"
  private val poa2Description: String = "ITSA- POA 2"
  val noDunningLock: List[String] = List("dunningLock", "dunningLock")
  val oneDunningLock: List[String] = List("Stand over order", "dunningLock")
  val twoDunningLocks: List[String] = List("Stand over order", "Stand over order")
  val noInterestLock: List[String] = List("Interest lock", "Interest lock")
  val twoInterestLocks: List[String] = List("Breathing Space Moratorium Act", "Manual RPI Signal")

  lazy val documentText = (isClass2Nic: Boolean, otherwise: String) => {
    if (isClass2Nic) {
      CODING_OUT_CLASS2_NICS.name
    } else {
      otherwise
    }
  }
  val singleBusinessResponseWoMigration: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1),
    properties = Nil,
    yearOfMigration = None
  )
  
  val businessAndPropertyResponseWoMigration: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1),
    properties = List(property),
    yearOfMigration = None
  )

  val multipleBusinessesAndPropertyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      business1,
      business2
    ),
    properties = List(property),
    yearOfMigration = Some("2018")
  )

  def testValidFinancialDetailsModelReviewAndReconcileDebitsJson(originalAmount: BigDecimal,
                                                                 outstandingAmount: BigDecimal,
                                                                 taxYear: String = "2018",
                                                                 dueDate: String = "2018-02-14"
                                                                ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA POA 1 Reconciliation Debit",
        "mainTransaction" -> "4911",
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
        "mainType" -> "SA POA 2 Reconciliation Debit",
        "mainTransaction" -> "4913",
        "transactionId" -> "1040000124",
        "chargeType" -> "ITSA NI",
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 9000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001")
        )
      )
    )
  )
  


  def testFinancialDetailsErrorModelJson(status: String = "500"): JsValue = Json.obj(
    "code" -> status,
    "message" -> "ERROR MESSAGE"
  )

  val multipleBusinessesAndPropertyResponseWoMigration: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      business1,
      business2
    ),
    properties = List(property),
    yearOfMigration = None
  )

  val noPropertyOrBusinessResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testNino,
    testMtditid, None,
    List(), Nil
  )

  val testEmptyFinancialDetailsModelJson: JsValue = Json.obj("balanceDetails" -> Json.obj(
    "balanceDueWithin30Days" -> 0.00,
    "overDueAmount" -> 0.00,
    "balanceNotDuein30Days" -> 0.00,
    "totalBalance" -> 0.00
  ), "codingDetails" -> Json.arr(), "documentDetails" -> Json.arr(), "financialDetails" -> Json.arr())

  def testValidFinancialDetailsModelJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                         dueDate: String = "2018-02-14", dunningLock: List[String] = noDunningLock,
                                         interestLocks: List[String] = noInterestLock,
                                         accruingInterestAmount: Option[BigDecimal] = Some(100),
                                         isClass2Nic: Boolean = false, poaRelevantAmount: Option[BigDecimal] = None
                                        ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "documentText" -> documentText(isClass2Nic, "TRM New Charge"),
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "interestRate" -> "3",
        "accruingInterestAmount" -> accruingInterestAmount,
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate,
        "poaRelevantAmount" -> poaRelevantAmount
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate,
        "poaRelevantAmount" -> poaRelevantAmount
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate,
        "poaRelevantAmount" -> poaRelevantAmount
      ),
      Json.obj(
        "taxYear" -> 9999,
        "transactionId" -> "PAYID01",
        "documentDescription" -> "TRM Amend Charge",
        "outstandingAmount" -> -outstandingAmount,
        "originalAmount" -> -originalAmount,
        "documentDate" -> "2018-03-29",
        "paymentLot" -> "081203010024",
        "paymentLotItem" -> "000001",
        "latePaymentInterestId" -> "latePaymentInterestId",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate,
        "poaRelevantAmount" -> poaRelevantAmount
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> "1040000123",
        "chargeType" -> "ITSA NI",
        "chargeReference" -> "ABCD1234",
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "mainTransaction" -> "4920",
        "transactionId" -> "1040000124",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> "ITSA NI",
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 9000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate),
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "mainTransaction" -> "4930",
        "transactionId" -> "1040000125",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> "ITSA NI",
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate),
          Json.obj(
            "interestLock" -> interestLocks(1),
            "dunningLock" -> dunningLock(1)
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> "PAYID01",
        "chargeType" -> "ITSA NI",
        "chargeReference" -> "ABCD1234",
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate))
      )
    )
  )
}
