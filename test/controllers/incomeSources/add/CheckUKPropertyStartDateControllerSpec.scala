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
import enums.IncomeSourceJourney.UkProperty
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
import views.html.incomeSources.add.CheckUKPropertyStartDate

import scala.concurrent.Future

class CheckUKPropertyStartDateControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockCheckUKPropertyStartDate: CheckUKPropertyStartDate = app.injector.instanceOf[CheckUKPropertyStartDate]

  object TestCheckUKPropertyStartDateController extends CheckUKPropertyStartDateController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockImplicitDateFormatter,
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[CheckUKPropertyStartDate],
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val heading: String = messages("radioForm.checkDate.heading")
    val headingAgent: String = messages("radioForm.checkDate.heading")
    val title: String = s"${messages("htmlTitle", heading)}"
    val titleAgent: String = s"${messages("htmlTitle.agent", headingAgent)}"
  }

  "Individual - AddUKPropertyBusinessController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()
        val result: Future[Result] = TestCheckUKPropertyStartDateController.show()(fakeRequestWithActiveSession
          .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestCheckUKPropertyStartDateController.title
        document.select("h1:nth-child(1)").text shouldBe TestCheckUKPropertyStartDateController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = TestCheckUKPropertyStartDateController.show()(fakeRequestWithActiveSession
          .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15"))
        val expectedContent: String = TestCheckUKPropertyStartDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestCheckUKPropertyStartDateController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - AddUKPropertyStartDateController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(UkProperty.key).url}" when {
      "user confirms the date is correct" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestCheckUKPropertyStartDateController.submit()(fakeRequestWithActiveSession
            .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15")
            .withFormUrlEncodedBody("check-uk-property-start-date" -> "yes"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(UkProperty.key).url)
      }
      s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty.url}" when {
        "user confirms the date is incorrect" in {
          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          enable(IncomeSources)
          mockSingleBusinessIncomeSource()

          when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(HttpResponse(OK)))

          lazy val result: Future[Result] = {
            TestCheckUKPropertyStartDateController.submit()(fakeRequestWithActiveSession
              .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15")
              .withFormUrlEncodedBody("check-uk-property-start-date" -> "no"))
          }

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty.url)
          result.futureValue.session.get(addUkPropertyStartDate) shouldBe None
        }
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        lazy val result: Future[Result] = {
          TestCheckUKPropertyStartDateController.submit()(fakeRequestWithActiveSession
            .withMethod("POST")
            .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15")
            .withFormUrlEncodedBody("check-uk-property-start-date" -> ""))
        }
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
        val result: Future[Result] = TestCheckUKPropertyStartDateController.showAgent()(fakeRequestConfirmedClient()
          .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestCheckUKPropertyStartDateController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestCheckUKPropertyStartDateController.headingAgent
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = TestCheckUKPropertyStartDateController.showAgent()(fakeRequestConfirmedClient()
          .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15"))
        val expectedContent: String = TestCheckUKPropertyStartDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestCheckUKPropertyStartDateController.showAgent()(fakeRequestConfirmedClient()
          .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15"))
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - AddUKPropertyStartDateController.submitAgent" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(UkProperty.key).url}" when {
      "user confirms the date is correct" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestCheckUKPropertyStartDateController.submitAgent()(fakeRequestConfirmedClient()
            .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15")
            .withFormUrlEncodedBody("check-uk-property-start-date" -> "yes"))
        }

        status(result) shouldBe Status.SEE_OTHER

        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(UkProperty.key).url)
      }
      "user confirms the date is incorrect" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestCheckUKPropertyStartDateController.submitAgent()(fakeRequestConfirmedClient()
            .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15")
            .withFormUrlEncodedBody("check-uk-property-start-date" -> "no"))
        }

        status(result) shouldBe Status.SEE_OTHER

        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKPropertyAgent.url)
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
          TestCheckUKPropertyStartDateController.submitAgent()(fakeRequestConfirmedClient()
            .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15")
            .withFormUrlEncodedBody("check-uk-property-start-date" -> ""))
        }

        status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }
}
