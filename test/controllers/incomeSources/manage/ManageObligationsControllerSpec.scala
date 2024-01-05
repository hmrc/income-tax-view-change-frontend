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

package controllers.incomeSources.manage

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney._
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockNextUpdatesService}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, NextUpdatesResponseModel, ObligationsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.SessionService
import testConstants.BaseTestConstants.{testNino, testPropertyIncomeId}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, foreignPropertyIncomeWithCeasedForiegnPropertyIncome, ukPropertyIncomeWithCeasedUkPropertyIncome}
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants.quarterlyObligationDatesSimple
import testUtils.TestSupport
import utils.IncomeSourcesUtils
import views.html.incomeSources.manage.ManageObligations

import java.time.LocalDate
import scala.concurrent.Future

class ManageObligationsControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with MockNextUpdatesService
  with FeatureSwitching {

  val mockSessionService: SessionService = mock(classOf[SessionService])
  val mockIncomeSourcesUtils: IncomeSourcesUtils = mock(classOf[IncomeSourcesUtils])

  object TestManageObligationsController extends ManageObligationsController(
    authorisedFunctions = mockAuthService,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    obligationsView = app.injector.instanceOf[ManageObligations],
    sessionService = mockSessionService,
    mockNextUpdatesService,
    testAuthenticator
  )(
    ec = ec,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig]
  )

  val taxYear = "2023-2024"
  val changeToA = "annual"
  val changeToQ = "quarterly"
  val testId = "XAIS00000000001"

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel("123", List(NextUpdateModel(
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

  private val propertyDetailsModelUK = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some("uk-property"),
    tradingStartDate = None,
    cessation = None,
    cashOrAccruals = false,
    latencyDetails = None
  )
  private val propertyDetailsModelForeign = propertyDetailsModelUK.copy(incomeSourceType = Some("foreign-property"))

  def setUpBusiness(isAgent: Boolean): OngoingStubbing[Future[NextUpdatesResponseModel]] = {
    setupMockAuthorisationSuccess(isAgent)

    val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel(testNino, "", Some("2022"), List(BusinessDetailsModel(
      testId,
      None,
      Some("Test name"),
      None,
      Some(LocalDate.of(2022, 1, 1)),
      None,
      cashOrAccruals = false
    )), List.empty)
    setupMockGetIncomeSourceDetails()(sources)

    val day = LocalDate.of(2023, 1, 1)
    val dates: Seq[DatesModel] = Seq(
      DatesModel(day, day, day, "EOPS", isFinalDec = false, obligationType = "EOPS")
    )
    when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(Future(ObligationsViewModel(
      quarterlyObligationDatesSimple,
      dates,
      dates,
      2023,
      showPrevTaxYears = true
    )))
    when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
      thenReturn(Future(testObligationsModel))
  }

  def setUpProperty(isAgent: Boolean, isUkProperty: Boolean): OngoingStubbing[Future[NextUpdatesResponseModel]] = {
    setupMockAuthorisationSuccess(isAgent)

    if (isUkProperty) {
      setupMockGetIncomeSourceDetails()(ukPropertyIncomeWithCeasedUkPropertyIncome)
      when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
        .thenReturn(Some(propertyDetailsModelUK))
    }
    else {
      setupMockGetIncomeSourceDetails()(foreignPropertyIncomeWithCeasedForiegnPropertyIncome)
      when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
        .thenReturn(Some(propertyDetailsModelForeign))
    }
    val day = LocalDate.of(2023, 1, 1)
    val dates: Seq[DatesModel] = Seq(
      DatesModel(day, day, day, "EOPS", isFinalDec = false, obligationType = "EOPS")
    )
    when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(Future(ObligationsViewModel(
      quarterlyObligationDatesSimple,
      dates,
      dates,
      2023,
      showPrevTaxYears = true
    )))
    when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
      thenReturn(Future(testObligationsModel))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeSourceDetailsService)
    disableAllSwitches()
    enable(IncomeSources)
  }

  "ManageObligationsController" should {
    "redirect a user" when {
      "the individual is not authenticated" should {
        "redirect them to sign in SE" in {
          setupMockAuthorisationException()
          val result = TestManageObligationsController.showSelfEmployment(changeToA, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
        "redirect them to sign in UK property" in {
          setupMockAuthorisationException()
          val result = TestManageObligationsController.showUKProperty(changeToQ, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
        "redirect them to sign in Foreign property" in {
          setupMockAuthorisationException()
          val result = TestManageObligationsController.showForeignProperty(changeToA, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
      "the agent is not authenticated" should {
        "redirect them to sign in SE" in {
          setupMockAgentAuthorisationException()
          val result = TestManageObligationsController.showAgentSelfEmployment(changeToQ, taxYear)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
        "redirect them to sign in UK property" in {
          setupMockAgentAuthorisationException()
          val result = TestManageObligationsController.showAgentUKProperty(changeToA, taxYear)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
        "redirect them to sign in Foreign property" in {
          setupMockAgentAuthorisationException()
          val result = TestManageObligationsController.showAgentForeignProperty(changeToQ, taxYear)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }

      "the user has timed out" should {
        "redirect to the session timeout page" in {
          setupMockAuthorisationException()

          val result = TestManageObligationsController.submit()(fakeRequestWithTimeoutSession)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
        }
      }

      "feature switch is disabled" should {
        "redirect to home page SE" in {
          disableAllSwitches()
          setupMockAuthorisationSuccess(false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestManageObligationsController.showSelfEmployment(changeToA, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
        "redirect to home page (agent) SE" in {
          disableAllSwitches()
          setupMockAuthorisationSuccess(true)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestManageObligationsController.showAgentSelfEmployment(changeToQ, taxYear)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
        }
        "redirect to home page UK property" in {
          disableAllSwitches()
          setupMockAuthorisationSuccess(false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestManageObligationsController.showUKProperty(changeToA, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
        "redirect to home page (agent) UK property" in {
          disableAllSwitches()
          setupMockAuthorisationSuccess(true)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestManageObligationsController.showAgentUKProperty(changeToQ, taxYear)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
        }
        "redirect to home page foreign property" in {
          disableAllSwitches()
          setupMockAuthorisationSuccess(false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestManageObligationsController.showForeignProperty(changeToA, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
        "redirect to home page (agent) foreign property" in {
          disableAllSwitches()
          setupMockAuthorisationSuccess(true)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestManageObligationsController.showAgentForeignProperty(changeToQ, taxYear)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
        }
      }
    }

    "showSelfEmployment" should {
      "show correct page when individual valid" in {
        setupMockAuthorisationSuccess(false)
        setUpBusiness(isAgent = false)
        when(mockSessionService.getMongoKey(any(), any())(any(), any())).thenReturn(Future(Right(Some(testId))))

        val result: Future[Result] = TestManageObligationsController.showSelfEmployment(changeToA, taxYear)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      "show correct page when agent valid" in {
        setupMockAuthorisationSuccess(true)
        setUpBusiness(isAgent = true)
        when(mockSessionService.getMongoKey(any(), any())(any(), any())).thenReturn(Future(Right(Some(testId))))

        val result: Future[Result] = TestManageObligationsController.showAgentSelfEmployment(changeToQ, taxYear)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
      "show page with 'Sole trader business' when business has no name" in {
        setupMockAuthorisationSuccess(true)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel(testNino, "", Some("2022"), List(
          BusinessDetailsModel(
            testId,
            None,
            None,
            None,
            Some(LocalDate.of(2022, 1, 1)),
            None,
            cashOrAccruals = true
          )), List.empty)

        val day = LocalDate.of(2023, 1, 1)
        val dates: Seq[DatesModel] = Seq(
          DatesModel(day, day, day, "EOPS", isFinalDec = false, obligationType = "EOPS")
        )

        setupMockGetIncomeSourceDetails()(sources)
        when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(Future(ObligationsViewModel(
          quarterlyObligationDatesSimple,
          dates,
          dates,
          2023,
          showPrevTaxYears = true
        )))
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(testObligationsModel))
        when(mockSessionService.getMongoKey(any(), any())(any(), any())).thenReturn(Future(Right(Some(testId))))

        val result: Future[Result] = TestManageObligationsController.showAgentSelfEmployment(changeToQ, taxYear)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
        contentAsString(result) should include("Sole trader business")
      }
    }

    "showUKProperty" should {
      "show correct page when individual valid" in {
        setupMockAuthorisationSuccess(false)
        setUpProperty(isAgent = false, isUkProperty = true)

        val result: Future[Result] = TestManageObligationsController.showUKProperty(changeToA, taxYear)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      "show correct page when agent valid" in {
        setupMockAuthorisationSuccess(true)
        setUpProperty(isAgent = true, isUkProperty = true)

        val result: Future[Result] = TestManageObligationsController.showAgentUKProperty(changeToQ, taxYear)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
      "return 500 INTERNAL_SERVER_ERROR" when {
        "user has no active UK properties" in {
          setupMockAuthorisationSuccess(false)
          mockNoIncomeSources()

          val result: Future[Result] = TestManageObligationsController.showUKProperty(changeToA, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        "user has more than one active UK property" in {
          setupMockAuthorisationSuccess(false)
          mockTwoActiveUkPropertyIncomeSourcesErrorScenario()

          when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
            .thenReturn(
              None
            )

          val result: Future[Result] = TestManageObligationsController.showUKProperty(changeToA, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "showForeignProperty" should {
      "show correct page when individual valid" in {
        setupMockAuthorisationSuccess(false)
        setUpProperty(isAgent = false, isUkProperty = false)

        val result: Future[Result] = TestManageObligationsController.showForeignProperty(changeToA, taxYear)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      "show correct page when agent valid" in {
        setupMockAuthorisationSuccess(true)
        setUpProperty(isAgent = true, isUkProperty = false)

        val result: Future[Result] = TestManageObligationsController.showAgentForeignProperty(changeToQ, taxYear)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
      "return 500 INTERNAL_SERVER_ERROR" when {
        "user has no active foreign properties" in {
          setupMockAuthorisationSuccess(false)
          mockNoIncomeSources()

          when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
            .thenReturn(
              None
            )

          val result: Future[Result] = TestManageObligationsController.showUKProperty(changeToA, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        "user has more than one active foreign properties" in {
          setupMockAuthorisationSuccess(false)
          mockTwoActiveForeignPropertyIncomeSourcesErrorScenario()

          when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
            .thenReturn(
              None
            )

          val result: Future[Result] = TestManageObligationsController.showUKProperty(changeToA, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "handleRequest" should {
      "return an error" when {
        "invalid taxYear in url" in {
          setupMockAuthorisationSuccess(false)
          setUpProperty(isAgent = false, isUkProperty = true)
          val invalidTaxYear = "2345"

          val result: Future[Result] = TestManageObligationsController.handleRequest(UkProperty, isAgent = false, invalidTaxYear, changeToQ, Some(mkIncomeSourceId("")))(individualUser, headerCarrier)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        "invalid changeTo in url" in {
          setupMockAuthorisationSuccess(true)
          setUpProperty(isAgent = true, isUkProperty = true)
          val invalidChangeTo = "2345"

          val result: Future[Result] = TestManageObligationsController.handleRequest(UkProperty, isAgent = true, taxYear, invalidChangeTo, Some(mkIncomeSourceId("")))(agentUserConfirmedClient(), headerCarrier)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "submit" should {
      "take the individual back to add income sources" in {
        setupMockAuthorisationSuccess(false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestManageObligationsController.submit(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(false).url)
      }
      "take the agent back to add income sources" in {
        setupMockAuthorisationSuccess(true)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestManageObligationsController.agentSubmit(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true).url)
      }
    }
  }
}
