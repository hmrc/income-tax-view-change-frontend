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

package controllers.manageBusinesses.cease

import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import play.api
import play.api.http.Status.OK
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.SessionService

class IncomeSourceCeasedBackErrorControllerSpec extends MockAuthActions with MockSessionService {

  override lazy val app = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[IncomeSourceCeasedBackErrorController]

  mtdAllRoles.foreach { mtdRole =>
    List(UkProperty, ForeignProperty).foreach { incomeSourceType =>
      val isAgent = mtdRole != MTDIndividual
      s"show${if (isAgent) "Agent"}($incomeSourceType)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        val action = if (!isAgent) testController.show(incomeSourceType) else testController.showAgent(incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          "return 200 OK" in {
            setupMockSuccess(mtdRole)
            mockUKPropertyIncomeSourceWithLatency2024()
            val result = action(fakeRequest)
            status(result) shouldBe OK
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}