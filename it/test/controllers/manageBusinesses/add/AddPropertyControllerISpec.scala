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

package controllers.manageBusinesses.add

import models.admin.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import forms.manageBusinesses.add.AddProprertyForm
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class AddPropertyControllerISpec extends ComponentSpecBase {

  val addPropertyShowUrl = controllers.manageBusinesses.add.routes.AddPropertyController.show(isAgent = false).url
  val addPropertySubmitUrl = controllers.manageBusinesses.add.routes.AddPropertyController.submit(isAgent = false).url

  val manageBusinessesUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent = false).url
  val startDateUkPropertyUrl = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent = false, incomeSourceType = UkProperty, isChange = false).url
  val startDateForeignPropertyUrl = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent = false, incomeSourceType = ForeignProperty, isChange = false).url

  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $addPropertyShowUrl" should {
    "render the Add Property page" when {
      "the user is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $addPropertyShowUrl")

        val result = IncomeTaxViewChangeFrontendManageBusinesses.get("/manage-your-businesses/add-property/property-type")
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("manageBusinesses.type-of-property.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling GET $addPropertySubmitUrl" should {
    "redirect to the add uk property start date page" when {
      "form response is UK" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call POST $addPropertySubmitUrl")

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/add-property/property-type")(Some("uk-property").fold(Map.empty[String, Seq[String]])(
          selection => AddProprertyForm.apply
            .fill(AddProprertyForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        ))
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(startDateUkPropertyUrl)
        )
      }
    }
    "redirect to the add foreign property start date page" when {
      "form response is UK" in {
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call POST $addPropertySubmitUrl")

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/add-property/property-type")(Some("foreign-property").fold(Map.empty[String, Seq[String]])(
          selection => AddProprertyForm.apply
            .fill(AddProprertyForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        ))
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(startDateForeignPropertyUrl)
        )
      }
    }
    "return a BAD_REQUEST" when {
      "form is empty" in {
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call POST $addPropertySubmitUrl")

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/add-property/property-type")(None.fold(Map.empty[String, Seq[String]])(
          selection => AddProprertyForm.apply
            .fill(AddProprertyForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        ))
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(BAD_REQUEST)
        )
      }
      "form is invalid" in {
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call POST $addPropertySubmitUrl")

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/add-property/property-type")(Some("£").fold(Map.empty[String, Seq[String]])(
          selection => AddProprertyForm.apply
            .fill(AddProprertyForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        ))
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }

}
