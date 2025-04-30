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

import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import enums.MTDIndividual
import forms.manageBusinesses.add.AddProprertyForm
import forms.manageBusinesses.add.AddProprertyForm._
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSourcesNewJourney
import models.core.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.SessionService

class AddPropertyControllerSpec extends MockAuthActions with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[AddPropertyController]


  def getRequest(isAgent: Boolean): FakeRequest[AnyContentAsEmpty.type] = {
    if (isAgent) fakeRequestConfirmedClient()
    else fakeRequestWithActiveSession
  }

  def postRequest(isAgent: Boolean): FakeRequest[AnyContentAsEmpty.type] = {
    if (isAgent) fakePostRequestConfirmedClient()
    else fakePostRequestWithActiveSession
  }

  def getValidationErrorTabTitle(): String = {
    s"${messages("htmlTitle.invalidInput", messages("manageBusinesses.type-of-property.heading"))}"
  }

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    s"show${if (mtdRole != MTDIndividual) "Agent"}" when {
      val action = testController.show(isAgent)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        "display the add property page" when {
          "IncomeSources FS is enabled" in {
            enable(IncomeSourcesNewJourney)
            setupMockSuccess(mtdRole)
            mockNoIncomeSources()
            val result = action(fakeRequest)

            val document: Document = Jsoup.parse(contentAsString(result))
            document.title should include(messages("manageBusinesses.type-of-property.heading"))
            val backUrl = if(isAgent) controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url else controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            document.getElementById("back-fallback").attr("href") shouldBe backUrl
            status(result) shouldBe OK
          }
        }

        "redirect to the home page" when {
          "IncomeSources FS is disabled" in {
            disable(IncomeSourcesNewJourney)
            setupMockSuccess(mtdRole)

            mockNoIncomeSources()

            val result = action(fakeRequest)

            status(result) shouldBe SEE_OTHER
            val redirectUrl = if (isAgent) controllers.routes.HomeController.showAgent().url else controllers.routes.HomeController.show().url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }

    s"submit($isAgent)" when {
      val action = testController.submit(isAgent)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
      s"the user is authenticated as a $mtdRole" should {
        s"return ${Status.SEE_OTHER}: redirect to the correct Add Start Date Page" when {
          "foreign property selected" in {
            enable(IncomeSourcesNewJourney)
            setupMockSuccess(mtdRole)

            mockNoIncomeSources()

            val result = action(fakeRequest.withFormUrlEncodedBody(
              AddProprertyForm.response -> responseUK
            ))

            status(result) shouldBe SEE_OTHER
            val redirectUrl = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent, mode = NormalMode, UkProperty).url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
          "uk property selected" in {
            enable(IncomeSourcesNewJourney)
            setupMockSuccess(mtdRole)

            mockNoIncomeSources()

            val result = action(fakeRequest.withFormUrlEncodedBody(
              AddProprertyForm.response -> responseForeign
            ))

            status(result) shouldBe SEE_OTHER
            val redirectUrl = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent, mode = NormalMode, ForeignProperty).url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }

        s"return ${Status.BAD_REQUEST}" when {
          "an invalid form is submitted" in {
            enable(IncomeSourcesNewJourney)
            setupMockSuccess(mtdRole)

            mockNoIncomeSources()

            val result = action(fakeRequest.withFormUrlEncodedBody("INVALID" -> "INVALID"))

            status(result) shouldBe BAD_REQUEST

            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe getValidationErrorTabTitle()
          }
          "an empty form is submitted" in {
            enable(IncomeSourcesNewJourney)
            setupMockSuccess(mtdRole)

            mockNoIncomeSources()

            val result = action(fakeRequest.withFormUrlEncodedBody("" -> ""))

            status(result) shouldBe BAD_REQUEST

            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe getValidationErrorTabTitle()
          }
          "no form is submitted" in {
            enable(IncomeSourcesNewJourney)
            setupMockSuccess(mtdRole)

            mockNoIncomeSources()

            val result = action(fakeRequest)

            status(result) shouldBe BAD_REQUEST
          }
        }

        "redirect to the home page" when {
          "IncomeSources FS is disabled" in {
            disable(IncomeSourcesNewJourney)
            setupMockSuccess(mtdRole)

            mockNoIncomeSources()

            val result = action(fakeRequest)

            status(result) shouldBe SEE_OTHER
            val redirectUrl = if (isAgent) controllers.routes.HomeController.showAgent().url else controllers.routes.HomeController.show().url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }
  }
}