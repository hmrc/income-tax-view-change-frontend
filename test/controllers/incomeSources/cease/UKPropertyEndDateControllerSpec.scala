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
import forms.incomeSources.cease.UKPropertyEndDateForm
import forms.utils.SessionKeys.ceaseUKPropertyEndDate
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
import views.html.incomeSources.cease.UKPropertyEndDate

import scala.concurrent.Future

class UKPropertyEndDateControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  object TestUKPropertyEndDateController extends UKPropertyEndDateController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[UKPropertyEndDateForm],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[UKPropertyEndDate],
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.cease.UKPropertyEndDate.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.cease.UKPropertyEndDate.heading"))}"
    val heading: String = messages("incomeSources.cease.UKPropertyEndDate.heading")
    
  }

  "Individual - UKPropertyEndDateController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        val result: Future[Result] = TestUKPropertyEndDateController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestUKPropertyEndDateController.title
        document.select("h1").text shouldBe TestUKPropertyEndDateController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestUKPropertyEndDateController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestUKPropertyEndDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestUKPropertyEndDateController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - CeaseUKPropertyController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.UKPropertyEndDateController.show().url}" when {
      "form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestUKPropertyEndDateController.submit()(fakeRequestCeaseUKPropertyDeclarationComplete
            .withFormUrlEncodedBody("uk-property-end-date.day" -> "20", "uk-property-end-date.month" -> "12",
              "uk-property-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(ceaseUKPropertyEndDate) shouldBe Some("2022-12-20")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.show().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestUKPropertyEndDateController.submit()(fakeRequestCeaseUKPropertyDeclarationComplete.withMethod("POST")
            .withFormUrlEncodedBody("uk-property-end-date.day" -> "", "uk-property-end-date.month" -> "12",
              "uk-property-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.BAD_REQUEST
        result.futureValue.session.get(ceaseUKPropertyEndDate) shouldBe None
      }
    }
  }

  "Agent - UKPropertyEndDateController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        val result: Future[Result] = TestUKPropertyEndDateController.showAgent()(fakeRequestCeaseUKPropertyDeclarationCompleteAgent)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestUKPropertyEndDateController.titleAgent
        document.select("legend:nth-child(1)").text shouldBe TestUKPropertyEndDateController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestUKPropertyEndDateController.showAgent()(fakeRequestConfirmedClient())
        val expectedContent: String = TestUKPropertyEndDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestUKPropertyEndDateController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - UKPropertyEndDateController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.UKPropertyEndDateController.showAgent().url}" when {
      "form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestUKPropertyEndDateController.submitAgent()(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("uk-property-end-date.day" -> "20", "uk-property-end-date.month" -> "12",
              "uk-property-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(ceaseUKPropertyEndDate) shouldBe Some("2022-12-20")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.showAgent().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestUKPropertyEndDateController.submitAgent()(fakeRequestCeaseUKPropertyDeclarationCompleteAgent.withMethod("POST")
            .withFormUrlEncodedBody("uk-property-end-date.day" -> "", "uk-property-end-date.month" -> "12",
              "uk-property-end-date.year" -> "2023"))
        }

        status(result) shouldBe Status.BAD_REQUEST
        result.futureValue.session.get(ceaseUKPropertyEndDate) shouldBe None
      }
    }
  }
  
  
}
