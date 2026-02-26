/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.reportingObligations.signUp

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import controllers.reportingObligations.signUp.YouMustWaitToSignUpController
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockOptInService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status.OK
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.DateServiceInterface
import services.reportingObligations.signUp.OptInService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class YouMustWaitToSignUpControllerSpec extends MockAuthActions with MockOptInService {

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[OptInService].toInstance(mockOptInService),
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
      ).build()

  lazy val testController = app.injector.instanceOf[YouMustWaitToSignUpController]

  mtdAllRoles.foreach { mtdRole =>

    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual
    val action = testController.show(isAgent)

    s"show(isAgent = $isAgent)" when {

      s"the user is authenticated as a $mtdRole" should {

        s"render the YouMustWaitToSignUp page" in {

          setupMockSuccess(mtdRole)
          mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          when(mockOptInService.updateJourneyStatusInSessionData(any())(any(), any(), any()))
            .thenReturn(Future(true))

          val result = action(fakeRequest)
          status(result) shouldBe OK
        }

        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}
