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

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.incomeSources.cease.DateUKPropertyCeasedForm
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
import views.html.incomeSources.cease.DateUKPropertyCeased

import scala.concurrent.Future

class DateUKPropertyCeasedControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  object TestDateUKPropertyCeasedController extends DateUKPropertyCeasedController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[DateUKPropertyCeasedForm],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[DateUKPropertyCeased],
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.cease.dateUKPropertyCeased.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.cease.dateUKPropertyCeased.heading"))}"
    val heading: String = messages("incomeSources.cease.dateUKPropertyCeased.heading")
    
  }

  "Individual - DateUKPropertyCeasedController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        val result: Future[Result] = TestDateUKPropertyCeasedController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestDateUKPropertyCeasedController.title
        document.select("h1").text shouldBe TestDateUKPropertyCeasedController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestDateUKPropertyCeasedController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestDateUKPropertyCeasedController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestDateUKPropertyCeasedController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - CeaseUKPropertyController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.DateUKPropertyCeasedController.show().url}" when {
      "form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestDateUKPropertyCeasedController.submit()(fakeRequestCeaseUKPropertyDeclarationComplete
            .withFormUrlEncodedBody("date-uk-property-stopped.day" -> "20", "date-uk-property-stopped.month" -> "12",
              "date-uk-property-stopped.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.show().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestDateUKPropertyCeasedController.submit()(fakeRequestCeaseUKPropertyDeclarationComplete.withMethod("POST")
            .withFormUrlEncodedBody("date-uk-property-stopped.day" -> "", "date-uk-property-stopped.month" -> "12",
              "date-uk-property-stopped.year" -> "2022"))
        }

        status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }

  "Agent - DateUKPropertyCeasedController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        val result: Future[Result] = TestDateUKPropertyCeasedController.showAgent()(fakeRequestCeaseUKPropertyDeclarationCompleteAgent)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestDateUKPropertyCeasedController.titleAgent
        document.select("legend:nth-child(1)").text shouldBe TestDateUKPropertyCeasedController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestDateUKPropertyCeasedController.showAgent()(fakeRequestConfirmedClient())
        val expectedContent: String = TestDateUKPropertyCeasedController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestDateUKPropertyCeasedController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - DateUKPropertyCeasedController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.DateUKPropertyCeasedController.showAgent().url}" when {
      "form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestDateUKPropertyCeasedController.submitAgent()(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("date-uk-property-stopped.day" -> "20", "date-uk-property-stopped.month" -> "12",
              "date-uk-property-stopped.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.showAgent().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestDateUKPropertyCeasedController.submitAgent()(fakeRequestCeaseUKPropertyDeclarationCompleteAgent.withMethod("POST")
            .withFormUrlEncodedBody("date-uk-property-stopped.day" -> "", "date-uk-property-stopped.month" -> "12",
              "date-uk-property-stopped.year" -> "2023"))
        }

        status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }
  
  
}
