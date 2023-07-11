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
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys.addForeignPropertyAccountingMethod
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
import views.html.incomeSources.add.ForeignPropertyAccountingMethod

import scala.concurrent.Future

class ForeignPropertyAccountingMethodControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockForeignPropertyAccountingMethod: ForeignPropertyAccountingMethod = app.injector.instanceOf[ForeignPropertyAccountingMethod]

  object TestForeignPropertyAccountingMethodController extends ForeignPropertyAccountingMethodController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[CustomNotFoundError],
    app.injector.instanceOf[ForeignPropertyAccountingMethod])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]){

    val heading: String = messages("incomeSources.add.foreignPropertyAccountingMethod.heading")
    val headingAgent: String = messages("incomeSources.add.foreignPropertyAccountingMethod.heading")
    val title: String = s"${messages("htmlTitle", heading)}"
    val titleAgent: String = s"${messages("htmlTitle.agent", headingAgent)}"
  }

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  "Individual - ForeignPropertyAccountingMethodController.show()" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource

        val result: Future[Result] = TestForeignPropertyAccountingMethodController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyAccountingMethodController.title
        document.select("legend").text shouldBe TestForeignPropertyAccountingMethodController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to home page" when {
      "navigating to the page with FS disabled" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        mockForeignPropertyIncomeSource()

        val result: Future[Result] = TestForeignPropertyAccountingMethodController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()

        val result: Future[Result] = TestForeignPropertyAccountingMethodController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - ForeignPropertyAccountingMethodController.submit()" should{
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.CheckForeignPropertyDetailsController.show().url}" when {
      "form is completed successfully with cash radio button selected" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()


        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestForeignPropertyAccountingMethodController.submit()(fakeRequestNoSession
            .withFormUrlEncodedBody("incomeSources.add.foreignPropertyAccountingMethod" -> "cash"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addForeignPropertyAccountingMethod) shouldBe Some("cash")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckForeignPropertyDetailsController.show().url)
      }
      "form is completed successfully with traditional radio button selected" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()


        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestForeignPropertyAccountingMethodController.submit()(fakeRequestNoSession
            .withFormUrlEncodedBody("incomeSources.add.foreignPropertyAccountingMethod" -> "traditional"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addForeignPropertyAccountingMethod) shouldBe Some("accruals")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckForeignPropertyDetailsController.show().url)
      }
    }

    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestForeignPropertyAccountingMethodController.submit()(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("incomeSources.add.foreignPropertyAccountingMethod" -> ""))
        }

        status(result) shouldBe Status.BAD_REQUEST
        result.futureValue.session.get(addForeignPropertyAccountingMethod) shouldBe None
      }
    }
  }

  "Agent - ForeignPropertyAccountingMethodController.showAgent()" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()

        val result: Future[Result] = TestForeignPropertyAccountingMethodController.showAgent()(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyAccountingMethodController.titleAgent
        document.select("legend").text shouldBe TestForeignPropertyAccountingMethodController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to home page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disableAllSwitches()
        mockForeignPropertyIncomeSource()

        val result: Future[Result] = TestForeignPropertyAccountingMethodController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestForeignPropertyAccountingMethodController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - BusinessAccountingMethodController.submit()" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.CheckForeignPropertyDetailsController.showAgent().url}" when {
      "form is completed successfully with cash radio button selected" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSourceWithCashAndAccruals()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestForeignPropertyAccountingMethodController.submitAgent()(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("incomeSources.add.foreignPropertyAccountingMethod" -> "cash"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addForeignPropertyAccountingMethod) shouldBe Some("cash")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckForeignPropertyDetailsController.showAgent().url)
      }
      "form is completed successfully with traditional radio button selected" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSourceWithCashAndAccruals()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = {
          TestForeignPropertyAccountingMethodController.submitAgent()(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("incomeSources.add.foreignPropertyAccountingMethod" -> "traditional"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addForeignPropertyAccountingMethod) shouldBe Some("accruals")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.CheckForeignPropertyDetailsController.showAgent().url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSourceWithCashAndAccruals()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestForeignPropertyAccountingMethodController.submitAgent()(fakeRequestConfirmedClient().withMethod("POST")
            .withFormUrlEncodedBody("incomeSources.add.foreignPropertyAccountingMethod" -> ""))
        }

        status(result) shouldBe Status.BAD_REQUEST
        result.futureValue.session.get(addForeignPropertyAccountingMethod) shouldBe None
      }
    }
  }


}


