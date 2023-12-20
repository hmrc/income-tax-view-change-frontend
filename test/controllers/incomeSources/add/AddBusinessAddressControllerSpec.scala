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

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.SelfEmployment
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockSessionService}
import models.incomeSourceDetails.{AddIncomeSourceData, Address, BusinessAddressModel, UIJourneySessionData}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.mvc.{Call, MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.{AddressLookupService, IncomeSourceDetailsService, SessionService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import utils.Authenticator

import scala.concurrent.Future


class AddBusinessAddressControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with FeatureSwitching
  with MockSessionService {

  val incomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])

  val postAction: Call = controllers.incomeSources.add.routes.AddBusinessAddressController.submit(None, isChange = false)
  val postActionChange: Call = controllers.incomeSources.add.routes.AddBusinessAddressController.submit(None, isChange = true)
  val redirectAction: Call = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
  val redirectActionAgent: Call = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
  val mockAddressLookupService: AddressLookupService = mock(classOf[AddressLookupService])

  val auth = new Authenticator(app.injector.instanceOf[SessionTimeoutPredicate], MockAuthenticationPredicate, mockAuthService, MockNavBarPredicate, MockIncomeSourceDetailsPredicate, mockIncomeSourceDetailsService)(
    app.injector.instanceOf[MessagesControllerComponents], app.injector.instanceOf[FrontendAppConfig], mockItvcErrorHandler, ec)


  object TestAddBusinessAddressController
    extends AddBusinessAddressController(
      authorisedFunctions = mockAuthService,
      retrieveNinoWithIncomeSources = MockIncomeSourceDetailsPredicate,
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      addressLookupService = mockAddressLookupService,
      auth
    )(
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      ec = ec,
      sessionService = mockSessionService
    )

  val testBusinessAddressModel: BusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("Line 1", "Line 2"), Some("AA1 1AA")))
  val testAddIncomeSourceSessionData: Option[AddIncomeSourceData] = Some(AddIncomeSourceData(address = Some(testBusinessAddressModel.address), countryCode = Some("GB")))
  val testUIJourneySessionData: UIJourneySessionData = UIJourneySessionData("", "", testAddIncomeSourceSessionData)

  def verifySetMongoData(): Unit = {
    val argument: ArgumentCaptor[UIJourneySessionData] = ArgumentCaptor.forClass(classOf[UIJourneySessionData])
    verify(mockSessionService).setMongoData(argument.capture())(any(), any())
    argument.getValue.addIncomeSourceData shouldBe testAddIncomeSourceSessionData
  }

  case class AddressError(status: String) extends RuntimeException


  "AddBusinessAddressController" should {
    "redirect a user back to the custom error page" when {
      "the individual is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestAddBusinessAddressController.show(isChange = false)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAuthorisationException()

        val result = TestAddBusinessAddressController.submit(None, isChange = false)(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

    ".show" should {
      "redirect to the address lookup service" when {
        "location redirect is returned by the lookup service to individual" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
            .thenReturn(Future(Right(Some("Sample location"))))

          val result: Future[Result] = TestAddBusinessAddressController.show(isChange = false)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) mustBe Some("Sample location")
        }
        "location redirect is returned by the lookup service to agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
            .thenReturn(Future(Right(Some("Sample location"))))

          val result: Future[Result] = TestAddBusinessAddressController.showAgent(isChange = false)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) mustBe Some("Sample location")
        }
      }
      "redirect to the address lookup service on change page" when {
        "location redirect is returned by the lookup service to individual and isChange = true" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
            .thenReturn(Future(Right(Some("Sample location"))))

          val result: Future[Result] = TestAddBusinessAddressController.show(isChange = true)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) mustBe Some("Sample location")
        }

        "location redirect is returned by the lookup service to agent and isChange = true" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
            .thenReturn(Future(Right(Some("Sample location"))))

          val result: Future[Result] = TestAddBusinessAddressController.showAgent(isChange = true)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) mustBe Some("Sample location")
        }
      }
      "redirect back to the home page" when {
        "incomeSources switch disabled for individual" in {
          disableAllSwitches()

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessAddressController.show(isChange = false)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
        "incomeSources switch disabled for agent" in {
          disableAllSwitches()

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessAddressController.showAgent(isChange = false)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
        }
      }
      "redirect back to the home page after pressing change link" when {
        "incomeSources switch disabled for individual" in {
          disableAllSwitches()

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessAddressController.show(isChange = true)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
        "incomeSources switch disabled for agent" in {
          disableAllSwitches()

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessAddressController.showAgent(isChange = true)(fakeRequestConfirmedClient())
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
          when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
            .thenReturn(Future(Right(None)))

          val result: Future[Result] = TestAddBusinessAddressController.show(isChange = false)(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        "no location returned to the agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
            .thenReturn(Future(Right(None)))

          val result: Future[Result] = TestAddBusinessAddressController.showAgent(isChange = false)(fakeRequestConfirmedClient())
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }

        "failure returned to the individual" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
            .thenReturn(Future(Left(AddressError("Test status"))))

          val result: Future[Result] = TestAddBusinessAddressController.show(isChange = false)(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        "failure returned to the agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
            .thenReturn(Future(Left(AddressError("Test status"))))

          val result: Future[Result] = TestAddBusinessAddressController.showAgent(isChange = false)(fakeRequestConfirmedClient())
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
      "return the correct error on change page" when {
        "no location returned to the individual and isChange = true" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
            .thenReturn(Future(Right(None)))

          val result: Future[Result] = TestAddBusinessAddressController.show(isChange = true)(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        "no location returned to the agent and isChange = true" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
            .thenReturn(Future(Right(None)))

          val result: Future[Result] = TestAddBusinessAddressController.showAgent(isChange = true)(fakeRequestConfirmedClient())
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }

        "failure returned to the individual and isChange = true" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
            .thenReturn(Future(Left(AddressError("Test status"))))

          val result: Future[Result] = TestAddBusinessAddressController.show(isChange = true)(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        "failure returned to the agent and isChange = true" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
            .thenReturn(Future(Left(AddressError("Test status"))))

          val result: Future[Result] = TestAddBusinessAddressController.showAgent(isChange = true)(fakeRequestConfirmedClient())
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

          setupMockGetMongo(Right(Some(UIJourneySessionData("", ""))))
          setupMockSetMongoData(result = true)
          when(mockAddressLookupService.fetchAddress(any())(any()))
            .thenReturn(Future(Right(testBusinessAddressModel)))

          val result: Future[Result] = TestAddBusinessAddressController.submit(Some("123"), isChange = false)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          verifySetMongoData()

        }
        "valid data received by agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          setupMockGetMongo(Right(Some(UIJourneySessionData("", ""))))
          setupMockSetMongoData(result = true)
          when(mockAddressLookupService.fetchAddress(any())(any()))
            .thenReturn(Future(Right(testBusinessAddressModel)))

          val result: Future[Result] = TestAddBusinessAddressController.agentSubmit(Some("123"), isChange = false)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          verifySetMongoData()
        }
      }
      "redirect to the check your details page" when {
        "valid data received by individual and isChange = true" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          setupMockGetMongo(Right(Some(UIJourneySessionData("", ""))))
          setupMockSetMongoData(result = true)
          when(mockAddressLookupService.fetchAddress(any())(any()))
            .thenReturn(Future(Right(testBusinessAddressModel)))

          val result: Future[Result] = TestAddBusinessAddressController.submit(Some("123"), isChange = true)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          verifySetMongoData()
        }
        "valid data received by agent and isChange = true" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          setupMockGetMongo(Right(Some(UIJourneySessionData("", ""))))
          setupMockSetMongoData(result = true)
          when(mockAddressLookupService.fetchAddress(any())(any()))
            .thenReturn(Future(Right(testBusinessAddressModel)))

          val result: Future[Result] = TestAddBusinessAddressController.agentSubmit(Some("123"), isChange = true)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          verifySetMongoData()
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

          val result: Future[Result] = TestAddBusinessAddressController.submit(Some("123"), isChange = false)(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        "error returned to agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          when(mockAddressLookupService.fetchAddress(any())(any()))
            .thenReturn(Future(Left(AddressError("Test status"))))

          val result: Future[Result] = TestAddBusinessAddressController.agentSubmit(Some("123"), isChange = false)(fakeRequestConfirmedClient())
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
      "show correct error on change page" when {
        "error returned to individual when isChange = true" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          when(mockAddressLookupService.fetchAddress(any())(any()))
            .thenReturn(Future(Left(AddressError("Test status"))))

          val result: Future[Result] = TestAddBusinessAddressController.submit(Some("123"), isChange = true)(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        "error returned to agent when isChange = true" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          when(mockAddressLookupService.fetchAddress(any())(any()))
            .thenReturn(Future(Left(AddressError("Test status"))))

          val result: Future[Result] = TestAddBusinessAddressController.agentSubmit(Some("123"), isChange = true)(fakeRequestConfirmedClient())
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}
