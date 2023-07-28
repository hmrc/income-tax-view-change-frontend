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

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockNextUpdatesService}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, NextUpdatesResponseModel, ObligationsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.DateService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, foreignPropertyIncome, ukPropertyIncome}
import testUtils.TestSupport
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
  with FeatureSwitching{

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  val mockDateService: DateService = mock(classOf[DateService])
  object TestManageObligationsController extends ManageObligationsController(
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    retrieveBtaNavBar = MockNavBarPredicate,
    obligationsView = app.injector.instanceOf[ManageObligations],
    mockNextUpdatesService
  )(
    ec = ec,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig]
  )

  val taxYear = "2023-2024"
  val changeTo = "annual"
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

  def setUpBusiness(isAgent: Boolean): OngoingStubbing[Future[NextUpdatesResponseModel]] = {
    if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
    else setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

    setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
    val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List(BusinessDetailsModel(
      Some(testId),
      None,
      Some("Test name"),
      None,
      Some(LocalDate.of(2022, 1, 1)),
      None
    )), List.empty)

    val day = LocalDate.of(2023, 1, 1)
    val dates: Seq[DatesModel] = Seq(
      DatesModel(day, day, day, "EOPS", isFinalDec = false)
    )

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
  }

  def setUpUKProperty(isAgent: Boolean, isUkProperty: Boolean): OngoingStubbing[Future[NextUpdatesResponseModel]] = {
    if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
    else setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

    if (isUkProperty) setupMockGetIncomeSourceDetails()(ukPropertyIncome)
    else setupMockGetIncomeSourceDetails()(foreignPropertyIncome)
    val day = LocalDate.of(2023, 1, 1)
    val dates: Seq[DatesModel] = Seq(
      DatesModel(day, day, day, "EOPS", isFinalDec = false)
    )
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
  }

  "ManageObligationsController" should {
    "redirect a user" when {
      "the individual is not authenticated" should {
        "redirect them to sign in SE" in {
          setupMockAuthorisationException()
          val result = TestManageObligationsController.showSelfEmployment(changeTo, taxYear, testId)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
        "redirect them to sign in UK property" in {
          setupMockAuthorisationException()
          val result = TestManageObligationsController.showUKProperty(changeTo, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
        "redirect them to sign in Foreign property" in {
          setupMockAuthorisationException()
          val result = TestManageObligationsController.showForeignProperty(changeTo, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
      "the agent is not authenticated" should {
        "redirect them to sign in SE" in {
          setupMockAgentAuthorisationException()
          val result = TestManageObligationsController.showAgentSelfEmployment(changeTo, taxYear, testId)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
        "redirect them to sign in UK property" in {
          setupMockAgentAuthorisationException()
          val result = TestManageObligationsController.showAgentUKProperty(changeTo, taxYear)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
        "redirect them to sign in Foreign property" in {
          setupMockAgentAuthorisationException()
          val result = TestManageObligationsController.showAgentForeignProperty(changeTo, taxYear)(fakeRequestConfirmedClient())
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

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestManageObligationsController.showSelfEmployment(changeTo, taxYear, testId)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
        "redirect to home page (agent) SE" in {
          disableAllSwitches()

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestManageObligationsController.showAgentSelfEmployment(changeTo, taxYear, testId)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
        }
        "redirect to home page UK property" in {
          disableAllSwitches()

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestManageObligationsController.showUKProperty(changeTo, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
        "redirect to home page (agent) UK property" in {
          disableAllSwitches()

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestManageObligationsController.showAgentUKProperty(changeTo, taxYear)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
        }
        "redirect to home page foreign property" in {
          disableAllSwitches()

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestManageObligationsController.showForeignProperty(changeTo, taxYear)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
        "redirect to home page (agent) foreign property" in {
          disableAllSwitches()

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestManageObligationsController.showAgentForeignProperty(changeTo, taxYear)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
        }
      }
    }

    "showSelfEmployment" should {
      "show correct page when individual valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setUpBusiness(isAgent= false)

        val result: Future[Result] = TestManageObligationsController.showSelfEmployment(changeTo, taxYear, testId)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      "show correct page when agent valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setUpBusiness(isAgent = true)

        val result: Future[Result] = TestManageObligationsController.showAgentSelfEmployment(changeTo, taxYear, testId)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
    }

    "showUKProperty" should {
      "show correct page when individual valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setUpUKProperty(isAgent = false, isUkProperty = true)

        val result: Future[Result] = TestManageObligationsController.showUKProperty(changeTo, taxYear)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      "show correct page when agent valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setUpUKProperty(isAgent = true, isUkProperty = true)

        val result: Future[Result] = TestManageObligationsController.showAgentUKProperty(changeTo, taxYear)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
    }

    "showForeignProperty" should {
      "show correct page when individual valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setUpUKProperty(isAgent = false, isUkProperty = false)

        val result: Future[Result] = TestManageObligationsController.showForeignProperty(changeTo, taxYear)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      "show correct page when agent valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setUpUKProperty(isAgent = true, isUkProperty = false)

        val result: Future[Result] = TestManageObligationsController.showAgentForeignProperty(changeTo, taxYear)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
    }

    "submit" should {
      "take the individual back to add income sources" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestManageObligationsController.submit(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show().url)
      }
      "take the agent back to add income sources" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestManageObligationsController.agentSubmit(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.manage.routes.ManageIncomeSourceController.showAgent().url)
      }
    }


    //Not covered:
    //mode = SE and no income source with provided id
    //mode = SE and business with supplied id has no name
    //mode = UK or FP and no property of that type with supplied id
    //mode = Any and invalid tax year
    //mode = any and invalid changeTo
  }
}
