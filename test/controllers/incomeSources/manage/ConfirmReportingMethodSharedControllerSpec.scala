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
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.manage.ConfirmReportingMethodForm
import implicits.ImplicitDateFormatter
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import models.updateIncomeSource.{UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{SessionService, UpdateIncomeSourceService}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import views.html.incomeSources.manage.{ConfirmReportingMethod, ManageIncomeSources}

import scala.concurrent.Future

class ConfirmReportingMethodSharedControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with ImplicitDateFormatter
  with MockIncomeSourceDetailsService
  with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching
  with TestSupport {

  val mockSessionService: SessionService = mock(classOf[SessionService])

  object TestConfirmReportingMethodSharedController
    extends ConfirmReportingMethodSharedController(
      manageIncomeSources = app.injector.instanceOf[ManageIncomeSources],
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      retrieveNinoWithIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      updateIncomeSourceService = mock(classOf[UpdateIncomeSourceService]),
      confirmReportingMethod = app.injector.instanceOf[ConfirmReportingMethod],
      auditingService = mockAuditingService,
      dateService = dateService,
      sessionService = mockSessionService
    )(
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      ec = ec
    )

  "ConfirmReportingMethodSharedController.show" should {
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
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "taxYear parameter has an invalid format for an Individual" in {
        val result = runShowTest(isAgent = false, taxYear = invalidTaxYear)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "changeTo parameter has an invalid format for an Individual" in {
        val result = runShowTest(isAgent = false, changeTo = invalidChangeTo)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "the given incomeSourceId cannot be found in the Individual's Sole Trader business income sources" in {
        val result = runShowTest(isAgent = false, incomeSourceId = None)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "taxYear parameter has an invalid format for an Agent" in {
        val result = runShowTest(isAgent = true, taxYear = invalidTaxYear)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "changeTo parameter has an invalid format for an Agent" in {
        val result = runShowTest(isAgent = true, changeTo = invalidChangeTo)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "the given incomeSourceId cannot be found in the Agent's Sole Trader business income sources" in {
        val result = runShowTest(isAgent = true, incomeSourceId = None)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
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
  }

  "ConfirmReportingMethodSharedController.submit" should {
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
            controllers.incomeSources.manage.routes
              .ReportingMethodChangeErrorController.show(isAgent = false, UkProperty).url
          )
        status(result) shouldBe Status.SEE_OTHER
      }

      "UpdateIncomeSourceService returns a UpdateIncomeSourceResponseError response for an Agent" in {

        val result = runSubmitTest(isAgent = true, incomeSourceType = UkProperty, withUpdateIncomeSourceResponseError = true)

        redirectLocation(result) shouldBe
          Some(
            controllers.incomeSources.manage.routes
              .ReportingMethodChangeErrorController.show(isAgent = true, UkProperty).url
          )
        status(result) shouldBe Status.SEE_OTHER
      }
    }

    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "taxYear parameter has an invalid format for an Individual" in {
        val result = runSubmitTest(isAgent = false, taxYear = invalidTaxYear)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "changeTo parameter has an invalid format for an Individual" in {
        val result = runSubmitTest(isAgent = false, changeTo = invalidChangeTo)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "taxYear parameter has an invalid format for an Agent" in {
        val result = runSubmitTest(isAgent = true, taxYear = invalidTaxYear)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "changeTo parameter has an invalid format for an Agent" in {
        val result = runSubmitTest(isAgent = true, changeTo = invalidChangeTo)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    s"return ${Status.BAD_REQUEST}" when {
      "the form is empty for an Individual" in {
        val result = runSubmitTest(isAgent = false, SelfEmployment, withValidForm = false)
        status(result) shouldBe Status.BAD_REQUEST
      }
      "the form is empty for an Agent" in {
        val result = runSubmitTest(isAgent = true, SelfEmployment, withValidForm = false)
        status(result) shouldBe Status.BAD_REQUEST
      }
    }

    s"return ${Status.SEE_OTHER} and redirect to the Manage Obligations page for a UK property" when {
      "the Individual's UK property reporting method is updated to annual" in {
        val result = runSubmitTest(isAgent = false, UkProperty, testChangeToAnnual)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            controllers.incomeSources.manage.routes
              .ManageObligationsController.showUKProperty(testChangeToAnnual, testTaxYear).url
          )
      }
      "the Agent's UK property reporting method is updated to annual" in {
        val result = runSubmitTest(isAgent = true, UkProperty, testChangeToAnnual)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            controllers.incomeSources.manage.routes
              .ManageObligationsController.showAgentUKProperty(testChangeToAnnual, testTaxYear).url
          )
      }
    }

    s"return ${Status.SEE_OTHER} and redirect to the Manage Obligations page for a Foreign property" when {
      "the Individual's Foreign property reporting method is updated to quarterly" in {
        val result = runSubmitTest(isAgent = false, ForeignProperty, testChangeToQuarterly)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            controllers.incomeSources.manage.routes
              .ManageObligationsController.showForeignProperty(testChangeToQuarterly, testTaxYear).url
          )
      }
      "the Agent's Foreign property reporting method is updated to quarterly" in {
        val result = runSubmitTest(isAgent = true, ForeignProperty, testChangeToQuarterly)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            controllers.incomeSources.manage.routes
              .ManageObligationsController.showAgentForeignProperty(testChangeToQuarterly, testTaxYear).url
          )
      }
    }

    s"return ${Status.SEE_OTHER} and redirect to the Manage Obligations page for a Sole Trader Business" when {
      "the Individual's Sole Trader Business reporting method is updated to annual" in {
        val result = runSubmitTest(isAgent = false, SelfEmployment)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            controllers.incomeSources.manage.routes
              .ManageObligationsController.showSelfEmployment(testChangeToAnnual, testTaxYear).url
          )
      }
      "the Agent's Foreign property reporting method is updated to annual" in {
        val result = runSubmitTest(isAgent = true, ForeignProperty)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            controllers.incomeSources.manage.routes
              .ManageObligationsController.showAgentForeignProperty(testChangeToAnnual, testTaxYear).url
          )
      }
    }
  }


  def runShowTest(isAgent: Boolean,
                  disableIncomeSources: Boolean = false,
                  changeTo: String = testChangeToAnnual,
                  taxYear: String = testTaxYear,
                  incomeSourceType: IncomeSourceType = SelfEmployment,
                  incomeSourceId: Option[String] = Some(testIncomeSourceId)
                 ): Future[Result] = {

    if (disableIncomeSources)
      disable(IncomeSources)

    mockBothPropertyBothBusiness()

    if (isAgent)
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

    when(mockSessionService.getMongoKey(any(), any())(any(), any()))
      .thenReturn(Future(Right(incomeSourceId)))

    TestConfirmReportingMethodSharedController
      .show(taxYear, changeTo, isAgent, incomeSourceType)(
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
                    withValidForm: Boolean = true,
                    disableIncomeSources: Boolean = false,
                    withUpdateIncomeSourceResponseError: Boolean = false,
                    incomeSourceId: Option[String] = Some(testIncomeSourceId)
                   ): Future[Result] = {

    if (disableIncomeSources)
      disable(IncomeSources)

    mockBothPropertyBothBusiness()

    if (isAgent)
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

    when(mockSessionService.getMongoKey(any(), any())(any(), any()))
      .thenReturn(Future(Right(incomeSourceId)))

    when(
      TestConfirmReportingMethodSharedController
        .updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
      .thenReturn(
        Future(
          if (withUpdateIncomeSourceResponseError)
            UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Dummy message")
          else
            UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")
        )
      )

    TestConfirmReportingMethodSharedController
      .submit(taxYear, changeTo, isAgent, incomeSourceType)(
        (if (isAgent)
          fakePostRequestConfirmedClient()
        else
          fakePostRequestWithActiveSession).withFormUrlEncodedBody(
          if (withValidForm)
            validTestForm
          else
            invalidTestForm
        )
      )
  }

  override def beforeEach(): Unit = {
    disableAllSwitches()
    enable(IncomeSources)
  }

  private lazy val testIncomeSourceId = "XA00001234"
  private lazy val testTaxYear = "2022-2023"
  private lazy val invalidTaxYear = "$$$$-££££"
  private lazy val invalidChangeTo = "randomText"
  private lazy val testChangeToAnnual = "annual"
  private lazy val testChangeToQuarterly = "quarterly"
  private lazy val validTestForm: (String, String) = ConfirmReportingMethodForm.confirmReportingMethod -> "true"
  private lazy val invalidTestForm: (String, String) = "INVALID_ENTRY" -> "INVALID_ENTRY"
}