/*
 * Copyright 2026 HM Revenue & Customs
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

package hub.v1.controllers

import common.enums.{MTDIndividual, MTDPrimaryAgent}
import common.mocks.auth.MockAuthActions
import common.mocks.services.MockYearOfMigrationService
import play.api
import play.api.Application
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}

class NewServiceRedirectControllerSpec extends MockAuthActions with MockYearOfMigrationService {

  override lazy val app: Application =
    applicationBuilderWithAuthBindings.build()

  private lazy val controller = app.injector.instanceOf[NewServiceRedirectController]

  "show" should {
    "redirect to the new url home page for individuals" in {
      setupMockUserAuthNoSAUtr

      val result = controller.show("/your-tasks")(fakeGetRequestBasedOnMTDUserType(MTDIndividual))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(appConfig.hubBasePath)
    }
  }

  "showAgent" should {
    "redirect to the new url home page for agents" in {
      setupMockAgentWithClientAuth(isSupportingAgent = false)

      val result = controller.showAgent("/your-tasks")(fakeGetRequestBasedOnMTDUserType(MTDPrimaryAgent))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(appConfig.agentHubBasePath + "/client-income-tax")
    }
  }
}
