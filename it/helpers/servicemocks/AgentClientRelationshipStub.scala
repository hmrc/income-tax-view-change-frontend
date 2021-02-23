/*
 * Copyright 2017 HM Revenue & Customs
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

package helpers.servicemocks

import helpers.WiremockHelper

object AgentClientRelationshipStub {

  def agentClientRelationshipUrl(mtditid: String, agentReferenceNumber: String): String = {
    s"/agent-client-relationships/agent/$agentReferenceNumber/service/HMRC-MTD-IT/client/MTDITID/$mtditid"
  }

  //Financial Transactions
  def stubAgentClientRelationship(mtditid: String, agentReferenceNumber: String)(status: Int): Unit =
    WiremockHelper.stubGet(agentClientRelationshipUrl(mtditid, agentReferenceNumber), status, body = "")

  //Verifications
  def verifyAgentClientRelationship(mtditid: String, agentReferenceNumber: String): Unit =
    WiremockHelper.verifyGet(agentClientRelationshipUrl(mtditid, agentReferenceNumber))
}
