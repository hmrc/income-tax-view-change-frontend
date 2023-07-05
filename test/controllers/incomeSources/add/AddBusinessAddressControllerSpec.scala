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

package controllers.incomeSources.add

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import connectors.AddressLookupConnector
import controllers.routes
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import models.incomeSourceDetails.{Address, BusinessAddressModel}
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.{PostAddressLookupSuccessResponse, UnexpectedPostStatusFailure}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Logger
import play.api.http.Status.{IM_A_TEAPOT, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{Call, MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.{AddressLookupService, IncomeSourceDetailsService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.AddBusinessTrade

import java.lang
import scala.concurrent.Future


class AddBusinessAddressControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with FeatureSwitching {

  val incomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])

  val postAction: Call = controllers.routes.AddBusinessAddressController.submit(None)

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  val mockAddressLookupService: AddressLookupService = mock(classOf[AddressLookupService])
  object TestAddBusinessAddressController
    extends AddBusinessAddressController(
      MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      addressLookupService = mockAddressLookupService
    )(
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      ec = ec
    )

  val testBusinessAddressModel: BusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("Line 1", "Line 2"), Some("AA1 1AA")))
  case class AddressError(status: String) extends RuntimeException



  "AddBusinessAddressController" should {
    "redirect a user back to the custom error page" when {
      "the indivdual is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestAddBusinessAddressController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAuthorisationException()

        val result = TestAddBusinessAddressController.submit(None)(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

    ".show" should {
      "redirect to the address lookup service" when{
        "location redirect is returned by the lookup service to individual" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any())(any(), any()))
            .thenReturn(Future(Right(Some("Sample location"))))

          val result: Future[Result] = TestAddBusinessAddressController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) mustBe Some("Sample location")
        }

        "location redirect is returned by the lookup service to agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any())(any(), any()))
            .thenReturn(Future(Right(Some("Sample location"))))

          val result: Future[Result] = TestAddBusinessAddressController.showAgent()(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) mustBe Some("Sample location")
        }
      }

      "redirect back to the home page" when {
        "incomeSources switch disabled for individual" in {
          disableAllSwitches()

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessAddressController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
        "incomeSources switch disabled for agent" in {
          disableAllSwitches()

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessAddressController.showAgent()(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
        }
      }

      "return the correct error" when {
        "no location returned to the individual" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any())(any(), any()))
            .thenReturn(Future(Right(None)))

          val result: Future[Result] = TestAddBusinessAddressController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        "no location returned to the agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any())(any(), any()))
            .thenReturn(Future(Right(None)))

          val result: Future[Result] = TestAddBusinessAddressController.showAgent()(fakeRequestConfirmedClient())
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }

        "failure returned to the individual" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any())(any(), any()))
            .thenReturn(Future(Left(AddressError("Test status"))))

          val result: Future[Result] = TestAddBusinessAddressController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        "failure returned to the agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any())(any(), any()))
            .thenReturn(Future(Left(AddressError("Test status"))))

          val result: Future[Result] = TestAddBusinessAddressController.showAgent()(fakeRequestConfirmedClient())
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    ".submit" should {
      "redirect to add accounting method page" when {
        "valid data received by individual" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          when(mockAddressLookupService.fetchAddress(any())(any()))
            .thenReturn(Future(Right(testBusinessAddressModel)))

          val result: Future[Result] = TestAddBusinessAddressController.submit(Some("123"))(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          session(result).get(SessionKeys.addBusinessAddressLine1) mustBe Some("Line 1")
          session(result).get(SessionKeys.addBusinessPostalCode) mustBe Some("AA1 1AA")
          session(result).get(SessionKeys.addBusinessCountryCode) mustBe Some("GB")
        }
        "valid data received by agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          when(mockAddressLookupService.fetchAddress(any())(any()))
            .thenReturn(Future(Right(testBusinessAddressModel)))

          val result: Future[Result] = TestAddBusinessAddressController.agentSubmit(Some("123"))(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          session(result).get(SessionKeys.addBusinessAddressLine1) mustBe Some("Line 1")
          session(result).get(SessionKeys.addBusinessPostalCode) mustBe Some("AA1 1AA")
          session(result).get(SessionKeys.addBusinessCountryCode) mustBe Some("GB")
        }
      }
      "show correct error" when {
        "error returned to individual" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          when(mockAddressLookupService.fetchAddress(any())(any()))
            .thenReturn(Future(Left(AddressError("Test status"))))

          val result: Future[Result] = TestAddBusinessAddressController.submit(Some("123"))(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        "error returned to agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          when(mockAddressLookupService.fetchAddress(any())(any()))
            .thenReturn(Future(Left(AddressError("Test status"))))

          val result: Future[Result] = TestAddBusinessAddressController.agentSubmit(Some("123"))(fakeRequestConfirmedClient())
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }









  }
}
