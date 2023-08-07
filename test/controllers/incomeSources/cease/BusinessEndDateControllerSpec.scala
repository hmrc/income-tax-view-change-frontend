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
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.incomeSources.cease.BusinessEndDateForm
import forms.utils.SessionKeys.{ceaseBusinessEndDate, ceaseBusinessIncomeSourceId}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testSelfEmploymentId}
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.BusinessEndDate

import scala.concurrent.Future

class BusinessEndDateControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  object TestBusinessEndDateController extends BusinessEndDateController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[BusinessEndDateForm],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[BusinessEndDate],
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.cease.BusinessEndDate.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.cease.BusinessEndDate.heading"))}"
    val heading: String = messages("incomeSources.cease.BusinessEndDate.heading")
  }

  "Individual - BusinessEndDateController.show(id)" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = TestBusinessEndDateController.show(testSelfEmploymentId)(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessEndDateController.title
        document.select("h1").text shouldBe TestBusinessEndDateController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = TestBusinessEndDateController.show(testSelfEmploymentId)(fakeRequestWithActiveSession)
        val expectedContent: String = TestBusinessEndDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestBusinessEndDateController.show(testSelfEmploymentId)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
  "Individual - CeaseBusinessController.submit(id)" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.BusinessEndDateController.show(testSelfEmploymentId).url}" when {
      "form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestBusinessEndDateController.submit(testSelfEmploymentId)(fakeRequestNoSession
            .withFormUrlEncodedBody("business-end-date.day" -> "27", "business-end-date.month" -> "8",
              "business-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(ceaseBusinessEndDate) shouldBe Some("2022-08-27")
        result.futureValue.session.get(ceaseBusinessIncomeSourceId) shouldBe Some(testSelfEmploymentId)
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.show().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestBusinessEndDateController.submit(testSelfEmploymentId)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("business-end-date.day" -> "", "business-end-date.month" -> "8",
              "business-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.BAD_REQUEST
        result.futureValue.session.get(ceaseBusinessEndDate) shouldBe None
        result.futureValue.session.get(ceaseBusinessIncomeSourceId) shouldBe None
      }
    }
  }
  "Agent - BusinessEndDateController.show(id)" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = TestBusinessEndDateController.showAgent(testSelfEmploymentId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessEndDateController.titleAgent
        document.select("legend:nth-child(1)").text shouldBe TestBusinessEndDateController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = TestBusinessEndDateController.showAgent(testSelfEmploymentId)(fakeRequestConfirmedClient())
        val expectedContent: String = TestBusinessEndDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestBusinessEndDateController.showAgent(testSelfEmploymentId)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - BusinessEndDateController.submit(id)" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.BusinessEndDateController.showAgent(testSelfEmploymentId).url}" when {
      "form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestBusinessEndDateController.submitAgent(testSelfEmploymentId)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("business-end-date.day" -> "27", "business-end-date.month" -> "8",
              "business-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(ceaseBusinessEndDate) shouldBe Some("2022-08-27")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.showAgent().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestBusinessEndDateController.submitAgent(testSelfEmploymentId)(fakeRequestConfirmedClient().withMethod("POST")
            .withFormUrlEncodedBody("business-end-date.day" -> "", "business-end-date.month" -> "8",
              "business-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.BAD_REQUEST
        result.futureValue.session.get(ceaseBusinessEndDate) shouldBe None
        result.futureValue.session.get(ceaseBusinessIncomeSourceId) shouldBe None
      }
    }
  }
}
