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

package financials.controllers

import common.enums.{MTDIndividual, MTDSupportingAgent}
import common.exceptions.SelfServeTimeToPayJourneyException
import common.mocks.auth.MockAuthActions
import financials.services.SelfServeTimeToPayService
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers.status
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation}
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future


class SelfServeTimeToPayControllerSpec extends MockAuthActions {

  lazy val selfServeTimeToPayService: SelfServeTimeToPayService = mock(classOf[SelfServeTimeToPayService])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SelfServeTimeToPayService].toInstance(selfServeTimeToPayService),
    ).build()
  lazy val testController: SelfServeTimeToPayController = app.injector.instanceOf[SelfServeTimeToPayController]

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.agentFetchUrl() else testController.fetchUrl()
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)

    s"fetchUrl${if (isAgent) "Agent" else ""}" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "returns correct URL" when {
            "service returns successful future with url" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()
              when(selfServeTimeToPayService.startSelfServeTimeToPayJourney(any()))
                .thenReturn(Future.successful(Right("/correct-url")))

              val result = action(fakeRequest)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some("/correct-url")
            }
          }
          "shows internal server error page" when {
            "service returns successful future without url" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()
              when(selfServeTimeToPayService.startSelfServeTimeToPayJourney(any()))
                .thenReturn(Future.successful(Left(SelfServeTimeToPayJourneyException(500, "message"))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR

            }
          }
        }
      }
    }
  }
}
