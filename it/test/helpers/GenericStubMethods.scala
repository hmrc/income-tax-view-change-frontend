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

package helpers

import helpers.servicemocks._

trait GenericStubMethods extends CustomMatchers {

  def isAuthorisedUser(authorised: Boolean): Unit = {
    if (authorised) {
      AuthStub.stubAuthorised()
    } else {
      AuthStub.stubUnauthorised()
    }
  }

  def stubAuthorisedAgentUser(authorised: Boolean, hasAgentEnrolment: Boolean = true, clientMtdId: String = "mtdbsaId"): Unit = {
    if (authorised) {
      if (hasAgentEnrolment) {
        Given("I stub the agent is authorised with an agent reference number")
        AuthStub.stubAuthorisedAgent(clientMtdId)
      } else {
        Given("I stub the agent is authorised without an agent reference number")
        AuthStub.stubAuthorisedAgentNoARN()
      }
    } else {
      Given("I stub the unauthorised agent")
      AuthStub.stubUnauthorised()
    }
  }

  def stubAgentAuthorisedUser(authorised: Boolean, hasAgentEnrolment: Boolean = true, clientMtdId: String = "mtdbsaId", isSupportingAgent: Boolean = false): Unit = {
    if (authorised) {
      if (hasAgentEnrolment) {
        Given("I stub the agent is authorised with an agent reference number")
        AgentAuthStub.stubAuthAgent(clientMtdId, isSupportingAgent)
      } else {
        Given("I stub the agent is authorised without an agent reference number")
        AgentAuthStub.stubNoAgentEnrolment(clientMtdId)
      }
    } else {
      Given("I stub the unauthorised agent")
      AgentAuthStub.stubNotAnAgent(clientMtdId)
    }
  }

  def stubPrimaryAuthorisedAgentUser(clientMtdId: String = "mtdbsaId", hasDelegatedEnrolment: Boolean): Unit = {
    if (hasDelegatedEnrolment) {
      Given("I stub the primary agent has a delegated MTDITID enrolment")
      AgentAuthStub.stubPrimaryAuthorisedAgent(clientMtdId)
    } else {
      Given("I stub the primary agent does not have the delegated MTDITID enrolment")
      AgentAuthStub.failedPrimaryAgent(clientMtdId)
    }
  }

  def stubSecondaryAuthorisedAgentUser(clientMtdId: String = "mtdbsaId", hasDelegatedEnrolment: Boolean): Unit = {
    if (hasDelegatedEnrolment) {
      Given("I stub the primary agent has a delegated MTDITID supporting enrolment")
      AgentAuthStub.stubSecondaryAuthorisedAgent(clientMtdId)
    } else {
      Given("I stub the primary agent does not have the delegated MTDITID supporting enrolment")
      AgentAuthStub.failedSecondaryAgent(clientMtdId)
    }
  }

  def verifyIncomeSourceDetailsCall(mtditid: String, noOfCalls: Int = 1): Unit = {
    Then(s"Verify that Income Source Details has been called for MTDITID = $mtditid")
    IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(mtditid, noOfCalls)
  }

  def verifyBtaNavBarCall(mtditid: String, noOfCalls: Int = 1): Unit = {
    Then(s"Verify that Income Source Details has been called for MTDITID = $mtditid")
    IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(mtditid, noOfCalls)
  }

  def verifyNextUpdatesCall(nino: String): Unit = {
    IncomeTaxViewChangeStub.verifyGetNextUpdates(nino)
  }
}
