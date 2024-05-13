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

package controllers.agent.manageBusinesses.manage

import config.featureswitch.IncomeSources
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithStartDate, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyAndCeasedBusiness, multipleBusinessesAndUkProperty}

class ManageIncomeSourceControllerISpec extends ComponentSpecBase {

  val showIndividualViewIncomeSourceControllerUrl: String = controllers.manageBusinesses.manage.routes.ManageIncomeSourceController.show(true).url
  val pageTitleMsgKey = "view-income-sources.heading"
  val soleTraderBusinessName1: String = "business"
  val soleTraderBusinessName2: String = "secondBusiness"
  val chooseMessage: String = messagesAPI("view-income-sources.choose")
  val startDateMessage: String = messagesAPI("view-income-sources.table-head.date-started")
  val ceasedDateMessage: String = messagesAPI("view-income-sources.table-head.date-ended")
  val businessNameMessage: String = messagesAPI("view-income-sources.table-head.business-name")
  val ukPropertyStartDate: String = "1 January 2017"
  val foreignPropertyStartDate: String = "1 January 2017"
  val ceasedBusinessMessage: String = messagesAPI("view-income-sources.ceased-businesses-h2")
  val ceasedBusinessName: String = "ceasedBusiness"

  s"calling GET ${showIndividualViewIncomeSourceControllerUrl}" should {
    "render the View Income Source page for an Individual" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("I wiremock stub a successful Income Source Details response with multiple businesses and a uk property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
        When(s"I call GET ${showIndividualViewIncomeSourceControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getManageIncomeSource(clientDetailsWithStartDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKey),
          elementTextByID("table-head-business-name")(businessNameMessage),
          elementTextByID("table-row-trading-name-0")(soleTraderBusinessName1),
          elementTextByID("table-row-trading-name-1")(soleTraderBusinessName2),
          elementTextByID("view-link-business-1")(chooseMessage),
          elementTextByID("table-head-date-started-uk")(startDateMessage),
          elementTextByID("table-row-trading-start-date-uk")(ukPropertyStartDate),
          elementAttributeBySelector("#back-fallback", "href")(s"/report-quarterly/income-and-expenses/view/agents/client-income-tax"),
        )
      }

      "User is authorised with different data" in {
        Given("I wiremock stub a successful Income Source Details response with a foreign property and a ceased business")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)
        When(s"I call GET ${showIndividualViewIncomeSourceControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getManageIncomeSource(clientDetailsWithStartDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKey),
          elementTextByID("ceased-businesses-heading")(ceasedBusinessMessage),
          elementTextByID("ceased-businesses-table-head-date-ended")(ceasedDateMessage),
          elementTextByID("ceased-business-table-row-trading-name-0")(ceasedBusinessName),
          elementTextByID("table-head-date-started-foreign")(startDateMessage),
          elementTextByID("table-row-trading-start-date-foreign")(foreignPropertyStartDate)
        )
      }
    }
  }
}
