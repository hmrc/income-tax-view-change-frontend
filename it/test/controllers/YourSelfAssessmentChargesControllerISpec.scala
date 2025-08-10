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

package controllers

import audit.models.WhatYouOweResponseAuditModel
import auth.MtdItUser
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.admin._
import models.core.SelfServeTimeToPayJourneyResponseModel
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import services.DateServiceInterface
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSaUtr}
import testConstants.ChargeConstants
import testConstants.FinancialDetailsIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.OutstandingChargesIntegrationTestConstants._
import testConstants.messages.WhatYouOweMessages.hmrcAdjustment

import java.time.LocalDate
import java.time.Month.APRIL

class YourSelfAssessmentChargesControllerISpec extends ControllerISpecHelper with ChargeConstants with TransactionUtils {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  def testUser(mtdUserRole: MTDUserRole): MtdItUser[_] = getTestUser(mtdUserRole, paymentHistoryBusinessAndPropertyResponse)

  val testTaxYear: Int = getCurrentTaxYearEnd.getYear - 1
  val testTaxYearPoa: Int = getCurrentTaxYearEnd.getYear

  val testDate: LocalDate = LocalDate.parse("2022-01-01")

  val configuredChargeItemGetter: List[FinancialDetail] => DocumentDetail => Option[ChargeItem] =
    getChargeItemOpt

  val testValidOutStandingChargeResponseJsonWithAciAndBcdCharges: JsValue = Json.parse(
    s"""
       |{
       |  "outstandingCharges": [{
       |         "chargeName": "BCD",
       |         "relevantDueDate": "$testDate",
       |         "chargeAmount": 123456789012345.67,
       |         "tieBreaker": 1234
       |       },
       |       {
       |         "chargeName": "ACI",
       |         "relevantDueDate": "$testDate",
       |         "chargeAmount": 12.67,
       |         "tieBreaker": 1234
       |       }
       |  ]
       |}
       |""".stripMargin)

  val testDateService: DateServiceInterface = new DateServiceInterface {
    override def getCurrentDate: LocalDate = LocalDate.of(2023, 4, 5)

    override protected def now(): LocalDate = LocalDate.of(2023, 4, 5)


    override def getCurrentTaxYearEnd: Int = 2022

    override def isBeforeLastDayOfTaxYear: Boolean = false

    override def getCurrentTaxYearStart: LocalDate = LocalDate.of(2022, 4, 6)

    override def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate = {
      val startDateYear = startDate.getYear
      val accountingPeriodEndDate = LocalDate.of(startDateYear, APRIL, 5)

      if (startDate.isBefore(accountingPeriodEndDate) || startDate.isEqual(accountingPeriodEndDate)) {
        accountingPeriodEndDate
      } else {
        accountingPeriodEndDate.plusYears(1)
      }
    }

    override def isAfterTaxReturnDeadlineButBeforeTaxYearEnd: Boolean = false

    override def getCurrentTaxYear: TaxYear = TaxYear.forYearEnd(getCurrentTaxYearEnd)

    override def isWithin30Days(date: LocalDate): Boolean = {
      val currentDate = getCurrentDate
      date.minusDays(30).isBefore(currentDate)
    }
  }

