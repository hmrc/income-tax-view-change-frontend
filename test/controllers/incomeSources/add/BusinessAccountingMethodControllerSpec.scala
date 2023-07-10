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

import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys.addBusinessAccountingMethod
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, contentType, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.BusinessAccountingMethod

import scala.concurrent.Future

class BusinessAccountingMethodControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockBusinessAccountingMethod: BusinessAccountingMethod = app.injector.instanceOf[BusinessAccountingMethod]

  object TestBusinessAccountingMethodController extends BusinessAccountingMethodController (
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[BusinessAccountingMethod],
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val heading: String = messages("incomeSources.add.business-accounting-method.heading")
    val headingAgent: String = messages("incomeSources.add.business-accounting-method.heading")
    val title: String = s"${messages("htmlTitle", heading)}"
    val titleAgent: String = s"${messages("htmlTitle.agent", headingAgent)}"
  }

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  disableAllSwitches()

  "Individual - BusinessAccountingMethodController.show()" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled and no active self employment businesses" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()

        val result: Future[Result] = TestBusinessAccountingMethodController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessAccountingMethodController.title
        document.select("legend").text shouldBe TestBusinessAccountingMethodController.heading
      }
    }
    "return 303 SEE_OTHER" when {
      "navigating to the page with FS Enabled and one self-employment businesses, with the cashOrAccruals field set to the string accruals" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSourceWithAccruals()

        val result: Future[Result] = TestBusinessAccountingMethodController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addBusinessAccountingMethod) shouldBe Some("accruals")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url)
      }
      "navigating to the page with FS Enabled and one self-employment businesses, with the cashOrAccruals field set to the string cash" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = TestBusinessAccountingMethodController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addBusinessAccountingMethod) shouldBe Some("cash")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url)
      }
      "navigating to the page with FS Enabled and two SE businesses, one cash, one accruals (should be impossible, but in this case, we use head of list)" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSourceWithCashAndAccruals()

        val result: Future[Result] = TestBusinessAccountingMethodController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addBusinessAccountingMethod) shouldBe Some("cash")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url)
      }
    }
    "return 500 INTERNAL_SERVER_ERROR" when {
      "navigating to the page with FS Enabled and a user with a SE business missing its cashOrAccruals field" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSourceMissingCashOrAccrualsField()

        val result: Future[Result] = TestBusinessAccountingMethodController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some("text/html")
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = TestBusinessAccountingMethodController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestBusinessAccountingMethodController.customNotFoundErrorView().toString()

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestBusinessAccountingMethodController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
  "Individual - BusinessAccountingMethodController.submit()" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url}" when {
      "form is completed successfully with cash radio button selected" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestBusinessAccountingMethodController.submit()(fakeRequestNoSession
            .withFormUrlEncodedBody("incomeSources.add.business-accounting-method" -> "cash"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addBusinessAccountingMethod) shouldBe Some("cash")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url)
      }
      "form is completed successfully with traditional radio button selected" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestBusinessAccountingMethodController.submit()(fakeRequestNoSession
            .withFormUrlEncodedBody("incomeSources.add.business-accounting-method" -> "traditional"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addBusinessAccountingMethod) shouldBe Some("accruals")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestBusinessAccountingMethodController.submit()(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("incomeSources.add.business-accounting-method" -> ""))
        }
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.BAD_REQUEST
        document.title shouldBe TestBusinessAccountingMethodController.title
        result.futureValue.session.get(addBusinessAccountingMethod) shouldBe None
      }
    }
  }

  "Agent - BusinessAccountingMethodController.showAgent()" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled and no active self employment businesses" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()

        val result: Future[Result] = TestBusinessAccountingMethodController.showAgent()(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessAccountingMethodController.titleAgent
        document.select("legend:nth-child(1)").text shouldBe TestBusinessAccountingMethodController.heading
      }
    }
    "return 303 SEE_OTHER" when {
      "navigating to the page with FS Enabled and client has one self-employment businesses, with the cashOrAccruals field set to the string accruals" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSourceWithAccruals()

        val result: Future[Result] = TestBusinessAccountingMethodController.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addBusinessAccountingMethod) shouldBe Some("accruals")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url)
      }
      "navigating to the page with FS Enabled and client has one self-employment businesses, with the cashOrAccruals field set to the string cash" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = TestBusinessAccountingMethodController.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addBusinessAccountingMethod) shouldBe Some("cash")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url)
      }
      "navigating to the page with FS Enabled and two SE businesses, one cash, one accruals (should be impossible, but in this case, we use head of list)" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disableAllSwitches()
        enable(IncomeSources)
        mockBusinessIncomeSourceWithCashAndAccruals()

        val result: Future[Result] = TestBusinessAccountingMethodController.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addBusinessAccountingMethod) shouldBe Some("cash")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url)
      }
    }
    "return 500 INTERNAL_SERVER_ERROR" when {
      "navigating to the page with FS Enabled and a user with a SE business missing its cashOrAccruals field" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSourceMissingCashOrAccrualsField()

        val result: Future[Result] = TestBusinessAccountingMethodController.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some("text/html")
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = TestBusinessAccountingMethodController.showAgent()(fakeRequestConfirmedClient())
        val expectedContent: String = TestBusinessAccountingMethodController.customNotFoundErrorView().toString()

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestBusinessAccountingMethodController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
  "Agent - BusinessAccountingMethodController.submit()" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url}" when {
      "form is completed successfully with cash radio button selected" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestBusinessAccountingMethodController.submitAgent()(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("incomeSources.add.business-accounting-method" -> "cash"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addBusinessAccountingMethod) shouldBe Some("cash")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url)
      }
      "form is completed successfully with traditional radio button selected" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestBusinessAccountingMethodController.submitAgent()(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("incomeSources.add.business-accounting-method" -> "traditional"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addBusinessAccountingMethod) shouldBe Some("accruals")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestBusinessAccountingMethodController.submitAgent()(fakeRequestConfirmedClient().withMethod("POST")
            .withFormUrlEncodedBody("incomeSources.add.business-accounting-method" -> ""))
        }
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.BAD_REQUEST
        document.title shouldBe TestBusinessAccountingMethodController.titleAgent
        result.futureValue.session.get(addBusinessAccountingMethod) shouldBe None
      }
    }
  }
}
