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

package controllers.newHomePage

import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.{MockDateService, MockFinancialDetailsService, MockITSAStatusService}
import models.admin.{NewHomePage, RecentActivity}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.newHomePage.RecentActivityViewModel
import models.obligations.ObligationsModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, mock as mMock}
import play.api
import play.api.http.Status
import play.api.inject
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.newHomePage.RecentActivityService
import services.{DateService, DateServiceInterface, ITSAStatusService, PaymentHistoryService}
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.BusinessDetailsTestConstants.business1

import java.time.LocalDate
import scala.concurrent.Future

class RecentActivityControllerSpec extends MockAuthActions
  with MockDateService
  with MockITSAStatusService
  with MockFinancialDetailsService {

  lazy val mockDateServiceInjected: DateService = mMock(classOfDateService)
  lazy val mockRecentActivityService: RecentActivityService = mMock(classOf[RecentActivityService])
  lazy val mockPaymentHistoryService: PaymentHistoryService = mMock(classOf[PaymentHistoryService])

  override lazy val app = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[RecentActivityService].toInstance(mockRecentActivityService),
      api.inject.bind[ITSAStatusService].toInstance(mockITSAStatusService),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInjected),
      api.inject.bind[PaymentHistoryService].toInstance(mockPaymentHistoryService),
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
    when(mockDateServiceInterface.getCurrentDate).thenReturn(LocalDate.of(2023, 1, 1))
    when(mockDateServiceInterface.getCurrentTaxYearEnd).thenReturn(2024)
  }

  val testController = app.injector.instanceOf[RecentActivityController]
  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual

    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      s"the user is authenticated as $mtdRole" should {
        "render the recent activity page" when {
          "the recent activity feature switch is enabled" in {
            enable(NewHomePage, RecentActivity)

            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)

            when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any())).thenReturn(Future(singleBusinessIncome))
            when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any())).thenReturn(Future(List.empty))
            when(mockPaymentHistoryService.getPaymentHistory(any(), any())).thenReturn(Future(Right(List.empty)))
            when(mockRecentActivityService.getFulfilledObligations()(any(), any())).thenReturn(Future(ObligationsModel(Seq.empty)))
            when(mockITSAStatusService.getITSAStatusDetail(any(), any(), any())(any(), any(), any())).thenReturn(Future(Seq.empty))
            when(mockRecentActivityService.recentActivityCards(any(), any())(any(), any())).thenReturn(RecentActivityViewModel(Seq.empty))
            setupMockSuccess(mtdRole)

            val result = action(fakeRequest)

            status(result) shouldBe Status.OK
          }
        }
        "redirect the user" when {
          "recent activity FS is disabled" in {
            enable(NewHomePage)
            disable(RecentActivity)

            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)

            when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any())).thenReturn(Future(singleBusinessIncome))
            when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any())).thenReturn(Future(singleBusinessIncome))
            when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any())).thenReturn(Future(List.empty))
            when(mockRecentActivityService.getFulfilledObligations()(any(), any())).thenReturn(Future(ObligationsModel(Seq.empty)))
            when(mockITSAStatusService.getITSAStatusDetail(any(), any(), any())(any(), any(), any())).thenReturn(Future(Seq.empty))
            when(mockRecentActivityService.recentActivityCards(any(), any())(any(), any())).thenReturn(RecentActivityViewModel(Seq.empty))
            setupMockSuccess(mtdRole)

            val result = action(fakeRequest)
            val yourTasksUrl = if(isAgent) "/report-quarterly/income-and-expenses/view/agents/your-tasks" else "/report-quarterly/income-and-expenses/view/your-tasks"

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(yourTasksUrl)
          }
        }
      }
    }
  }
}
