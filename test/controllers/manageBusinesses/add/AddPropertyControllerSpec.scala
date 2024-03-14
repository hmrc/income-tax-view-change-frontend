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

package controllers.manageBusinesses.add

import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import forms.manageBusinesses.add.AddProprertyForm
import forms.manageBusinesses.add.AddProprertyForm._
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents}
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testUtils.TestSupport
import views.html.manageBusinesses.add.AddProperty

class AddPropertyControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching with MockSessionService {

  object TestAddPropertyController extends AddPropertyController(
    testAuthenticator,
    app.injector.instanceOf[AddProperty],
    mockAuthService,
  )(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]
  )

  def getRequest(isAgent: Boolean): FakeRequest[AnyContentAsEmpty.type] = {
    if (isAgent) fakeRequestConfirmedClient()
    else fakeRequestWithActiveSession
  }

  def postRequest(isAgent: Boolean): FakeRequest[AnyContentAsEmpty.type] = {
    if (isAgent) fakePostRequestConfirmedClient()
    else fakePostRequestWithActiveSession
  }

  for (isAgent <- Seq(true, false)) yield {
    s"AddPropertyController.show: isAgent = $isAgent" should {
      "redirect to the appropriate page" when {
        "IncomeSources FS is disabled" in {
          disableAllSwitches()
          disable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthorisationSuccess(isAgent)

          val result = TestAddPropertyController.show(isAgent = isAgent)(getRequest(isAgent))

          status(result) shouldBe SEE_OTHER
          val redirectUrl = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
        "User is not authorised" in {
          if (isAgent) setupMockAgentAuthorisationException() else setupMockAuthorisationException()
          val result = TestAddPropertyController.show(isAgent)(getRequest(isAgent))
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
      "display the add property page" when {
        "IncomeSources FS is enabled" in {
          disableAllSwitches()
          enable(IncomeSources)
          mockNoIncomeSources()
          setupMockAuthorisationSuccess(isAgent)
          val result = TestAddPropertyController.show(isAgent = isAgent)(getRequest(isAgent))

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title should include(messages("manageBusinesses.type-of-property.heading"))
          val backUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent).url
          document.getElementById("back-fallback").attr("href") shouldBe backUrl
          status(result) shouldBe OK
        }
      }
    }

    s"AddPropertyController.submit: isAgent = $isAgent" should {
      "redirect to the home page" when {
        "IncomeSources FS is disabled" in {
          disableAllSwitches()
          disable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthorisationSuccess(isAgent)

          val result = TestAddPropertyController.submit(isAgent = isAgent)(postRequest(isAgent))

          status(result) shouldBe SEE_OTHER
          val redirectUrl = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
      }
      s"return ${Status.BAD_REQUEST}" when {
        "an invalid form is submitted" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthorisationSuccess(isAgent)

          val result = TestAddPropertyController.submit(isAgent = isAgent)(postRequest(isAgent).withFormUrlEncodedBody("INVALID" -> "INVALID"))

          status(result) shouldBe BAD_REQUEST
        }
        "an empty form is submitted" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthorisationSuccess(isAgent)

          val result = TestAddPropertyController.submit(isAgent = isAgent)(postRequest(isAgent).withFormUrlEncodedBody("" -> ""))

          status(result) shouldBe BAD_REQUEST
        }
        "no form is submitted" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthorisationSuccess(isAgent)

          val result = TestAddPropertyController.submit(isAgent = isAgent)(postRequest(isAgent))

          status(result) shouldBe BAD_REQUEST
        }
      }
      s"return ${Status.SEE_OTHER}: redirect to the correct Add Start Date Page" when {
        "foreign property selected" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthorisationSuccess(isAgent)

          val result = TestAddPropertyController.submit(isAgent = isAgent)(postRequest(isAgent).withFormUrlEncodedBody(
            AddProprertyForm.response -> responseUK
          ))

          status(result) shouldBe SEE_OTHER
          val redirectUrl = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent, isChange = false, UkProperty).url
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
        "uk property selected" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthorisationSuccess(isAgent)

          val result = TestAddPropertyController.submit(isAgent = isAgent)(postRequest(isAgent).withFormUrlEncodedBody(
            AddProprertyForm.response -> responseForeign
          ))

          status(result) shouldBe SEE_OTHER
          val redirectUrl = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent, isChange = false, ForeignProperty).url
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
      }
    }
  }
}