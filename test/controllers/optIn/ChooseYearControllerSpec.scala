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

package controllers.optIn

import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import forms.optIn.ChooseTaxYearForm
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockOptInService, MockOptOutService}
import models.incomeSourceDetails.TaxYear
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optIn.ChooseTaxYearView

class ChooseYearControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockOptOutService with MockOptInService {

  val controller = new ChooseYearController(
    mockOptInService,
    view = app.injector.instanceOf[ChooseTaxYearView], authorisedFunctions = mockAuthService, auth = testAuthenticator,
  )(
    appConfig = appConfig,
    ec = ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents]
  )

  val endTaxYear = 2023
  val taxYear2023 = TaxYear.forYearEnd(endTaxYear)

  def tests(isAgent: Boolean): Unit = {
    "show page" should {
      s"return result with $OK status" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockAvailableOptInTaxYear(List(taxYear2023, taxYear2023.nextYear))
        mockFetchSavedChosenTaxYear(Some(taxYear2023))

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")
        val result = controller.show(isAgent).apply(requestGET)
        status(result) shouldBe Status.OK
      }
    }

    "submit page" when {

      val requestPOST = if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithNinoAndOrigin("PTA")
      val requestPOSTWithChoice = requestPOST.withFormUrlEncodedBody(
        ChooseTaxYearForm.choiceField -> taxYear2023.toString
      )

      "with selection made" should {
        s"return result with $SEE_OTHER status" in {

          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockAvailableOptInTaxYear(List(taxYear2023, taxYear2023.nextYear))
          mockSaveIntent(taxYear2023)

          val result = controller.submit(isAgent).apply(requestPOSTWithChoice)
          status(result) shouldBe Status.SEE_OTHER
        }
      }

      "without selection made" should {
        s"return result with $BAD_REQUEST status" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockAvailableOptInTaxYear(List(taxYear2023, taxYear2023.nextYear))
          mockSaveIntent(taxYear2023)
          val requestPOST = if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithNinoAndOrigin("PTA")
          val requestPOSTWithChoice = requestPOST.withFormUrlEncodedBody(
            ChooseTaxYearForm.choiceField -> ""
          )

          val result = controller.submit(isAgent).apply(requestPOSTWithChoice)
          status(result) shouldBe Status.BAD_REQUEST
        }
      }

      "failed save intent" should {
        s"return result with $INTERNAL_SERVER_ERROR status" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockAvailableOptInTaxYear(List(taxYear2023, taxYear2023.nextYear))
          mockSaveIntent(taxYear2023, isSuccessful = false)

          val result = controller.submit(isAgent).apply(requestPOSTWithChoice)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  "OptInChooseYearController - Individual" when {
    tests(isAgent = false)
  }

  "OptInChooseYearController - Agent" when {
    tests(isAgent = true)
  }

}