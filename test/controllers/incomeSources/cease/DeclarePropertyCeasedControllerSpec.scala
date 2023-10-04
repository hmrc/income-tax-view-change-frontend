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

import config.featureswitch.{FeatureSwitching, IncomeSources, NavBarFs}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import forms.incomeSources.cease.DeclarePropertyCeasedForm
import forms.utils.SessionKeys.{ceaseForeignPropertyDeclare, ceaseUKPropertyDeclare}
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
import services.SessionService
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import views.html.incomeSources.cease.{DeclarePropertyCeased, IncomeSourceEndDate}

import scala.concurrent.Future

class DeclarePropertyCeasedControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockDeclarePropertyCeased: DeclarePropertyCeased = app.injector.instanceOf[DeclarePropertyCeased]
  val mockBusinessEndDateView: IncomeSourceEndDate = mock(classOf[IncomeSourceEndDate])

  object TestDeclarePropertyCeasedController extends DeclarePropertyCeasedController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[DeclarePropertyCeased],
    app.injector.instanceOf[SessionService])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val titleUkProperty: String = s"${messages("htmlTitle", messages("incomeSources.cease.UK.property.heading"))}"
    val titleAgentUkProperty: String = s"${messages("htmlTitle.agent", messages("incomeSources.cease.UK.property.heading"))}"
    val headingUkProperty: String = messages("incomeSources.cease.UK.property.heading")

    val titleForeignProperty: String = s"${messages("htmlTitle", messages("incomeSources.cease.FP.property.heading"))}"
    val titleAgentForeignProperty: String = s"${messages("htmlTitle.agent", messages("incomeSources.cease.FP.property.heading"))}"
    val headingForeignProperty: String = messages("incomeSources.cease.FP.property.heading")
  }


  "Individual - DeclarePropertyCeasedController.show" should {
    "return 200 OK" when {
      "income source is UK Property and FS Enabled" in {
        enable(IncomeSources)
        mockPropertyIncomeSource()
        val result: Future[Result] = TestDeclarePropertyCeasedController.show(UkProperty)(fakeRequestWithNinoAndOrigin("pta"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestDeclarePropertyCeasedController.titleUkProperty
        document.select("legend:nth-child(1)").text shouldBe TestDeclarePropertyCeasedController.headingUkProperty
      }
      "income source is Foreign Property and FS Enabled" in {
        enable(IncomeSources)
        mockPropertyIncomeSource()
        val result: Future[Result] = TestDeclarePropertyCeasedController.show(ForeignProperty)(fakeRequestWithNinoAndOrigin("pta"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestDeclarePropertyCeasedController.titleForeignProperty
        document.select("legend:nth-child(1)").text shouldBe TestDeclarePropertyCeasedController.headingForeignProperty
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockPropertyIncomeSource()
        val result: Future[Result] = TestDeclarePropertyCeasedController.show(UkProperty)(fakeRequestWithNinoAndOrigin("pta"))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestDeclarePropertyCeasedController.show(UkProperty)(fakeRequestWithNinoAndOrigin("pta"))
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - DeclarePropertyCeasedController.submit" should {
    "return 303 SEE_OTHER" when {
      "UK Property - form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockPropertyIncomeSource()
        val incomeSourceType = UkProperty
        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "true")))

        lazy val result: Future[Result] = {
          TestDeclarePropertyCeasedController.submit(incomeSourceType)(fakeRequestCeaseUKPropertyDeclarationComplete
            .withFormUrlEncodedBody(DeclarePropertyCeasedForm.declaration -> "true", DeclarePropertyCeasedForm.ceaseCsrfToken -> "12345"))
        }
        result.futureValue.session(fakeRequestCeaseUKPropertyDeclarationComplete).get(ceaseUKPropertyDeclare) shouldBe Some("true")
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(None, incomeSourceType).url)
      }
      "Foreign Property - form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockPropertyIncomeSource()
        val incomeSourceType = ForeignProperty
        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "true")))

        lazy val result: Future[Result] = {
          TestDeclarePropertyCeasedController.submit(incomeSourceType)(fakeRequestCeaseForeignPropertyDeclarationComplete
            .withFormUrlEncodedBody(DeclarePropertyCeasedForm.declaration -> "true", DeclarePropertyCeasedForm.ceaseCsrfToken -> "12345"))
        }
        result.futureValue.session(fakeRequestCeaseForeignPropertyDeclarationComplete).get(ceaseForeignPropertyDeclare) shouldBe Some("true")
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(None, incomeSourceType).url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "UK Property - the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        disable(NavBarFs)
        mockPropertyIncomeSource()
        val incomeSourceType = UkProperty
        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "invalid")))

        val incompleteForm = DeclarePropertyCeasedForm.form(incomeSourceType).withError(FormError(DeclarePropertyCeasedForm.declaration, "incomeSources.cease.UK.property.checkboxError"))
        val expectedContent: String = mockDeclarePropertyCeased(
          declarePropertyCeasedForm = incompleteForm,
          incomeSourceType = incomeSourceType,
          postAction = controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.submit(incomeSourceType),
          backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url,
          isAgent = false
        ).toString

        lazy val result: Future[Result] = {
          TestDeclarePropertyCeasedController.submit(incomeSourceType)(fakeRequestCeaseUKPropertyDeclarationIncomplete
            .withFormUrlEncodedBody(DeclarePropertyCeasedForm.declaration -> "invalid", DeclarePropertyCeasedForm.ceaseCsrfToken -> "12345"))
        }

        result.futureValue.session(fakeRequestCeaseUKPropertyDeclarationIncomplete).get(ceaseUKPropertyDeclare) shouldBe Some("false")
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe expectedContent
      }
      "Foreign Property - the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        disable(NavBarFs)
        mockPropertyIncomeSource()
        val incomeSourceType = ForeignProperty
        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "invalid")))

        val incompleteForm = DeclarePropertyCeasedForm.form(incomeSourceType).withError(FormError(DeclarePropertyCeasedForm.declaration, "incomeSources.cease.FP.property.checkboxError"))
        val expectedContent: String = mockDeclarePropertyCeased(
          declarePropertyCeasedForm = incompleteForm,
          incomeSourceType = incomeSourceType,
          postAction = controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.submit(incomeSourceType),
          backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url,
          isAgent = false
        ).toString

        lazy val result: Future[Result] = {
          TestDeclarePropertyCeasedController.submit(incomeSourceType)(fakeRequestCeaseForeignPropertyDeclarationIncomplete
            .withFormUrlEncodedBody(DeclarePropertyCeasedForm.declaration -> "invalid", DeclarePropertyCeasedForm.ceaseCsrfToken -> "12345"))
        }

        result.futureValue.session(fakeRequestCeaseForeignPropertyDeclarationIncomplete).get(ceaseForeignPropertyDeclare) shouldBe Some("false")
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe expectedContent
      }
    }
  }

  "Agent - DeclarePropertyCeasedController.show" should {
    "return 200 OK" when {
      "income source is UK Property and FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockPropertyIncomeSource()
        val result: Future[Result] = TestDeclarePropertyCeasedController.showAgent(UkProperty)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestDeclarePropertyCeasedController.titleAgentUkProperty
        document.select("legend:nth-child(1)").text shouldBe TestDeclarePropertyCeasedController.headingUkProperty
      }
      "income source is Foreign Property and FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockPropertyIncomeSource()
        val result: Future[Result] = TestDeclarePropertyCeasedController.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestDeclarePropertyCeasedController.titleAgentForeignProperty
        document.select("legend:nth-child(1)").text shouldBe TestDeclarePropertyCeasedController.headingForeignProperty
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockPropertyIncomeSource()
        val result: Future[Result] = TestDeclarePropertyCeasedController.showAgent(UkProperty)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestDeclarePropertyCeasedController.showAgent(UkProperty)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - DeclarePropertyCeasedController.submit" should {
    "return 303 SEE_OTHER" when {
      "UK Property - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockPropertyIncomeSource()
        val incomeSourceType = UkProperty
        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "true")))

        lazy val result: Future[Result] = {
          TestDeclarePropertyCeasedController.submitAgent(incomeSourceType)(fakeRequestCeaseUKPropertyDeclarationCompleteAgent
            .withFormUrlEncodedBody(DeclarePropertyCeasedForm.declaration -> "true", DeclarePropertyCeasedForm.ceaseCsrfToken -> "12345"))
        }
        result.futureValue.session(fakeRequestCeaseUKPropertyDeclarationCompleteAgent).get(ceaseUKPropertyDeclare) shouldBe Some("true")
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showAgent(None, incomeSourceType).url)
      }
      "Foreign Property - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockPropertyIncomeSource()
        val incomeSourceType = ForeignProperty
        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "true")))

        lazy val result: Future[Result] = {
          TestDeclarePropertyCeasedController.submitAgent(incomeSourceType)(fakeRequestCeaseForeignPropertyDeclarationCompleteAgent
            .withFormUrlEncodedBody(DeclarePropertyCeasedForm.declaration -> "true", DeclarePropertyCeasedForm.ceaseCsrfToken -> "12345"))
        }
        result.futureValue.session(fakeRequestCeaseForeignPropertyDeclarationCompleteAgent).get(ceaseForeignPropertyDeclare) shouldBe Some("true")
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showAgent(None, incomeSourceType).url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "UK Property - the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockPropertyIncomeSource()
        val incomeSourceType = UkProperty
        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "invalid")))

        val incompleteForm = DeclarePropertyCeasedForm.form(incomeSourceType).withError(FormError(DeclarePropertyCeasedForm.declaration, "incomeSources.cease.UK.property.checkboxError"))
        val expectedContent: String = mockDeclarePropertyCeased(
          declarePropertyCeasedForm = incompleteForm,
          incomeSourceType = incomeSourceType,
          postAction = controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.submitAgent(incomeSourceType),
          backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url,
          isAgent = true
        ).toString

        lazy val result: Future[Result] = {
          TestDeclarePropertyCeasedController.submitAgent(incomeSourceType)(fakeRequestCeaseUKPropertyDeclarationIncompleteAgent
            .withFormUrlEncodedBody(DeclarePropertyCeasedForm.declaration -> "invalid", DeclarePropertyCeasedForm.ceaseCsrfToken -> "12345"))
        }

        result.futureValue.session(fakeRequestCeaseUKPropertyDeclarationIncompleteAgent).get(ceaseUKPropertyDeclare) shouldBe Some("false")
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe expectedContent
      }
      "Foreign Property - the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockPropertyIncomeSource()
        val incomeSourceType = ForeignProperty
        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "invalid")))

        val incompleteForm = DeclarePropertyCeasedForm.form(incomeSourceType).withError(FormError(DeclarePropertyCeasedForm.declaration, "incomeSources.cease.FP.property.checkboxError"))
        val expectedContent: String = mockDeclarePropertyCeased(
          declarePropertyCeasedForm = incompleteForm,
          incomeSourceType = incomeSourceType,
          postAction = controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.submitAgent(incomeSourceType),
          backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url,
          isAgent = true
        ).toString

        val result: Future[Result] = {
          TestDeclarePropertyCeasedController.submitAgent(incomeSourceType)(fakeRequestCeaseForeignPropertyDeclarationIncompleteAgent
            .withFormUrlEncodedBody(DeclarePropertyCeasedForm.declaration -> "invalid", DeclarePropertyCeasedForm.ceaseCsrfToken -> "12345"))
        }

        result.futureValue.session(fakeRequestCeaseForeignPropertyDeclarationIncompleteAgent).get(ceaseForeignPropertyDeclare) shouldBe Some("false")
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe expectedContent
      }
    }
  }
}
