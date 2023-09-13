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
import enums.IncomeSourceJourney.ForeignProperty
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockNextUpdatesService}
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, PropertyDetailsModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{DateService, DateServiceInterface}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testPropertyIncomeId}
import testUtils.TestSupport
import utils.GetActivePropertyBusinesses
import views.html.incomeSources.cease.IncomeSourceCeasedObligations

import java.time.LocalDate
import scala.concurrent.Future


class ForeignPropertyCeasedObligationsControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with MockNextUpdatesService
  with FeatureSwitching {

  val mockDateService: DateService = mock(classOf[DateService])
  val mockGetActivePropertyBusinesses: GetActivePropertyBusinesses = mock(classOf[GetActivePropertyBusinesses])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGetActivePropertyBusinesses)
  }

  object TestForeignPropertyObligationsController extends ForeignPropertyCeasedObligationsController(
    obligationsView = app.injector.instanceOf[IncomeSourceCeasedObligations],
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    authenticate = MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    retrieveBtaNavBar = MockNavBarPredicate,
    nextUpdatesService = mockNextUpdatesService,
    dateService = app.injector.instanceOf[DateServiceInterface],
    getActivePropertyBusinesses = mockGetActivePropertyBusinesses
  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    ec = ec,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
  )

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

  "Individual - ForeignPropertyCeasedObligationsController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List.empty, List(PropertyDetailsModel(
          "123456",
          None,
          None,
          Some("03-foreign-property"),
          Some(LocalDate.of(2022, 4, 21)),
          None
        )))

        val day = LocalDate.of(2023, 1, 1)
        val dates: Seq[DatesModel] = Seq(
          DatesModel(day, day, day, "EOPS", isFinalDec = false)
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
        when(mockGetActivePropertyBusinesses.getActiveForeignPropertyFromUserIncomeSources(any()))
          .thenReturn(
            Right(
              PropertyDetailsModel(
                incomeSourceId = testPropertyIncomeId,
                accountingPeriod = None,
                firstAccountingPeriodEndDate = None,
                incomeSourceType = Some("03-foreign-property"),
                tradingStartDate = None,
                cessation = None,
                cashOrAccruals = None,
                latencyDetails = None
              )
            )
          )

        val result: Future[Result] = TestForeignPropertyObligationsController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe OK

      }
    }

    "return 303 SEE_OTHER" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockForeignPropertyIncomeSource()

        val result: Future[Result] = TestForeignPropertyObligationsController.show()(fakeRequestWithActiveSession)
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        status(result) shouldBe SEE_OTHER
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()

        val result: Future[Result] = TestForeignPropertyObligationsController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
      }
      "user has timed out, show timeout" in {
        setupMockAuthorisationException()

        val result = TestForeignPropertyObligationsController.show()(fakeRequestWithTimeoutSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
      "user has no active foreign properties" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockGetActivePropertyBusinesses.getActiveForeignPropertyFromUserIncomeSources(any()))
          .thenReturn(
            Left(
              new Error("No active foreign properties found.")
            )
          )

        val result: Future[Result] = TestForeignPropertyObligationsController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "user has more than one active foreign properties" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockTwoActiveForeignPropertyIncomeSourcesErrorScenario()

        when(mockGetActivePropertyBusinesses.getActiveForeignPropertyFromUserIncomeSources(any()))
          .thenReturn(
            Left(
              new Error("Too many active foreign properties found. There should only be one.")
            )
          )

        val result: Future[Result] = TestForeignPropertyObligationsController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }


  "Agent - ForeignPropertyAddedObligationsController.showAgent" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)

        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List.empty, List(PropertyDetailsModel(
          "123",
          None,
          None,
          Some("03-foreign-property"),
          Some(LocalDate.of(2022, 4, 21)),
          None
        )))

        val day = LocalDate.of(2023, 1, 1)
        val dates: Seq[DatesModel] = Seq(
          DatesModel(day, day, day, "EOPS", isFinalDec = false)
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
        when(mockGetActivePropertyBusinesses.getActiveForeignPropertyFromUserIncomeSources(any()))
          .thenReturn(
            Right(
              PropertyDetailsModel(
                incomeSourceId = testPropertyIncomeId,
                accountingPeriod = None,
                firstAccountingPeriodEndDate = None,
                incomeSourceType = Some("03-foreign-property"),
                tradingStartDate = None,
                cessation = None,
                cashOrAccruals = None,
                latencyDetails = None
              )
            )
          )

        val result: Future[Result] = TestForeignPropertyObligationsController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe OK

      }
    }

    "return 303 SEE_OTHER" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockForeignPropertyIncomeSource()

        val result: Future[Result] = TestForeignPropertyObligationsController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()

        val result: Future[Result] = TestForeignPropertyObligationsController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
      }
    }

    "return 500 INTERNAL_SERVER_ERROR" when {
      "user has no active foreign properties" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockGetActivePropertyBusinesses.getActiveForeignPropertyFromUserIncomeSources(any()))
          .thenReturn(
            Left(
              new Error("No active foreign properties found.")
            )
          )

        val result: Future[Result] = TestForeignPropertyObligationsController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "user has more than one active foreign properties" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockTwoActiveForeignPropertyIncomeSourcesErrorScenario()

        when(mockGetActivePropertyBusinesses.getActiveForeignPropertyFromUserIncomeSources(any()))
          .thenReturn(
            Left(
              new Error("Too many active foreign properties found. There should only be one.")
            )
          )

        val result: Future[Result] = TestForeignPropertyObligationsController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
