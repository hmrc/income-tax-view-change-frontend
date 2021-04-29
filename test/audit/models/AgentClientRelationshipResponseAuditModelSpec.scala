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

package audit.models

import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.libs.json.Json

class AgentClientRelationshipResponseAuditModelSpec extends WordSpecLike with MustMatchers {

  val transactionName: String = "agent-client-relationship"
  val auditType: String = "AgentClientRelationship"

  def agentClientRelationshipResponseAudit(validRelationship: Boolean): AgentClientRelationshipResponseAuditModel =
    AgentClientRelationshipResponseAuditModel(
      saUtr = "saUtr",
      agentReferenceNumber = Some("agentReferenceNumber"),
      validRelationship = validRelationship
    )

  val agentClientRelationshipResponseAuditMin: AgentClientRelationshipResponseAuditModel =
    AgentClientRelationshipResponseAuditModel(
      saUtr = "saUtr",
      agentReferenceNumber = None,
      validRelationship = true
    )
  "AgentClientRelationShipResponseAuditModel(saUtr, agentReferenceNumber, validRelationship)" should {

    s"have the correct transaction name of '$transactionName''" in {
      agentClientRelationshipResponseAudit(
        validRelationship = true
      ).transactionName mustBe transactionName
    }

    s"have the correct audit type of '$auditType'" in {
      agentClientRelationshipResponseAudit(
        validRelationship = true
      ).auditType mustBe auditType
    }

    "have the correct details for the Audit event" when {
      "there is a valid relationship" in {
        agentClientRelationshipResponseAudit(
          validRelationship = true
        ).detail mustBe Json.obj(
          "saUtr" -> "saUtr",
          "agentReferenceNumber" -> "agentReferenceNumber",
          "validRelationship" -> true
        )
      }

      "there is not a valid relationship" in {
        agentClientRelationshipResponseAudit(
          validRelationship = false
        ).detail mustBe Json.obj(
          "saUtr" -> "saUtr",
          "agentReferenceNumber" -> "agentReferenceNumber",
          "validRelationship" -> false
        )
      }
    }

    "the agentClientRelationship audit has minimal details" in {
      agentClientRelationshipResponseAuditMin.detail mustBe Json.obj(
        "saUtr" -> "saUtr",
        "validRelationship" -> true
      )
    }

  }

}
