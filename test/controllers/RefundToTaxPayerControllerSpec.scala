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

import audit.models.RefundToTaxPayerResponseAuditModel
import connectors.RepaymentHistoryConnector
import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import mocks.connectors.MockRepaymentHistoryConnector
import models.admin.PaymentHistoryRefunds
import models.creditsandrefunds.RefundToTaxPayerViewModel
import models.repaymentHistory._
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import testConstants.BaseTestConstants.testMtditid
import views.html.RefundToTaxPayer

import java.time.LocalDate

class RefundToTaxPayerControllerSpec extends MockAuthActions
  with MockRepaymentHistoryConnector {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[RepaymentHistoryConnector].toInstance(mockRepaymentHistoryConnector)
    ).build()

  lazy val testController = app.injector.instanceOf[RefundToTaxPayerController]

  val repaymentRequestNumber: String = "023942042349"
  val testNino: String = "AB123456C"

  lazy val paymentRefundHistoryBackLink: Boolean => String = isAgent => {
    if(isAgent) {
      "/report-quarterly/income-and-expenses/view/agents/payment-refund-history"
    } else {
      "/report-quarterly/income-and-expenses/view/payment-refund-history"
    }
  }

  lazy val refundToTaxPayerView: RefundToTaxPayer = app.injector.instanceOf[RefundToTaxPayer]

  val testRepaymentHistory: RepaymentHistory = RepaymentHistory(
    Some(705.2),
    705.2,
    Some("BACS"),
    Some(12345),
    Some(Vector(
      RepaymentItem(
        Vector(
          RepaymentSupplementItem(
            Some("002420002231"),
            Some(3.78),
            Some(LocalDate.of(2021, 7, 31)),
            Some(LocalDate.of(2021, 9, 15)),
            Some(2.01)
          ),
          RepaymentSupplementItem(
            Some("002420002231"),
            Some(2.63),
            Some(LocalDate.of(2021, 9, 15)),
            Some(LocalDate.of(2021, 10, 24)),
            Some(1.76)
          ),
          RepaymentSupplementItem(
            Some("002420002231"),
            Some(3.26),
            Some(LocalDate.of(2021, 10, 24)),
            Some(LocalDate.of(2021, 11, 30)),
            Some(2.01))
        )
      )
    )), Some(LocalDate.of(2021, 7, 23)), Some(LocalDate.of(2021, 7, 21)), "000000003135",
    status = RepaymentHistoryStatus("A"))

  val testRefundViewModel: RefundToTaxPayerViewModel = RefundToTaxPayerViewModel(
    Some(705.2),
    705.2,
    "BACS",
    12345,
    Vector(
      RepaymentItem(
        Vector(
          RepaymentSupplementItem(
            Some("002420002231"),
            Some(3.78),
            Some(LocalDate.of(2021, 7, 31)),
            Some(LocalDate.of(2021, 9, 15)),
            Some(2.01)
          ),
          RepaymentSupplementItem(
            Some("002420002231"),
            Some(2.63),
            Some(LocalDate.of(2021, 9, 15)),
            Some(LocalDate.of(2021, 10, 24)),
            Some(1.76)
          ),
          RepaymentSupplementItem(
            Some("002420002231"),
            Some(3.26),
            Some(LocalDate.of(2021, 10, 24)),
            Some(LocalDate.of(2021, 11, 30)),
            Some(2.01))
        )
      )
    ), LocalDate.of(2021, 7, 23), LocalDate.of(2021, 7, 21), "000000003135",
    status = RepaymentHistoryStatus("A")
  )

  val testRepaymentHistoryModel: RepaymentHistoryModel = RepaymentHistoryModel(
    List(
      testRepaymentHistory
    )
  )

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.showAgent(repaymentRequestNumber) else testController.show(repaymentRequestNumber)
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
    s"show${if (isAgent) "Agent"}" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the refund to tax payer page" when {
            "PaymentHistoryRefunds FS is enabled" in {
              enable(PaymentHistoryRefunds)
              setupMockSuccess(mtdUserRole)
              mockSingleBusinessIncomeSource()
              setupGetRepaymentHistoryByRepaymentId(testNino, repaymentRequestNumber)(testRepaymentHistoryModel)

              val expectedContent: String = refundToTaxPayerView(
                backUrl = paymentRefundHistoryBackLink(isAgent),
                viewModel = testRefundViewModel,
                saUtr = Some(testMtditid),
                paymentHistoryRefundsEnabled = true,
                isAgent = isAgent
              ).toString

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              contentAsString(result) shouldBe expectedContent
              contentType(result) shouldBe Some(HTML)

              verifyExtendedAudit(RefundToTaxPayerResponseAuditModel(testRepaymentHistoryModel))
            }
          }

          "redirect to the home page" when {
            "PaymentHistoryRefunds FS is disabled" in {
              disable(PaymentHistoryRefunds)
              setupMockSuccess(mtdUserRole)
              mockSingleBusinessIncomeSource()

              val result = action(fakeRequest)
              status(result) shouldBe Status.SEE_OTHER
              val homeUrl = if(isAgent) routes.HomeController.showAgent() else routes.HomeController.show()
              redirectLocation(result) shouldBe Some(homeUrl.url)
            }
          }

          "render the error page" when {
            "Failing to retrieve a user's re-payments history" in {
              enable(PaymentHistoryRefunds)
              setupMockSuccess(mtdUserRole)
              mockSingleBusinessIncomeSource()
              setupGetRepaymentHistoryByRepaymentIdError(testNino, repaymentRequestNumber)(RepaymentHistoryErrorModel(404, "Not found"))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole, false)(fakeRequest)
    }
  }
}
