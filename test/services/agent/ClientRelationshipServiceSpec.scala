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

package services.agent

import assets.BaseTestConstants.{testArn, testMtditid}
import services.agent.mocks.TestClientRelationshipService


class ClientRelationshipServiceSpec extends TestClientRelationshipService {
  "isAgentClientRelationship" should {
    "return true if the connector returns true" in {
      preExistingRelationship(testArn, testMtditid)(agentClientRelationship = true)

      val res = TestClientRelationshipService.isAgentClientRelationship(testArn, testMtditid)

      await(res) shouldBe true
    }

    "return false if the connector returns false" in {
      preExistingRelationship(testArn, testMtditid)(agentClientRelationship = false)

      val res = TestClientRelationshipService.isAgentClientRelationship(testArn, testMtditid)

      await(res) shouldBe false
    }

    "return a failed future if the connection fails" in {
      val exception = new Exception()

      preExistingRelationshipFailure(testArn, testMtditid)(exception)

      val res = TestClientRelationshipService.isAgentClientRelationship(testArn, testMtditid)

      intercept[Exception](await(res)) shouldBe exception
    }
  }

}
