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
import forms.incomeSources.cease.ForeignPropertyEndDateForm
import forms.utils.SessionKeys.ceaseForeignPropertyEndDate
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
import views.html.incomeSources.cease.ForeignPropertyEndDate

import scala.concurrent.Future

class ForeignPropertyEndDateControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  object TestForeignPropertyEndDateController extends ForeignPropertyEndDateController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[ForeignPropertyEndDateForm],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[ForeignPropertyEndDate],
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.cease.ForeignPropertyEndDate.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.cease.ForeignPropertyEndDate.heading"))}"
    val heading: String = messages("incomeSources.cease.ForeignPropertyEndDate.heading")
    
  }

  "Individual - ForeignPropertyEndDateController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        val result: Future[Result] = TestForeignPropertyEndDateController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyEndDateController.title
        document.select("h1").text shouldBe TestForeignPropertyEndDateController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestForeignPropertyEndDateController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestForeignPropertyEndDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestForeignPropertyEndDateController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - CeaseForeignPropertyController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.ForeignPropertyEndDateController.show().url}" when {
      "form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestForeignPropertyEndDateController.submit()(fakeRequestCeaseForeignPropertyDeclarationComplete
            .withFormUrlEncodedBody("foreign-property-end-date.day" -> "20", "foreign-property-end-date.month" -> "12",
              "foreign-property-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(ceaseForeignPropertyEndDate) shouldBe Some("2022-12-20")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.show().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestForeignPropertyEndDateController.submit()(fakeRequestCeaseForeignPropertyDeclarationComplete.withMethod("POST")
            .withFormUrlEncodedBody("foreign-property-end-date.day" -> "", "foreign-property-end-date.month" -> "12",
              "foreign-property-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.BAD_REQUEST
        result.futureValue.session.get(ceaseForeignPropertyEndDate) shouldBe None
      }
    }
  }

  "Agent - ForeignPropertyEndDateController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        val result: Future[Result] = TestForeignPropertyEndDateController.showAgent()(fakeRequestCeaseForeignPropertyDeclarationCompleteAgent)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyEndDateController.titleAgent
        document.select("legend:nth-child(1)").text shouldBe TestForeignPropertyEndDateController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestForeignPropertyEndDateController.showAgent()(fakeRequestConfirmedClient())
        val expectedContent: String = TestForeignPropertyEndDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestForeignPropertyEndDateController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - ForeignPropertyEndDateController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.ForeignPropertyEndDateController.showAgent().url}" when {
      "form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestForeignPropertyEndDateController.submitAgent()(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("foreign-property-end-date.day" -> "20", "foreign-property-end-date.month" -> "12",
              "foreign-property-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(ceaseForeignPropertyEndDate) shouldBe Some("2022-12-20")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.showAgent().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestForeignPropertyEndDateController.submitAgent()(fakeRequestCeaseForeignPropertyDeclarationCompleteAgent.withMethod("POST")
            .withFormUrlEncodedBody("foreign-property-end-date.day" -> "", "foreign-property-end-date.month" -> "12",
              "foreign-property-end-date.year" -> "2023"))
        }

        status(result) shouldBe Status.BAD_REQUEST
        result.futureValue.session.get(ceaseForeignPropertyEndDate) shouldBe None
      }
    }
  }
  
  
}
