/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.claimToAdjustPoa

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitching
import enums.IncomeSourceJourney.SelfEmployment
import mocks.controllers.predicates.MockAuthenticationPredicate
import models.admin.AdjustPaymentsOnAccount
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{HTML, OK, contentAsString, contentType, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.claimToAdjustPoa.ApiFailureSubmittingPoaView

import scala.concurrent.{ExecutionContext, Future}

class ApiFailureSubmittingPoaControllerSpec extends MockAuthenticationPredicate with TestSupport with FeatureSwitching {

  object TestApiFailureSubmittingPoaController extends ApiFailureSubmittingPoaController(
    authorisedFunctions = mockAuthService,
    auth = testAuthenticator,
    view = app.injector.instanceOf[ApiFailureSubmittingPoaView],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler]
  )(appConfig = app.injector.instanceOf[FrontendAppConfig],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  val firstParagraphView = "Your payments on account could not be updated."

  def setupTests(isAgent: Boolean): Unit = {
    if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
    mockBusinessIncomeSource()
  }

  "Individual - ApiFailureSubmittingPoaController.show" should {
    s"return status: $OK" when {
      "called when the AdjustPaymentsOnAccount FS is on" in {
        enable(AdjustPaymentsOnAccount)
        setupTests(isAgent = false)

        val result = TestApiFailureSubmittingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
        document.getElementById("paragraph-text-1").text() shouldBe firstParagraphView
      }
    }
    s"return status: $SEE_OTHER" when {
      "called when the AdjustPaymentsOnAccount FS is off" in {
        disable(AdjustPaymentsOnAccount)
        setupTests(isAgent = false)

        val result = TestApiFailureSubmittingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestApiFailureSubmittingPoaController.show(isAgent = false)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - ApiFailureSubmittingPoaController.show" should {
    s"return status: $OK" when {
      "called when the AdjustPaymentsOnAccount FS is on" in {
        enable(AdjustPaymentsOnAccount)
        setupTests(isAgent = true)

        val result = TestApiFailureSubmittingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
        document.getElementById("paragraph-text-1").text() shouldBe firstParagraphView
      }
    }
    s"return status: $SEE_OTHER" when {
      "called when the AdjustPaymentsOnAccount FS is off" in {
        disable(AdjustPaymentsOnAccount)
        setupTests(isAgent = true)

        val result = TestApiFailureSubmittingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestApiFailureSubmittingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
}
