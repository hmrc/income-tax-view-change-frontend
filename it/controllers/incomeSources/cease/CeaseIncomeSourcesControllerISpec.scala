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

package controllers.incomeSources.cease

import config.featureswitch.IncomeSources
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyAndCeasedBusiness, multipleBusinessesAndUkProperty}

class CeaseIncomeSourcesControllerISpec extends ComponentSpecBase {

  val showIndividualCeaseIncomeSourceControllerUrl: String = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url
  val showAgentCeaseIncomeSourceControllerUrl: String = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url
  val pageTitleMsgKey = "cease-income-sources.heading"
  val soleTraderBusinessName1: String = "business"
  val soleTraderBusinessName2: String = "secondBusiness"
  val ceaseMessage: String = messagesAPI("cease-income-sources.cease") + "cease"
  val startDateMessage: String = messagesAPI("cease-income-sources.table-head.date-started")
  val ceasedDateMessage: String = messagesAPI("cease-income-sources.table-head.date-ended")
  val businessNameMessage: String = messagesAPI("cease-income-sources.table-head.business-name")
  val ukPropertyStartDate: String = "1 January 2017"
  val foreignPropertyStartDate: String = "1 January 2017"
  val ceasedBusinessMessage: String = messagesAPI("cease-income-sources.ceased-businesses.h1")
  val ceasedBusinessName: String = "ceasedBusiness"


  s"calling GET ${showIndividualCeaseIncomeSourceControllerUrl}" should {
    "render the Cease Income Source page for an Individual" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple businesses and a uk property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
        When(s"I call GET ${showIndividualCeaseIncomeSourceControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getCeaseIncomeSourcesIndividual
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
          elementTextByID("table-head-business-name")(businessNameMessage),
          elementTextByID("table-row-trading-name-0")(soleTraderBusinessName1),
          elementTextByID("table-row-trading-name-1")(soleTraderBusinessName2),
          elementTextByID("cease-link-business-0")(ceaseMessage),
          elementTextByID("cease-link-business-1")(ceaseMessage),
          elementTextByID("table-head-date-started-uk")(startDateMessage),
          elementTextByID("table-row-trading-start-date-uk")(ukPropertyStartDate)
        )
      }
    }
  }

  s"calling GET ${showAgentCeaseIncomeSourceControllerUrl}" should {
    "render the Cease Income Source page for an Agent" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a foreign property and a ceased business")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)
        When(s"I call GET ${showAgentCeaseIncomeSourceControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getCeaseIncomeSourcesAgent
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKey),
          elementTextByID("ceased-businesses-h1")(businessNameMessage),
          elementTextByID("table-head-date-ended-ceased")(ceasedDateMessage),
          elementTextByID("table-row-trading-name-0-ceased")(soleTraderBusinessName1),
          elementTextByID("cease-link-foreign")(ceaseMessage),
          elementTextByID("table-head-date-started-foreign")(startDateMessage),
          elementTextByID("table-row-trading-start-date-foreign")(foreignPropertyStartDate)
        )
      }
    }
  }
}
