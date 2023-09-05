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

import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import views.html.errorPages.templates.ErrorTemplateWithLink

class IncomeSourceNotCeasedControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate {

  object TestIncomeSourceNotCeasedController extends IncomeSourceNotCeasedController(MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[ErrorTemplateWithLink])(appConfig, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    app.injector.instanceOf[MessagesControllerComponents],
    ec)

  "IncomeSourceNotCeasedController.show" should {
    "200 OK" when {
      "authenticated user navigates to page with a UK Property income source type in the request" in {
        disableAllSwitches()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        mockUKPropertyIncomeSource()
        val result = TestIncomeSourceNotCeasedController.show(isAgent = false, UkProperty.key)(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe messages("htmlTitle.errorPage", messages("standardError.heading"))
      }
      "authenticated agent navigates to page with a UK Property income source type in the request" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockUKPropertyIncomeSource()
        val result = TestIncomeSourceNotCeasedController.show(isAgent = true, UkProperty.key)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe messages("htmlTitle.errorPage", messages("standardError.heading"))
      }
      "authenticated user navigates to page with a Foreign Property income source type in the request" in {
        disableAllSwitches()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        mockUKPropertyIncomeSource()
        val result = TestIncomeSourceNotCeasedController.show(isAgent = false, ForeignProperty.key)(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe messages("htmlTitle.errorPage", messages("standardError.heading"))
      }
      "authenticated agent navigates to page with a Foreign Property income source type in the request" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockUKPropertyIncomeSource()
        val result = TestIncomeSourceNotCeasedController.show(isAgent = true, ForeignProperty.key)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe messages("htmlTitle.errorPage", messages("standardError.heading"))
      }
      "authenticated user navigates to page with a Self-employment business income source type in the request" in {
        disableAllSwitches()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        mockUKPropertyIncomeSource()
        val result = TestIncomeSourceNotCeasedController.show(isAgent = false, SelfEmployment.key)(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe messages("htmlTitle.errorPage", messages("standardError.heading"))
      }
      "authenticated agent navigates to page with a Self-employment business income source type in the request" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockUKPropertyIncomeSource()
        val result = TestIncomeSourceNotCeasedController.show(isAgent = true, SelfEmployment.key)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe messages("htmlTitle.errorPage", messages("standardError.heading"))
      }
    }
    "500 ISE" when {
      "authenticated user navigates to page with an invalid income source type in the request" in {
        disableAllSwitches()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        mockUKPropertyIncomeSource()
        val result = TestIncomeSourceNotCeasedController.show(isAgent = false, "zzz")(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        document.title shouldBe messages("htmlTitle.errorPage", messages("standardError.heading"))
      }
      "authenticated agent navigates to page with an invalid income source type in the request" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockUKPropertyIncomeSource()
        val result = TestIncomeSourceNotCeasedController.show(isAgent = true, "zzz")(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        document.title shouldBe messages("htmlTitle.errorPage", messages("standardError.heading"))
      }
    }
    "303 SEE_OTHER" when {
      "unauthenticated user navigates to page" in {
        disableAllSwitches()
        setupMockAuthorisationException()
        mockUKPropertyIncomeSource()
        val result = TestIncomeSourceNotCeasedController.show(isAgent = false, UkProperty.key)(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
      }
      "unauthenticated agent navigates to page" in {
        disableAllSwitches()
        setupMockAgentAuthorisationException()
        mockUKPropertyIncomeSource()
        val result = TestIncomeSourceNotCeasedController.show(isAgent = true, UkProperty.key)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
      }

    }
  }
}
