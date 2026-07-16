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

package hub.controllers.agent

import common.connectors.ITSAStatusConnector
import common.mocks.auth.MockAuthActions
import common.services.DateServiceInterface
import common.viewUtils.InternalUrlHelper
import hub.mocks.views.agent.MockEnterClientsUTR
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.BearerTokenExpired
import hub.views.html.agent.EnterClientsUTRView

class RemoveClientDetailsSessionsControllerSpec extends MockAuthActions
  with MockEnterClientsUTR {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[EnterClientsUTRView].toInstance(enterClientsUTR),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testRemoveClientDetailsSessionsController = app.injector.instanceOf[RemoveClientDetailsSessionsController]

  ".show" when {
    s"there is a user" that {
      "is not authenticated" should {
        "redirect the user to authenticate" in {

          setupMockAgentWithClientAuthorisationException()
          mockItsaStatusRetrievalAction()

          val result = testRemoveClientDetailsSessionsController.show()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(InternalUrlHelper.signinUrl)
        }
      }

      "the user has timed out" should {

        "redirect to the session timeout page" in {

          setupMockFeatureSwitches()
          setupMockAgentWithClientAuthorisationException(exception = BearerTokenExpired())
          mockItsaStatusRetrievalAction()

          val result = testRemoveClientDetailsSessionsController.show()(fakeRequestWithActiveSession)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(InternalUrlHelper.timeoutUrl)
        }
      }
    }

    Map("primary agent" -> false, "supporting agent" -> true).foreach { case (agentType, isSupportingAgent) =>
      val fakeRequest = fakeRequestConfirmedClient(isSupportingAgent = isSupportingAgent)

      s"the user is a $agentType" should {

        "remove client details session keys and redirect to the enter client UTR page" in {

          setupMockFeatureSwitches()
          setupMockAgentWithClientAuthAndIncomeSources(isSupportingAgent = isSupportingAgent)
          mockItsaStatusRetrievalAction()

          val result = testRemoveClientDetailsSessionsController.show()(fakeRequest)

          val removedSessionKeys: List[String] =
            List(
              "SessionKeys.clientLastName",
              "SessionKeys.clientFirstName",
              "SessionKeys.clientNino",
              "SessionKeys.clientUTR",
              "SessionKeys.isSupportingAgent",
              "SessionKeys.confirmedClient"
            )

          removedSessionKeys.foreach(key => result.futureValue.header.headers.get(key) shouldBe None)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/client-utr")
        }
      }
    }
  }
}
