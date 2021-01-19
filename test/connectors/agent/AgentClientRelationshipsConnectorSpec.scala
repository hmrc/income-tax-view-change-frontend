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

package connectors.agent

import connectors.agent.mocks.TestAgentClientRelationshipsConnector
import org.scalatest.MustMatchers._
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException



class AgentClientRelationshipsConnectorSpec extends TestAgentClientRelationshipsConnector {
    val testArn = "123456"
    val testMTDITID = "XE0001234567890"


  "agentClientRelationship" should {
    "return true if the agent and client have a pre existing relationship" in {
      mockAgentClientRelationship(testArn, testMTDITID)(OK)

      val res = TestAgentClientRelationshipsConnector.agentClientRelationship(testArn, testMTDITID)

      await(res) mustBe true
    }

    "return false if the agent and client do not have a pre existing relationship" in {
      mockAgentClientRelationship(testArn, testMTDITID)(NOT_FOUND)

      val res = TestAgentClientRelationshipsConnector.agentClientRelationship(testArn, testMTDITID)

      await(res) mustBe false
    }

    "return a failure on a non OK status" in {
      val invalidBody = Json.toJson("invalid")
      mockAgentClientRelationship(testArn, testMTDITID)(INTERNAL_SERVER_ERROR, Option(invalidBody))

      val res = TestAgentClientRelationshipsConnector.agentClientRelationship(testArn, testMTDITID)

      val ex = intercept[InternalServerException](await(res))
      ex.getMessage mustBe s"[AgentClientRelationshipConnector][agentClientRelationship] failure, status: $INTERNAL_SERVER_ERROR body=$invalidBody"
    }
  }

}
