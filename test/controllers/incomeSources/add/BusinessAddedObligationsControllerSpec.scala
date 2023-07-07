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
import models.incomeSourceDetails.viewmodels.DatesModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{DateService, ObligationsRetrievalService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.incomeSources.add.BusinessAddedObligations

import java.time.LocalDate
import scala.concurrent.Future


class BusinessAddedObligationsControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with MockNextUpdatesService
  with FeatureSwitching {

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  val mockDateService: DateService = mock(classOf[DateService])
  val mockObligationsService: ObligationsRetrievalService = mock(classOf[ObligationsRetrievalService])

  object TestObligationsController extends BusinessAddedObligationsController(
    MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    retrieveBtaNavBar = MockNavBarPredicate,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    obligationsView = app.injector.instanceOf[BusinessAddedObligations],
    mockObligationsService
  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = ec,
    mockDateService
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


  "BusinessAddedObligationsController" should {
    "redirect a user back to the custom error page" when {
      "the user is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestObligationsController.show("")(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAuthorisationException()

        val result = TestObligationsController.submit()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

    "feature switch is disabled" should {
      "redirect to home page" in {
        disableAllSwitches()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestObligationsController.show("123")(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
    }


    ".show" should {
      "show correct page when individual valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List(BusinessDetailsModel(
          Some("123"),
          None,
          Some("Test name"),
          None,
          Some(LocalDate.of(2022,1,1)),
          None
        )), List.empty)

        val day = LocalDate.of(2023,1,1)
        val dates: Seq[DatesModel] = Seq(
          DatesModel(day, day, day, "EOPS", isFinalDec = false)
        )
        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023,1,1))
        setupMockGetIncomeSourceDetails()(sources)
        when(mockObligationsService.getObligationDates(any())(any(), any(), any())).thenReturn(Future(dates))
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(testObligationsModel))

        val result: Future[Result] = TestObligationsController.show("123")(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      "show correct page when agent valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockSingleBusinessIncomeSourceWithDeadlines()
        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List(BusinessDetailsModel(
          Some("123"),
          None,
          Some("Test name"),
          None,
          Some(LocalDate.of(2022,1,1)),
          None
        )), List.empty)

        val day = LocalDate.of(2023, 1, 1)
        val dates: Seq[DatesModel] = Seq(
          DatesModel(day, day, day, "EOPS", isFinalDec = false)
        )
        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023,12,1))
        setupMockGetIncomeSourceDetails()(sources)
        when(mockObligationsService.getObligationDates(any())(any(), any(), any())).thenReturn(Future(dates))
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(testObligationsModel))

        val result: Future[Result] = TestObligationsController.showAgent("123")(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
      //common code edge/error cases:
      "throw an error when supplied business has no name" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List(BusinessDetailsModel(
          Some("123"),
          None,
          None,
          None, None, None
        )), List.empty)
        setupMockGetIncomeSourceDetails()(sources)
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(testObligationsModel))

        val result: Future[Result] = TestObligationsController.show("123")(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "throw an error when no id is supplied" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List(BusinessDetailsModel(
          Some("123"),
          None,
          Some("Test name"),
          None, None, None
        )), List.empty)
        setupMockGetIncomeSourceDetails()(sources)
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(testObligationsModel))

        val result: Future[Result] = TestObligationsController.show("")(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "throw an error when no start date for supplied business" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List(BusinessDetailsModel(
          Some("123"),
          None,
          None,
          None, None, None
        )), List.empty)
        setupMockGetIncomeSourceDetails()(sources)
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(testObligationsModel))

        val result: Future[Result] = TestObligationsController.show("")(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    ".submit" should {
      "take the individual back to add income sources" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestObligationsController.submit(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceController.show().url)
      }
      "take the agent back to add income sources" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestObligationsController.agentSubmit(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url)
      }
    }
  }

}
