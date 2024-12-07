/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.agent.manageBusinesses.cease

import controllers.agent.ControllerISpecHelper
import enums.{MTDPrimaryAgent, MTDSupportingAgent}
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDAgentAuthStub}
import models.admin.{IncomeSourcesFs, NavBarFs}
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{getAgentClientDetailsForCookie, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyAndCeasedBusiness, multipleBusinessesAndUkProperty}

class CeaseIncomeSourcesControllerISpec extends ControllerISpecHelper {

  val ceaseIncomeSourcesPath = "/agents/manage-your-businesses/cease/cease-an-income-source"
  val pageTitleMsgKey = "cease-income-sources.heading"
  val soleTraderBusinessName1: String = "business"
  val soleTraderBusinessName2: String = "secondBusiness"
  val ceaseMessage: String = messagesAPI("cease-income-sources.cease")
  val startDateMessage: String = messagesAPI("cease-income-sources.table-head.date-started")
  val ceasedDateMessage: String = messagesAPI("cease-income-sources.table-head.date-ended")
  val businessNameMessage: String = messagesAPI("cease-income-sources.table-head.business-name")
  val startDate = "1 January 2017"
  val ceasedBusinessMessage: String = messagesAPI("cease-income-sources.ceased-businesses.h1")
  val ceasedBusinessName: String = "ceasedBusiness"

  s"calling GET $ceaseIncomeSourcesPath" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"an authenticated user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "render the Cease Income Source page for an Agent" when {
          "Income source details are enabled for UK property" in {
            disable(NavBarFs)
            enable(IncomeSourcesFs)
            MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)

            When(s"I call GET ${ceaseIncomeSourcesPath}")
            val result = buildGETMTDClient(ceaseIncomeSourcesPath, additionalCookies).futureValue
            verifyIncomeSourceDetailsCall(testMtditid)

            result should have(
              httpStatus(OK),
              pageTitleAgent(pageTitleMsgKey),
              elementTextByID("table-head-business-name")(businessNameMessage),
              elementTextByID("table-row-trading-name-0")(soleTraderBusinessName1),
              elementTextByID("table-row-trading-name-1")(soleTraderBusinessName2),
              elementTextByID("cease-link-business-0")(ceaseMessage),
              elementTextByID("cease-link-business-1")(ceaseMessage),
              elementTextByID("table-head-date-started-uk")(startDateMessage),
              elementTextByID("table-row-trading-start-date-uk")(startDate)
            )
          }

          "Income source details are enabled for for foreign property" in {
            disable(NavBarFs)
            enable(IncomeSourcesFs)
            MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)

            When(s"I call GET ${ceaseIncomeSourcesPath}")
            val result = buildGETMTDClient(ceaseIncomeSourcesPath, additionalCookies).futureValue
            verifyIncomeSourceDetailsCall(testMtditid)

            result should have(
              httpStatus(OK),
              pageTitleAgent(pageTitleMsgKey),
              elementTextByID("ceased-businesses-heading")(ceasedBusinessMessage),
              elementTextByID("ceased-businesses-table-head-date-ended")(ceasedDateMessage),
              elementTextByID("ceased-business-table-row-trading-name-0")(ceasedBusinessName),
              elementTextByID("cease-link-foreign")(ceaseMessage),
              elementTextByID("table-head-date-started-foreign")(startDateMessage),
              elementTextByID("table-row-trading-start-date-foreign")(startDate)
            )
          }
        }
      }
    }
  }

}
