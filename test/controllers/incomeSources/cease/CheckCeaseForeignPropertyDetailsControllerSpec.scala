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

import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import connectors.IncomeTaxViewChangeConnector
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{UpdateIncomeSourceError, UpdateIncomeSourceService, UpdateIncomeSourceSuccess}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testConstants.UpdateIncomeSourceTestConstants.cessationDate
import testUtils.TestSupport
import views.html.incomeSources.cease.CheckCeaseForeignPropertyDetails

import scala.concurrent.Future

class CheckCeaseForeignPropertyDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with FeatureSwitching {

  val mockUpdateIncomeSourceService: UpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])

  object TestCheckCeaseForeignPropertyDetailsController extends CheckCeaseForeignPropertyDetailsController(
    authenticate = MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    retrieveBtaNavBar = app.injector.instanceOf[NavBarPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    updateIncomeSourceservice = mockUpdateIncomeSourceService,
    checkCeaseForeignPropertyDetails = app.injector.instanceOf[CheckCeaseForeignPropertyDetails]
  )(
    appConfig = appConfig,
    languageUtils = languageUtils,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler]
  ) {

    val title: String = s"${messages("htmlTitle", messages("check-cease-foreign-property-details.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("check-cease-foreign-property-details.heading"))}"
    val heading: String = messages("check-cease-foreign-property-details.heading")

  }

  val testIncomeSourceId = "123"


  "Individual - CheckCeaseForeignPropertyDetailsController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        val result: Future[Result] = TestCheckCeaseForeignPropertyDetailsController.show()(fakeRequestWithCeaseForeignPropertyDate(cessationDate))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestCheckCeaseForeignPropertyDetailsController.title
        document.select("h1").text shouldBe TestCheckCeaseForeignPropertyDetailsController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to the home page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestCheckCeaseForeignPropertyDetailsController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestCheckCeaseForeignPropertyDetailsController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
    "return 500 INTERNAL_SERVER_ERROR" when {
      "session does not contain: ceaseForeignPropertyEndDate" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        val result: Future[Result] = TestCheckCeaseForeignPropertyDetailsController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "Individual - CheckCeaseForeignPropertyDetailsController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.ForeignPropertyCeasedObligationsController.show().url}" when {
      "submitted" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        when(mockUpdateIncomeSourceService.updateCessationDatev2(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testIncomeSourceId))))

        lazy val result: Future[Result] = {
          TestCheckCeaseForeignPropertyDetailsController.submit(cessationDate)(fakeRequestWithCeaseForeignPropertyDate(cessationDate))
        }
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.ForeignPropertyCeasedObligationsController.show().url)
      }
    }
    s"return 203 SEE_OTHER and redirect to the home page" when {
      "FS disabled" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disable(IncomeSources)
        mockForeignPropertyIncomeSource()

        lazy val result: Future[Result] = {
          TestCheckCeaseForeignPropertyDetailsController.submit(cessationDate)(fakeRequestWithCeaseForeignPropertyDate(cessationDate))
        }

        status(result) shouldBe Status.SEE_OTHER
      }
    }
    s"return 500 INTERNAL_SERVER_ERROR" when {
      "UpdateIncomeSourceError model returned from UpdateIncomeSourceService" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        when(mockUpdateIncomeSourceService.updateCessationDatev2(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Left(UpdateIncomeSourceError("Failed to update cessationDate"))))

        lazy val result: Future[Result] = {
          TestCheckCeaseForeignPropertyDetailsController.submit(cessationDate)(fakeRequestWithCeaseForeignPropertyDate(cessationDate))
        }
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    s"return 500 INTERNAL_SERVER_ERROR" when {
      "user has no foreign property income sources" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockUpdateIncomeSourceService.updateCessationDatev2(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testIncomeSourceId))))

        lazy val result: Future[Result] = {
          TestCheckCeaseForeignPropertyDetailsController.submit(cessationDate)(fakeRequestWithCeaseForeignPropertyDate(cessationDate))
        }
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "Agent - CheckCeaseForeignPropertyDetailsController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        val result: Future[Result] = TestCheckCeaseForeignPropertyDetailsController.showAgent()(fakeRequestConfirmedClient()
          .withSession(forms.utils.SessionKeys.ceaseForeignPropertyEndDate -> cessationDate))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestCheckCeaseForeignPropertyDetailsController.titleAgent
        document.select("h1").text shouldBe TestCheckCeaseForeignPropertyDetailsController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to home page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestCheckCeaseForeignPropertyDetailsController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestCheckCeaseForeignPropertyDetailsController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
    "return 500 INTERNAL_SERVER_ERROR" when {
      "session does not contain: ceaseForeignPropertyEndDate" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestCheckCeaseForeignPropertyDetailsController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "Agent - CheckCeaseForeignPropertyDetailsController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.ForeignPropertyEndDateController.showAgent().url}" when {
      "form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        when(mockUpdateIncomeSourceService.updateCessationDatev2(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testIncomeSourceId))))

        lazy val result: Future[Result] = {
          TestCheckCeaseForeignPropertyDetailsController.submitAgent(cessationDate)(fakeRequestConfirmedClient()
            .withSession(forms.utils.SessionKeys.ceaseForeignPropertyEndDate -> cessationDate)
            .withMethod("POST"))
        }

        status(result) shouldBe Status.SEE_OTHER
      }
    }
    s"redirect to home page 303 SEE_OTHER" when {
      "FS disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockForeignPropertyIncomeSource()

        when(mockUpdateIncomeSourceService.updateCessationDatev2(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testIncomeSourceId))))

        lazy val result: Future[Result] = {
          TestCheckCeaseForeignPropertyDetailsController.submitAgent(cessationDate)(fakeRequestConfirmedClient()
            .withSession(forms.utils.SessionKeys.ceaseForeignPropertyEndDate -> cessationDate)
            .withMethod("POST"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    s"return 500 INTERNAL_SERVER_ERROR" when {
      "user has no foreign property income sources" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockNoIncomeSources()

        lazy val result: Future[Result] = {
          TestCheckCeaseForeignPropertyDetailsController.submitAgent(cessationDate)(fakeRequestConfirmedClient()
            .withMethod("POST"))
        }

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
