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

import audit.AuditingService
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
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
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
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      updateIncomeSourceService = mock(classOf[UpdateIncomeSourceService]),
      confirmReportingMethod = app.injector.instanceOf[ConfirmReportingMethod],
      dateService = dateService,
      auditingService = app.injector.instanceOf[AuditingService],
      sessionService = mockSessionService
    )(itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      ec = ec
    )

  override def beforeEach(): Unit = {
    disableAllSwitches()
    enable(IncomeSources)
  }

  def mockAuthRetrieval(isAgent: Boolean): Unit = {
    if (isAgent)
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
  }

  def getUserSession(isAgent: Boolean): FakeRequest[AnyContentAsEmpty.type] = {
    if (isAgent)
      fakeRequestConfirmedClient()
    else
      fakeRequestWithActiveSession
  }

  private lazy val manageObligationsController = controllers.incomeSources.manage.routes
    .ManageObligationsController

  private lazy val reportingMethodChangeErrorController = controllers.incomeSources.manage.routes
    .ReportingMethodChangeErrorController

  val testIncomeSourceId = "XA00001234"

  val testTaxYear = "2022-2023"

  val invalidTaxYear = "$$$$-££££"

  val invalidChangeTo = "randomText"

  val testChangeToAnnual = "annual"

  val testChangeToQuarterly = "quarterly"

  val testForm: (String, String) = ConfirmReportingMethodForm.confirmReportingMethod -> "true"

  "ConfirmReportingMethodSharedController.show" should {
    s"return ${Status.SEE_OTHER} and redirect to the home page" when {
      "the IncomeSources FS is disabled for an Individual" in {
        runShowWithReturnSeeOtherTest(isAgent = false)
      }
      "the IncomeSources FS is disabled for an Agent" in {
        runShowWithReturnSeeOtherTest(isAgent = true)
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "taxYear parameter has an invalid format for an Individual" in {
        runShowWithReturnISETest(isAgent = false, taxYear = invalidTaxYear)
      }
      "changeTo parameter has an invalid format for an Individual" in {
        runShowWithReturnISETest(isAgent = false, changeTo = invalidChangeTo)
      }
      "the given incomeSourceId cannot be found in the Individual's Sole Trader business income sources" in {
        runShowWithReturnISETest(isAgent = false, incomeSourceId = None)
      }
      "taxYear parameter has an invalid format for an Agent" in {
        runShowWithReturnISETest(isAgent = true, taxYear = invalidTaxYear)
      }
      "changeTo parameter has an invalid format for an Agent" in {
        runShowWithReturnISETest(isAgent = true, changeTo = invalidChangeTo)
      }
      "the given incomeSourceId cannot be found in the Agent's Sole Trader business income sources" in {
        runShowWithReturnISETest(isAgent = true, incomeSourceId = None)
      }
    }
    s"return ${Status.OK}" when {
      "all query parameters are valid for an Individual" in {
        runShowWithReturnOKTest(isAgent = false)
      }
      "all query parameters are valid for an Agent" in {
        runShowWithReturnOKTest(isAgent = true)
      }
    }
  }

  "ConfirmReportingMethodSharedController.submit" should {
    s"return ${Status.SEE_OTHER} and redirect to the home page" when {
      "the IncomeSources FS is disabled for an Individual" in {

        val result = runSubmitWithReturnSeeOtherTest(isAgent = false, disableIncomeSourcesFS = true)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "the IncomeSources FS is disabled for an Agent" in {

        val result = runSubmitWithReturnSeeOtherTest(isAgent = true, disableIncomeSourcesFS = true)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }

      "UpdateIncomeSourceService returns a UpdateIncomeSourceResponseError response for an Individual" in {

        val result = runSubmitWithReturnSeeOtherTest(isAgent = false, withUpdateIncomeSourceResponseError = true)

        redirectLocation(result) shouldBe Some(reportingMethodChangeErrorController.show(isAgent = false, SelfEmployment).url)
        status(result) shouldBe Status.SEE_OTHER
      }

      "UpdateIncomeSourceService returns a UpdateIncomeSourceResponseError response for an Agent" in {

        val result = runSubmitWithReturnSeeOtherTest(isAgent = true, withUpdateIncomeSourceResponseError = true)

        redirectLocation(result) shouldBe Some(reportingMethodChangeErrorController.show(isAgent = true, SelfEmployment).url)
        status(result) shouldBe Status.SEE_OTHER
      }
    }

    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "taxYear parameter has an invalid format for an Individual" in {
        runSubmitWithReturnISETest(isAgent = false, taxYear = invalidTaxYear)
      }
      "changeTo parameter has an invalid format for an Individual" in {
        runSubmitWithReturnISETest(isAgent = false, changeTo = invalidChangeTo)
      }
      "taxYear parameter has an invalid format for an Agent" in {
        runSubmitWithReturnISETest(isAgent = true, taxYear = invalidTaxYear)
      }
      "changeTo parameter has an invalid format for an Agent" in {
        runSubmitWithReturnISETest(isAgent = true, changeTo = invalidChangeTo)
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
        redirectLocation(result) shouldBe Some(manageObligationsController.showUKProperty(testChangeToAnnual, testTaxYear).url)
      }
      "the Agent's UK property reporting method is updated to annual" in {

        val result = runSubmitTest(isAgent = true, UkProperty, testChangeToAnnual)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showAgentUKProperty(testChangeToAnnual, testTaxYear).url)
      }
    }

    s"return ${Status.SEE_OTHER} and redirect to the Manage Obligations page for a Foreign property" when {
      "the Individual's Foreign property reporting method is updated to quarterly" in {

        val result = runSubmitTest(isAgent = false, ForeignProperty, testChangeToQuarterly)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showForeignProperty(testChangeToQuarterly, testTaxYear).url)
      }
      "the Agent's Foreign property reporting method is updated to quarterly" in {

        val result = runSubmitTest(isAgent = true, ForeignProperty, testChangeToQuarterly)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showAgentForeignProperty(testChangeToQuarterly, testTaxYear).url)
      }
    }

    s"return ${Status.SEE_OTHER} and redirect to the Manage Obligations page for a Sole Trader Business" when {
      "the Individual's Sole Trader Business reporting method is updated to annual" in {

        val result = runSubmitTest(isAgent = false, SelfEmployment)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showSelfEmployment(testChangeToAnnual, testTaxYear).url)
      }
      "the Agent's Foreign property reporting method is updated to annual" in {

        val result = runSubmitTest(isAgent = true, SelfEmployment)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showAgentSelfEmployment(testChangeToAnnual, testTaxYear).url)
      }
    }
  }


  def runShowWithReturnSeeOtherTest(isAgent: Boolean): Unit = {
    disableAllSwitches()
    mockSingleBISWithCurrentYearAsMigrationYear()
    mockAuthRetrieval(isAgent)

    val result: Future[Result] = TestConfirmReportingMethodSharedController
      .show(testTaxYear, testChangeToAnnual, isAgent, SelfEmployment)(
        getUserSession(isAgent)
      )

    status(result) shouldBe Status.SEE_OTHER

    redirectLocation(result) shouldBe Some(
      if (isAgent)
        controllers.routes.HomeController.showAgent
      else
        controllers.routes.HomeController.show()
    ).map(_.url)
  }

  def runShowWithReturnISETest(isAgent: Boolean,
                               taxYear: String = testTaxYear,
                               changeTo: String = testChangeToAnnual,
                               incomeSourceId: Option[String] = Some(testIncomeSourceId)
                              ): Unit = {

    mockSingleBISWithCurrentYearAsMigrationYear()
    mockAuthRetrieval(isAgent)
    when(mockSessionService.getMongoKey(any(), any())(any(), any()))
      .thenReturn(Future(Right(incomeSourceId)))

    val result: Future[Result] = TestConfirmReportingMethodSharedController
      .show(taxYear, changeTo, isAgent, SelfEmployment)(
        getUserSession(isAgent)
      )

    status(result) shouldBe Status.INTERNAL_SERVER_ERROR
  }

  def runShowWithReturnOKTest(isAgent: Boolean): Unit = {

    mockUKPropertyIncomeSource()
    mockAuthRetrieval(isAgent)

    val result: Future[Result] = TestConfirmReportingMethodSharedController
      .show(testTaxYear, testChangeToQuarterly, isAgent, UkProperty)(
        getUserSession(isAgent)
      )

    status(result) shouldBe Status.OK
  }

  def runSubmitWithReturnSeeOtherTest(isAgent: Boolean,
                                      disableIncomeSourcesFS: Boolean = false,
                                      withUpdateIncomeSourceResponseError: Boolean = false
                                     ): Future[Result] = {
    if (disableIncomeSourcesFS)
      disable(IncomeSources)

    mockSingleBISWithCurrentYearAsMigrationYear()
    mockAuthRetrieval(isAgent)

    when(mockSessionService.getMongoKey(any(), any())(any(), any()))
      .thenReturn(Future(Right(Some(testIncomeSourceId))))

    if (withUpdateIncomeSourceResponseError)
      when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
        .thenReturn(Future(
          UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, "Dummy message")
        ))

    TestConfirmReportingMethodSharedController
      .submit(testTaxYear, testChangeToAnnual, isAgent, SelfEmployment)(
        getUserSession(isAgent)
          .withFormUrlEncodedBody(
            ConfirmReportingMethodForm.confirmReportingMethod -> "true"
          )
      )
  }

  def runSubmitWithReturnISETest(isAgent: Boolean,
                                 taxYear: String = testTaxYear,
                                 changeTo: String = testChangeToAnnual,
                                 incomeSourceId: Option[String] = Some(testIncomeSourceId)
                                ): Unit = {

    mockSingleBISWithCurrentYearAsMigrationYear()
    mockAuthRetrieval(isAgent)

    when(mockSessionService.getMongoKey(any(), any())(any(), any()))
      .thenReturn(Future(Right(incomeSourceId)))

    val result: Future[Result] = TestConfirmReportingMethodSharedController
      .submit(taxYear, changeTo, isAgent, SelfEmployment)(
        getUserSession(isAgent)
      )

    status(result) shouldBe Status.INTERNAL_SERVER_ERROR
  }

  def runSubmitTest(isAgent: Boolean,
                    incomeSourceType: IncomeSourceType,
                    changeTo: String = testChangeToAnnual,
                    withValidForm: Boolean = true
                   ): Future[Result] = {

    mockBothPropertyBothBusiness()
    mockAuthRetrieval(isAgent)

    when(mockSessionService.getMongoKey(any(), any())(any(), any()))
      .thenReturn(Future(Right(Some(testIncomeSourceId))))

    when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
      .thenReturn(Future(
        UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")
      ))

    TestConfirmReportingMethodSharedController
      .submit(testTaxYear, changeTo, isAgent, incomeSourceType)(
        getUserSession(isAgent)
          .withFormUrlEncodedBody(
            if (withValidForm)
              testForm
            else
              "INVALID_ENTRY" -> "INVALID_ENTRY"
          )
      )
  }
}