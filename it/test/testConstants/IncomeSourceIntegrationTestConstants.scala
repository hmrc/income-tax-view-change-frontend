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

import enums.ChargeType.{ITSA_NI, NIC4_SCOTLAND}
import enums.CodingOutType._
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.IncomeSourceJourneyType
import models.UIJourneySessionData
import models.incomeSourceDetails._
import play.api.libs.json.{JsObject, JsValue, Json}
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants._
import testConstants.PaymentHistoryTestConstraints.oldBusiness1
import testConstants.PropertyDetailsIntegrationTestConstants._

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object IncomeSourceIntegrationTestConstants {

  val singleBusinessResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(business1),
    properties = Nil,
    yearOfMigration = Some("2018")
  )

  val singleBusinessResponse2: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(business1WithAddress2),
    properties = Nil,
    yearOfMigration = Some("2018")
  )

  val singleBusinessResponseManageYourDetailsAudit: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(businessWithLatencyForManageYourDetailsAudit),
    properties = Nil,
    yearOfMigration = Some("2018")
  )

  val singleUkPropertyResponseManageYourDetailsAudit: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = Nil,
    properties = List(ukPropertyAudit),
    yearOfMigration = Some("2018")
  )

  val singleForeignPropertyResponseManageYourDetailsAudit: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = Nil,
    properties = List(foreignPropertyAudit),
    yearOfMigration = Some("2018")
  )

  val singleBusinessResponseWoMigration: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(business1),
    properties = Nil,
    yearOfMigration = None
  )

  def singleBusinessResponseInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(business1.copy(latencyDetails = Some(latencyDetails))),
    properties = Nil,
    yearOfMigration = Some("2018")
  )

  def singleBusinessResponseInLatencyPeriod2(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(business1WithAddress2.copy(latencyDetails = Some(latencyDetails))),
    properties = Nil,
    yearOfMigration = Some("2018")
  )

  def singleBusinessResponseWithUnknownsInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(business3WithUnknowns.copy(latencyDetails = Some(latencyDetails))),
    properties = Nil,
    yearOfMigration = Some("2018")
  )

  def singleUKPropertyResponseInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1.copy(latencyDetails = Some(latencyDetails))),
    properties = List(ukProperty.copy(latencyDetails = Some(latencyDetails))),
    yearOfMigration = Some("2018")
  )

  def singleUKForeignPropertyResponseInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(businessWithId.copy(latencyDetails = Some(latencyDetails))),
    properties = List(ukProperty.copy(latencyDetails = Some(latencyDetails)), foreignProperty.copy(latencyDetails = Some(latencyDetails))),
    yearOfMigration = Some("2018")
  )

  def singleUKPropertyResponseWithUnknownsInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1.copy(latencyDetails = Some(latencyDetails))),
    properties = List(ukPropertyWithUnknowns.copy(latencyDetails = Some(latencyDetails))),
    yearOfMigration = Some("2018")
  )

  def singleForeignPropertyResponseInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1.copy(latencyDetails = Some(latencyDetails))),
    properties = List(foreignProperty.copy(latencyDetails = Some(latencyDetails))),
    yearOfMigration = Some("2018")
  )

  def singleForeignPropertyResponseWithUnknownsInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1.copy(latencyDetails = Some(latencyDetails))),
    properties = List(foreignPropertyWithUnknowns.copy(latencyDetails = Some(latencyDetails))),
    yearOfMigration = Some("2018")
  )

  def allBusinessesAndPropertiesInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1.copy(latencyDetails = Some(latencyDetails)), business2.copy(latencyDetails = Some(latencyDetails))),
    properties = List(ukProperty.copy(latencyDetails = Some(latencyDetails)), foreignProperty.copy(latencyDetails = Some(latencyDetails))),
    yearOfMigration = Some("2018")
  )

  val misalignedBusinessWithPropertyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(business2),
    properties = List(property),
    yearOfMigration = Some("2018")
  )

  val multipleBusinessesResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtdItId,
    businesses = List(
      business1,
      business2
    ),
    properties = Nil,
    yearOfMigration = Some("2019")
  )

  val multiplePropertiesResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = Nil,
    properties = List(
      property,
      oldProperty
    ),
    yearOfMigration = Some("2019")
  )

  val businessAndPropertyResponse: IncomeSourceDetailsModel =
    IncomeSourceDetailsModel(
      testNino,
      testMtdItId,
      businesses = List(business1),
      properties = List(property),
      yearOfMigration = Some("2018")
    )

  val businessAndPropertyResponseWoMigration: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(business1),
    properties = List(property),
    yearOfMigration = None
  )

  val paymentHistoryBusinessAndPropertyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    Some(getCurrentTaxYearEnd.minusYears(1).getYear.toString),
    businesses = List(oldBusiness1),
    properties = List(oldProperty)
  )

  val multipleBusinessesAndPropertyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(
      business1,
      business2
    ),
    properties = List(property),
    yearOfMigration = Some("2018")
  )

  val multipleBusinessesAndUkProperty: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(
      business1,
      business2
    ),
    properties = List(ukProperty),
    yearOfMigration = Some("2018")
  )

  val multipleBusinessesWithBothPropertiesAndCeasedBusiness: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(
      business1,
      business2,
      business3
    ),
    properties = List(ukProperty, foreignProperty),
    yearOfMigration = Some("2018")
  )

  val propertyOnlyBusiness: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(),
    properties = List(ukProperty, foreignProperty),
    yearOfMigration = Some("2018")
  )

  val foreignPropertyAndCeasedBusiness: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(
      ceasedBusiness1
    ),
    properties = List(foreignProperty),
    yearOfMigration = Some("2018")
  )

  val allCeasedBusinesses: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(
      ceasedBusiness1
    ),
    properties = List(),
    yearOfMigration = Some("2018")
  )

  val foreignAndSoleTraderCeasedBusiness: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(ceasedBusiness1),
    properties = List(ceasedForeignProperty),
    yearOfMigration = Some("2018")
  )

  val businessWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(
      businessWithLatencyForManageYourDetailsAudit
    ),
    properties = List(foreignProperty),
    yearOfMigration = Some("2018")
  )

  val propertyWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(business1),
    properties = List(foreignPropertyAudit),
    yearOfMigration = Some("2018")
  )

  val allBusinessesWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(businessWithLatencyForManageYourDetailsAudit),
    properties = List(foreignPropertyAudit),
    yearOfMigration = Some("2018")
  )

  val multipleBusinessesAndPropertyResponseWoMigration: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(
      business1,
      business2
    ),
    properties = List(property),
    yearOfMigration = None
  )

  val businessOnlyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(
      business1
    ),
    properties = List(),
    yearOfMigration = Some("2018")
  )

  val businessOnlyResponseWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(
      business1.copy(latencyDetails = Some(testLatencyDetails3))
    ),
    properties = List(),
    yearOfMigration = Some("2018")
  )

  val businessOnlyResponseAllCeased: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(
      ceasedBusiness1
    ),
    properties = List(),
    yearOfMigration = Some("2018")
  )

  val propertyOnlyResponse: IncomeSourceDetailsModel =
    IncomeSourceDetailsModel(
      testNino,
      testMtdItId,
      businesses = List(),
      properties = List(property),
      yearOfMigration = Some("2018")
    )

  val ukPropertyOnlyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(),
    properties = List(ukProperty),
    yearOfMigration = Some("2018")
  )

  val ukPropertyOnlyResponseWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(),
    properties = List(ukProperty.copy(latencyDetails = Some(testLatencyDetails3))),
    yearOfMigration = Some("2018")
  )

  val ukPropertyOnlyResponseAllCeased: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(),
    properties = List(ceasedUkProperty),
    yearOfMigration = Some("2018")
  )

  val foreignPropertyOnlyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(),
    properties = List(foreignProperty),
    yearOfMigration = Some("2018")
  )

  val foreignPropertyOnlyResponseWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(),
    properties = List(foreignProperty.copy(latencyDetails = Some(testLatencyDetails3))),
    yearOfMigration = Some("2018")
  )

  val foreignPropertyOnlyResponseAllCeased: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(),
    properties = List(ceasedForeignProperty),
    yearOfMigration = Some("2018")
  )

  val noPropertyOrBusinessResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testNino,
    testMtdItId, None,
    List(), Nil
  )
  val errorResponse: IncomeSourceDetailsError = IncomeSourceDetailsError(500, "ISE")
  val testEmptyFinancialDetailsModelJson: JsValue = Json.obj("balanceDetails" -> Json.obj(
    "balanceDueWithin30Days" -> 0.00,
    "overDueAmount" -> 0.00,
    "totalBalance" -> 0.00
  ), "codingDetails" -> Json.arr(), "documentDetails" -> Json.arr(), "financialDetails" -> Json.arr())

  def propertyOnlyResponseWithMigrationData(year: Int,
                                            yearOfMigration: Option[String]): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(),
    properties = List(propertyWithCurrentYear(year)),
    yearOfMigration = yearOfMigration
  )

  def testValidFinancialDetailsModelJsonSingleCharge(originalAmount: BigDecimal, outstandingAmount: BigDecimal,
                                                     taxYear: String = "2018", dueDate: String = "2018-02-14"): JsValue = Json.obj(
    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "transactionId" -> "1040000123",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "mainTransaction" -> "4920",
        "items" -> Json.arr(
          Json.obj(
            "dueDate" -> dueDate
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

  val documentText = (isClass2Nic: Boolean, otherwise: String) => {
    if (isClass2Nic) {
      CODING_OUT_CLASS2_NICS.name
    } else {
      otherwise
    }
  }

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  private def documentDetailsPayment(transactionId: String,
                                     amount: BigDecimal,
                                     clearingDate: LocalDate): JsObject = {
    Json.obj(
      "taxYear" -> testTaxYear,
      "transactionId" -> transactionId,
      "documentDescription" -> "Payment on Account",
      "outstandingAmount" -> amount,
      "originalAmount" -> amount,
      "documentDueDate" -> dateFormatter.format(clearingDate),
      "effectiveDateOfPayment" -> dateFormatter.format(clearingDate),
      "documentDate" -> dateFormatter.format(clearingDate),
      "paymentLot" -> "081203010025",
      "paymentLotItem" -> "000001",
    )
  }

  private def financialDetailsPayment(transactionId: String,
                                      amount: BigDecimal,
                                      clearingDate: LocalDate,
                                      clearingSapDocument: String): JsObject = {
    Json.obj(
      "taxYear" -> s"$testTaxYear",
      "mainType" -> "Payment on Account",
      "mainTransaction" -> "0060",
      "transactionId" -> transactionId,
      "chargeReference" -> "ABCD1234",
      "originalAmount" -> amount,
      "items" -> Json.arr(
        Json.obj(
          "subItemId" -> "001",
          "amount" -> amount,
          "clearingDate" -> dateFormatter.format(clearingDate),
          "dueDate" -> dateFormatter.format(clearingDate)),
        "paymentAmount" -> amount,
        "paymentReference" -> "GF235687",
        "paymentMethod" -> "Payment",
        "clearingSAPDocument" -> clearingSapDocument
      )
    )
  }

  def testValidFinancialDetailsModelJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                         dueDate: String = "2018-02-14", dunningLock: List[String] = noDunningLock,
                                         interestLocks: List[String] = noInterestLock,
                                         accruingInterestAmount: Option[BigDecimal] = Some(100),
                                         isClass2Nic: Boolean = false, poaRelevantAmount: Option[BigDecimal] = None
                                        ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
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
        "chargeType" -> ITSA_NI,
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
        "chargeType" -> ITSA_NI,
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
        "chargeType" -> ITSA_NI,
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
        "chargeType" -> ITSA_NI,
        "chargeReference" -> "ABCD1234",
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate))
      )
    )
  )

  def testValidFinancialDetailsModelWithPaymentAllocationJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                                              dueDate: String = "2018-02-14", dunningLock: List[String] = noDunningLock,
                                                              interestLocks: List[String] = noInterestLock,
                                                              accruingInterestAmount: Option[BigDecimal] = Some(100),
                                                              isClass2Nic: Boolean = false
                                                             ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
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
        "accruingInterestAmount" -> accruingInterestAmount,
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
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
        "documentDueDate" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "9999",
        "mainType" -> "Payment On Account",
        "transactionId" -> "PAYID01",
        "outstandingAmount" -> -outstandingAmount,
        "originalAmount" -> -originalAmount,
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("amount" -> -10000,
            "subItemId" -> "001",
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "clearingSAPDocument" -> "012345678912",
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> "1040000123",
        "mainTransaction" -> "4910",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingSAPDocument" -> "012345678912",
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "transactionId" -> "1040000124",
        "mainTransaction" -> "4920",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "chargeReference" -> "chargeRef",
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
        "transactionId" -> "1040000125",
        "mainTransaction" -> "4930",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate),
          Json.obj(
            "interestLock" -> interestLocks(1),
            "dunningLock" -> dunningLock(1)
          ))
      )
    )
  )

  def testValidFinancialDetailsModelJsonAccruingInterest(originalAmount: BigDecimal, outstandingAmount: BigDecimal,
                                                         taxYear: String = "2018", dueDate: String = "2018-04-14",
                                                         accruingInterestAmount: Option[BigDecimal] = Some(0)): JsValue = Json.obj(
    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-04-14",
        "interestEndDate" -> "2019-01-01",
        "accruingInterestAmount" -> accruingInterestAmount,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> "2019-01-01",
        "accruingInterestAmount" -> accruingInterestAmount,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> "2019-01-01",
        "accruingInterestAmount" -> accruingInterestAmount,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> "1040000123",
        "chargeReference" -> "chargeRef",
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
        "mainTransaction" -> "4920",
        "transactionId" -> "1040000124",
        "chargeReference" -> "chargeRef",
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
        "mainTransaction" -> "4930",
        "transactionId" -> "1040000125",
        "chargeReference" -> "chargeRef",
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
                                            accruingInterestAmount: BigDecimal = 0, interestDueDate: String = "2019-01-01"): JsValue = Json.obj(
    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-04-14",
        "interestEndDate" -> interestDueDate,
        "accruingInterestAmount" -> accruingInterestAmount,
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> interestDueDate,
        "accruingInterestAmount" -> accruingInterestAmount,
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> interestDueDate,
        "accruingInterestAmount" -> accruingInterestAmount,
        "effectiveDateOfPayment" -> dueDate
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
        "mainTransaction" -> "4920",
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
        "mainTransaction" -> "4930",
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

  def testFinancialDetailsModelWithMissingOriginalAmountJson(): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2018".toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "documentText" -> documentText(false, "TRM New Charge"),
        "outstandingAmount" -> 1.2,
        "originalAmount-added-this-text-to-exclude-it" -> 10.34,
        "documentDate" -> "2018-03-29",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> Some(100),
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> "2018-02-14",
        "documentDueDate" -> "2018-02-14"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2018",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> "1040000123",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> 10.34,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> "2018-02-14",
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      )
    )
  )

  def testValidFinancialDetailsModelJsonCodingOut(originalAmount: BigDecimal, outstandingAmount: BigDecimal,
                                                  taxYear: String = "2018", dueDate: String = "2018-04-14",
                                                  accruingInterestAmount: BigDecimal = 0,
                                                  payeSaTaxYear: String = "2018", totalLiabilityAmount: BigDecimal = 0): JsValue = Json.obj(
    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
    "codingDetails" -> Json.arr(Json.obj(
      "totalLiabilityAmount" -> totalLiabilityAmount,
      "taxYearReturn" -> taxYear
    )),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "documentText" -> CODING_OUT_CLASS2_NICS,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> payeSaTaxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> "TRM New Charge",
        "documentText" -> CODING_OUT_ACCEPTED,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "amountCodedOut" -> totalLiabilityAmount,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> "TRM Amend Charge",
        "documentText" -> CODING_OUT_CANCELLED,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000126",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> "2019-01-01",
        "accruingInterestAmount" -> accruingInterestAmount,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
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
        "mainTransaction" -> "4910",
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
        "mainTransaction" -> "4910",
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
        "mainTransaction" -> "4930",
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
                         taxYear: Int = 2018,
                         documentDescription: String = "TRM New Charge",
                         transactionId: String = "1040000123",
                         dueDate: String = "2018-02-14"): JsValue = Json.obj(
    "taxYear" -> taxYear,
    "transactionId" -> transactionId,
    "documentDescription" -> documentDescription,
    "outstandingAmount" -> outstandingAmount,
    "originalAmount" -> originalAmount,
    "documentDate" -> "2018-03-29",
    "effectiveDateOfPayment" -> dueDate,
    "documentDueDate" -> dueDate
  )

  def financialDetailJson(taxYear: String = "2018",
                          mainType: String = "SA Balancing Charge",
                          mainTransaction: String = "4910",
                          dueDate: String = "2018-02-14",
                          transactionId: String = "1040000123"): JsValue = Json.obj(
    "taxYear" -> taxYear,
    "mainType" -> mainType,
    "mainTransaction" -> mainTransaction,
    "transactionId" -> transactionId,
    "items" -> Json.arr(
      Json.obj("dueDate" -> dueDate)
    )
  )

  def testAuditFinancialDetailsModelJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                         dueDate: String = "2018-04-14", dunningLock: List[String] = noDunningLock,
                                         interestLocks: List[String] = noInterestLock, totalAmount: BigDecimal = 100,
                                         accruingInterestAmount: Option[BigDecimal] = Some(100.0)): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
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
        "interestFromDate" -> "2018-04-14",
        "interestEndDate" -> "2019-01-01",
        "accruingInterestAmount" -> accruingInterestAmount,
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> 9999,
        "transactionId" -> "PAYID01",
        "documentDescription" -> "Payment On Account",
        "outstandingAmount" -> -outstandingAmount,
        "originalAmount" -> -originalAmount,
        "documentDate" -> "2018-03-29",
        "paymentLot" -> "081203010024",
        "paymentLotItem" -> "000001",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "9999",
        "mainType" -> "Payment On Account",
        "mainTransaction" -> "0060",
        "chargeReference" -> "ABCD1234",
        "transactionId" -> "PAYID01",
        "outstandingAmount" -> -outstandingAmount,
        "originalAmount" -> -originalAmount,
        "totalAmount" -> totalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> -10000,
            "subItemId" -> "001",
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "clearingSAPDocument" -> "012345678912",
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> "1040000123",
        "mainTransaction" -> "4910",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "totalAmount" -> totalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "clearingSAPDocument" -> "012345678912"
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "mainTransaction" -> "4920",
        "transactionId" -> "1040000123",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> NIC4_SCOTLAND,
        "originalAmount" -> originalAmount,
        "totalAmount" -> totalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 9000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "clearingSAPDocument" -> "012345678912"
          ),
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "mainTransaction" -> "4930",
        "transactionId" -> "1040000123",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> NIC4_SCOTLAND,
        "originalAmount" -> originalAmount,
        "totalAmount" -> totalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "clearingSAPDocument" -> "012345678912"
          ),
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

  private val poa1Description: String = "ITSA- POA 1"
  private val poa2Description: String = "ITSA - POA 2"

  def testChargeHistoryJson(mtdBsa: String, documentId: String, amount: BigDecimal): JsValue = Json.obj(
    "idType" -> "MTDBSA",
    "idValue" -> mtdBsa,
    "regimeType" -> "ITSA",
    "chargeHistoryDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "documentId" -> documentId,
        "documentDate" -> "2018-02-14",
        "documentDescription" -> poa1Description,
        "totalAmount" -> amount,
        "reversalDate" -> "2019-02-14T09:30:45Z",
        "reversalReason" -> "Customer Request",
        "poaAdjustmentReason" -> "002"
      )
    )
  )

  def testValidFinancialDetailsModelCreditAndRefundsJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                                         dueDate: String = "2018-02-14", dunningLock: List[String] = noDunningLock,
                                                         interestLocks: List[String] = noInterestLock,
                                                         accruingInterestAmount: Option[BigDecimal] = Some(100)
                                                        ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "totalBalance" -> 3.00,
      "totalCreditAvailableForRepayment" -> 5.00,
      "firstPendingAmountRequested" -> 3.00,
      "secondPendingAmountRequested" -> 2.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      documentDetailsPayment("PAYID01", -500.0, LocalDate.of(taxYear.toInt, 3, 29)),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> accruingInterestAmount,
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000126",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000127",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000129",
        "documentDescription" -> "SA Repayment Supplement Credit",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      financialDetailsPayment("PAYID01", -500.0, LocalDate.of(taxYear.toInt, 3, 29), "123456"),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Cutover Credits",
        "mainTransaction" -> "6110",
        "transactionId" -> "1040000123",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> ITSA_NI,
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
        "mainType" -> "ITSA Cutover Credits",
        "mainTransaction" -> "6110",
        "transactionId" -> "1040000124",
        "chargeReference" -> "ABCD1235",
        "chargeType" -> ITSA_NI,
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
        "mainType" -> "ITSA Cutover Credits",
        "mainTransaction" -> "6110",
        "transactionId" -> "1040000125",
        "chargeReference" -> "ABCD1236",
        "chargeType" -> ITSA_NI,
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
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Overpayment Relief",
        "mainTransaction" -> "4004",
        "transactionId" -> "1040000126",
        "chargeReference" -> "ABCD1237",
        "chargeType" -> ITSA_NI,
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
        "mainType" -> "SA Balancing Charge Credit",
        "mainTransaction" -> "4905",
        "transactionId" -> "1040000127",
        "chargeReference" -> "ABCD1238",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Repayment Supplement Credit",
        "mainTransaction" -> "6020",
        "transactionId" -> "1040000129",
        "chargeReference" -> "ABCD1239",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 2000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate),
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
    )
  )

  def testValidFinancialDetailsModelCreditAndRefundsJsonV2(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                                           dueDate: String = "2018-02-14", dunningLock: List[String] = noDunningLock,
                                                           interestLocks: List[String] = noInterestLock,
                                                           accruingInterestAmount: Option[BigDecimal] = Some(100)
                                                          ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "totalBalance" -> 3.00,
      "totalCreditAvailableForRepayment" -> 5.00,
      "firstPendingAmountRequested" -> 3.00,
      "secondPendingAmountRequested" -> 2.00
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
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> accruingInterestAmount,
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000126",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000127",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Cutover Credits",
        "mainTransaction" -> "6110",
        "transactionId" -> "1040000123",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> ITSA_NI,
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
        "mainType" -> "ITSA Cutover Credits",
        "mainTransaction" -> "6110",
        "transactionId" -> "1040000124",
        "chargeReference" -> "ABCD1235",
        "chargeType" -> ITSA_NI,
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
        "mainType" -> "ITSA Cutover Credits",
        "mainTransaction" -> "6110",
        "transactionId" -> "1040000125",
        "chargeReference" -> "ABCD1236",
        "chargeType" -> ITSA_NI,
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
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge Credit",
        "mainTransaction" -> "4905",
        "transactionId" -> "1040000126",
        "chargeReference" -> "ABCD1237",
        "chargeType" -> ITSA_NI,
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
        "mainType" -> "SA Balancing Charge Credit",
        "mainTransaction" -> "4905",
        "transactionId" -> "1040000127",
        "chargeReference" -> "ABCD1238",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      )
    )
  )

  def testValidFinancialDetailsModelMFADebitsJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                                  dueDate: String = "2018-02-14", dunningLock: List[String] = noDunningLock,
                                                  interestLocks: List[String] = noInterestLock,
                                                  accruingInterestAmount: Option[BigDecimal] = Some(100)
                                                 ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
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
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000126",
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
        "mainType" -> "ITSA PAYE Charge",
        "mainTransaction" -> "4000",
        "transactionId" -> "1040000123",
        "chargeType" -> ITSA_NI,
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
        "mainType" -> "ITSA Calc Error Correction",
        "mainTransaction" -> "4001",
        "transactionId" -> "1040000124",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 9000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001")
        )
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Manual Penalty Pre CY-4",
        "mainTransaction" -> "4002",
        "transactionId" -> "1040000125",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001")
        )
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Misc Charge",
        "mainTransaction" -> "4003",
        "transactionId" -> "1040000126",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001")
        )
      )
    )
  )

  def testValidFinancialDetailsModelReviewAndReconcileDebitsJson(
                                                                  originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                                                  dueDate: String = "2018-02-14", dunningLock: List[String] = noDunningLock,
                                                                  interestLocks: List[String] = noInterestLock,
                                                                  accruingInterestAmount: Option[BigDecimal] = Some(100)
                                                                ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
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
        "chargeType" -> ITSA_NI,
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
        "chargeType" -> ITSA_NI,
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


  val businessOnlyResponseWithUnknownAddressName: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testNino,
    testMtdItId,
    businesses = List(
      businessUnknownAddressName
    ),
    properties = List(),
    yearOfMigration = Some("2018")
  )

  lazy val completedUIJourneySessionData: IncomeSourceJourneyType => UIJourneySessionData = (incomeSources: IncomeSourceJourneyType) => {
    incomeSources.operation.operationType match {
      case "ADD" => UIJourneySessionData(testSessionId, incomeSources.toString,
        addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceCreatedJourneyComplete = Some(true))))
      case "MANAGE" => if (incomeSources.businessType == SelfEmployment) UIJourneySessionData(testSessionId, incomeSources.toString,
        manageIncomeSourceData = Some(ManageIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId),
          taxYear = Some(2024), reportingMethod = Some("annual"), journeyIsComplete = Some(true))))
      else UIJourneySessionData(testSessionId, incomeSources.toString,
        manageIncomeSourceData = Some(ManageIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId),
          taxYear = Some(2024), reportingMethod = Some("annual"), journeyIsComplete = Some(true))))
      case "CEASE" => UIJourneySessionData(testSessionId, incomeSources.toString,
        ceaseIncomeSourceData = Some(CeaseIncomeSourceData(journeyIsComplete = Some(true))))
    }
  }

  val emptyUIJourneySessionData: IncomeSourceJourneyType => UIJourneySessionData = incomeSources => {
    incomeSources.operation.operationType match {
      case "ADD" =>
        UIJourneySessionData(
          sessionId = testSessionId,
          journeyType = incomeSources.toString,
          addIncomeSourceData = Some(AddIncomeSourceData())
        )
      case "MANAGE" =>
        UIJourneySessionData(
          sessionId = testSessionId,
          journeyType = incomeSources.toString,
          manageIncomeSourceData = Some(ManageIncomeSourceData())
        )
      case "CEASE" =>
        UIJourneySessionData(
          sessionId = testSessionId,
          journeyType = incomeSources.toString,
          ceaseIncomeSourceData = Some(CeaseIncomeSourceData())
        )
    }
  }
}


