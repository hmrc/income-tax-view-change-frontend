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
import forms.incomeSources.add.BusinessNameForm
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockSessionService}
import models.incomeSourceDetails.AddIncomeSourceData
import models.incomeSourceDetails.AddIncomeSourceData.{businessNameField, businessTradeField}
import org.mockito.Mockito.mock
import org.scalatest.matchers.must.Matchers._
import play.api.http.Status
import play.api.mvc.{Call, MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import views.html.incomeSources.add.AddBusinessName

import scala.concurrent.Future


class AddBusinessNameControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with FeatureSwitching
  with MockSessionService {

  val mockAddBusinessNameView: AddBusinessName = mock(classOf[AddBusinessName])
  val mockBusinessNameForm: BusinessNameForm = mock(classOf[BusinessNameForm])
  val postAction: Call = controllers.manageBusinesses.add.routes.AddBusinessNameController.submit()

  object TestAddBusinessNameController
    extends AddBusinessNameController(
      authorisedFunctions = mockAuthService,
      addBusinessView = app.injector.instanceOf[AddBusinessName],
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      sessionService = mockSessionService,
      testAuthenticator
    )(
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      ec = ec
    )

  val validBusinessName: String = "Test Business Name"
  val journeyType: JourneyType = JourneyType(Add, SelfEmployment)

  for (isAgent <- Seq(true, false)) yield {
    for (isChange <- Seq(true, false)) yield {
      s"ADD - AddBusinessNameController.show (${if (isAgent) "Agent" else "Individual"}, isChange = $isChange)" should {
        "return 200 OK" when {
          "the user is authenticated" in {
            disableAllSwitches()
            enable(IncomeSources)
            setupMockAuthorisationSuccess(isAgent)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            if (isChange) {
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Add, SelfEmployment)))))
              setupMockGetSessionKeyMongoTyped[String](businessNameField, journeyType, Right(Some(validBusinessName)))
              setupMockGetSessionKeyMongoTyped[String](businessTradeField, journeyType, Right(Some("Test Business Trade")))
            }
            else {
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, SelfEmployment)))))
            }

            val result: Future[Result] = (isChange, isAgent) match {
              case (false, false) => TestAddBusinessNameController.show()(fakeRequestWithActiveSession)
              case (false, true) => TestAddBusinessNameController.showAgent()(fakeRequestConfirmedClient())
              case (true, false) => TestAddBusinessNameController.changeBusinessName()(fakeRequestWithActiveSession)
              case (true, true) => TestAddBusinessNameController.changeBusinessNameAgent()(fakeRequestConfirmedClient())
            }

            status(result) mustBe OK
          }
        }

        "return 303 and redirect to the sign in" when {
          "the user is not authenticated" in {
            if (isAgent) setupMockAgentAuthorisationException() else setupMockAuthorisationException()
            val result = (isChange, isAgent) match {
              case (false, false) => TestAddBusinessNameController.show()(fakeRequestWithActiveSession)
              case (false, true) => TestAddBusinessNameController.showAgent()(fakeRequestConfirmedClient())
              case (true, false) => TestAddBusinessNameController.changeBusinessName()(fakeRequestWithActiveSession)
              case (true, true) => TestAddBusinessNameController.changeBusinessNameAgent()(fakeRequestConfirmedClient())
            }
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
          }
        }
        "redirect to the session timeout page" when {
          "the user has timed out" in {
            if (isAgent) setupMockAgentAuthorisationException(exception = BearerTokenExpired()) else setupMockAuthorisationException()

            val result = (isChange, isAgent) match {
              case (false, false) => TestAddBusinessNameController.show()(fakeRequestWithTimeoutSession)
              case (false, true) => TestAddBusinessNameController.showAgent()(fakeRequestConfirmedClient())
              case (true, false) => TestAddBusinessNameController.changeBusinessName()(fakeRequestWithTimeoutSession)
              case (true, true) => TestAddBusinessNameController.changeBusinessNameAgent()(fakeRequestConfirmedClient())
            }

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
          }
        }

        "return 303 and show home page" when {
          "when feature switch is disabled" in {
            disableAllSwitches()

            setupMockAuthorisationSuccess(isAgent)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            setupMockCreateSession(true)

            val result: Future[Result] = (isChange, isAgent) match {
              case (false, false) => TestAddBusinessNameController.show()(fakeRequestWithActiveSession)
              case (false, true) => TestAddBusinessNameController.showAgent()(fakeRequestConfirmedClient())
              case (true, false) => TestAddBusinessNameController.changeBusinessName()(fakeRequestWithActiveSession)
              case (true, true) => TestAddBusinessNameController.changeBusinessNameAgent()(fakeRequestConfirmedClient())
            }
            status(result) shouldBe SEE_OTHER
            val homeUrl = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
            redirectLocation(result) shouldBe Some(homeUrl)
          }
        }
        s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page" when {
          "user has already completed the journey" in {
            disableAllSwitches()
            enable(IncomeSources)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockGetMongo(Right(Some(completedUIJourneySessionData(JourneyType(Add, SelfEmployment)))))

            val result: Future[Result] = (isChange, isAgent) match {
              case (false, false) => TestAddBusinessNameController.show()(fakeRequestWithActiveSession)
              case (false, true) => TestAddBusinessNameController.showAgent()(fakeRequestConfirmedClient())
              case (true, false) => TestAddBusinessNameController.changeBusinessName()(fakeRequestWithActiveSession)
              case (true, true) => TestAddBusinessNameController.changeBusinessNameAgent()(fakeRequestConfirmedClient())
            }
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

            val result: Future[Result] = (isChange, isAgent) match {
              case (false, false) => TestAddBusinessNameController.show()(fakeRequestWithActiveSession)
              case (false, true) => TestAddBusinessNameController.showAgent()(fakeRequestConfirmedClient())
              case (true, false) => TestAddBusinessNameController.changeBusinessName()(fakeRequestWithActiveSession)
              case (true, true) => TestAddBusinessNameController.changeBusinessNameAgent()(fakeRequestConfirmedClient())
            }
            status(result) shouldBe SEE_OTHER
            val redirectUrl = if (isAgent) controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.showAgent(SelfEmployment).url
            else controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.show(SelfEmployment).url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }

      s"ADD - AddBusinessNameController.submit (${if (isAgent) "Agent" else "Individual"}, isChange = $isChange)" should {
        "return 303 and redirect to add business start date" when {
          "the individual is authenticated and the business name entered is valid" in {
            disableAllSwitches()
            enable(IncomeSources)

            setupMockAuthorisationSuccess(isAgent)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            setupMockCreateSession(true)
            setupMockSetMongoData(true)
            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Add, SelfEmployment)))))

            val (result, redirectUrl) = (isChange, isAgent) match {
              case (false, false) => (TestAddBusinessNameController.submit()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
                BusinessNameForm.businessName -> validBusinessName)), controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = isAgent, isChange = isChange).url)
              case (false, true) => (TestAddBusinessNameController.submitAgent()(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
                BusinessNameForm.businessName -> validBusinessName)), controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = isAgent, isChange = isChange).url)
              case (true, false) => (TestAddBusinessNameController.submitChange()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
                BusinessNameForm.businessName -> validBusinessName)), controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url)
              case (true, true) => (TestAddBusinessNameController.submitChangeAgent()(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
                BusinessNameForm.businessName -> validBusinessName)), controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url)
            }

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(redirectUrl)
          }

          "show AddBusinessName with error" when {
            "Business name is empty" in {
              disableAllSwitches()
              enable(IncomeSources)

              val invalidBusinessNameEmpty: String = ""
              setupMockAuthorisationSuccess(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, SelfEmployment)))))

              val result: Future[Result] = (isChange, isAgent) match {
                case (false, false) => TestAddBusinessNameController.submit()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> invalidBusinessNameEmpty))
                case (false, true) => TestAddBusinessNameController.submitAgent()(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> invalidBusinessNameEmpty))
                case (true, false) => TestAddBusinessNameController.submitChange()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> invalidBusinessNameEmpty))
                case (true, true) => TestAddBusinessNameController.submitChangeAgent()(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> invalidBusinessNameEmpty))
              }

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Enter your name or the name of your business")
            }

            "Business name is too long" in {
              disableAllSwitches()
              enable(IncomeSources)

              val invalidBusinessNameLength: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"
              setupMockAuthorisationSuccess(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, SelfEmployment)))))

              val result: Future[Result] = (isChange, isAgent) match {
                case (false, false) => TestAddBusinessNameController.submit()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> invalidBusinessNameLength))
                case (false, true) => TestAddBusinessNameController.submitAgent()(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> invalidBusinessNameLength))
                case (true, false) => TestAddBusinessNameController.submitChange()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> invalidBusinessNameLength))
                case (true, true) => TestAddBusinessNameController.submitChangeAgent()(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> invalidBusinessNameLength))
              }

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Business name must be 105 characters or fewer")
            }

            "Business name has invalid characters" in {
              disableAllSwitches()
              enable(IncomeSources)
              val invalidBusinessNameEmpty: String = "££"
              setupMockAuthorisationSuccess(isAgent)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, SelfEmployment)))))

              val result: Future[Result] = (isChange, isAgent) match {
                case (false, false) => TestAddBusinessNameController.submit()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> invalidBusinessNameEmpty))
                case (false, true) => TestAddBusinessNameController.submitAgent()(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> invalidBusinessNameEmpty))
                case (true, false) => TestAddBusinessNameController.submitChange()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> invalidBusinessNameEmpty))
                case (true, true) => TestAddBusinessNameController.submitChangeAgent()(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> invalidBusinessNameEmpty))
              }

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Business name cannot include !, &quot;&quot;, * or ?")
            }
            if (isChange) {
              "show invalid error when business name is same as business trade name" in {
                disableAllSwitches()
                enable(IncomeSources)
                setupMockAuthorisationSuccess(isAgent)
                setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
                setupMockCreateSession(true)
                val businessName: String = "Plumbing"
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(businessName),
                    businessTrade = Some(businessName)))))))

                val result: Future[Result] = if (isAgent) TestAddBusinessNameController.submitChangeAgent()(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> businessName))
                else TestAddBusinessNameController.submitChange()(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> businessName))

                status(result) mustBe BAD_REQUEST
                contentAsString(result) must include("Trade and business name cannot be the same")
              }
            }
          }
        }
      }
    }
  }
}
