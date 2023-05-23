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

import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.incomeSources.add.AddUKPropertyBusinessStartDateForm
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddUKPropertyBusinessStartDate

import scala.concurrent.Future

class AddUKPropertyBusinessStartDateControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  object TestAddUKPropertyBusinessStartDateController extends AddUKPropertyBusinessStartDateController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[AddUKPropertyBusinessStartDateForm],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[AddUKPropertyBusinessStartDate],
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.add.UKPropertyBusinessStartDate.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.add.UKPropertyBusinessStartDate.heading"))}"
    val heading: String = messages("incomeSources.add.UKPropertyBusinessStartDate.heading")
    val headingAgent: String = messages("incomeSources.add.UKPropertyBusinessStartDate.heading")
  }

  "Individual - AddUKPropertyBusinessController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()
        val result: Future[Result] = TestAddUKPropertyBusinessStartDateController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestAddUKPropertyBusinessStartDateController.title
        document.select("h1:nth-child(1)").text shouldBe TestAddUKPropertyBusinessStartDateController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = TestAddUKPropertyBusinessStartDateController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestAddUKPropertyBusinessStartDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestAddUKPropertyBusinessStartDateController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - AddUKPropertyBusinessStartDateController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.CheckUKPropertyBusinessStartDateController.show().url}" when {
      "form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestAddUKPropertyBusinessStartDateController.submit()(fakeRequestWithActiveSession
            .withFormUrlEncodedBody("add-uk-property-business-start-date.day" -> "20", "add-uk-property-business-start-date.month" -> "04",
              "add-uk-property-business-start-date.year" -> "2023"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckUKPropertyBusinessStartDateController.show().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestAddUKPropertyBusinessStartDateController.submit()(fakeRequestWithActiveSession.withMethod("POST")
            .withFormUrlEncodedBody("add-uk-property-business-start-date.day" -> "", "add-uk-property-business-start-date.month" -> "12",
              "add-uk-property-business-start-date.year" -> "2022"))
        }

        status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }

  "Agent - AddUKPropertyBusinessController.showAgent" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()
        val result: Future[Result] = TestAddUKPropertyBusinessStartDateController.showAgent()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestAddUKPropertyBusinessStartDateController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestAddUKPropertyBusinessStartDateController.headingAgent
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = TestAddUKPropertyBusinessStartDateController.showAgent()(fakeRequestWithActiveSession)
        val expectedContent: String = TestAddUKPropertyBusinessStartDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestAddUKPropertyBusinessStartDateController.showAgent()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - AddUKPropertyBusinessStartDateController.submitAgent" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.CheckUKPropertyBusinessStartDateController.showAgent().url}" when {
      "form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestAddUKPropertyBusinessStartDateController.submitAgent()(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("add-uk-property-business-start-date.day" -> "20", "add-uk-property-business-start-date.month" -> "04",
              "add-uk-property-business-start-date.year" -> "2023"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckUKPropertyBusinessStartDateController.showAgent().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestAddUKPropertyBusinessStartDateController.submitAgent()(fakeRequestConfirmedClient().withMethod("POST")
            .withFormUrlEncodedBody("add-uk-property-business-start-date.day" -> "", "add-uk-property-business-start-date.month" -> "12",
              "add-uk-property-business-start-date.year" -> "2022"))
        }

        status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }
}
