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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.incomeSources.manage.ConfirmReportingMethodForm
import implicits.ImplicitDateFormatter
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import models.updateIncomeSource.{UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.UpdateIncomeSourceService
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testPropertyIncomeId, testSelfEmploymentId}
import testUtils.TestSupport
import views.html.incomeSources.manage.{ConfirmReportingMethod, ManageIncomeSources}
import play.i18n.MessagesApi


import scala.concurrent.Future

class ConfirmReportingMethodSharedControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with ImplicitDateFormatter
  with MockIncomeSourceDetailsService
  with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching
  with TestSupport {

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
      confirmReportingMethod = app.injector.instanceOf[ConfirmReportingMethod]
    )(itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      ec = ec
    )


  private lazy val manageObligationsController = controllers.incomeSources.manage.routes
    .ManageObligationsController

  val testIncomeSourceId = "XAIS00000099004"

  val testTaxYear = "2022-2023"

  val testChangeToAnnual = "annual"

  val testChangeToQuarterly = "quarterly"

  "Individual: ConfirmReportingMethodSharedController.show" should {
    "redirect to home page" when {
      "the IncomeSources FS is disabled" in {
        disableAllSwitches()
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestConfirmReportingMethodSharedController.show(
          Some(testIncomeSourceId), testTaxYear, testChangeToAnnual, SelfEmployment.key, isAgent = false)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "taxYear parameter has an invalid format" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestConfirmReportingMethodSharedController.show(
          Some(testIncomeSourceId), "$$$$-££££", testChangeToAnnual, SelfEmployment.key, isAgent = false)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "changeTo parameter has an invalid format" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestConfirmReportingMethodSharedController.show(
          None, testTaxYear, "randomText", ForeignProperty.key, isAgent = false)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "the given incomeSourceId cannot be found in the user's Sole Trader business income sources" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestConfirmReportingMethodSharedController.show(
          Some("RANDOM_ID"), testTaxYear, testChangeToQuarterly, SelfEmployment.key, isAgent = false)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.OK}" when {
      "all query parameters are valid" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestConfirmReportingMethodSharedController.show(
          None, testTaxYear, testChangeToQuarterly, UkProperty.key, isAgent = false)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.OK
      }
    }
  }

  "Individual: ConfirmReportingMethodSharedController.submit" should {
    "redirect to home page" when {
      "the IncomeSources FS is disabled" in {
        disableAllSwitches()
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testIncomeSourceId, testTaxYear, testChangeToAnnual, SelfEmployment.key, isAgent = false)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "taxYear parameter has an invalid format" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testIncomeSourceId, "$$$$-££££", testChangeToAnnual, SelfEmployment.key, isAgent = false)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "changeTo parameter has an invalid format" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testPropertyIncomeId, testTaxYear, "randomText", ForeignProperty.key, isAgent = false)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.BAD_REQUEST}" when {
      "the form is empty" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testPropertyIncomeId, testTaxYear, testChangeToAnnual, UkProperty.key, isAgent = false)(fakeRequestWithActiveSession.withFormUrlEncodedBody())
        status(result) shouldBe Status.BAD_REQUEST
      }
    }
    "redirect to the Manage Obligations page for a UK property" when {
      "the user's UK property reporting method is updated to annual" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testPropertyIncomeId, testTaxYear, testChangeToAnnual, UkProperty.key, isAgent = false)(fakeRequestWithActiveSession.withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showUKProperty(testChangeToAnnual, testTaxYear).url)
      }
    }
    "redirect to the Manage Obligations page for a UK property" when {
      "the user's UK property reporting method is updated to quarterly" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testPropertyIncomeId, testTaxYear, testChangeToQuarterly, UkProperty.key, isAgent = false)(fakeRequestWithActiveSession.withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showUKProperty(testChangeToQuarterly, testTaxYear).url)
      }
    }
    "redirect to the Manage Obligations page for a Foreign property" when {
      "the user's Foreign property reporting method is updated to annual" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testPropertyIncomeId, testTaxYear, testChangeToAnnual, ForeignProperty.key, isAgent = false)(fakeRequestWithActiveSession.withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showForeignProperty(testChangeToAnnual, testTaxYear).url)
      }
    }
    "redirect to the Manage Obligations page for a Foreign property" when {
      "the user's Foreign property reporting method is updated to quarterly" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testPropertyIncomeId, testTaxYear, testChangeToQuarterly, ForeignProperty.key, isAgent = false)(fakeRequestWithActiveSession.withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showForeignProperty(testChangeToQuarterly, testTaxYear).url)
      }
    }
    "redirect to the Manage Obligations page for a Sole Trader Business" when {
      "the user's Sole Trader Business reporting method is updated to annual" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testSelfEmploymentId, testTaxYear, testChangeToAnnual, SelfEmployment.key, isAgent = false)(fakeRequestWithActiveSession.withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showSelfEmployment(testChangeToAnnual, testTaxYear, testSelfEmploymentId).url)
      }
    }
    "redirect to the Manage Obligations page for a Sole Trader Business" when {
      "the user's Sole Trader Business reporting method is updated to quarterly" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testSelfEmploymentId, testTaxYear, testChangeToQuarterly, SelfEmployment.key, isAgent = false)(fakeRequestWithActiveSession.withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showSelfEmployment(testChangeToQuarterly, testTaxYear, testSelfEmploymentId).url)
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "UpdateIncomeSourceService returns a UpdateIncomeSourceResponseError response" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, "Dummy message")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testSelfEmploymentId, testTaxYear, testChangeToQuarterly, SelfEmployment.key, isAgent = false)(fakeRequestWithActiveSession.withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))

        redirectLocation(result) shouldBe Some(controllers.incomeSources.manage.routes.ReportingMethodChangeErrorController.show(Some(testSelfEmploymentId), SelfEmployment.key, isAgent = false).url)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent: ConfirmReportingMethodSharedController.show" should {
    "redirect to home page" when {
      "the IncomeSources FS is disabled" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBISWithCurrentYearAsMigrationYear()

        val result: Future[Result] = TestConfirmReportingMethodSharedController.show(
          Some(testIncomeSourceId), testTaxYear, testChangeToAnnual, SelfEmployment.key, isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "taxYear parameter has an invalid format" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestConfirmReportingMethodSharedController.show(
          Some(testIncomeSourceId), "$$$$-££££", testChangeToAnnual, SelfEmployment.key, isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "changeTo parameter has an invalid format" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestConfirmReportingMethodSharedController.show(
          None, testTaxYear, "randomText", ForeignProperty.key, isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "the given incomeSourceId is can not be found in the user's income sources" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestConfirmReportingMethodSharedController.show(
          Some("RANDOM_ID"), testTaxYear, testChangeToQuarterly, SelfEmployment.key, isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.OK}" when {
      "all query parameters are valid" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestConfirmReportingMethodSharedController.show(
          None, testTaxYear, testChangeToQuarterly, UkProperty.key, isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.OK
      }
    }
    s"return ${Status.OK}" when {
      "all query parameters are valid for a Sole Trader Business" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestConfirmReportingMethodSharedController.show(
          Some(testSelfEmploymentId), testTaxYear, testChangeToQuarterly, SelfEmployment.key, isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.OK
      }
    }
    s"return ${Status.OK}" when {
      "all query parameters are valid for a Foreign Property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestConfirmReportingMethodSharedController.show(
          None, testTaxYear, testChangeToQuarterly, ForeignProperty.key, isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.OK
      }
    }
  }

  "ConfirmReportingMethodSharedController.submitAgent" should {
    "redirect to home page" when {
      "the IncomeSources FS is disabled" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBISWithCurrentYearAsMigrationYear()

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testIncomeSourceId, testTaxYear, testChangeToAnnual, SelfEmployment.key, isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "taxYear parameter has an invalid format" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testIncomeSourceId, "$$$$-££££", testChangeToAnnual, SelfEmployment.key, isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "changeTo parameter has an invalid format" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testPropertyIncomeId, testTaxYear, "randomText", ForeignProperty.key, isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.BAD_REQUEST}" when {
      "the form is empty" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testPropertyIncomeId, testTaxYear, testChangeToAnnual, UkProperty.key, isAgent = true)(fakeRequestConfirmedClient().withFormUrlEncodedBody())
        status(result) shouldBe Status.BAD_REQUEST
      }
    }
    "redirect to the Manage Obligations page for a UK property" when {
      "the user's UK property reporting method is updated to annual" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testPropertyIncomeId, testTaxYear, testChangeToAnnual, UkProperty.key, isAgent = true)(fakeRequestConfirmedClient().withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showAgentUKProperty(testChangeToAnnual, testTaxYear).url)

      }
    }
    "redirect to the Manage Obligations page for a UK property" when {
      "the user's UK property reporting method is updated to quarterly" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testPropertyIncomeId, testTaxYear, testChangeToQuarterly, UkProperty.key, isAgent = true)(fakeRequestConfirmedClient().withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showAgentUKProperty(testChangeToQuarterly, testTaxYear).url)

      }
    }
    "redirect to the Manage Obligations page for a Foreign property" when {
      "the user's Foreign property reporting method is updated to annual" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testPropertyIncomeId, testTaxYear, testChangeToAnnual, ForeignProperty.key, isAgent = true)(fakeRequestConfirmedClient().withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showAgentForeignProperty(testChangeToAnnual, testTaxYear).url)
      }
    }
    "redirect to the Manage Obligations page for a Foreign property" when {
      "the user's Foreign property reporting method is updated to quarterly" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testPropertyIncomeId, testTaxYear, testChangeToQuarterly, ForeignProperty.key, isAgent = true)(fakeRequestConfirmedClient().withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showAgentForeignProperty(testChangeToQuarterly, testTaxYear).url)

      }
    }
    "redirect to the Manage Obligations page for a Sole Trader Business" when {
      "the user's Sole Trader Business reporting method is updated to annual" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testSelfEmploymentId, testTaxYear, testChangeToAnnual, SelfEmployment.key, isAgent = true)(fakeRequestConfirmedClient().withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController.showAgentSelfEmployment(testChangeToAnnual, testTaxYear, testSelfEmploymentId).url)
      }
    }
    "redirect to the Manage Obligations page for a Sole Trader Business" when {
      "the user's Sole Trader Business reporting method is updated to quarterly" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testSelfEmploymentId, testTaxYear, testChangeToQuarterly, SelfEmployment.key, isAgent = true)(fakeRequestConfirmedClient().withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(manageObligationsController
          .showAgentSelfEmployment(testChangeToQuarterly, testTaxYear, testSelfEmploymentId).url)
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "UpdateIncomeSourceService returns a UpdateIncomeSourceResponseError response" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        when(TestConfirmReportingMethodSharedController.updateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
          .thenReturn(Future(UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, "Dummy message")))

        val result: Future[Result] = TestConfirmReportingMethodSharedController.submit(
          testSelfEmploymentId, testTaxYear, testChangeToQuarterly, SelfEmployment.key, isAgent = true)(fakeRequestConfirmedClient().withFormUrlEncodedBody(
          ConfirmReportingMethodForm.confirmReportingMethod -> "true"
        ))

        redirectLocation(result) shouldBe Some(controllers.incomeSources.manage.routes.ReportingMethodChangeErrorController.show(Some(testSelfEmploymentId), SelfEmployment.key, isAgent = true).url)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
}
