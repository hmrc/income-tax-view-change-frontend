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

import audit.mocks.MockAuditingService
import config.ItvcErrorHandler
import config.featureswitch.FeatureSwitching
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatterImpl
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockFinancialDetailsService, MockFinancialTransactionsService}
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import testUtils.TestSupport

class PollingCalcControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with MockFinancialTransactionsService with MockFinancialDetailsService
  with FeatureSwitching with MockAuditingService {

  object TestPollingCalcController extends PollingCalcController(
    MockAuthenticationPredicate,
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[ItvcErrorHandler],
    mockAuditingService
  )(appConfig,
    languageUtils,
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[ImplicitDateFormatterImpl])

  "PollingCalcController returned calculationId and nino" should {
    "return OK" in {

      val result = TestPollingCalcController.sessionCheck(fakeRequestWithNinoAndCalc)
      status(result) shouldBe Status.OK

    }
  }


  "calculationId and nino not found from PollingCalcController" should {
    "return the internal server error page" in {

      val result = TestPollingCalcController.sessionCheck(fakeRequestWithoutNinoOrCalc)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR

    }
  }
}
