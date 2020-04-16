/*
 * Copyright 2020 HM Revenue & Customs
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

import assets.CalcBreakdownTestConstants.calculationDataSuccessModel
import assets.EstimatesTestConstants._
import assets.FinancialTransactionsTestConstants.transactionModel
import assets.Messages
import config.ItvcErrorHandler
import config.featureswitch.FeatureSwitching
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockFinancialTransactionsService}
import models.calculation.CalcOverview
import play.api.http.Status
import play.api.i18n.Lang
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.Helpers._
import testUtils.TestSupport

class CalculationControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockFinancialTransactionsService with FeatureSwitching {

  object TestCalculationController extends CalculationController(
    MockAuthenticationPredicate,
    mockCalculationService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockFinancialTransactionsService,
    app.injector.instanceOf[ItvcErrorHandler],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate]
  )(appConfig, messagesApi, ec)

  lazy val messages = new Messages.Calculation(testYear)

  "The CalculationController.renderCalculationPage(year) action" when {
    "Called with an Unauthenticated User" should {
      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "Called with an Authenticated HMRC-MTD-IT User" when {
      "provided with a negative tax year" should {
        "return Status Bad Request Error (400)" in {
          mockPropertyIncomeSource()

          val result = TestCalculationController.renderCalculationPage(-testYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.BAD_REQUEST
        }
      }

      "the calculation returned from the calculation service was not found" should {
        "return the internal server error page" in {
          mockSingleBusinessIncomeSource()
          mockCalculationNotFound()

          val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          contentType(result) shouldBe Some("text/html")
        }
      }

      "the calculation returned from the calculation service was an error" should {
        "return the internal server error page" in {
          mockSingleBusinessIncomeSource()
          mockCalculationError()

          val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          contentType(result) shouldBe Some("text/html")
        }
      }

      "the calculation returned from the calculation service is not crystallised" should {
        "return OK (200) with the correct view" in {
          mockSingleBusinessIncomeSource()
          mockCalculationSuccess()

          val calcOverview: CalcOverview = CalcOverview(calculationDataSuccessModel, None)
          val expectedContent: String = views.html.taxYearOverview(testYear, calcOverview, None)(
            implicitly, applicationMessages(Lang("en"), implicitly), implicitly
          ).toString

          val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          contentAsString(result) shouldBe expectedContent
          contentType(result) shouldBe Some("text/html")
        }
      }

      "the calculation returned from the calculation service is crystallised" when {
        "the financial transaction returned from the service was an error" should {
          "return the internal server error page" in {
            mockSingleBusinessIncomeSource()
            mockCalculationCrystalisationSuccess()
            mockFinancialTransactionFailed()

            val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentType(result) shouldBe Some("text/html")
          }
        }

        "the financial transaction returned from the service is successful" should {
          "return OK (200) with the correct view" in {
            mockSingleBusinessIncomeSource()
            mockCalculationCrystalisationSuccess()
            mockFinancialTransactionSuccess()

            val calcOverview: CalcOverview = CalcOverview(calculationDataSuccessModel, Some(transactionModel()))
            val expectedContent: String = views.html.taxYearOverview(testYear, calcOverview, Some(transactionModel()))(
              implicitly, applicationMessages(Lang("en"), implicitly), implicitly
            ).toString

            val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)

            status(result) shouldBe Status.OK
            contentAsString(result) shouldBe expectedContent
            contentType(result) shouldBe Some("text/html")
          }
        }
      }
    }
  }
}
