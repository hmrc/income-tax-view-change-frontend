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

package controllers.agent.manageBusinesses

import models.admin.IncomeSources
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithStartDate, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyAndCeasedBusiness, multipleBusinessesAndUkProperty}

class ManageYourBusinessesControllerISpec extends ComponentSpecBase {

  val showIndividualViewIncomeSourceControllerUrl: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.show(true).url
  val pageTitleMsgKey = "manage.your.businesses.heading"
  val soleTraderBusinessesHeading = messagesAPI("manage.your.businesses.self-employed-h2")
  val propertyBusinessesHeading = messagesAPI("manage.your.businesses.property-h2")
  val dateStarted: String = messagesAPI("manage.your.businesses.datestarted")
  val businessName: String = messagesAPI("manage.your.businesses.name")
  val ukPropertyHeading: String = messagesAPI("manage.your.businesses.UK")
  val foreignPropertyHeading: String = messagesAPI("manage.your.businesses.Foreign")
  val ceasedBusinessHeading: String = messagesAPI("manage.your.businesses.ceasedBusinesses.heading")
  val ukPropertyStartDate: String = "1 January 2017"
  val foreignPropertyStartDate: String = "1 January 2018"

  s"calling GET ${showIndividualViewIncomeSourceControllerUrl}" should {
    "render the manage your businesses page for an agent" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("I wiremock stub a successful manage your businesses response with multiple businesses and a uk property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
        When(s"I call GET ${showIndividualViewIncomeSourceControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getManageYourBusinesses(clientDetailsWithStartDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKey),
          elementTextByID("manage-your-businesses-h1")(messagesAPI(pageTitleMsgKey)),
          elementTextByID("self-employed-h1")(soleTraderBusinessesHeading),
          elementTextByID("business-type-0")("Fruit Ltd"),
          elementTextByID("business-trade-name-0")("business"),
          elementTextByID("business-date-0")(ukPropertyStartDate),
          elementTextByID("business-date-1")(foreignPropertyStartDate),
          elementTextByID("property-h2")(propertyBusinessesHeading),
          elementTextByID("uk-date")(ukPropertyStartDate),
          elementAttributeBySelector("#back-fallback", "href")(s"/report-quarterly/income-and-expenses/view/agents/client-income-tax"),
        )
      }

      "User is authorised with different data" in {
        Given("I wiremock stub a successful manage your businesses response with a foreign property and a ceased business")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)
        When(s"I call GET ${showIndividualViewIncomeSourceControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getManageYourBusinesses(clientDetailsWithStartDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKey),
          elementTextByID("manage-your-businesses-h1")(messagesAPI(pageTitleMsgKey)),
          elementTextByID("foreign-date")(ukPropertyStartDate),
          elementTextByID("ceasedBusinesses-heading")(ceasedBusinessHeading)
        )
      }
    }
  }
}
