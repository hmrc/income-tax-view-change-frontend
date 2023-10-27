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

package controllers.incomeSources.cease

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockNextUpdatesService, MockSessionService}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.Assertion
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.DateService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.PropertyDetailsTestConstants.{foreignPropertyDetails, ukPropertyDetails}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants
import testUtils.TestSupport
import views.html.incomeSources.cease.IncomeSourceCeasedObligations

import java.time.LocalDate
import scala.concurrent.Future


class IncomeSourceCeasedObligationsControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with MockNextUpdatesService
  with FeatureSwitching
  with MockSessionService {

  val mockDateService: DateService = mock(classOf[DateService])

  object TestIncomeSourceObligationController extends IncomeSourceCeasedObligationsController(
    MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    retrieveBtaNavBar = MockNavBarPredicate,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    obligationsView = app.injector.instanceOf[IncomeSourceCeasedObligations],
    mockNextUpdatesService,
    sessionService = mockSessionService)(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = ec,
    mockDateService)


  val heading: IncomeSourceType => String = {
    case SelfEmployment => messages("business-ceased.obligation.heading1.se.part2") + " " + messages("business-ceased.obligation.heading1.base")
    case ForeignProperty => messages("business-ceased.obligation.heading1.foreign-property.part2") + " " + messages("business-ceased.obligation.heading1.base")
    case UkProperty => messages("business-ceased.obligation.heading1.uk-property.part2") + " " + messages("business-ceased.obligation.heading1.base")
  }
  val title: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) => {
    if (isAgent) {
      messages("htmlTitle.agent", heading(incomeSourceType))
    } else {
      messages("htmlTitle", heading(incomeSourceType))
    }
  }

  def showCall(isAgent: Boolean, incomeSourceType: IncomeSourceType): Future[Result] = {
    (isAgent, incomeSourceType) match {
      case (true, UkProperty) => TestIncomeSourceObligationController.showAgent(UkProperty)(fakeRequestConfirmedClient())
      case (_, UkProperty) => TestIncomeSourceObligationController.show(UkProperty)(fakeRequestWithNinoAndOrigin("pta"))
      case (true, ForeignProperty) => TestIncomeSourceObligationController.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
      case (_, ForeignProperty) => TestIncomeSourceObligationController.show(ForeignProperty)(fakeRequestWithNinoAndOrigin("pta"))
      case (true, SelfEmployment) => TestIncomeSourceObligationController.showAgent(SelfEmployment)(fakeRequestConfirmedClient())
      case (_, _) => TestIncomeSourceObligationController.show(SelfEmployment)(fakeRequestWithNinoAndOrigin("pta"))
    }
  }

  "IncomeSourceCeasedObligationsController.show / showAgent" should {
    def testViewReturnsOkWithCorrectContent(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
      setupMockAuthorisationSuccess(isAgent)
      enable(IncomeSources)
      mockBothPropertyBothBusiness()
      mockGetObligationsViewModel(IncomeSourcesObligationsTestConstants.viewModel)
      setupMockGetSessionKeyMongoTyped[String](Right(Some("2022-08-27")))

      when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 1, 1))
      when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
        thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

      if (incomeSourceType.equals(ForeignProperty)) {
        when(mockIncomeSourceDetailsService.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(any())(any()))
          .thenReturn(Right(foreignPropertyDetails))
      } else if (incomeSourceType.equals(UkProperty)) {
        when(mockIncomeSourceDetailsService.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(any())(any()))
          .thenReturn(Right(ukPropertyDetails))
      }

      val result: Future[Result] = showCall(isAgent, incomeSourceType)
      val document: Document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe OK
      document.title shouldBe title(isAgent, incomeSourceType)
      document.select("#banner").text shouldBe heading(incomeSourceType)
    }

    "return 200 OK" when {
      "navigating to the Self Employment ceased page with FS enabled - Individual" in {
        testViewReturnsOkWithCorrectContent(isAgent = false, SelfEmployment)
      }
      "navigating to the Self Employment ceased page with FS enabled - Agent" in {
        testViewReturnsOkWithCorrectContent(isAgent = true, SelfEmployment)
      }
      "navigating to the UK Property ceased page with FS enabled - Individual" in {
        testViewReturnsOkWithCorrectContent(isAgent = false, UkProperty)
      }
      "navigating to the UK Property ceased page with FS enabled - Agent" in {
        testViewReturnsOkWithCorrectContent(isAgent = true, UkProperty)
      }
      "navigating to the Foreign Property ceased page with FS enabled - Individual" in {
        testViewReturnsOkWithCorrectContent(isAgent = false, ForeignProperty)
      }
      "navigating to the Foreign Property ceased page with FS enabled - Agent" in {
        testViewReturnsOkWithCorrectContent(isAgent = true, ForeignProperty)
      }
    }

    "return 303 SEE_OTHER" when {
      def testFeatureSwitch(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        setupMockAuthorisationSuccess(isAgent)
        disable(IncomeSources)
        mockBothPropertyBothBusiness()

        val result: Future[Result] = showCall(isAgent, incomeSourceType)
        val expectedRedirectUrl: String = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url

        redirectLocation(result) shouldBe Some(expectedRedirectUrl)
      }

      "navigating to the Self Employment ceased page with FS disabled - Individual" in {
        testFeatureSwitch(isAgent = false, SelfEmployment)
      }
      "navigating to the Self Employment ceased page with FS disabled - Agent" in {
        testFeatureSwitch(isAgent = true, SelfEmployment)
      }
      "navigating to the UK Property ceased page with FS disabled - Individual" in {
        testFeatureSwitch(isAgent = false, UkProperty)
      }
      "navigating to the UK Property ceased page with FS disabled - Agent" in {
        testFeatureSwitch(isAgent = true, UkProperty)
      }
      "navigating to the Foreign Property ceased page with FS disabled - Individual" in {
        testFeatureSwitch(isAgent = false, ForeignProperty)
      }
      "navigating to the Foreign Property ceased page with FS disabled - Agent" in {
        testFeatureSwitch(isAgent = true, ForeignProperty)
      }
    }
  }


  "IncomeSourceCeaObligationsController" should {
    "redirect user back to the Sign In page" when {
      "the user is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestIncomeSourceObligationController.show(SelfEmployment)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
    }
    "redirect to the session timeout page" when {
      "the user has timed out" in {
        setupMockAuthorisationException()

        val result = TestIncomeSourceObligationController.show(SelfEmployment)(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }
    "redirect to home page" when {
      "Individual - feature switch is disabled" in {
        disable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceObligationController.show(SelfEmployment)(fakeRequestWithNinoAndOrigin("pta"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "Agent - feature switch is disabled" in {
        disable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceObligationController.showAgent(SelfEmployment)(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    "redirect to ISE page" when {
      "Self-employment - missing income source ID  " in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        setupMockGetSessionKeyMongoTyped[String](Right(None))

        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

        val result: Future[Result] = TestIncomeSourceObligationController.showAgent(SelfEmployment)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "user has no active foreign properties" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockGetSessionKeyMongoTyped[String](Right(None))

        when(mockIncomeSourceDetailsService.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(any())(any()))
          .thenReturn(
            Left(
              new Error("No active foreign properties found.")
            )
          )

        val result: Future[Result] = TestIncomeSourceObligationController.show(ForeignProperty)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "user has more than one active foreign properties" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockTwoActiveForeignPropertyIncomeSourcesErrorScenario()
        setupMockGetSessionKeyMongoTyped[String](Right(None))

        when(mockIncomeSourceDetailsService.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(any())(any()))
          .thenReturn(
            Left(
              new Error("Too many active foreign properties found. There should only be one.")
            )
          )

        val result: Future[Result] = TestIncomeSourceObligationController.show(ForeignProperty)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "Individual - show obligations page for ceased income source" when {
      "IncomeSourceType is Self-employment" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockBusinessIncomeSource()
        mockGetObligationsViewModel(IncomeSourcesObligationsTestConstants.viewModel)
        setupMockGetSessionKeyMongoTyped[String](Right(Some("2022-08-27")))

        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 1, 1))
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

        val result: Future[Result] = TestIncomeSourceObligationController.show(SelfEmployment)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      "IncomeSourceType is Foreign property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        mockGetObligationsViewModel(IncomeSourcesObligationsTestConstants.viewModel)

        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 1, 1))
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))
        when(mockIncomeSourceDetailsService.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(any())(any()))
          .thenReturn(Right(foreignPropertyDetails))

        val result: Future[Result] = TestIncomeSourceObligationController.show(ForeignProperty)(fakeRequestWithActiveSession)
        status(result) shouldBe OK

      }
      "IncomeSourceType is UK property" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockUKPropertyIncomeSource()
        mockGetObligationsViewModel(IncomeSourcesObligationsTestConstants.viewModel)

        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 4, 6))
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))
        when(mockIncomeSourceDetailsService.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(any())(any()))
          .thenReturn(Right(ukPropertyDetails))

        val result = TestIncomeSourceObligationController.show(UkProperty)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
    }

    "Agent - show obligations page for ceased income source" when {
      "IncomeSourceType is Self-employment" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        mockBusinessIncomeSource()
        mockGetObligationsViewModel(IncomeSourcesObligationsTestConstants.viewModel)
        setupMockGetSessionKeyMongoTyped[String](Right(Some("2022-08-27")))

        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 12, 1))
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

        val result: Future[Result] = TestIncomeSourceObligationController.showAgent(SelfEmployment)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
      "IncomeSourceType is Foreign property" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockForeignPropertyIncomeSource()

        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 1, 1))
        mockGetObligationsViewModel(IncomeSourcesObligationsTestConstants.viewModel)
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))
        when(mockIncomeSourceDetailsService.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(any())(any()))
          .thenReturn(Right(foreignPropertyDetails))

        val result: Future[Result] = TestIncomeSourceObligationController.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
      "IncomeSourceType is UK property" in {
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockUKPropertyIncomeSourceWithCeasedUkProperty()
        mockGetObligationsViewModel(IncomeSourcesObligationsTestConstants.viewModel)

        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 4, 6))
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))
        when(mockIncomeSourceDetailsService.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(any())(any()))
          .thenReturn(Right(ukPropertyDetails))

        val result = TestIncomeSourceObligationController.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
    }
  }
}
