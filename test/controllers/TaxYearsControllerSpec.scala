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

import config.featureswitch.FeatureSwitching
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import services.CalculationService
import testConstants.EstimatesTestConstants._
import testConstants.IncomeSourceDetailsTestConstants._
import testConstants.MessagesLookUp
import testUtils.TestSupport
import views.html.TaxYears

class TaxYearsControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter with TestSupport with FeatureSwitching {

  val calculationService: CalculationService = mock[CalculationService]

  object TestTaxYearsController extends TaxYearsController(app.injector.instanceOf[TaxYears])(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[ItvcErrorHandler]
  )

  lazy val CalcMessages = new MessagesLookUp.Calculation(testYear)

  ".viewTaxYears" when {
    "called with an authenticated HMRC-MTD-IT user and successfully retrieved income source" when {
      "and firstAccountingPeriodEndDate is missing from income sources" should {
        "return an ISE (500)" in {

          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)

          lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }


      "successfully retrieves income sources and and display tax year page" should {
        "return an OK (200)" in {
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
        }
      }

    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {

        setupMockAuthorisationException()
        val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }


  }
}
