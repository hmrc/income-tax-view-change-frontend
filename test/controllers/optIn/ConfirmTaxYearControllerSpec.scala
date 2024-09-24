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
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import controllers.routes
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockDateService, MockOptInService, MockOptOutService}
import models.incomeSourceDetails.TaxYear
import models.optin.ConfirmTaxYearViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optIn.ConfirmTaxYear

import scala.concurrent.Future

class ConfirmTaxYearControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockOptOutService with MockOptInService with MockDateService {

  val controller = new ConfirmTaxYearController(
    view = app.injector.instanceOf[ConfirmTaxYear],
    mockOptInService,
    authorisedFunctions = mockAuthService,
    auth = testAuthenticator,
  )(
    dateService = mockDateService,
    appConfig = appConfig,
    ec = ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents]
  )

  val endTaxYear = 2023
  val taxYear2023 = TaxYear.forYearEnd(endTaxYear)

  def showTests(isAgent: Boolean): Unit = {
    "show page" should {

      s"return result with $OK status" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        when(mockOptInService.getConfirmTaxYearViewModel(any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(ConfirmTaxYearViewModel(
            taxYear2023, routes.ReportingFrequencyPageController.show(isAgent).url,
            isAgent)
          )))

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")
        val result = controller.show(isAgent).apply(requestGET)
        status(result) shouldBe Status.OK
      }

      s"return result with $INTERNAL_SERVER_ERROR status" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        when(mockOptInService.getConfirmTaxYearViewModel(any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")
        val result = controller.show(isAgent).apply(requestGET)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  def submitTest(isAgent: Boolean): Unit = {
    val testName = "MultiYear Opt-In"
    val requestPOST = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")

    s"submit method is invoked $testName" should {

      s"return result with $OK status for $testName" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        when(mockOptInService.makeOptInCall()(any(), any(), any()))
          .thenReturn(Future.successful(ITSAStatusUpdateResponseSuccess()))

        val result: Future[Result] = controller.submit(isAgent)(requestPOST)

        status(result) shouldBe Status.SEE_OTHER
      }
    }

    s"return result with $SEE_OTHER status for $testName and update fails" in {
      setupMockAuthorisationSuccess(isAgent)
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

      when(mockOptInService.makeOptInCall()(any(), any(), any()))
        .thenReturn(Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure()))

      val result: Future[Result] = controller.submit(isAgent)(requestPOST)

      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "CheckYourAnswersController - Individual" when {
    showTests(isAgent = false)
    submitTest(isAgent = false)
  }

  "CheckYourAnswersController - Agent" when {
    showTests(isAgent = true)
    submitTest(isAgent = true)
  }
}