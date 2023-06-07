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

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import forms.utils.SessionKeys.addUkPropertyStartDate
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
import views.html.incomeSources.add.AddUKPropertyStartDate

import scala.concurrent.Future

class AddUKPropertyStartDateControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  object TestAddUKPropertyStartDateController extends AddUKPropertyStartDateController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[AddUKPropertyStartDate],
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mockImplicitDateFormatter,
    dateService,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.add.UKPropertyStartDate.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.add.UKPropertyStartDate.heading"))}"
    val heading: String = messages("incomeSources.add.UKPropertyStartDate.heading")
    val headingAgent: String = messages("incomeSources.add.UKPropertyStartDate.heading")
  }

  "Individual - AddUKPropertyBusinessController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()
        val result: Future[Result] = TestAddUKPropertyStartDateController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestAddUKPropertyStartDateController.title
        document.select("h1:nth-child(1)").text shouldBe TestAddUKPropertyStartDateController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = TestAddUKPropertyStartDateController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestAddUKPropertyStartDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestAddUKPropertyStartDateController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - AddUKPropertyStartDateController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.show().url}" when {
      "form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestAddUKPropertyStartDateController.submit()(fakeRequestWithActiveSession
            .withSession(SessionKeys.addUkPropertyStartDate -> "2023-04-20")
            .withFormUrlEncodedBody("add-uk-property-start-date.day" -> "20", "add-uk-property-start-date.month" -> "04",
              "add-uk-property-start-date.year" -> "2023"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addUkPropertyStartDate) shouldBe Some("2023-04-20")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.show().url)
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
          TestAddUKPropertyStartDateController.submit()(fakeRequestWithActiveSession
            .withFormUrlEncodedBody("add-uk-property-start-date.day" -> "", "add-uk-property-start-date.month" -> "",
              "add-uk-property-start-date.year" -> ""))
        }

        result.futureValue.session.get(addUkPropertyStartDate) shouldBe None
        status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }

  "Agent - AddUKPropertyBusinessController.showAgent" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()
        val result: Future[Result] = TestAddUKPropertyStartDateController.showAgent()(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestAddUKPropertyStartDateController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestAddUKPropertyStartDateController.headingAgent
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = TestAddUKPropertyStartDateController.showAgent()(fakeRequestConfirmedClient())
        val expectedContent: String = TestAddUKPropertyStartDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestAddUKPropertyStartDateController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - AddUKPropertyStartDateController.submitAgent" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.showAgent().url}" when {
      "form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestAddUKPropertyStartDateController.submitAgent()(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("add-uk-property-start-date.day" -> "20", "add-uk-property-start-date.month" -> "04",
              "add-uk-property-start-date.year" -> "2023"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.showAgent().url)
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
          TestAddUKPropertyStartDateController.submitAgent()(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("add-uk-property-start-date.day" -> "", "add-uk-property-start-date.month" -> "12",
              "add-uk-property-start-date.year" -> "2022"))
        }

        status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }
}
