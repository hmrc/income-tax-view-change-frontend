/*
 * Copyright 2023 HM Revenue & Customs
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

import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import mocks.services.{MockCalculationService, MockCreditHistoryService, MockFinancialDetailsService}
import models.financialDetails.{BalanceDetails, DocumentDetail}
import play.api
import play.api.Application
import play.api.http.{HeaderNames, Status}
import play.api.test.Helpers._
import services.{CalculationService, CreditHistoryService}
import testConstants.BaseTestConstants.{calendarYear2018, testSaUtr}
import testConstants.FinancialDetailsTestConstants._
import views.html.CreditsSummary


class CreditsSummaryControllerSpec extends MockAuthActions with MockCalculationService
  with MockFinancialDetailsService
  with MockCreditHistoryService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[CalculationService].toInstance(mockCalculationService),
      api.inject.bind[CreditHistoryService].toInstance(mockCreditHistoryService)
    ).build()

  lazy val testController = app.injector.instanceOf[CreditsSummaryController]

  val testCharges: List[DocumentDetail] = List(
    documentDetailModel(
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = -1400.00,
      paymentLotItem = None,
      paymentLot = None
    )
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
  }

  lazy val creditsSummaryView = app.injector.instanceOf[CreditsSummary]
  mtdAllRoles.foreach { mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    s"show${if (isAgent) "AgentCreditsSummary"}" when {
      val action = if (isAgent) testController.showAgentCreditsSummary(calendarYear2018) else testController.showCreditsSummary(calendarYear2018)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the credit summary page" that {
            "has a list of MFA/CutOver/BC credits and back link to the Payment Refund History page" when {
              "all calls are returned correctly and Referer was a Payment Refund History page" in {
                val chargesList = creditAndRefundCreditDetailListMFAWithCutoverAndBCC
                setupMockSuccess(mtdUserRole)

                mockSingleBusinessIncomeSource()
                mockCreditHistoryService(chargesList)

                val backUrl = if (isAgent) {
                  routes.PaymentHistoryController.showAgent().url
                } else {
                  routes.PaymentHistoryController.show().url
                }
                val expectedContent: String = creditsSummaryView(
                  backUrl = backUrl,
                  utr = Some(testSaUtr),
                  isAgent = isAgent,
                  charges = chargesList,
                  maybeAvailableCredit = financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment,
                  calendarYear = calendarYear2018
                ).toString

                val result = action(fakeRequest.withHeaders(
                  HeaderNames.REFERER -> backUrl
                ))

                status(result) shouldBe Status.OK

                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some(HTML)
              }
            }

            "contains a back link to the Payment Refund History and the Money in your account section should not be available" when {
              "available credit is Some(0.00)" in {
                val emptyBalanceDetails = BalanceDetails(0.00, 0.00, 0.00, Some(0.0), None, None, None, None, None, None)
                val chargesList = creditAndRefundCreditDetailListMFA.map(_.copy(availableCredit = emptyBalanceDetails.totalCreditAvailableForRepayment))
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCreditHistoryService(chargesList)

                val backUrl = if (isAgent) {
                  routes.PaymentHistoryController.showAgent().url
                } else {
                  routes.PaymentHistoryController.show().url
                }
                val expectedContent: String = creditsSummaryView(
                  backUrl = backUrl,
                  utr = Some(testSaUtr),
                  isAgent = isAgent,
                  charges = chargesList,
                  maybeAvailableCredit = None,
                  calendarYear = calendarYear2018
                ).toString

                val result = action(fakeRequest.withHeaders(
                  HeaderNames.REFERER -> backUrl
                ))

                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some(HTML)
              }
            }

            "all calls are returned correctly and Referer was a Credit and Refund page" should {
              "show the Credits Summary Page and back link should be to the Credit and Refund page" in {
                val chargesList = creditAndRefundCreditDetailListMFA
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCreditHistoryService(chargesList)
                val backUrl = if (isAgent) {
                  routes.CreditAndRefundController.showAgent().url
                } else {
                  routes.CreditAndRefundController.show().url
                }
                val expectedContent: String = creditsSummaryView(
                  backUrl = backUrl,
                  utr = Some(testSaUtr),
                  isAgent = isAgent,
                  charges = chargesList,
                  maybeAvailableCredit = financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment,
                  calendarYear = calendarYear2018
                ).toString

                val result = action(fakeRequest.withHeaders(
                  HeaderNames.REFERER -> backUrl
                ))
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some(HTML)
              }
            }

            "all calls are returned correctly and Referer was a Credits Summary page when referrer is not provided" should {
              "show the Credits Summary Page and back link should be to the Credits Summary page" in {
                val chargesList = creditAndRefundCreditDetailListMFA
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCreditHistoryService(chargesList)
                val backUrl = if (isAgent) {
                  routes.CreditsSummaryController.showAgentCreditsSummary(2018).url
                } else {
                  routes.CreditsSummaryController.showCreditsSummary(2018).url
                }
                val expectedContent: String = creditsSummaryView(
                  backUrl = backUrl,
                  utr = Some(testSaUtr),
                  isAgent = isAgent,
                  charges = chargesList,
                  maybeAvailableCredit = financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment,
                  calendarYear = calendarYear2018
                ).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some(HTML)
              }
            }

            "all calls are returned correctly and Referer was a Payment Refund History page" should {
              "show the Credits Summary Page with multiple records ordered properly and back link should be to the Payment Refund History page" in {
                val chargesList = creditAndRefundCreditDetailListMultipleChargesMFA
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCreditHistoryService(chargesList)

                val backUrl = if (isAgent) {
                  routes.PaymentHistoryController.showAgent().url
                } else {
                  routes.PaymentHistoryController.show().url
                }
                val expectedContent: String = creditsSummaryView(
                  backUrl = backUrl,
                  utr = Some(testSaUtr),
                  isAgent = isAgent,
                  charges = chargesList,
                  maybeAvailableCredit = financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment,
                  calendarYear = calendarYear2018
                ).toString

                val result = action(fakeRequest.withHeaders(
                  HeaderNames.REFERER -> backUrl
                ))
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some(HTML)
                contentAsString(result) shouldBe expectedContent
              }
            }
          }

          "render the error page" when {
            "getCreditsHistory returns an error" in {
              setupMockSuccess(mtdUserRole)
              mockSingleBusinessIncomeSource()
              mockCreditHistoryFailed()

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some(HTML)
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole, false)(fakeRequest)
    }
  }
}
