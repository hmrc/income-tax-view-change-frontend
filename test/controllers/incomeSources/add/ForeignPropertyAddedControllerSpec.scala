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
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockNextUpdatesService}
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, PropertyDetailsModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{DateService, DateServiceInterface, SessionService}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import views.html.incomeSources.add.IncomeSourceAddedObligations

import java.time.LocalDate
import scala.concurrent.Future


class ForeignPropertyAddedControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with MockNextUpdatesService
  with FeatureSwitching {

  val mockDateService: DateService = mock(classOf[DateService])

  object TestForeignPropertyObligationsController extends ForeignPropertyAddedController(
    view = app.injector.instanceOf[IncomeSourceAddedObligations],
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    authenticate = MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    retrieveBtaNavBar = MockNavBarPredicate,
    sessionService = app.injector.instanceOf[SessionService],
    mockNextUpdatesService
  )(
    ec = ec,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    dateService = app.injector.instanceOf[DateServiceInterface]
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

  "Individual - ForeignPropertyAddedObligationsController.show" should {
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

        val result = TestForeignPropertyObligationsController.submit()(fakeRequestWithTimeoutSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }
  }

  "Individual - ForeignPropertyAddedObligationsController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.AddIncomeSourceController.show().url}" when {
      "view income sources button pressed" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        val result: Future[Result] = TestForeignPropertyObligationsController.submit()(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceController.show().url)
      }
    }
  }

  "Agent - ForeignPropertyAddedObligationsController.showAgent" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

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
  }

  "Agent - ForeignPropertyAddedObligationsController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url}" when {
      "view income sources button pressed" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        val result: Future[Result] = TestForeignPropertyObligationsController.submitAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url)
      }
    }
  }
}

