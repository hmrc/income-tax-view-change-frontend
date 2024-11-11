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
import models.admin.IncomeSources
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

class ManageYourBusinessesControllerSpec extends MockAuthActions
  with ImplicitDateFormatter
  with MockSessionService {

  override def fakeApplication(): Application = applicationBuilderWithAuthBindings()
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  val testManageYourBusinessesController = fakeApplication().injector.instanceOf[ManageYourBusinessesController]

  "show()" when {
    "the user is authenticated" should {

      "render the manage businesses page" when {
        "the IncomeSources FS in enabled" in {
          setupMockUserAuth
          enable(IncomeSources)
          mockBothIncomeSources()
          setupMockCreateSession(true)
          setupMockClearSession(true)
          when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any()))
            .thenReturn(
              Right(
                ViewIncomeSourcesViewModel(
                  viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
                  viewUkProperty = Some(viewUkPropertyDetailsViewModel),
                  viewForeignProperty = None,
                  viewCeasedBusinesses = Nil
                )
              )
            )

          val result = testManageYourBusinessesController.show()(fakeRequestWithActiveSession)
          result.map(res => println(res))
          status(result) shouldBe Status.OK
        }
      }

      "redirect to the home page" when {
        "the IncomeSources FS is disabled" in {
          disable(IncomeSources)
          setupMockUserAuth
          mockBothIncomeSources()

          val result = testManageYourBusinessesController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
      }

      "render the error page" when {
        "the call to get income source view model fails" in {
          setupMockUserAuth
          enable(IncomeSources)
          mockBothIncomeSources()
          when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any()))
            .thenReturn(
              Left(MissingFieldException("Trading Name"))
            )

          val result = testManageYourBusinessesController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "the header carrier is missing the X-sessionId" in {
          setupMockUserAuth
          enable(IncomeSources)
          mockBothIncomeSources()
          setupMockCreateSession(true)
          setupMockClearSession(true)
          when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any()))
            .thenReturn(
              Right(
                ViewIncomeSourcesViewModel(
                  viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
                  viewUkProperty = Some(viewUkPropertyDetailsViewModel),
                  viewForeignProperty = None,
                  viewCeasedBusinesses = Nil
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
          "the IncomeSources FS in enabled" in {
            setupMockAgentWithClientAuth(isSupportingAgent)
            enable(IncomeSources)
            mockBothIncomeSources()
            setupMockCreateSession(true)
            setupMockClearSession(true)
            when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any()))
              .thenReturn(
                Right(
                  ViewIncomeSourcesViewModel(
                    viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
                    viewUkProperty = Some(viewUkPropertyDetailsViewModel),
                    viewForeignProperty = None,
                    viewCeasedBusinesses = Nil
                  )
                )
              )

            val result = testManageYourBusinessesController.showAgent()(fakeRequest)
            status(result) shouldBe Status.OK
          }
        }

        "redirect to the home page" when {
          "the IncomeSources FS is disabled" in {
            disable(IncomeSources)
            setupMockAgentWithClientAuth(isSupportingAgent)
            mockBothIncomeSources()

            val result = testManageYourBusinessesController.showAgent()(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
          }
        }

        "render the error page" when {
          "the call to get income source view model fails" in {
            setupMockAgentWithClientAuth(isSupportingAgent)
            enable(IncomeSources)
            mockBothIncomeSources()
            when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any()))
              .thenReturn(
                Left(MissingFieldException("Trading Name"))
              )

            val result = testManageYourBusinessesController.showAgent()(fakeRequest)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

          "the header carrier is missing the X-sessionId" in {
            setupMockAgentWithClientAuth(isSupportingAgent)
            enable(IncomeSources)
            mockBothIncomeSources()
            setupMockCreateSession(true)
            setupMockClearSession(true)
            when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any()))
              .thenReturn(
                Right(
                  ViewIncomeSourcesViewModel(
                    viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
                    viewUkProperty = Some(viewUkPropertyDetailsViewModel),
                    viewForeignProperty = None,
                    viewCeasedBusinesses = Nil
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
