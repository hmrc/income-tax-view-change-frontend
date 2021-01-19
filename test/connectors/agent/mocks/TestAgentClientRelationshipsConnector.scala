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

package connectors.agent.mocks

import connectors.agent.AgentClientRelationshipsConnector
import mocks.MockHttp
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsValue
import testUtils.TestSupport

import scala.concurrent.Future


trait TestAgentClientRelationshipsConnector extends TestSupport with MockHttp {

  object TestAgentClientRelationshipsConnector extends AgentClientRelationshipsConnector(mockHttpGet,appConfig)

  def mockAgentClientRelationship(arn: String, mtditid: String)(status: Int, response: Option[JsValue] = None): Unit =
    setupAgentMockHttpGet(Some(TestAgentClientRelationshipsConnector.agentClientURL(arn, mtditid)))(status, response)
}

trait MockAgentServicesConnector extends TestSupport with MockitoSugar with BeforeAndAfterEach {

  val mockAgentServicesConnector: AgentClientRelationshipsConnector = mock[AgentClientRelationshipsConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAgentServicesConnector)
  }

  def preExistingRelationship(arn: String, mtditid: String)(agentClientRelationship: Boolean): Unit =
    when(mockAgentServicesConnector.agentClientRelationship(arn, mtditid)).thenReturn(Future.successful(agentClientRelationship))

  def preExistingRelationshipFailure(arn: String, mtditid: String)(failure: Throwable): Unit =
    when(mockAgentServicesConnector.agentClientRelationship(arn, mtditid)).thenReturn(Future.failed(failure))

}
