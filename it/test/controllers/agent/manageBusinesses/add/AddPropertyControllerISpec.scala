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

package controllers.agent.manageBusinesses.add

import models.admin.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import forms.manageBusinesses.add.AddProprertyForm
import helpers.agent.AgentComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class AddPropertyControllerISpec extends AgentComponentSpecBase {

  val addPropertyShowUrl = controllers.manageBusinesses.add.routes.AddPropertyController.show(isAgent = true).url
  val addPropertySubmitUrl = controllers.manageBusinesses.add.routes.AddPropertyController.submit(isAgent = true).url

  val manageBusinessesUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent = true).url
  val startDateUkPropertyUrl = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent = true, incomeSourceType = UkProperty, isChange = false).url
  val startDateForeignPropertyUrl = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent = true, incomeSourceType = ForeignProperty, isChange = false).url

  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $addPropertyShowUrl" should {
    "render the Add Property page" when {
      "the user is authorised" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $addPropertyShowUrl")

        val result = IncomeTaxViewChangeFrontend.get("/manage-your-businesses/add-property/property-type", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("manageBusinesses.type-of-property.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling GET $addPropertySubmitUrl" should {
    "redirect to the add uk property start date page" when {
      "form response is UK" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call POST $addPropertySubmitUrl")

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/add-property/property-type", clientDetailsWithConfirmation)(Some("uk-property").fold(Map.empty[String, Seq[String]])(
          selection => AddProprertyForm.apply
            .fill(AddProprertyForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        ))

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(startDateUkPropertyUrl)
        )
      }
    }
    "redirect to the add foreign property start date page" when {
      "form response is UK" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call POST $addPropertySubmitUrl")

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/add-property/property-type", clientDetailsWithConfirmation)(Some("foreign-property").fold(Map.empty[String, Seq[String]])(
          selection => AddProprertyForm.apply
            .fill(AddProprertyForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        ))

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(startDateForeignPropertyUrl)
        )
      }
    }
    "return a BAD_REQUEST" when {
      "form is empty" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call POST $addPropertySubmitUrl")

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/add-property/property-type", clientDetailsWithConfirmation)(None.fold(Map.empty[String, Seq[String]])(
          selection => AddProprertyForm.apply
            .fill(AddProprertyForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        ))

        result should have(
          httpStatus(BAD_REQUEST)
        )
      }
      "form is invalid" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call POST $addPropertySubmitUrl")

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/add-property/property-type", clientDetailsWithConfirmation)(Some("£").fold(Map.empty[String, Seq[String]])(
          selection => AddProprertyForm.apply
            .fill(AddProprertyForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        ))

        result should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }

}
