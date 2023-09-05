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
import enums.IncomeSourceJourney.ForeignProperty
import forms.CeaseForeignPropertyForm
import forms.utils.SessionKeys.ceaseForeignPropertyDeclare
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.data.FormError
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.{IncomeSourceEndDate, CeaseForeignProperty}

import scala.concurrent.Future

class CeaseForeignPropertyControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockCeaseForeignProperty: CeaseForeignProperty = app.injector.instanceOf[CeaseForeignProperty]
  val mockBusinessEndDate: IncomeSourceEndDate = mock(classOf[IncomeSourceEndDate])

  object TestCeaseForeignPropertyController extends CeaseForeignPropertyController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[CeaseForeignProperty],
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.ceaseForeignProperty.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.ceaseForeignProperty.heading"))}"
    val heading: String = messages("incomeSources.ceaseForeignProperty.heading")
  }


  "Individual - CeaseForeignPropertyController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        enable(IncomeSources)
        mockPropertyIncomeSource()
        val result: Future[Result] = TestCeaseForeignPropertyController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestCeaseForeignPropertyController.title
        document.select("legend:nth-child(1)").text shouldBe TestCeaseForeignPropertyController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestCeaseForeignPropertyController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestCeaseForeignPropertyController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestCeaseForeignPropertyController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - CeaseForeignPropertyController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(None, ForeignProperty.key).url}" when {
      "form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "true")))
        when(mockBusinessEndDate(any(), any(), any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)

        lazy val result: Future[Result] = {
          TestCeaseForeignPropertyController.submit()(fakeRequestCeaseForeignPropertyDeclarationComplete
            .withFormUrlEncodedBody(CeaseForeignPropertyForm.declaration -> "true", CeaseForeignPropertyForm.ceaseCsrfToken -> "12345"))
        }
        result.futureValue.session(fakeRequestCeaseForeignPropertyDeclarationComplete).get(ceaseForeignPropertyDeclare) shouldBe Some("true")
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(None, ForeignProperty.key).url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "invalid")))

        val incompleteForm = CeaseForeignPropertyForm.form.withError(FormError(CeaseForeignPropertyForm.declaration, CeaseForeignPropertyForm.declarationUnselectedError))
        val expectedContent: String = mockCeaseForeignProperty(
          ceaseForeignPropertyForm = incompleteForm,
          postAction = controllers.incomeSources.cease.routes.CeaseForeignPropertyController.submit,
          backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url,
          isAgent = false
        ).toString

        lazy val result: Future[Result] = {
          TestCeaseForeignPropertyController.submit()(fakeRequestCeaseForeignPropertyDeclarationIncomplete
            .withFormUrlEncodedBody(CeaseForeignPropertyForm.declaration -> "invalid", CeaseForeignPropertyForm.ceaseCsrfToken -> "12345"))
        }

        result.futureValue.session(fakeRequestCeaseForeignPropertyDeclarationIncomplete).get(ceaseForeignPropertyDeclare) shouldBe Some("false")
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe expectedContent
      }
    }
  }


  "Agent - CeaseForeignPropertyController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestCeaseForeignPropertyController.showAgent()(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestCeaseForeignPropertyController.titleAgent
        document.select("legend:nth-child(1)").text shouldBe TestCeaseForeignPropertyController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestCeaseForeignPropertyController.showAgent()(fakeRequestConfirmedClient())
        val expectedContent: String = TestCeaseForeignPropertyController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestCeaseForeignPropertyController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - CeaseForeignPropertyController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showAgent(None, ForeignProperty.key).url}" when {
      "form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "true")))
        when(mockBusinessEndDate(any(), any(), any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)

        lazy val result: Future[Result] = {
          TestCeaseForeignPropertyController.submitAgent()(fakeRequestCeaseForeignPropertyDeclarationCompleteAgent
            .withFormUrlEncodedBody(CeaseForeignPropertyForm.declaration -> "true", CeaseForeignPropertyForm.ceaseCsrfToken -> "12345"))
        }
        result.futureValue.session(fakeRequestCeaseForeignPropertyDeclarationComplete).get(ceaseForeignPropertyDeclare) shouldBe Some("true")
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showAgent(None, ForeignProperty.key).url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "invalid")))

        val incompleteForm = CeaseForeignPropertyForm.form.withError(FormError(CeaseForeignPropertyForm.declaration, CeaseForeignPropertyForm.declarationUnselectedError))
        val expectedContent: String = mockCeaseForeignProperty(
          ceaseForeignPropertyForm = incompleteForm,
          postAction = controllers.incomeSources.cease.routes.CeaseForeignPropertyController.submitAgent,
          backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url,
          isAgent = true
        ).toString

        lazy val result: Future[Result] = {
          TestCeaseForeignPropertyController.submitAgent()(fakeRequestCeaseForeignPropertyDeclarationIncompleteAgent
            .withFormUrlEncodedBody(CeaseForeignPropertyForm.declaration -> "false", CeaseForeignPropertyForm.ceaseCsrfToken -> "12345"))
        }

        result.futureValue.session(fakeRequestCeaseForeignPropertyDeclarationIncomplete).get(ceaseForeignPropertyDeclare) shouldBe Some("false")
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe expectedContent
      }
    }
  }
}
