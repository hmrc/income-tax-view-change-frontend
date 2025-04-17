/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.optIn

import enums.MTDIndividual
import mocks.auth.MockAuthActions
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

class OptInErrorControllerSpec extends MockAuthActions {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .build()

  lazy val testController = app.injector.instanceOf[OptInErrorController]

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      s"the user is authenticated as a $mtdRole" should {
        s"render the error page" in {
          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          val result = action(fakeRequest)
          status(result) shouldBe Status.OK
        }

        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}