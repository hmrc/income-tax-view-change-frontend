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

package common.utils.audit

import common.auth.actions.AuthActionsTestData.defaultMTDITUser
import common.enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent}
import common.models.incomeSourceDetails.IncomeSourceDetailsModel
import common.testConstants.BaseTestConstants.testArn
import common.testUtils.TestSupport
import common.utils.audit.Utilities.arnToJson
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

class UtilitiesSpec extends TestSupport {

  private val incomeSources = IncomeSourceDetailsModel("nino", "mtditid", None, Nil, Nil, "1")

  "agentReferenceNumber" should {
    "omit the field for an individual" in {
      val individual = defaultMTDITUser(Some(Individual), incomeSources).copy(usersRole = MTDIndividual)

      arnToJson(individual.agentReferenceNumber) shouldBe Json.obj()
    }

    "include the ARN for a primary agent" in {
      val agent = defaultMTDITUser(Some(Agent), incomeSources).copy(usersRole = MTDPrimaryAgent)

      arnToJson(agent.agentReferenceNumber) shouldBe Json.obj("agentReferenceNumber" -> testArn)
    }

    "include the ARN for a supporting agent" in {
      val supportingAgent = defaultMTDITUser(Some(Agent), incomeSources).copy(usersRole = MTDSupportingAgent)

      arnToJson(supportingAgent.agentReferenceNumber) shouldBe Json.obj("agentReferenceNumber" -> testArn)
    }

    "omit the field when an agent has no ARN" in {
      val agentWithoutArn = defaultMTDITUser(Some(Individual), incomeSources).copy(usersRole = MTDPrimaryAgent)

      arnToJson(agentWithoutArn.agentReferenceNumber) shouldBe Json.obj()
    }
  }
}
