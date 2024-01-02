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
import services.{AddressLookupService, IncomeSourceDetailsService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired

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

  object TestAddBusinessAddressController
    extends AddBusinessAddressController(
      authorisedFunctions = mockAuthService,
      retrieveNinoWithIncomeSources = MockIncomeSourceDetailsPredicate,
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      addressLookupService = mockAddressLookupService,
      testAuthenticator
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

  def authenticate(isAgent: Boolean): Unit = {
    if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
  }

  case class AddressError(status: String) extends RuntimeException


  for (isChange <- Seq(true, false)) yield {
    for (isAgent <- Seq(true, false)) yield {
      s"AddBusinessAddressController (${if (isAgent) "Agent" else "Individual"}, isChange = $isChange)" should {
        "redirect a user back to the custom error page" when {
          "the individual is not authenticated" should {
            "redirect them to sign in" in {
              if (isAgent) setupMockAgentAuthorisationException() else setupMockAuthorisationException()
              val result = if (isAgent) TestAddBusinessAddressController.showAgent(isChange)(fakeRequestConfirmedClient())
              else TestAddBusinessAddressController.show(isChange)(fakeRequestWithActiveSession)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
            }
          }
        }
        "the user has timed out" should {
          "redirect to the session timeout page" in {
            if (isAgent) setupMockAgentAuthorisationException(exception = BearerTokenExpired()) else setupMockAuthorisationException()

            val result = if (isAgent) TestAddBusinessAddressController.agentSubmit(None, isChange)(fakeRequestWithClientDetails)
            else TestAddBusinessAddressController.submit(None, isChange)(fakeRequestWithTimeoutSession)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
          }
        }

        ".show" should {
          "redirect to the address lookup service" when {
            "location redirect is returned by the lookup service" in {
              disableAllSwitches()
              enable(IncomeSources)

              authenticate(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
              when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
                .thenReturn(Future(Right(Some("Sample location"))))

              val result: Future[Result] = if (isAgent) TestAddBusinessAddressController.showAgent(isChange)(fakeRequestConfirmedClient())
              else TestAddBusinessAddressController.show(isChange)(fakeRequestWithActiveSession)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) mustBe Some("Sample location")
            }
          }
          "redirect back to the home page" when {
            "incomeSources switch disabled" in {
              disableAllSwitches()

              authenticate(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

              val result: Future[Result] = if (isAgent) TestAddBusinessAddressController.showAgent(isChange)(fakeRequestConfirmedClient())
              else TestAddBusinessAddressController.show(isChange)(fakeRequestWithActiveSession)
              status(result) shouldBe SEE_OTHER
              val homeUrl = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
              redirectLocation(result) shouldBe Some(homeUrl)
            }
          }
          "return the correct error" when {
            "no location returned" in {
              disableAllSwitches()
              enable(IncomeSources)

              authenticate(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
              when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
                .thenReturn(Future(Right(None)))

              val result: Future[Result] = if (isAgent) TestAddBusinessAddressController.showAgent(isChange)(fakeRequestConfirmedClient())
              else TestAddBusinessAddressController.show(isChange)(fakeRequestWithActiveSession)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "failure returned" in {
              disableAllSwitches()
              enable(IncomeSources)

              authenticate(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
              when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
                .thenReturn(Future(Left(AddressError("Test status"))))

              val result: Future[Result] = if (isAgent) TestAddBusinessAddressController.showAgent(isChange)(fakeRequestConfirmedClient())
              else TestAddBusinessAddressController.show(isChange)(fakeRequestWithActiveSession)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }

        ".submit" should {
          "redirect to add accounting method page" when {
            "valid data received" in {
              disableAllSwitches()
              enable(IncomeSources)

              authenticate(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

              setupMockGetMongo(Right(Some(UIJourneySessionData("", ""))))
              setupMockSetMongoData(result = true)
              when(mockAddressLookupService.fetchAddress(any())(any()))
                .thenReturn(Future(Right(testBusinessAddressModel)))

              val result: Future[Result] = if (isAgent) TestAddBusinessAddressController.agentSubmit(Some("123"), isChange)(fakeRequestConfirmedClient())
              else TestAddBusinessAddressController.submit(Some("123"), isChange)(fakeRequestWithActiveSession)
              status(result) shouldBe SEE_OTHER
              verifySetMongoData()

            }
          }
          "show correct error" when {
            "error returned to individual" in {
              disableAllSwitches()
              enable(IncomeSources)

              authenticate(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

              when(mockAddressLookupService.fetchAddress(any())(any()))
                .thenReturn(Future(Left(AddressError("Test status"))))

              val result: Future[Result] = if (isAgent) TestAddBusinessAddressController.agentSubmit(Some("123"), isChange)(fakeRequestConfirmedClient())
              else TestAddBusinessAddressController.submit(Some("123"), isChange)(fakeRequestWithActiveSession)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }
      }
    }
  }
}
