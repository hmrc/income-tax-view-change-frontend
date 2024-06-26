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

package controllers.manageBusinesses.manage

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{JourneyType, Manage}
import implicits.ImplicitDateFormatter
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockIncomeSourceDetailsService, MockSessionService}
import models.admin.IncomeSources
import models.incomeSourceDetails.ManageIncomeSourceData
import models.updateIncomeSource.{UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.Assertion
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.UpdateIncomeSourceService
import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{completedUIJourneySessionData, emptyUIJourneySessionData, notCompletedUIJourneySessionData}
import testUtils.TestSupport
import views.html.manageBusinesses.manage.CheckYourAnswers

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with ImplicitDateFormatter
  with MockIncomeSourceDetailsService
  with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching
  with TestSupport
  with MockSessionService {

  object TestCheckYourAnswersController
    extends CheckYourAnswersController(
      checkYourAnswers = app.injector.instanceOf[CheckYourAnswers],
      authorisedFunctions = mockAuthService,
      updateIncomeSourceService = mock(classOf[UpdateIncomeSourceService]),
      sessionService = mockSessionService,
      auditingService = mockAuditingService,
      auth = testAuthenticator
    )(
      ec = ec,
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      appConfig = app.injector.instanceOf[FrontendAppConfig]
    )

  "CheckYourAnswersController.show" should {
    s"return ${Status.SEE_OTHER} and redirect to the home page" when {
      "the IncomeSources FS is disabled for an Individual" in {
        val result = runShowTest(isAgent = false, disableIncomeSources = true)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "the IncomeSources FS is disabled for an Agent" in {
        val result = runShowTest(isAgent = true, disableIncomeSources = true)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    s"return ${Status.OK}" when {
      "all query parameters are valid for an Individual" in {
        val result = runShowTest(isAgent = false)
        status(result) shouldBe Status.OK
      }
      "all query parameters are valid for an Agent" in {
        val result = runShowTest(isAgent = true)
        status(result) shouldBe Status.OK
      }
    }
    "redirect to the Cannot Go Back page" when {
      def setupCompletedManageJourney(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        setupMockAuthorisationSuccess(isAgent)
        mockBothPropertyBothBusiness()
        setupMockCreateSession(true)
        setupMockGetMongo(Right(Some(completedUIJourneySessionData(JourneyType(Manage, incomeSourceType)))))
        val result = if (isAgent) TestCheckYourAnswersController
          .show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
        else TestCheckYourAnswersController
          .show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)

        val expectedRedirectUrl = routes.CannotGoBackErrorController.show(isAgent = isAgent, incomeSourceType).url

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirectUrl)
      }

      "UK Property journey is complete - Individual" in {
        setupCompletedManageJourney(isAgent = false, UkProperty)
      }
      "UK Property journey is complete - Agent" in {
        setupCompletedManageJourney(isAgent = true, UkProperty)
      }
      "Foreign Property journey is complete - Individual" in {
        setupCompletedManageJourney(isAgent = false, ForeignProperty)
      }
      "Foreign Property journey is complete - Agent" in {
        setupCompletedManageJourney(isAgent = true, ForeignProperty)
      }
      "Self Employment journey is complete - Individual" in {
        setupCompletedManageJourney(isAgent = false, SelfEmployment)
      }
      "Self Employment journey is complete - Agent" in {
        setupCompletedManageJourney(isAgent = true, SelfEmployment)
      }
    }
  }

  "CheckYourAnswersController.submit" should {
    s"return ${Status.SEE_OTHER} and redirect to the home page" when {
      "the IncomeSources FS is disabled for an Individual" in {

        val result = runSubmitTest(isAgent = false, disableIncomeSources = true)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "the IncomeSources FS is disabled for an Agent" in {

        val result = runSubmitTest(isAgent = true, disableIncomeSources = true)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }

      "UpdateIncomeSourceService returns a UpdateIncomeSourceResponseError response for an Individual" in {

        val result = runSubmitTest(isAgent = false, incomeSourceType = UkProperty, withUpdateIncomeSourceResponseError = true)

        redirectLocation(result) shouldBe
          Some(
            controllers.manageBusinesses.manage.routes
              .ReportingMethodChangeErrorController.show(isAgent = false, UkProperty).url
          )
        status(result) shouldBe Status.SEE_OTHER
      }

      "UpdateIncomeSourceService returns a UpdateIncomeSourceResponseError response for an Agent" in {

        val result = runSubmitTest(isAgent = true, incomeSourceType = UkProperty, withUpdateIncomeSourceResponseError = true)

        redirectLocation(result) shouldBe
          Some(
            controllers.manageBusinesses.manage.routes
              .ReportingMethodChangeErrorController.show(isAgent = true, UkProperty).url
          )
        status(result) shouldBe Status.SEE_OTHER
      }
    }

    s"return ${Status.SEE_OTHER} and redirect to the Manage Obligations page for a UK property" when {
      "the Individual's UK property reporting method is updated to annual" in {
        val result = runSubmitTest(isAgent = false, UkProperty, testChangeToAnnual)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            controllers.manageBusinesses.manage.routes
              .ManageObligationsController.show(isAgent = false, UkProperty).url
          )
      }
      "the Agent's UK property reporting method is updated to annual" in {
        val result = runSubmitTest(isAgent = true, UkProperty, testChangeToAnnual)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            controllers.manageBusinesses.manage.routes
              .ManageObligationsController.show(isAgent = true, UkProperty).url
          )
      }
    }

    s"return ${Status.SEE_OTHER} and redirect to the Manage Obligations page for a Foreign property" when {
      "the Individual's Foreign property reporting method is updated to quarterly" in {
        val result = runSubmitTest(isAgent = false, ForeignProperty, testChangeToQuarterly)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            controllers.manageBusinesses.manage.routes
              .ManageObligationsController.show(isAgent = false, ForeignProperty).url
          )
      }
      "the Agent's Foreign property reporting method is updated to quarterly" in {
        val result = runSubmitTest(isAgent = true, ForeignProperty, testChangeToQuarterly)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            controllers.manageBusinesses.manage.routes
              .ManageObligationsController.show(isAgent = true, ForeignProperty).url
          )
      }
    }

    s"return ${Status.SEE_OTHER} and redirect to the Manage Obligations page for a Sole Trader Business" when {
      "the Individual's Sole Trader Business reporting method is updated to annual" in {
        val result = runSubmitTest(isAgent = false, SelfEmployment)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            controllers.manageBusinesses.manage.routes
              .ManageObligationsController.show(isAgent = false, SelfEmployment).url
          )
      }
      "the Agent's Foreign property reporting method is updated to annual" in {
        val result = runSubmitTest(isAgent = true, ForeignProperty)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            controllers.manageBusinesses.manage.routes
              .ManageObligationsController.show(isAgent = true, ForeignProperty).url
          )
      }
    }
  }


  def runShowTest(isAgent: Boolean,
                  disableIncomeSources: Boolean = false,
                  changeTo: String = testChangeToAnnual,
                  taxYear: String = testTaxYear,
                  incomeSourceType: IncomeSourceType = SelfEmployment,
                  emptyMongo: Boolean = false
                 ): Future[Result] = {

    if (disableIncomeSources)
      disable(IncomeSources)

    mockBothPropertyBothBusinessWithLatency()
    setupMockAuthorisationSuccess(isAgent)

    setupMockCreateSession(true)
    if (emptyMongo) setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, incomeSourceType)))))
    else setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Manage, incomeSourceType))
      .copy(manageIncomeSourceData = Some(ManageIncomeSourceData(
        incomeSourceId = Some(testSelfEmploymentId),
        reportingMethod = Some(changeTo),
        taxYear = Some(taxYear.toInt))
      )))
    ))

    TestCheckYourAnswersController
      .show(isAgent, incomeSourceType)(
        if (isAgent)
          fakeRequestConfirmedClient()
        else
          fakeRequestWithActiveSession
      )
  }

  def runSubmitTest(isAgent: Boolean,
                    incomeSourceType: IncomeSourceType = SelfEmployment,
                    changeTo: String = testChangeToAnnual,
                    taxYear: String = testTaxYear,
                    disableIncomeSources: Boolean = false,
                    withUpdateIncomeSourceResponseError: Boolean = false
                   ): Future[Result] = {

    if (disableIncomeSources)
      disable(IncomeSources)

    mockBothPropertyBothBusiness()

    setupMockAuthorisationSuccess(isAgent)

    setupMockCreateSession(true)
    setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Manage, incomeSourceType))
      .copy(manageIncomeSourceData = Some(ManageIncomeSourceData(
        incomeSourceId = Some(testSelfEmploymentId),
        reportingMethod = Some(changeTo),
        taxYear = Some(taxYear.toInt))
      )))
    ))
    setupMockSetMongoData(true)

    when(
      TestCheckYourAnswersController
        .updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
      .thenReturn(
        Future(
          if (withUpdateIncomeSourceResponseError)
            UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Dummy message")
          else
            UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")
        )
      )

    TestCheckYourAnswersController
      .submit(isAgent, incomeSourceType)(
        (if (isAgent)
          fakePostRequestConfirmedClient()
        else
          fakePostRequestWithActiveSession)
      )
  }

  override def beforeEach(): Unit = {
    disableAllSwitches()
    enable(IncomeSources)
  }

  private lazy val testTaxYear = "2023"
  private lazy val testChangeToAnnual = "annual"
  private lazy val testChangeToQuarterly = "quarterly"
}
