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

import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import enums.ChargeType.ITSA_ENGLAND_AND_NI
import enums.CodingOutType._
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.ChargeItemStub.{chargeItemWithInterestAndOverdue, docDetail}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.admin.ChargeHistory
import models.financialDetails._
import play.api.http.Status._
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testTaxYear, testTaxYearTyped}
import testConstants.FinancialDetailsIntegrationTestConstants.financialDetailModelPartial
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.messages.ChargeSummaryMessages._

import java.time.LocalDate

class ChargeSummaryControllerISpec extends ChargeSummaryISpecHelper {

  def testUser(mtdUserRole: MTDUserRole): MtdItUser[_] = {
    getTestUser(mtdUserRole, multipleBusinessesAndPropertyResponse)
  }

  def getPath(mtdRole: MTDUserRole, taxYear: String = "2018"): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/tax-years/$taxYear/charge"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path + "?id=1040000123", additionalCookies)
          } else {
            "render the charge summary page" that {
              "has expected Payments Breakdown" when {
                "Charge History feature is disabled" in {
                  disable(ChargeHistory)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2,
                    dunningLock = twoDunningLocks, interestLocks = twoInterestLocks))
                  IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))

                  val result = buildGETMTDClient(path +"?id=1040000124", additionalCookies).futureValue

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  result should have(
                    httpStatus(OK)
                  )

                  AuditStub.verifyAuditEvent(ChargeSummaryAudit(
                    testUser(mtdUserRole),
                    docDetail(PoaOneDebit),
                    paymentBreakdown = List(financialDetailModelPartial(chargeType = ITSA_ENGLAND_AND_NI, originalAmount = 10.34, dunningLock = Some("Stand over order"), interestLock = Some("Breathing Space Moratorium Act"))),
                    chargeHistories = List.empty,
                    paymentAllocations = List.empty,
                    isLatePaymentCharge = false,
                    taxYear = testTaxYearTyped
                  )(dateService))
                }
              }
              "includes the importantPaymentBreakdown" when {
                "Charge History feature is disabled" in {
                  disable(ChargeHistory)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testAuditFinancialDetailsModelJson(123.45, 1.2,
                    dunningLock = oneDunningLock, interestLocks = twoInterestLocks, accruingInterestAmount = None,
                    dueDate = dateService.getCurrentDate.plusDays(20).toString))
                  IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))

                  val res = buildGETMTDClient(path +"?id=1040000123", additionalCookies).futureValue

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                  AuditStub.verifyAuditEvent(ChargeSummaryAudit(
                    testUser(mtdUserRole),
                    chargeItemWithInterestAndOverdue(BalancingCharge, None,
                      Some(dateService.getCurrentDate.plusDays(20))),
                    paymentBreakdown = paymentBreakdown,
                    chargeHistories = List.empty,
                    paymentAllocations = paymentAllocation,
                    isLatePaymentCharge = false,
                    taxYear = testTaxYearTyped
                  )(dateService))

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "chargeSummary.balancingCharge.text"),
                    elementTextByClass("govuk-notification-banner__title")(important)
                  )
                }

                "when charge history is enabled" in {
                  enable(ChargeHistory)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
                  val json = testAuditFinancialDetailsModelJson(123.45, 1.2,
                    dunningLock = oneDunningLock, interestLocks = twoInterestLocks, accruingInterestAmount = None, dueDate =
                      dateService.getCurrentDate.plusDays(20).toString)
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, json)
                  IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))

                  val res = buildGETMTDClient(path +"?id=1040000123", additionalCookies).futureValue

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                  val x = chargeItemWithInterestAndOverdue(BalancingCharge, None, Some(dateService.getCurrentDate.plusDays(20)))

                  val expectedAuditEvent = ChargeSummaryAudit(
                    testUser(mtdUserRole),
                    x,
                    paymentBreakdown = paymentBreakdown,
                    chargeHistories = chargeHistories,
                    paymentAllocations = paymentAllocation,
                    isLatePaymentCharge = false,
                    taxYear = testTaxYearTyped
                  )(dateService)

                  AuditStub.verifyAuditEvent(expectedAuditEvent)
                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "chargeSummary.balancingCharge.text"),
                    elementTextByClass("govuk-notification-banner__title")(important)
                  )
                }
              }

              //              "includes late payment interest" when {
              //                "late payment interest flag is true and chargeHistory FS is enabled" in {
              //                  enable(ChargeHistory)
              //                  stubAuthorised(mtdUserRole)
              //                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
              //                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJsonAccruingInterest(
              //                    123.45, 0.0, accruingInterestAmount = Some(54.32)))
              //
              //                  val res = buildGETMTDClient(path +"?id=1040000123&isInterestCharge=true", additionalCookies).futureValue
              //
              //                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
              //                  AuditStub.verifyAuditEvent(
              //                    ChargeSummaryAudit(
              //                      testUser(mtdUserRole),
              //                      chargeItem = chargeItemWithInterestAndOverdue(BalancingCharge, None, Some(LocalDate.of(2018, 2, 14))),
              //                      paymentBreakdown = List.empty,
              //                      chargeHistories = List.empty,
              //                      paymentAllocations = paymentAllocation,
              //                      isLatePaymentCharge = true,
              //                      taxYear = testTaxYearTyped
              //                    ))
              //
              //                  res should have(
              //                    httpStatus(OK),
              //                    pageTitle(mtdUserRole, "chargeSummary.lpi.balancingCharge.text"),
              //                    elementTextBySelector("main h2")(lpiHistory),
              //                    elementTextBySelector("tbody tr:nth-child(1) td:nth-child(2)")(lpiCreated)
              //                  )
              //                }
              //
              //                "late payment interest flag is true and chargeHistory FS is disabled" in {
              //                  disable(ChargeHistory)
              //                  stubAuthorised(mtdUserRole)
              //                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
              //                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelWithPaymentAllocationJson(10.34, 0.0))
              //
              //                  val res = buildGETMTDClient(path +"?id=1040000123&isInterestCharge=true", additionalCookies).futureValue
              //
              //                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
              //                  res should have(
              //                    httpStatus(OK),
              //                    pageTitle(mtdUserRole, "chargeSummary.lpi.balancingCharge.text"),
              //                    elementTextBySelector("main h2")(lpiHistory),
              //                    elementTextBySelector("tbody tr:nth-child(1) td:nth-child(2)")("")
              //                  )
              //                }
              //              }

              //              "is expected" when {
              //                "late payment interest flag is true but there are no payment allocations" in {
              //                  disable(ChargeHistory)
              //                  stubAuthorised(mtdUserRole)
              //                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
              //                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, Json.obj(
              //                    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
              //                    "codingDetails" -> Json.arr(),
              //                    "documentDetails" -> Json.arr(
              //                      Json.obj("taxYear" -> 2018,
              //                        "transactionId" -> "1040001234",
              //                        "documentDescription" -> "ITSA - POA 2",
              //                        "outstandingAmount" -> 0.0,
              //                        "originalAmount" -> 10.34,
              //                        "documentDate" -> "2018-03-29",
              //                        "interestFromDate" -> "2018-03-29",
              //                        "interestEndDate" -> "2018-03-29",
              //                        "accruingInterestAmount" -> 100.0,
              //                        "interestOutstandingAmount" -> 80.0
              //                      )),
              //                    "financialDetails" -> Json.arr(
              //                      Json.obj(
              //                        "transactionId" -> "1040001234",
              //                        "taxYear" -> "2018",
              //                        "mainTransaction" -> "4930",
              //                        "chargeReference" -> "chargeRef",
              //                      )
              //                    )))
              //
              //                  val res = buildGETMTDClient(path +"?id=1040001234&isInterestCharge=true", additionalCookies).futureValue
              //
              //                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
              //
              //                  Then("the result should have a HTTP status of OK (200) and load the correct page")
              //                  res should have(
              //                    httpStatus(OK),
              //                    pageTitle(mtdUserRole, "chargeSummary.lpi.paymentOnAccount2.text"),
              //                    elementTextBySelector("main h2")("")
              //                  )
              //                }
              //              }

              //              "has coding out details" when {
              //                "coding out is enabled and a coded out documentDetail id is passed" in {
              //                  enable(ChargeHistory)
              //                  stubAuthorised(mtdUserRole)
              //                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
              //                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, Json.obj(
              //                    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
              //                    "codingDetails" -> Json.arr(),
              //                    "documentDetails" -> Json.arr(
              //                      Json.obj("taxYear" -> 2018,
              //                        "transactionId" -> "CODINGOUT01",
              //                        "documentDescription" -> "TRM New Charge",
              //                        "documentText" -> CODING_OUT_ACCEPTED,
              //                        "outstandingAmount" -> 2500.00,
              //                        "originalAmount" -> 2500.00,
              //                        "documentDate" -> "2018-03-29"
              //                      )),
              //                    "financialDetails" -> Json.arr(
              //                      Json.obj(
              //                        "transactionId" -> "CODINGOUT01",
              //                        "taxYear" -> "2018",
              //                        "mainTransaction" -> "4910",
              //                        "chargeReference" -> "chargeRef",
              //                        "items" -> Json.arr(
              //                          "codedOutStatus" -> "I"
              //                        )
              //                      )
              //                    )))
              //
              //                  val res = buildGETMTDClient(path +"?id=CODINGOUT01", additionalCookies).futureValue
              //
              //                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
              //                  res should have(
              //                    httpStatus(OK),
              //                    pageTitle(mtdUserRole, "tax-year-summary.payments.codingOut.text"),
              //                    elementTextBySelector("#coding-out-notice")(codingOutInsetPara),
              //                    elementTextBySelector("#codedOutBCDExplanation")(codingOutMessageWithStringMessagesArgument(2017, 2018))
              //                  )
              //                }
              //              }

              "has payments you make" when {
                "the charge is not a Review & Reconcile or POA charge" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2,
                    dunningLock = twoDunningLocks, interestLocks = twoInterestLocks))
                  IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))

                  Given("the ChargeHistory feature switch is disabled")
                  disable(ChargeHistory)

                  val res = buildGETMTDClient(path +"?id=1040000123", additionalCookies).futureValue

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                  Then("the result should have a HTTP status of OK (200) and load the correct page")
                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "chargeSummary.balancingCharge.text"),
                  )
                }
              }

              //              "has balancing payment title" when {
              //                s"ChargeHistory FS is enabled and the charge history details API responds with a $NOT_FOUND" in {
              //                  enable(ChargeHistory)
              //                  stubAuthorised(mtdUserRole)
              //                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
              //                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2))
              //                  IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(NOT_FOUND, Json.parse(
              //                    """
              //                      |{
              //                      |   "code": "NO_DATA_FOUND",
              //                      |   "reason": "The remote endpoint has indicated that no match found for the reference provided."
              //                      |}
              //                      |""".stripMargin))
              //
              //                  val result = buildGETMTDClient(path +"?id=1040000123", additionalCookies).futureValue
              //
              //                  result should have(
              //                    httpStatus(OK),
              //                    pageTitle(mtdUserRole, "chargeSummary.balancingCharge.text")
              //                  )
              //                }
              //                s"ChargeHistory FS is enabled and the charge history details API responds with a $FORBIDDEN" in {
              //                  enable(ChargeHistory)
              //                  stubAuthorised(mtdUserRole)
              //                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
              //                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2))
              //                  IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(FORBIDDEN, Json.parse(
              //                    """
              //                      |{
              //                      |   "code": "REQUEST_NOT_PROCESSED",
              //                      |   "reason": "The remote endpoint has indicated that request could not be processed."
              //                      |}
              //                      |""".stripMargin))
              //
              //                  val result = buildGETMTDClient(path +"?id=1040000123", additionalCookies).futureValue
              //
              //                  result should have(
              //                    httpStatus(OK),
              //                    pageTitle(mtdUserRole, "chargeSummary.balancingCharge.text")
              //                  )
              //                }
              //              }
              //
              //              "has an UNPAID MFADebit" in {
              //                enable(ChargeHistory)
              //                stubAuthorised(mtdUserRole)
              //                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
              //                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, financialDetailsUnpaidMFA)
              //                IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))
              //
              //                val res = buildGETMTDClient(path +"?id=1040000123", additionalCookies).futureValue
              //
              //                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
              //                val summaryListText = "Due date Overdue 30 March 2018 Amount £1,200.00 Still to pay £1,200.00"
              //                val hmrcCreated = messagesAPI("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
              //                val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,200.00"
              //
              //                res should have(
              //                  httpStatus(OK),
              //                  pageTitle(mtdUserRole, "chargeSummary.hmrcAdjustment.text"),
              //                  elementTextBySelector(".govuk-summary-list")(summaryListText),
              //                  elementCountBySelector("#payment-history-table tr")(2),
              //                  elementTextBySelector("#payment-history-table tr")(paymentHistoryText)
              //                )
              //
              //                AuditStub.verifyAuditEvent(ChargeSummaryAudit(
              //                  testUser(mtdUserRole),
              //                  chargeItemUnpaid.copy(
              //                    dueDate = Some(LocalDate.parse("2018-03-30"))
              //                  ),
              //                  paymentBreakdown = List(),
              //                  chargeHistories = List.empty,
              //                  paymentAllocations = List.empty,
              //                  isLatePaymentCharge = false,
              //                  isMFADebit = true,
              //                  taxYear = testTaxYearTyped
              //                ))
              //              }
              //
              //              "has a PAID MFADebit" in {
              //                enable(ChargeHistory)
              //                stubAuthorised(mtdUserRole)
              //                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
              //                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, financialDetailsPaidMFA)
              //                IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testNino, "ABCD1234", 2500))
              //
              //                val res = buildGETMTDClient(path +"?id=1", additionalCookies).futureValue
              //
              //                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
              //
              //                val summaryListText = "Due date 30 March 2018 Amount £1,200.00 Still to pay £0.00"
              //                val hmrcCreated = messagesAPI("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
              //                val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,200.00"
              //                val paymentHistoryText2 = "28 Jul 2022 Payment put towards HMRC adjustment 2018 £1,200.00"
              //
              //                res should have(
              //                  httpStatus(OK),
              //                  pageTitle(mtdUserRole, "chargeSummary.hmrcAdjustment.text"),
              //                  elementTextBySelector(".govuk-summary-list")(summaryListText),
              //                  elementCountBySelector(s"#payment-link-$testTaxYear")(0),
              //                  elementCountBySelector("#payment-history-table tr")(3),
              //                  elementTextBySelector("#payment-history-table tr:nth-child(1)")(paymentHistoryText),
              //                  elementTextBySelector("#payment-history-table tr:nth-child(2)")(paymentHistoryText2)
              //                )
              //
              //                AuditStub.verifyAuditEvent(ChargeSummaryAudit(
              //                  testUser(mtdUserRole),
              //                  chargeItemPaid.copy(
              //                    dueDate = Some(LocalDate.parse("2018-03-30"))
              //                  ),
              //                  paymentBreakdown = List(),
              //                  chargeHistories = List.empty,
              //                  paymentAllocations = List.empty,
              //                  isLatePaymentCharge = false,
              //                  isMFADebit = true,
              //                  taxYear = testTaxYearTyped
              //                ))
              //              }
            }
            //            "return a technical difficulties page to the user" when {
            //              "ChargeHistory FS is enabled and the charge history details API responded with an error" in {
            //                enable(ChargeHistory)
            //                stubAuthorised(mtdUserRole)
            //                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
            //                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2))
            //                IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(INTERNAL_SERVER_ERROR, Json.parse(
            //                  """
            //                    |{
            //                    |   "code": "SERVER_ERROR",
            //                    |   "reason": "DES is currently experiencing problems that require live service intervention."
            //                    |}
            //                    |""".stripMargin))
            //
            //                val result = buildGETMTDClient(path +"?id=1040000123", additionalCookies).futureValue
            //
            //                result should have(
            //                  httpStatus(INTERNAL_SERVER_ERROR),
            //                  pageTitle(mtdUserRole, titleInternalServer, isErrorPage = true)
            //                )
            //              }
            //
            //              "When Original Amount value is missing from financial details / document details" in {
            //                enable(ChargeHistory)
            //                stubAuthorised(mtdUserRole)
            //                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
            //                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testFinancialDetailsModelWithMissingOriginalAmountJson())
            //
            //                val result = buildGETMTDClient(path +"?id=1040000123", additionalCookies).futureValue
            //
            //                result should have(
            //                  httpStatus(INTERNAL_SERVER_ERROR),
            //                  pageTitle(mtdUserRole, titleInternalServer, isErrorPage = true)
            //                )
            //              }
            //            }
          }
        }
        testAuthFailures(path+"?id=1040000123", mtdUserRole)
      }
    }
  }
}