  def getPath(mtdRole: MTDUserRole): String = {
    if (mtdRole == MTDIndividual) {
      "/your-self-assessment-charges"
    } else {
      "/agents/your-self-assessment-charges"
    }
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path, additionalCookies)
          } else {
            "render the your self assessment charges page" which {
              "displays the payments due totals" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
                  propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                  testValidFinancialDetailsModelJsonCodingOut(2000, 2000, (testTaxYear - 1).toString, testDate.plusYears(1).toString))
                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)
                IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))
                val res = buildGETMTDClient(path, additionalCookies).futureValue

                AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweFinancialDetailsEmptyBCDCharge, testDateService).detail)

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "selfAssessmentCharges.heading")
                )
              }

              "has a multiple charge from financial details and BCD and ACI charges from CESA" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                  testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString))
                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)
                IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))
                val res = buildGETMTDClient(path, additionalCookies).futureValue

                AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweDataWithDataDueIn30DaysIt, dateService).detail)

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                  isElementVisibleById("balancing-charge")(expectedValue = true),
                  isElementVisibleById("pre-mtd-digital")(expectedValue = true),
                  isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
                  isElementVisibleById("due-0")(expectedValue = true),
                  isElementVisibleById("due-1")(expectedValue = true),
                  isElementVisibleById("payment-button")(expectedValue = mtdUserRole == MTDIndividual),
                  isElementVisibleById("sa-note-migrated")(expectedValue = true),
                  isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
                  isElementVisibleById("overdue-payment-text")(expectedValue = true)
                )
              }

              "has a multiple charge, without BCD and ACI charges from CESA" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

                val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString)
                val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                  financialDetailsResponseJson)
                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)
                IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))
                val res = buildGETMTDClient(path, additionalCookies).futureValue

                val whatYouOweChargesList = {
                  val documentDetailsForTestTaxYear = financialDetailsModel.documentDetailsFilterByTaxYear(testTaxYear)

                  val financialDetails = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear)
                  val chargeItems = financialDetails.toChargeItem

                  WhatYouOweChargesList(
                    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                    chargesList = chargeItems
                  )
                }
                AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweChargesList, dateService))

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

                Then("the result should have a HTTP status of OK (200) and the payments due page")
                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                  isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
                  isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
                  isElementVisibleById("due-0")(expectedValue = true),
                  isElementVisibleById("charge-interest-0")(expectedValue = false),
                  isElementVisibleById("due-1")(expectedValue = true),
                  isElementVisibleById("charge-interest-1")(expectedValue = false),
                  isElementVisibleById(s"payment-button")(expectedValue = mtdUserRole == MTDIndividual),
                  isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
                  isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
                  isElementVisibleById("overdue-payment-text")(expectedValue = true)
                )

              }

              "has multiple charges and one charge equals zero" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

                val mixedJson = Json.obj(
                  "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
                  "codingDetails" -> Json.arr(),
                  "documentDetails" -> Json.arr(
                    documentDetailJson(3400.00, 1000.00, testTaxYear - 1, "ITSA- POA 1", transactionId = "transId1"),
                    documentDetailJson(1000.00, 100.00, testTaxYear - 1, "ITSA- POA 1", transactionId = "transId2", dueDate = testDate.plusDays(1).toString),
                    documentDetailJson(1000.00, 0.00, testTaxYear - 1, "ITSA - POA 2", transactionId = "transId3", dueDate = testDate.minusDays(1).toString)
                  ),
                  "financialDetails" -> Json.arr(
                    financialDetailJson((testTaxYear - 1).toString, transactionId = "transId1"),
                    financialDetailJson((testTaxYear - 1).toString, "SA Payment on Account 1", "4920", testDate.plusDays(1).toString, "transId2"),
                    financialDetailJson((testTaxYear - 1).toString, "SA Payment on Account 2", "4930", testDate.minusDays(1).toString, "transId3")
                  )
                )

                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)
                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, testValidOutStandingChargeResponseJsonWithAciAndBcdCharges)
                IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweWithAZeroOutstandingAmount(), dateService).detail)

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

                Then("the result should have a HTTP status of OK (200) and the payments due page")
                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                  isElementVisibleById("balancing-charge-type-0")(expectedValue = true),

                  isElementVisibleById("due-0")(expectedValue = true),
                  isElementVisibleById("due-1")(expectedValue = true),
                  isElementVisibleById("due-2")(expectedValue = false),
                  isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
                  isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
                  isElementVisibleById("overdue-payment-text")(expectedValue = true),

                )
              }

              "has no dunningLocks" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

                val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString, dunningLock = noDunningLock)
                val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                  financialDetailsResponseJson)
                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)
                IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                val whatYouOweChargesList = {
                  val documentDetailsForTestTaxYear = financialDetailsModel.documentDetailsFilterByTaxYear(testTaxYear)
                  val financialDetails = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear)
                  val chargeItems = financialDetails.toChargeItem

                  WhatYouOweChargesList(
                    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                    chargesList = chargeItems
                  )
                }
                AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweChargesList, dateService))

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                  isElementVisibleById("disagree-with-tax-appeal-link")(expectedValue = false)
                )
              }

              "has a dunningLocks against a charge" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

                val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString, dunningLock = oneDunningLock)
                val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                  financialDetailsResponseJson)
                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)
                IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                val whatYouOweChargesList = {
                  val documentDetailsForTestTaxYear = financialDetailsModel.documentDetailsFilterByTaxYear(testTaxYear)

                  val financialDetails = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear)
                  val chargeItems = financialDetails.toChargeItem


                  WhatYouOweChargesList(
                    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                    chargesList = chargeItems
                  )
                }
                AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweChargesList, dateService))

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                  isElementVisibleById("disagree-with-tax-appeal-link")(expectedValue = true)
                )
              }

              "has multiple dunningLocks" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))
                val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString, dunningLock = twoDunningLocks)
                val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                  financialDetailsResponseJson)
                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)
                IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                val whatYouOweChargesList = {
                  val documentDetailsForTestTaxYear = financialDetailsModel.documentDetailsFilterByTaxYear(testTaxYear)
                  val financialDetails = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear)
                  val chargeItems = financialDetails.toChargeItem

                  WhatYouOweChargesList(
                    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                    chargesList = chargeItems)
                }
                AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweChargesList, dateService))

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

                Then("the result should have a HTTP status of OK (200) and the payments due page")
                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                  isElementVisibleById("disagree-with-tax-appeal-link")(expectedValue = true)
                )
              }

              "no charge" when {
                "YearOfMigration does not exists" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, None))

                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06",
                    s"$testTaxYear-04-05")(OK, testEmptyFinancialDetailsModelJson)
                  IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweNoChargeList, dateService).detail)

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                    isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
                    isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
                    isElementVisibleById(s"payment-button")(expectedValue = false),
                    isElementVisibleById("no-payments-due")(expectedValue = true),
                    isElementVisibleById("sa-note-migrated")(expectedValue = true),
                    isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
                    isElementVisibleById("overdue-payment-text")(expectedValue = false),
                    isElementVisibleById(s"sa-tax-bill")(expectedValue = false)
                  )
                }

                "YearOfMigration exists but not the first year" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino,
                    s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, testEmptyFinancialDetailsModelJson)
                  IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                    "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)
                  IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweNoChargeList, dateService).detail)

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                    isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
                    isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
                    isElementVisibleById(s"payment-button")(expectedValue = false),
                    isElementVisibleById("no-payments-due")(expectedValue = true),
                    isElementVisibleById("sa-note-migrated")(expectedValue = true),
                    isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
                    isElementVisibleById("overdue-payment-text")(expectedValue = false),
                    isElementVisibleById(s"sa-tax-bill")(expectedValue = false)
                  )
                }

                "YearOfMigration exists and No valid charges exists" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
                    propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

                  val mixedJson = Json.obj(
                    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
                    "codingDetails" -> Json.arr(),
                    "documentDetails" -> Json.arr(
                    ),
                    "financialDetails" -> Json.arr(

                    )
                  )
                  IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                    "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)
                  IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                  IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

                  AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweNoChargeList, dateService).detail)

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                    isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
                    isElementVisibleById(s"payment-button")(expectedValue = false),
                    isElementVisibleById("no-payments-due")(expectedValue = true),
                    isElementVisibleById("sa-note-migrated")(expectedValue = true),
                    isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
                    isElementVisibleById("overdue-payment-text")(expectedValue = false),
                    isElementVisibleById(s"sa-tax-bill")(expectedValue = false)
                  )

                }
              }

              "has ACI and BCD charge" when {

                "YearOfMigration exists with Invalid financial details charges and valid outstanding charges" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
                    propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))
                  val mixedJson = Json.obj(
                    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
                    "codingDetails" -> Json.arr(),
                    "documentDetails" -> Json.arr(
                      documentDetailJson(3400.00, 1000.00, testTaxYear, transactionId = "transId1"),
                      documentDetailJson(1000.00, 0, testTaxYear, transactionId = "transId2"),
                      documentDetailJson(1000.00, 3000.00, testTaxYear, transactionId = "transId3")
                    ),
                    "financialDetails" -> Json.arr(
                      financialDetailJson(testTaxYear.toString, transactionId = "transId4"),
                      financialDetailJson(testTaxYear.toString, transactionId = "transId5"),
                      financialDetailJson(testTaxYear.toString, transactionId = "transId6")
                    ))

                  IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                    "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)
                  IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweOutstandingChargesOnly, dateService).detail)

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                  IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                    isElementVisibleById("balancing-charge-type-0")(expectedValue = true),

                    isElementVisibleById(s"payment-button")(expectedValue = mtdUserRole == MTDIndividual),
                    isElementVisibleById(s"no-payments-due")(expectedValue = false),
                    isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
                    isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
                    isElementVisibleById("overdue-payment-text")(expectedValue = true),

                  )
                }
              }

              "has empty BCD charge" when {
                "YearOfMigration exists with valid financial details charges and invalid outstanding charges" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
                    propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                    testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.plusYears(1).toString))
                  IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                    "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)
                  IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweFinancialDetailsEmptyBCDCharge, dateService).detail)

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                  IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                    isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
                    isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
                    isElementVisibleById(s"payment-button")(expectedValue = mtdUserRole == MTDIndividual),
                    isElementVisibleById(s"no-payments-due")(expectedValue = false),
                    isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
                    isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
                    isElementVisibleById("overdue-payment-text")(expectedValue = true),

                  )
                }
              }

              "has a Coding out banner" when {
                "CodingOut FS is enabled" in {
                  isEnabled(FilterCodedOutPoas)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
                    propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                    testValidFinancialDetailsModelJsonCodingOut(2000, 2000, testTaxYear.toString,
                      testDate.toString, 0, (testTaxYear - 1).toString, 2000))
                  IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                    "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)
                  IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                  IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                    isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
                    isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
                    isElementVisibleById(s"payment-button")(expectedValue = mtdUserRole == MTDIndividual),
                    isElementVisibleById(s"no-payments-due")(expectedValue = false),
                    isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
                    isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
                    isElementVisibleById("coding-out-notice")(expectedValue = true),
                    isElementVisibleById("overdue-payment-text")(expectedValue = true),

                  )
                }
              }

              "has a multiple charges ~ TxM extension" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                  testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, isClass2Nic = true))
                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)
                IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser(mtdUserRole), whatYouOweDataWithDataDueInSomeDays, dateService).detail)

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                  isElementVisibleById("balancing-charge-type-0")(expectedValue = true),

                  isElementVisibleById("due-0")(expectedValue = true),
                  isElementVisibleById("due-1")(expectedValue = true),
                  isElementVisibleById("payment-button")(expectedValue = mtdUserRole == MTDIndividual),
                  isElementVisibleById("sa-note-migrated")(expectedValue = true),
                  isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
                  isElementVisibleById("overdue-payment-text")(expectedValue = true),

                )
              }

              "has MFA Debits on the Payment Tab" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                  testValidFinancialDetailsModelMFADebitsJson(2000, 2000, testTaxYear.toString, testDate.plusYears(1).toString))
                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)
                IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
                IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)
                Then("The expected result is returned")
                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                  elementTextBySelectorList("#charges-due-now-table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)", "a:nth-of-type(1)")(s"$hmrcAdjustment 1"),
                  elementTextBySelectorList("#charges-due-now-table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)", "a:nth-of-type(1)")(s"$hmrcAdjustment 2"),
                  elementTextBySelectorList("#charges-due-now-table", "tbody", "tr:nth-of-type(3)", "td:nth-of-type(2)", "a:nth-of-type(1)")(s"$hmrcAdjustment 3"),
                  elementTextBySelectorList("#charges-due-now-table", "tbody", "tr:nth-of-type(4)", "td:nth-of-type(2)", "a:nth-of-type(1)")(s"$hmrcAdjustment 4"))
              }

              "has a POA section" when {
                  "a user has valid POAs" in {
                    stubAuthorised(mtdUserRole)
                    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYearPoa - 1, Some(testTaxYearPoa.toString)))
                    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYearPoa - 1}-04-06", s"$testTaxYearPoa-04-05")(OK,
                      testValidFinancialDetailsModelJson(2000, 2000, (testTaxYearPoa - 1).toString, testDate.toString))
                    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYearPoa - 2}-04-06", s"${testTaxYearPoa - 1}-04-05")(OK,
                      testValidFinancialDetailsModelJson(2000, 2000, (testTaxYearPoa - 1).toString, testDate.toString))
                    IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                    val res = buildGETMTDClient(path, additionalCookies).futureValue

                    res should have(
                      httpStatus(OK),
                      pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                      isElementVisibleById("adjust-poa-link")(expectedValue = true),
                      isElementVisibleById("adjust-poa-content")(expectedValue = true))
                  }

                  "has valid POAs that have been paid in full" in {
                    stubAuthorised(mtdUserRole)
                    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYearPoa - 1, Some(testTaxYearPoa.toString)))
                    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYearPoa - 1}-04-06", s"$testTaxYearPoa-04-05")(OK,
                      testValidFinancialDetailsModelJson(2000, 0, (testTaxYearPoa - 1).toString, testDate.toString))
                    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYearPoa - 2}-04-06", s"${testTaxYearPoa - 1}-04-05")(OK,
                      testValidFinancialDetailsModelJson(2000, 0, (testTaxYearPoa - 1).toString, testDate.toString))
                    IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                    val res = buildGETMTDClient(path, additionalCookies).futureValue

                    res should have(
                      httpStatus(OK),
                      pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                      isElementVisibleById("adjust-poa-link")(expectedValue = true),
                      isElementVisibleById("adjust-paid-poa-content")(expectedValue = true))
                  }
              }
              "not show POA section" when {
                  "a user does not have valid POAs" in {
                    stubAuthorised(mtdUserRole)
                    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYearPoa - 1, Some(testTaxYearPoa.toString)))
                    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYearPoa - 1}-04-06", s"$testTaxYearPoa-04-05")(OK,
                      testEmptyFinancialDetailsModelJson)
                    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYearPoa - 2}-04-06", s"${testTaxYearPoa - 1}-04-05")(OK,
                      testEmptyFinancialDetailsModelJson)
                    IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                    val res = buildGETMTDClient(path, additionalCookies).futureValue

                    res should have(
                      httpStatus(OK),
                      pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                      isElementVisibleById("adjust-poa-link")(expectedValue = false))
                  }
              }

              "has a money in your account section" when {
                "CreditsRefundsRepay FS is enabled and a user" that {
                  "has available credits" in {
                    enable(CreditsRefundsRepay)
                    stubAuthorised(mtdUserRole)
                    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))
                    val mixedJson = Json.obj(
                      "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00, "availableCredit" -> 300.00),
                      "codingDetails" -> Json.arr(),
                      "documentDetails" -> Json.arr(
                        documentDetailJson(3400.00, 1000.00, testTaxYear, "ITSA- POA 1", transactionId = "transId1"),
                        documentDetailJson(1000.00, 100.00, testTaxYear, "ITSA- POA 1", transactionId = "transId2"),
                        documentDetailJson(1000.00, 0.00, testTaxYear, "ITSA - POA 2", transactionId = "transId3")
                      ),
                      "financialDetails" -> Json.arr(
                        financialDetailJson(testTaxYear.toString, transactionId = "transId1"),
                        financialDetailJson(testTaxYear.toString, "SA Payment on Account 1", "4920", testDate.plusDays(1).toString, "transId2"),
                        financialDetailJson(testTaxYear.toString, "SA Payment on Account 2", "4930", testDate.minusDays(1).toString, "transId3")
                      )
                    )

                    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, mixedJson)
                    IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                      "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)
                    IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                    val res = buildGETMTDClient(path, additionalCookies).futureValue
                    res should have(
                      httpStatus(OK),
                      pageTitle(mtdUserRole, "selfAssessmentCharges.heading"),
                      isElementVisibleById(s"money-in-your-account")(expectedValue = true),
                      elementTextBySelector("#money-in-your-account")(
                        messagesAPI(s"whatYouOwe.moneyOnAccount${if (mtdUserRole != MTDIndividual) "-agent" else ""}") + " " +
                          messagesAPI("whatYouOwe.moneyOnAccount-1") + " £300.00" + " " +
                          messagesAPI(s"whatYouOwe.moneyOnAccount${if (mtdUserRole != MTDIndividual) "-agent" else ""}-2") + " " +
                          messagesAPI("whatYouOwe.moneyOnAccount-3") + "."
                      )
                    )
                  }
                }
              }
            }

            "render the internal server error page" when {
              "both connectors return internal server error" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
                  propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino,
                  s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())
                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(INTERNAL_SERVER_ERROR, testOutstandingChargesErrorModelJson)
                IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

                Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }

              "financial connector return internal server error" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
                  propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino,
                  s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())
                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)
                IncomeTaxViewChangeStub.stubPostStartSelfServeTimeToPayJourney()(CREATED, Json.toJson(SelfServeTimeToPayJourneyResponseModel("some-id", "nextUrl")))

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

                Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }

              "Outstanding charges connector return internal server error" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
                  propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
                  testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.toString))
                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(INTERNAL_SERVER_ERROR, testOutstandingChargesErrorModelJson)

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
