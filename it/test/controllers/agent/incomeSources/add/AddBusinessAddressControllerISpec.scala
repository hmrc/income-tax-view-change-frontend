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

package controllers.agent.incomeSources.add

import controllers.agent.ControllerISpecHelper
import enums.{MTDPrimaryAgent, MTDSupportingAgent}
import helpers.servicemocks.{AddressLookupStub, IncomeTaxViewChangeStub, MTDAgentAuthStub}
import models.admin.{IncomeSourcesFs, NavBarFs}
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{getAgentClientDetailsForCookie, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class AddBusinessAddressControllerISpec extends ControllerISpecHelper {

  val path = "/agents/income-sources/add/business-address"
  val changePath = "/agents/income-sources/add/change-business-address-lookup"

  s"GET $path" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "redirect to address lookup" in {
            enable(IncomeSourcesFs)
            disable(NavBarFs)
            MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

            AddressLookupStub.stubPostInitialiseAddressLookup()

            val result = buildGETMTDClient(path, additionalCookies).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI("TestRedirect")
            )
          }
        }
        testAuthFailuresForMTDAgent(path, isSupportingAgent)
      }
    }
  }

  s"GET $changePath" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "redirect to address lookup" in {
            enable(IncomeSourcesFs)
            disable(NavBarFs)
            MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

            AddressLookupStub.stubPostInitialiseAddressLookup()

            val result = buildGETMTDClient(changePath, additionalCookies).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI("TestRedirect")
            )
          }
        }
        testAuthFailuresForMTDAgent(changePath, isSupportingAgent)
      }
    }
  }
}
