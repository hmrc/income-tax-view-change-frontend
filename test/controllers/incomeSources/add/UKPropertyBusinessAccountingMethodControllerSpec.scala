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
import forms.utils.SessionKeys.addUkPropertyAccountingMethod
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
import views.html.incomeSources.add.UKPropertyBusinessAccountingMethod

import scala.concurrent.Future

class UKPropertyAccountingMethodControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockUKPropertyBusinessAccountingMethod: UKPropertyBusinessAccountingMethod = app.injector.instanceOf[UKPropertyBusinessAccountingMethod]

  object TestUKPropertyAccountingMethodController extends UKPropertyAccountingMethodController (
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[UKPropertyBusinessAccountingMethod],
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val heading: String = messages("incomeSources.add.uk-property-business-accounting-method.heading")
    val headingAgent: String = messages("incomeSources.add.uk-property-business-accounting-method.heading")
    val title: String = s"${messages("htmlTitle", heading)}"
    val titleAgent: String = s"${messages("htmlTitle.agent", headingAgent)}"
  }

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  "Individual - UKPropertyAccountingMethodController.show()" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled and no active UK Property" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()

        val result: Future[Result] = TestUKPropertyAccountingMethodController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestUKPropertyAccountingMethodController.title
        document.select("legend").text shouldBe TestUKPropertyAccountingMethodController.heading
      }
    }
    "return 200 OK" when {
      "navigating to the page with FS Enabled and has only Foreign Property and no UK property" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        val result: Future[Result] = TestUKPropertyAccountingMethodController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestUKPropertyAccountingMethodController.title
        document.select("legend").text shouldBe TestUKPropertyAccountingMethodController.heading
      }
    }
    "return 303 SEE_OTHER" when {
      "navigating to the page with FS Enabled and has UK Property" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        val result: Future[Result] = TestUKPropertyAccountingMethodController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addUkPropertyAccountingMethod) shouldBe Some("CASH")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.show().url)
      }
      "navigating to the page with FS Enabled and has both UK and Foreign Property" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBothPropertyBothBusiness()

        val result: Future[Result] = TestUKPropertyAccountingMethodController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addUkPropertyAccountingMethod) shouldBe Some("CASH")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.show().url)
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = TestUKPropertyAccountingMethodController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestUKPropertyAccountingMethodController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestUKPropertyAccountingMethodController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
  "Individual - UKPropertyAccountingMethodController.submit()" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.show().url}" when {
      "form is completed successfully with cash radio button selected" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestUKPropertyAccountingMethodController.submit()(fakeRequestNoSession
            .withFormUrlEncodedBody("incomeSources.add.uk-property-business-accounting-method" -> "cash"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addUkPropertyAccountingMethod) shouldBe Some("CASH")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.show().url)
      }
      "form is completed successfully with traditional radio button selected" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestUKPropertyAccountingMethodController.submit()(fakeRequestNoSession
            .withFormUrlEncodedBody("incomeSources.add.uk-property-business-accounting-method" -> "traditional"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addUkPropertyAccountingMethod) shouldBe Some("ACCRUALS")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.show().url)
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
          TestUKPropertyAccountingMethodController.submit()(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("incomeSources.add.uk-property-business-accounting-method" -> ""))
        }

        status(result) shouldBe Status.BAD_REQUEST
        result.futureValue.session.get(addUkPropertyAccountingMethod) shouldBe None
      }
    }
  }

  "Agent - UKPropertyAccountingMethodController.showAgent()" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled and no active UK Property" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()

        val result: Future[Result] = TestUKPropertyAccountingMethodController.showAgent()(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestUKPropertyAccountingMethodController.titleAgent
        document.select("legend").text shouldBe TestUKPropertyAccountingMethodController.headingAgent
      }
    }
    "return 200 OK" when {
      "navigating to the page with FS Enabled and has only Foreign Property and no UK property" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        val result: Future[Result] = TestUKPropertyAccountingMethodController.showAgent()(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestUKPropertyAccountingMethodController.titleAgent
        document.select("legend").text shouldBe TestUKPropertyAccountingMethodController.headingAgent
      }
    }
    "return 303 SEE_OTHER" when {
      "navigating to the page with FS Enabled and has UK Property" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        val result: Future[Result] = TestUKPropertyAccountingMethodController.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addUkPropertyAccountingMethod) shouldBe Some("CASH")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent().url)
      }
      "navigating to the page with FS Enabled and has both UK and Foreign Property" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBothPropertyBothBusiness()

        val result: Future[Result] = TestUKPropertyAccountingMethodController.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addUkPropertyAccountingMethod) shouldBe Some("CASH")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent().url)
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = TestUKPropertyAccountingMethodController.showAgent()(fakeRequestConfirmedClient())
        val expectedContent: String = TestUKPropertyAccountingMethodController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestUKPropertyAccountingMethodController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
  "Agent - UKPropertyAccountingMethodController.submitAgent()" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent().url}" when {
      "form is completed successfully with cash radio button selected" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestUKPropertyAccountingMethodController.submitAgent()(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("incomeSources.add.uk-property-business-accounting-method" -> "cash"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addUkPropertyAccountingMethod) shouldBe Some("CASH")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent().url)
      }
      "form is completed successfully with traditional radio button selected" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestUKPropertyAccountingMethodController.submitAgent()(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("incomeSources.add.uk-property-business-accounting-method" -> "traditional"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addUkPropertyAccountingMethod) shouldBe Some("ACCRUALS")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent().url)
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
          TestUKPropertyAccountingMethodController.submitAgent()(fakeRequestConfirmedClient().withMethod("POST")
            .withFormUrlEncodedBody("incomeSources.add.uk-property-business-accounting-method" -> ""))
        }

        status(result) shouldBe Status.BAD_REQUEST
        result.futureValue.session.get(addUkPropertyAccountingMethod) shouldBe None
      }
    }
  }
}
