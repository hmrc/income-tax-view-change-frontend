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

package controllers.manageBusinesses

import enums.{MTDPrimaryAgent, MTDSupportingAgent}
import exceptions.MissingFieldException
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.DisplayBusinessStartDate
import models.incomeSourceDetails.viewmodels.ViewIncomeSourcesViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.SessionService
import testConstants.BusinessDetailsTestConstants.viewBusinessDetailsViewModel
import testConstants.PropertyDetailsTestConstants.viewUkPropertyDetailsViewModel

class ManageYourBusinessesControllerSpec extends MockAuthActions with ImplicitDateFormatter with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testManageYourBusinessesController: ManageYourBusinessesController = app.injector.instanceOf[ManageYourBusinessesController]

  "show()" when {
    "the user is authenticated" should {

      "render the manage businesses page" when {
        "the IncomeSources FS in enabled and the DisplayBusinessStartDate FS is enabled" in {
          setupMockUserAuth
          enable(DisplayBusinessStartDate)
          mockBothIncomeSources()
          setupMockCreateSession(true)
          setupMockClearSession(true)
          when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any(), any()))
            .thenReturn(
              Right(
                ViewIncomeSourcesViewModel(
                  viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
                  viewUkProperty = Some(viewUkPropertyDetailsViewModel),
                  viewForeignProperty = None,
                  viewCeasedBusinesses = Nil,
                  displayStartDate = true
                )
              )
            )

          val result = testManageYourBusinessesController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.OK
        }
      }

      "render the manage businesses page with no business start date" when {
        "the DisplayBusinessStartDate FS is disabled" in {
          setupMockUserAuth
          disable(DisplayBusinessStartDate)
          mockBothIncomeSources()
          setupMockCreateSession(true)
          setupMockClearSession(true)
          when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any(), any()))
            .thenReturn(
              Right(
                ViewIncomeSourcesViewModel(
                  viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
                  viewUkProperty = Some(viewUkPropertyDetailsViewModel),
                  viewForeignProperty = None,
                  viewCeasedBusinesses = Nil,
                  displayStartDate = false
                )
              )
            )

          val result = testManageYourBusinessesController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.OK
        }
      }

      "render the error page" when {
        "the call to get income source view model fails" in {
          setupMockUserAuth
          enable(DisplayBusinessStartDate)
          mockBothIncomeSources()
          when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any(), any()))
            .thenReturn(
              Left(MissingFieldException("Trading Name"))
            )

          val result = testManageYourBusinessesController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "the header carrier is missing the X-sessionId" in {
          setupMockUserAuth
          enable(DisplayBusinessStartDate)
          mockBothIncomeSources()
          setupMockCreateSession(true)
          setupMockClearSession(true)
          when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any(), any()))
            .thenReturn(
              Right(
                ViewIncomeSourcesViewModel(
                  viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
                  viewUkProperty = Some(viewUkPropertyDetailsViewModel),
                  viewForeignProperty = None,
                  viewCeasedBusinesses = Nil,
                  displayStartDate = true
                )
              )
            )

          val headersWithoutSessionId = fakeRequestWithActiveSession.headers.remove("X-Session-ID")

          val result = testManageYourBusinessesController.show()(fakeRequestWithActiveSession.withHeaders(headersWithoutSessionId))
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
    testMTDIndividualAuthFailures(testManageYourBusinessesController.show())
  }

  "showAgent()" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      val isSupportingAgent = mtdUserRole == MTDSupportingAgent
      val fakeRequest = fakeRequestConfirmedClient(isSupportingAgent = isSupportingAgent)
      s"the $mtdUserRole is authenticated" should {
        "render the manage businesses page" when {
          "the DisplayBusinessStartDate FS in enabled" in {
            setupMockAgentWithClientAuth(isSupportingAgent)
            enable(DisplayBusinessStartDate)
            mockBothIncomeSources()
            setupMockCreateSession(true)
            setupMockClearSession(true)
            when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any(), any()))
              .thenReturn(
                Right(
                  ViewIncomeSourcesViewModel(
                    viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
                    viewUkProperty = Some(viewUkPropertyDetailsViewModel),
                    viewForeignProperty = None,
                    viewCeasedBusinesses = Nil,
                    displayStartDate = true
                  )
                )
              )

            val result = testManageYourBusinessesController.showAgent()(fakeRequest)
            status(result) shouldBe Status.OK
          }
        }

        "render the error page" when {
          "the call to get income source view model fails" in {
            setupMockAgentWithClientAuth(isSupportingAgent)
            enable(DisplayBusinessStartDate)
            mockBothIncomeSources()
            when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any(), any()))
              .thenReturn(
                Left(MissingFieldException("Trading Name"))
              )

            val result = testManageYourBusinessesController.showAgent()(fakeRequest)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

          "the header carrier is missing the X-sessionId" in {
            setupMockAgentWithClientAuth(isSupportingAgent)
            enable(DisplayBusinessStartDate)
            mockBothIncomeSources()
            setupMockCreateSession(true)
            setupMockClearSession(true)
            when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any(), any()))
              .thenReturn(
                Right(
                  ViewIncomeSourcesViewModel(
                    viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
                    viewUkProperty = Some(viewUkPropertyDetailsViewModel),
                    viewForeignProperty = None,
                    viewCeasedBusinesses = Nil,
                    displayStartDate = true
                  )
                )
              )

            val headersWithoutSessionId = fakeRequest.headers.remove("X-Session-ID")

            val result = testManageYourBusinessesController.showAgent()(fakeRequest.withHeaders(headersWithoutSessionId))
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }
      testMTDAgentAuthFailures(testManageYourBusinessesController.showAgent(), isSupportingAgent)
    }
  }
}
