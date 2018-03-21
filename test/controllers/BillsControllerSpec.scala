/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.Messages
import assets.TestConstants.BusinessDetails._
import assets.TestConstants.Estimates._
import assets.TestConstants._
import audit.AuditingService
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockCalculationService
import models.incomeSourcesWithDeadlines.IncomeSourcesWithDeadlinesModel
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import utils.TestSupport

class BillsControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate {

  object TestCalculationController extends BillsController()(
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

  "The BillsController.viewCrystallisedCalculations action" when {

    "the Bills Feature is disabled" should {

      lazy val result = TestCalculationController.viewCrystallisedCalculations(fakeRequestWithActiveSession)

      "return redirect SEE_OTHER (303)" in {
        TestCalculationController.config.features.billsEnabled(false)
        setupMockGetIncomeSourceDetails(testMtditid, testNino)(IncomeSourcesWithDeadlinesModel(List(businessIncomeModel, business2018IncomeModel), None))
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to home page" in {
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.home().url)
      }

    }

    "the Bills Features is enabled" should {

      "Called with an Authenticated HMRC-MTD-IT User" which {

        "returns an estimate and a noCalcData response for two years" should {

          lazy val result = TestCalculationController.viewCrystallisedCalculations(fakeRequestWithActiveSession)

          lazy val document = result.toHtmlDocument

          "return status OK (200)" in {
            TestCalculationController.config.features.billsEnabled(true)
            setupMockGetIncomeSourceDetails(testMtditid, testNino)(IncomeSourcesWithDeadlinesModel(List(businessIncomeModel, business2018IncomeModel), None))
            mockGetAllLatestCalcSuccess()
            status(result) shouldBe Status.OK
          }
          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }
          "render the Bills sub-page" in {
            document.title shouldBe messages.Bills.billsTitle
          }
          "render content to say 'you have no bills yet'" in {
            document.getElementById("no-bills").text() shouldBe messages.Bills.noBills
          }
        }

        "successfully receives income sources from the Income Sources predicate" should {

          lazy val result = TestCalculationController.viewCrystallisedCalculations(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status OK (200)" in {
            TestCalculationController.config.features.billsEnabled(true)
            setupMockGetIncomeSourceDetails(testMtditid, testNino)(IncomeSources.business2018And19IncomeSourceSuccess)
            mockGetAllLatestCrystallisedCalcSuccess()
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the Bills page" in {
            document.title() shouldBe messages.Bills.billsTitle
          }
        }

        "successfully receives income sources, but an empty list from the CalculationService" should {

          lazy val result = TestCalculationController.viewCrystallisedCalculations(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status OK (200)" in {
            TestCalculationController.config.features.billsEnabled(true)
            setupMockGetIncomeSourceDetails(testMtditid, testNino)(IncomeSources.business2018And19IncomeSourceSuccess)
            mockGetAllLatestCalcSuccessEmpty()
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the Bills page" in {
            document.title() shouldBe messages.Bills.billsTitle
          }
        }

        "successfully retrieves income sources, but the list returned from the service has a calcNotFound" should {
          lazy val result = TestCalculationController.viewCrystallisedCalculations(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return an OK (200)" in {
            TestCalculationController.config.features.billsEnabled(true)
            setupMockGetIncomeSourceDetails(testMtditid, testNino)(IncomeSources.business2018And19IncomeSourceSuccess)
            mockGetAllLatestCrystallisedCalcWithCalcNotFound()
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }
          "render the Bills page" in {
            document.title() shouldBe messages.Bills.billsTitle
          }

        }

        "successfully retrieves income sources, but the list returned from the service has an error model" should {
          lazy val result = TestCalculationController.viewCrystallisedCalculations(fakeRequestWithActiveSession)

          "return an ISE (500)" in {
            TestCalculationController.config.features.billsEnabled(true)
            setupMockGetIncomeSourceDetails(testMtditid, testNino)(IncomeSources.business2018And19IncomeSourceSuccess)
            mockGetAllLatestCrystallisedCalcWithError()
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }

      "Called with an Unauthenticated User" should {

        "return redirect SEE_OTHER (303)" in {
          TestCalculationController.config.features.billsEnabled(true)
          setupMockAuthorisationException()
          val result = TestCalculationController.viewCrystallisedCalculations(fakeRequestWithActiveSession)
          status(result) shouldBe Status.SEE_OTHER
        }
      }
    }
  }
}
