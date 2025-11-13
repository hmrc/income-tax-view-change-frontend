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
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation}
import testConstants.BaseTestConstants.testNino

class UpliftSuccessControllerSpec extends MockAuthActions {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .build()

  lazy val testController = app.injector.instanceOf[UpliftSuccessController]

  val action = testController.success()
  val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDIndividual)
  s"success()" when {
    s"the user is an authenticated individual" should {
      "audit and redirect to the home controller" in {
        setupMockSuccess(MTDIndividual)
        mockSingleBusinessIncomeSource()
        val result = action(fakeRequest)
        val expectedIvOutcomeSuccessAuditModel = IvOutcomeSuccessAuditModel(testNino)
        whenReady(result) { response =>
          verifyAudit(expectedIvOutcomeSuccessAuditModel)
          response.header.status shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
      }
    }
    testMTDAuthFailuresForRole(action, MTDIndividual)(fakeRequest)
  }
}
