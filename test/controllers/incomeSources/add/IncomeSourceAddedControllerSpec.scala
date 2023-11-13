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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney._
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockNextUpdatesService}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{redirectLocation, status}
import services.DateService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testSelfEmploymentId}
import testUtils.TestSupport
import views.html.incomeSources.add.IncomeSourceAddedObligations
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.defaultAwaitTimeout
import testConstants.BaseTestConstants.testPropertyIncomeId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants

import java.time.LocalDate
import scala.collection.immutable.List
import scala.concurrent.Future

class IncomeSourceAddedControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with MockNextUpdatesService
  with FeatureSwitching {

  val mockDateService: DateService = mock(classOf[DateService])

  object TestIncomeSourceAddedController extends IncomeSourceAddedController(
    MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    retrieveBtaNavBar = MockNavBarPredicate,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    obligationsView = app.injector.instanceOf[IncomeSourceAddedObligations],
    mockNextUpdatesService
  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = ec,
    mockDateService
  )

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(testSelfEmploymentId, List(NextUpdateModel(
      LocalDate.of(2022, 7, 1),
      LocalDate.of(2022, 7, 2),
      LocalDate.of(2022, 8, 2),
      "Quarterly",
      None,
      "#001"
    ),
      NextUpdateModel(
        LocalDate.of(2022, 7, 1),
        LocalDate.of(2022, 7, 2),
        LocalDate.of(2022, 8, 2),
        "Quarterly",
        None,
        "#002"
      )
    ))
  ))

  def mockSelfEmployment(): Unit = {
    when(mockIncomeSourceDetailsService.getIncomeSourceFromUser(any(),any())(any())).thenReturn(
      Some((LocalDate.parse("2022-01-01"), Some("Business Name")))
    )
  }

  def mockProperty(): Unit = {
    when(mockIncomeSourceDetailsService.getIncomeSourceFromUser(any(), any())(any())).thenReturn(
      Some((LocalDate.parse("2022-01-01"), None))
    )
  }

  def mockFailure(): Unit = {
    when(mockIncomeSourceDetailsService.getIncomeSourceFromUser(any(), any())(any())).thenReturn(
      None
    )
  }


  "IncomeSourceAddedController" should {
    "redirect a user back to the custom error page" when {
      "the user is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestIncomeSourceAddedController.show("", SelfEmployment)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAuthorisationException()

        val result = TestIncomeSourceAddedController.submit()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

    "feature switch is disabled" should {
      "redirect to home page" in {
        disableAllSwitches()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceAddedController.show("123", UkProperty)(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "redirect to home page (agent)" in {
        disableAllSwitches()

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceAddedController.showAgent("123", ForeignProperty)(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }


    ".show with IncomeSourceType = SelfEmployment" should {
      "show correct page when individual valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List(BusinessDetailsModel(
          testSelfEmploymentId,
          None,
          Some("Test name"),
          None,
          Some(LocalDate.of(2022, 1, 1)),
          None
        )), List.empty)
        mockSelfEmployment()

        val day = LocalDate.of(2023, 1, 1)
        val dates: Seq[DatesModel] = Seq(
          DatesModel(day, day, day, "EOPS", isFinalDec = false, obligationType = "EOPS")
        )
        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 1, 1))
        setupMockGetIncomeSourceDetails()(sources)
        when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(Future(ObligationsViewModel(
          dates,
          dates,
          dates,
          dates,
          2023,
          showPrevTaxYears = true
        )))
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(testObligationsModel))

        val result: Future[Result] = TestIncomeSourceAddedController.show(testSelfEmploymentId, SelfEmployment)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      "show correct page when agent valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        mockSingleBusinessIncomeSourceWithDeadlines()
        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List(BusinessDetailsModel(
          testSelfEmploymentId,
          None,
          Some("Test name"),
          None,
          Some(LocalDate.of(2022, 1, 1)),
          None
        )), List.empty)
        mockSelfEmployment()
        val day = LocalDate.of(2023, 1, 1)
        val dates: Seq[DatesModel] = Seq(
          DatesModel(day, day, day, "EOPS", isFinalDec = false, obligationType = "EOPS")
        )
        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 12, 1))
        setupMockGetIncomeSourceDetails()(sources)
        when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(Future(ObligationsViewModel(
          dates,
          dates,
          dates,
          dates,
          2023,
          showPrevTaxYears = true
        )))
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(testObligationsModel))

        val result: Future[Result] = TestIncomeSourceAddedController.showAgent(testSelfEmploymentId, SelfEmployment)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
      //common code edge/error cases:
      "throw an error when supplied business has no name" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List(BusinessDetailsModel(
          testSelfEmploymentId,
          None,
          None,
          None,
          Some(LocalDate.of(2022, 1, 1)),
          None
        )), List.empty)
        setupMockGetIncomeSourceDetails()(sources)
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(testObligationsModel))
        mockProperty()
        val result: Future[Result] = TestIncomeSourceAddedController.show(testSelfEmploymentId, SelfEmployment)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "throw an error when no id is supplied" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(testObligationsModel))
        mockFailure()

        val result: Future[Result] = TestIncomeSourceAddedController.showAgent("", SelfEmployment)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "throw an error when no start date for supplied business" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List(BusinessDetailsModel(
          testSelfEmploymentId,
          None,
          Some("test"),
          None, None, None
        )), List.empty)
        setupMockGetIncomeSourceDetails()(sources)
        mockFailure()
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(testObligationsModel))

        val result: Future[Result] = TestIncomeSourceAddedController.show(testSelfEmploymentId, SelfEmployment)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    ".show with IncomeSourceType = UkProperty" should {
      "return 200 OK" when {
        "FS enabled with newly added UK Property and obligations view model" in {
          disableAllSwitches()
          enable(IncomeSources)
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          mockUKPropertyIncomeSource()
          mockProperty()

          when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 4, 6))

          when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(
            Future(IncomeSourcesObligationsTestConstants.viewModel))

          when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
            thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

          val result = TestIncomeSourceAddedController.show(testSelfEmploymentId, UkProperty)(fakeRequestWithActiveSession)
          status(result) shouldBe OK
        }
      }
      "return 303 SEE_OTHER" when {
        "Income Sources FS is disabled" in {
          disable(IncomeSources)
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          mockUKPropertyIncomeSource()

          val result = TestIncomeSourceAddedController.show(testPropertyIncomeId, UkProperty)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
      }
      "return 500 ISE" when {
        "UK Property start date was not retrieved" in {
          enable(IncomeSources)
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          mockUKPropertyIncomeSource()
          mockFailure()
          val result = TestIncomeSourceAddedController.show("", UkProperty)(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
      "return 200 OK (Agent)" when {
        "FS enabled with newly added UK Property and obligations view model" in {
          enable(IncomeSources)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockUKPropertyIncomeSource()

          when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 4, 6))
          mockProperty()
          when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(
            Future(IncomeSourcesObligationsTestConstants.viewModel))

          when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
            thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

          val result = TestIncomeSourceAddedController.showAgent(testSelfEmploymentId, UkProperty)(fakeRequestConfirmedClient())
          status(result) shouldBe OK
        }
      }
      "return 303 SEE_OTHER (Agent)" when {
        "Income Sources FS is disabled" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          disable(IncomeSources)
          mockUKPropertyIncomeSource()

          val result = TestIncomeSourceAddedController.showAgent(testPropertyIncomeId, UkProperty)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
        }
      }
      "return 500 ISE (Agent)" when {
        "income source id is invalid" in {
          enable(IncomeSources)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockUKPropertyIncomeSource()
          mockFailure()

          val result = TestIncomeSourceAddedController.showAgent("", UkProperty)(fakeRequestConfirmedClient())
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    ".show with IncomeSourceType = ForeignProperty" should {
      "return 200 OK" when {
        "navigating to the page with FS Enabled" in {
          disableAllSwitches()
          enable(IncomeSources)
          mockForeignPropertyIncomeSource()
          mockProperty()
          val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List.empty, List(PropertyDetailsModel(
            "123456",
            None,
            None,
            Some("foreign-property"),
            Some(LocalDate.of(2022, 4, 21)),
            None
          )))

          val day = LocalDate.of(2023, 1, 1)
          val dates: Seq[DatesModel] = Seq(
            DatesModel(day, day, day, "EOPS", isFinalDec = false, obligationType = "EOPS")
          )

          when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 1, 1))
          setupMockGetIncomeSourceDetails()(sources)
          when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(Future(ObligationsViewModel(
            dates,
            dates,
            dates,
            dates,
            2023,
            showPrevTaxYears = true
          )))
          when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
            thenReturn(Future(testObligationsModel))

          val result: Future[Result] = TestIncomeSourceAddedController.show("123456", ForeignProperty)(fakeRequestWithActiveSession)
          status(result) shouldBe OK

        }
      }
      "return 200 OK (Agent)" when {
        "navigating to the page with FS Enabled" in {
          disableAllSwitches()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          enable(IncomeSources)
          mockForeignPropertyIncomeSource()
          mockProperty()
          val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List.empty, List(PropertyDetailsModel(
            "123",
            None,
            None,
            Some("foreign-property"),
            Some(LocalDate.of(2022, 4, 21)),
            None
          )))

          val day = LocalDate.of(2023, 1, 1)
          val dates: Seq[DatesModel] = Seq(
            DatesModel(day, day, day, "EOPS", isFinalDec = false, obligationType = "EOPS")
          )

          when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 1, 1))
          setupMockGetIncomeSourceDetails()(sources)
          when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(Future(ObligationsViewModel(
            dates,
            dates,
            dates,
            dates,
            2023,
            showPrevTaxYears = true
          )))
          when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
            thenReturn(Future(testObligationsModel))

          val result: Future[Result] = TestIncomeSourceAddedController.showAgent("123", ForeignProperty)(fakeRequestConfirmedClient())
          status(result) shouldBe OK

        }
      }
    }

    ".submit" should {
      "take the individual back to add income sources" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceAddedController.submit(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceController.show().url)
      }
      "take the agent back to add income sources" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceAddedController.agentSubmit(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url)
      }
    }
  }
}
