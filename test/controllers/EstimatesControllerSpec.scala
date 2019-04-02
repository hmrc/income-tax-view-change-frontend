/*
 * Copyright 2019 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.BusinessDetailsTestConstants._
import assets.EstimatesTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import assets.Messages
import audit.AuditingService
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockCalculationService
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import testUtils.TestSupport

class EstimatesControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate {

  object TestCalculationController extends EstimatesController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockCalculationService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AuditingService]
  )

  lazy val messages = new Messages.Calculation(testYear)

  "The CalculationController.viewEstimateCalculations action" when {


    "Estimates Feature is disabled" should {

      lazy val result = TestCalculationController.viewEstimateCalculations(fakeRequestWithActiveSession)

      "return redirect SEE_OTHER (303)" in {
        TestCalculationController.config.features.estimatesEnabled(false)
        setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(List(business1, business2018), None))
        status(result) shouldBe Status.SEE_OTHER
      }
      "redirect to home page" in {
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.home().url)
      }
    }

    "Estimates Feature is enabled" which {

      "called with an authenticated HMRC-MTD-IT user" which {

        "successfully retrieves Business only income from the Income Sources predicate" should {

          lazy val result = TestCalculationController.viewEstimateCalculations(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument
          lazy val messages = new Messages.Estimates

          "return status OK (200)" in {
            TestCalculationController.config.features.estimatesEnabled(true)
            setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(List(business1, business2018), None))
            mockGetAllLatestCalcSuccess()
            status(result) shouldBe Status.OK
          }
          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }
          "render the Estimates sub-page" in {
            document.title shouldBe messages.title
          }
        }

        "successfully retrieves income sources, but the list returned from the service has a not found error model" should {
          lazy val result = TestCalculationController.viewEstimateCalculations(fakeRequestWithActiveSession)

          "return SEE_OTHER (303)" in {
            TestCalculationController.config.features.estimatesEnabled(true)
            setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
            mockGetAllLatestCalcSuccessOneNotFound()
            status(result) shouldBe Status.SEE_OTHER
          }
        }

        "successfully retrieves income sources, but the list returned from the service has a internal server error model" should {

          lazy val result = TestCalculationController.viewEstimateCalculations(fakeRequestWithActiveSession)

          "return an ISE (500)" in {
            TestCalculationController.config.features.estimatesEnabled(true)
            setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
            mockGetAllLatestCrystallisedCalcWithError()
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

      }

      "Called with an Unauthenticated User" should {

        "return redirect SEE_OTHER (303)" in {
          setupMockAuthorisationException()
          val result = TestCalculationController.viewEstimateCalculations(fakeRequestWithActiveSession)
          TestCalculationController.config.features.estimatesEnabled(true)
          status(result) shouldBe Status.SEE_OTHER
        }
      }
    }
  }
}
