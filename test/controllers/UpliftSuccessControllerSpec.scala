/*
 * Copyright 2023 HM Revenue & Customs
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

import audit.models.IvOutcomeSuccessAuditModel
import config.FrontendAppConfig
import controllers.predicates.NinoPredicate
import mocks.controllers.predicates.MockAuthenticationPredicate
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation}
import testConstants.BaseTestConstants.testNino
import testUtils.TestSupport

import scala.concurrent.ExecutionContext

class UpliftSuccessControllerSpec extends TestSupport with MockAuthenticationPredicate {

  object TestUpliftSuccessController extends UpliftSuccessController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesControllerComponents],
    mockAuditingService,
    app.injector.instanceOf[ExecutionContext],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate]
  )

  "the UpliftSuccessController.success action" should {

    "audit IV-uplift-success-outcome when nino is defined" in {

      lazy val result = TestUpliftSuccessController.success("PTA")(fakeRequestWithNino)

      val expectedIvOutcomeSuccessAuditModel = IvOutcomeSuccessAuditModel(testNino)

      whenReady(result) { response =>
        verifyAudit(expectedIvOutcomeSuccessAuditModel)
        response.header.status shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
    }
  }
}
