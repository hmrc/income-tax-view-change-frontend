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

import models.admin.IncomeSourcesFs
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyAndCeasedBusiness, multipleBusinessesWithBothPropertiesAndCeasedBusiness}

class ViewAllCeasedBusinessesControllerISpec extends ComponentSpecBase {

  val ViewAllCeasedBusinessesControllerUrl: String = controllers.manageBusinesses.cease.routes.ViewAllCeasedBusinessesController.show(true).url
  val pageTitleMsgKey = "manageBusinesses.ceased.heading"
  val soleTraderBusinessName1: String = "thirdBusiness"
  val ceaseMessage: String = messagesAPI("cease-income-sources.cease")
  val startDateMessage: String = messagesAPI("cease-income-sources.table-head.date-started")
  val ceasedDateMessage: String = messagesAPI("cease-income-sources.table-head.date-ended")
  val businessNameMessage: String = messagesAPI("cease-income-sources.table-head.business-name")
  val propertyEndDate: String = "1 January 2020"
  val propertyStartDate: String = "1 January 2018"
  val propertyStartDate1: String = "31 December 2019"
  val ceasedBusinessName: String = "ceasedBusiness"

  s"calling GET $ViewAllCeasedBusinessesControllerUrl" should {
    "render the Cease Income Source page for an Individual" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("I wiremock stub a successful Income Source Details response with multiple businesses and a uk property")
        enable(IncomeSourcesFs)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesWithBothPropertiesAndCeasedBusiness)
        When(s"I call GET ${ViewAllCeasedBusinessesControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getViewAllCeasedBusinesses()
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKey),
          elementTextByID("ceased-businesses-table-head-name")(businessNameMessage),
          elementTextByID("ceased-business-table-row-trading-name-0")(soleTraderBusinessName1),
          elementTextByID("ceased-business-table-row-date-ended-0")(propertyEndDate),
          elementTextByID("ceased-businesses-table-head-date-started")(startDateMessage),
          elementTextByID("ceased-business-table-row-date-started-0")(propertyStartDate),
        )
      }

      "User is authorised with different data" in {
        Given("I wiremock stub a successful Income Source Details response with a foreign property and a ceased business")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSourcesFs)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)
        When(s"I call GET ${ViewAllCeasedBusinessesControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getViewAllCeasedBusinesses()
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKey),
          elementTextByID("ceased-businesses-table-head-name")(businessNameMessage),
          elementTextByID("ceased-businesses-table-head-date-ended")(ceasedDateMessage),
          elementTextByID("ceased-business-table-row-trading-name-0")(ceasedBusinessName),
          elementTextByID("ceased-businesses-table-head-date-started")(startDateMessage),
          elementTextByID("ceased-business-table-row-date-ended-0")(propertyStartDate1),
          elementTextByID("ceased-business-table-row-date-started-0")(propertyStartDate)
        )
      }
    }
  }
}
