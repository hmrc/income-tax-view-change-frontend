/*
 * Copyright 2017 HM Revenue & Customs
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
import assets.Messages.EstimatedTaxLiabilityError
import assets.TestConstants.BusinessDetails._
import assets.TestConstants.Estimates._
import assets.TestConstants.PropertyDetails._
import assets.TestConstants._
import audit.AuditingService
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockServiceInfoPartialService}
import models.IncomeSourcesModel
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import utils.TestSupport

class EstimatesControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockServiceInfoPartialService {

  object TestCalculationController extends EstimatesController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockCalculationService,
    mockServiceInfoPartialService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AuditingService]
  )

  lazy val messages = new Messages.Calculation(testYear)

  "The CalculationController.viewEstimateCalculations action" when {

    "called with an authenticated HMRC-MTD-IT user" which {

      "successfully retrieves Business only income from the Income Sources predicate" should {

        lazy val result = TestCalculationController.viewEstimateCalculations(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument
        lazy val messages = new Messages.Estimates

        "return status OK (200)" in {
          mockServiceInfoPartialSuccess()
          setupMockGetIncomeSourceDetails(testNino)(IncomeSourcesModel(List(businessIncomeModel, business2018IncomeModel), None))
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

      "successfully retrieves income sources, but the list returned from the service has a calcNotFound" should {
        lazy val result = TestCalculationController.viewEstimateCalculations(fakeRequestWithActiveSession)

        "return an OK (200)" in {
          mockServiceInfoPartialSuccess()
          setupMockGetIncomeSourceDetails(testNino)(IncomeSourceDetails.business2018And19IncomeSourceSuccess)
          mockGetAllLatestCrystallisedCalcWithCalcNotFound()
          status(result) shouldBe Status.OK
        }
      }

      "successfully retrieves income sources, but the list returned from the service has an error model" should {
        lazy val result = TestCalculationController.viewEstimateCalculations(fakeRequestWithActiveSession)

        "return an ISE (500)" in {
          mockServiceInfoPartialSuccess()
          setupMockGetIncomeSourceDetails(testNino)(IncomeSourceDetails.business2018And19IncomeSourceSuccess)
          mockGetAllLatestCrystallisedCalcWithError()
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        val result = TestCalculationController.viewEstimateCalculations(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

}
