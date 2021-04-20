/*
 * Copyright 2021 HM Revenue & Customs
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
import assets.EstimatesTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import assets.MessagesLookUp
import audit.AuditingService
import config.featureswitch.{API5, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockCalculationService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import services.CalculationService
import testUtils.TestSupport

import scala.concurrent.Future

class TaxYearsControllerSpec extends MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter with TestSupport with FeatureSwitching {

  val calculationService: CalculationService = mock[CalculationService]

  object TestTaxYearsController extends TaxYearsController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    calculationService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AuditingService]
  )

  lazy val CalcMessages = new MessagesLookUp.Calculation(testYear)

  ".viewTaxYears" when {
    "called with an authenticated HMRC-MTD-IT user and successfully retrieved income source" when {
      "successfully retrieves all latest calculations but the list returned from the service has an error model" should {
        "return an ISE (500)" in {

          lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
          enable(API5)
          when(calculationService.getAllLatestCalculations(any(), any())(any()))
            .thenReturn(Future.successful(lastTaxCalcWithYearListWithError))

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }


      "successfully retrieves all latest calculations and and display tax year page" should {
        "return an OK (200)" in {
          lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
          enable(API5)
          when(calculationService.getAllLatestCalculations(any(), any())(any()))
            .thenReturn(Future.successful(lastThreeTaxCalcWithYear))

          status(result) shouldBe Status.OK
        }
      }

      "successfully retrieves all latest calculations but the list returned from the service has an exception" should {
        "return an IST (500)" in {
          lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
          enable(API5)
          when(calculationService.getAllLatestCalculations(any(), any())(any()))
            .thenThrow(new RuntimeException)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR

        }
      }

    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {

        disable(API5)
        setupMockAuthorisationException()
        val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }


  }
}
