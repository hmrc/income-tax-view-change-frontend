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

package controllers.manageBusinesses.add

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.BusinessTradeForm
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockIncomeSourceDetailsService, MockSessionService}
import models.incomeSourceDetails.AddIncomeSourceData
import org.mockito.Mockito.mock
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{addedIncomeSourceUIJourneySessionData, businessesAndPropertyIncome, completedUIJourneySessionData, emptyUIJourneySessionData}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import views.html.manageBusinesses.add.AddBusinessTrade

import scala.concurrent.Future


class AddBusinessTradeControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockIncomeSourceDetailsService
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with FeatureSwitching
  with MockSessionService {

  val mockAddBusinessTradeView: AddBusinessTrade = mock(classOf[AddBusinessTrade])
  val mockBusinessTradeForm: BusinessTradeForm = mock(classOf[BusinessTradeForm])

  val validBusinessTrade: String = "Test Business Trade"
  val validBusinessName: String = "Test Business Name"
  val journeyType: JourneyType = JourneyType(Add, SelfEmployment)

  object TestAddBusinessTradeController
    extends AddBusinessTradeController(
      authorisedFunctions = mockAuthService,
      addBusinessTradeView = app.injector.instanceOf[AddBusinessTrade],
      retrieveNinoWithIncomeSources = MockIncomeSourceDetailsPredicate,
      sessionService = mockSessionService,
      testAuthenticator
    )(
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      ec = ec
    )

  def getRequest(isAgent: Boolean) = {
    if (isAgent) fakeRequestConfirmedClient()
    else fakeRequestWithActiveSession
  }

  def postRequest(isAgent: Boolean) = {
    if (isAgent) fakePostRequestConfirmedClient()
    else fakePostRequestWithActiveSession
  }

  for (isAgent <- Seq(true, false)) yield {
    for (isChange <- Seq(true, false)) yield {
      s"AddBusinessTradeController (${if (isAgent) "Agent" else "Individual"}, isChange = $isChange)" should {
        ".show" should {
          "redirect a user back to the custom error page" when {
            "the individual is not authenticated" should {
              "redirect them to sign in" in {
                if (isAgent) setupMockAgentAuthorisationException() else setupMockAuthorisationException()
                val result = TestAddBusinessTradeController.show(isAgent, isChange)(getRequest(isAgent))
                status(result) shouldBe SEE_OTHER
                redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
              }
            }
          }

          "show correct page when user valid" in {
            disableAllSwitches()
            enable(IncomeSources)

            setupMockAuthorisationSuccess(isAgent)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            setupMockCreateSession(true)
            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, SelfEmployment))
              .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName)))))))

            val result: Future[Result] = TestAddBusinessTradeController.show(isAgent, isChange)(getRequest(isAgent))
            status(result) shouldBe OK
          }
        }


        ".submit trade" when {
          "the user has timed out" should {
            "redirect to the session timeout page" in {
              if (isAgent) setupMockAgentAuthorisationException(exception = BearerTokenExpired()) else setupMockAuthorisationException()

              val result = TestAddBusinessTradeController.submit(isAgent, isChange)(
                if (isAgent) fakeRequestWithClientDetails else fakeRequestWithTimeoutSession)

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
            }
          }
          "redirect to the add business address page" when {
            "the individual is authenticated and the business trade entered is valid" in {
              disableAllSwitches()
              enable(IncomeSources)

              setupMockAuthorisationSuccess(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName), businessTrade = Some(validBusinessTrade)))))))
              setupMockSetMongoData(true)

              val result: Future[Result] =
                TestAddBusinessTradeController.submit(isAgent, isChange)(postRequest(isAgent).withFormUrlEncodedBody(
                  BusinessTradeForm.businessTrade -> validBusinessTrade
                ))
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some {
                (isChange, isAgent) match {
                  case (false, false) => controllers.manageBusinesses.add.routes.AddBusinessAddressController.show(isChange).url
                  case (false, true) => controllers.manageBusinesses.add.routes.AddBusinessAddressController.showAgent(isChange).url
                  case (true, false) => controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url
                  case (true, true) => controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url
                }
              }
            }
          }

          "return to add business trade page" when {
            "trade name is same as business name" in {
              disableAllSwitches()
              enable(IncomeSources)

              setupMockAuthorisationSuccess(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

              setupMockCreateSession(true)
              val businessNameAsTrade: String = "Test Name"
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(businessNameAsTrade),
                  businessTrade = Some(businessNameAsTrade)))))))
              val result: Future[Result] =
                TestAddBusinessTradeController.submit(isAgent, isChange)(postRequest(isAgent).withFormUrlEncodedBody(
                  BusinessTradeForm.businessTrade -> businessNameAsTrade
                ))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Trade and business name cannot be the same")
            }

            "trade name contains invalid characters" in {
              disableAllSwitches()
              enable(IncomeSources)

              val invalidBusinessTradeChar: String = "££"
              setupMockAuthorisationSuccess(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName),
                  businessTrade = Some(invalidBusinessTradeChar)))))))

              val result: Future[Result] =
                TestAddBusinessTradeController.submit(isAgent, isChange)(postRequest(isAgent).withFormUrlEncodedBody(
                  BusinessTradeForm.businessTrade -> invalidBusinessTradeChar
                ))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Business trade cannot include !, &quot;&quot;, * or ?")
            }

            "trade name is empty" in {
              disableAllSwitches()
              enable(IncomeSources)

              val invalidBusinessTradeEmpty: String = ""
              setupMockAuthorisationSuccess(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName),
                  businessTrade = Some(invalidBusinessTradeEmpty)))))))

              val result: Future[Result] =
                TestAddBusinessTradeController.submit(isAgent, isChange)(postRequest(isAgent).withFormUrlEncodedBody(
                  BusinessTradeForm.businessTrade -> invalidBusinessTradeEmpty
                ))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Enter the trade of your business")
            }

            "trade name is too short" in {
              disableAllSwitches()
              enable(IncomeSources)

              val invalidBusinessTradeShort: String = "A"
              setupMockAuthorisationSuccess(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName),
                  businessTrade = Some(invalidBusinessTradeShort)))))))

              val result: Future[Result] =
                TestAddBusinessTradeController.submit(isAgent, isChange)(postRequest(isAgent).withFormUrlEncodedBody(
                  BusinessTradeForm.businessTrade -> invalidBusinessTradeShort
                ))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Business trade must have at least two letters")
            }

            "trade name is too long" in {
              disableAllSwitches()
              enable(IncomeSources)

              val invalidBusinessTradeLong: String = "This trade name is far too long to be accepted"
              setupMockAuthorisationSuccess(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName),
                  businessTrade = Some(invalidBusinessTradeLong)))))))

              val result: Future[Result] =
                TestAddBusinessTradeController.submit(isAgent, isChange)(postRequest(isAgent).withFormUrlEncodedBody(
                  BusinessTradeForm.businessTrade -> invalidBusinessTradeLong
                ))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Business trade must be 35 characters or fewer")
            }
          }
        }

        "when feature switch is disabled" in {
          disableAllSwitches()

          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessTradeController.show(isAgent, isChange)(getRequest(isAgent))
          status(result) shouldBe SEE_OTHER
          val homeUrl = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
          redirectLocation(result) shouldBe Some(homeUrl)
        }
        s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page" when {
          "user has already completed the journey" in {
            disableAllSwitches()
            enable(IncomeSources)
            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockGetMongo(Right(Some(completedUIJourneySessionData(JourneyType(Add, SelfEmployment)))))

            val result: Future[Result] = TestAddBusinessTradeController.show(isAgent, isChange)(getRequest(isAgent))
            status(result) shouldBe SEE_OTHER
            val redirectUrl = if (isAgent) controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.showAgent(SelfEmployment).url
            else controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.show(SelfEmployment).url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
          "user has already added their income source" in {
            disableAllSwitches()
            enable(IncomeSources)
            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockGetMongo(Right(Some(addedIncomeSourceUIJourneySessionData(SelfEmployment))))

            val result: Future[Result] = TestAddBusinessTradeController.show(isAgent, isChange)(getRequest(isAgent))
            status(result) shouldBe SEE_OTHER
            val redirectUrl = if (isAgent) controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.showAgent(SelfEmployment).url
            else controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.show(SelfEmployment).url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }
    }
  }
}
