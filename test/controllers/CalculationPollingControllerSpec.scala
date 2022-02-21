/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import config.ItvcErrorHandler
import config.featureswitch.FeatureSwitching
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockCalculationPollingService
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testConstants.BaseTestConstants.testTaxYear
import testUtils.TestSupport

class CalculationPollingControllerSpec extends TestSupport with MockCalculationPollingService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  object TestCalculationPollingController extends CalculationPollingController(
    MockAuthenticationPredicate,
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[NinoPredicate],
    mockCalculationPollingService,
    app.injector.instanceOf[ItvcErrorHandler]
  )(appConfig,
    app.injector.instanceOf[MessagesControllerComponents],
    ec)

  "The CalculationPollingController.calculationPoller(year) action" when {
    "Called with an Unauthenticated User" should {
      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        val result = TestCalculationPollingController.calculationPoller(testTaxYear, isFinalCalc = false)(fakeRequestWithNinoAndCalc)
        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "the calculation returned from the calculation service is found" should {
      "return the redirect to calculation page" in {
        mockCalculationPollingSuccess()

        val result = TestCalculationPollingController.calculationPoller(testTaxYear, isFinalCalc = false)(fakeRequestWithNinoAndCalc)

        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "the calculation returned from the calculation service was not found" should {
      "return the internal server error page" in {
        mockCalculationPollingRetryableError()

        val result = TestCalculationPollingController.calculationPoller(testTaxYear, isFinalCalc = false)(fakeRequestWithNinoAndCalc)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some("text/html")
      }
    }

    "the calculation returned from the calculation service was an error" should {
      "return the internal server error page" in {
        mockCalculationPollingNonRetryableError()

        val result = TestCalculationPollingController.calculationPoller(testTaxYear, isFinalCalc = false)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some("text/html")
      }
    }
  }
}
