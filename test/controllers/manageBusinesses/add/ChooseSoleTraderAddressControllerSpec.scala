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

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.MTDIndividual
import forms.manageBusinesses.add.ChooseSoleTraderAddressForm
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.OverseasBusinessAddress
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{DateServiceInterface, SessionService}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessIncome2018and2019AndProp, businessInternational}

class ChooseSoleTraderAddressControllerSpec extends MockAuthActions with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController = app.injector.instanceOf[ChooseSoleTraderAddressController]


  def getRequest(isAgent: Boolean): FakeRequest[AnyContentAsEmpty.type] = {
    if (isAgent) fakeRequestConfirmedClient()
    else fakeRequestWithActiveSession
  }

  def postRequest(isAgent: Boolean): FakeRequest[AnyContentAsEmpty.type] = {
    if (isAgent) fakePostRequestConfirmedClient()
    else fakePostRequestWithActiveSession
  }

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    s"show${if (mtdRole != MTDIndividual) "Agent"}" when {
      val action = testController.show(isAgent)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        "display the ChooseSoleTraderAddress page when OverseasBusinessAddress FS is enabled" when {
          "user has active UK business addresses" in {
            enable(OverseasBusinessAddress)
            setupMockSuccess(mtdRole)
            mockItsaStatusRetrievalAction(businessIncome2018and2019AndProp)
            mockNoIncomeSources()
            val result = action(fakeRequest)

            val document: Document = Jsoup.parse(contentAsString(result))
            document.title should include(messages("manageBusinesses.add.chooseSoleTraderAddress.heading"))
            val backUrl = if(isAgent) controllers.routes.HomeController.showAgent().url else controllers.routes.HomeController.show().url
            document.getElementById("back-fallback").attr("href") shouldBe backUrl
            status(result) shouldBe OK
          }
          "user has an active international business address" in {
            enable(OverseasBusinessAddress)
            setupMockSuccess(mtdRole)
            mockItsaStatusRetrievalAction(businessInternational)
            mockNoIncomeSources()
            val result = action(fakeRequest)

            val document: Document = Jsoup.parse(contentAsString(result))
            document.title should include(messages("manageBusinesses.add.chooseSoleTraderAddress.heading"))
            val backUrl = if (isAgent) controllers.routes.HomeController.showAgent().url else controllers.routes.HomeController.show().url
            document.getElementById("back-fallback").attr("href") shouldBe backUrl
            status(result) shouldBe OK
          }
        }
        "redirect to the homepage" when {
          "OverseasBusinessAddress FS is disabled" in {
            disable(OverseasBusinessAddress)
            setupMockSuccess(mtdRole)
            mockItsaStatusRetrievalAction(businessIncome2018and2019AndProp)
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
      val actionTrigMig = testController.submit(isAgent)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
      s"the user is authenticated as a $mtdRole" should {
        //TODO change as part of the nav ticket
        s"return ${Status.SEE_OTHER}: reload the page" when {
          "existing address selected" in {
            enable(OverseasBusinessAddress)
            setupMockSuccess(mtdRole)
            mockItsaStatusRetrievalAction(businessIncome2018and2019AndProp)

            mockNoIncomeSources()

            val result = action(fakeRequest.withFormUrlEncodedBody(
              "0" -> "some fake pre-existing business address, some post code"
            ))

            status(result) shouldBe SEE_OTHER
            val redirectUrl = controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.show(isAgent).url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
          "new address selected" in {
            enable(OverseasBusinessAddress)
            setupMockSuccess(mtdRole)
            mockItsaStatusRetrievalAction(businessIncome2018and2019AndProp)

            mockNoIncomeSources()

            val result = action(fakeRequest.withFormUrlEncodedBody(
              "new-address" -> "User wants to add a New address"
            ))

            status(result) shouldBe SEE_OTHER
            val redirectUrl = controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.show(isAgent).url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }
  }
}