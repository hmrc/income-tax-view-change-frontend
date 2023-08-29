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

package controllers.agent.incomeSources.manage

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.incomeSources.manage.ConfirmReportingMethodForm
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.updateIncomeSource.{UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testPropertyIncomeId, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

class ConfirmReportingMethodSharedControllerISpec extends ComponentSpecBase {

  val annual = "annual"
  val quarterly = "quarterly"
  val taxYear = "2023-2024"

  val timestamp = "2023-01-31T09:26:17Z"

  private lazy val manageObligationsController = controllers.incomeSources.manage.routes
    .ManageObligationsController
  private lazy val confirmReportingMethodSharedController = controllers.incomeSources.manage.routes
    .ConfirmReportingMethodSharedController

  val confirmReportingMethodShowUKPropertyUrl: String = confirmReportingMethodSharedController
    .show(None, taxYear = testPropertyIncomeId, changeTo = annual, UkProperty, isAgent = true).url
  val confirmReportingMethodShowForeignPropertyUrl: String = confirmReportingMethodSharedController
    .show(None, taxYear = testPropertyIncomeId, changeTo = annual, ForeignProperty, isAgent = true).url
  val confirmReportingMethodShowSoleTraderBusinessUrl: String = confirmReportingMethodSharedController
    .show(id = Some(testSelfEmploymentId), taxYear = taxYear, changeTo = annual, SelfEmployment, isAgent = true).url

  val confirmReportingMethodSubmitUKPropertyUrl: String = confirmReportingMethodSharedController
    .submit(id = testPropertyIncomeId, taxYear = taxYear, changeTo = annual, UkProperty, isAgent = true).url
  val confirmReportingMethodSubmitForeignPropertyUrl: String = confirmReportingMethodSharedController
    .submit(id = testPropertyIncomeId, taxYear = taxYear, changeTo = annual, ForeignProperty, isAgent = true).url
  val confirmReportingMethodSubmitSoleTraderBusinessUrl: String = confirmReportingMethodSharedController
    .submit(id = testSelfEmploymentId, taxYear = taxYear, changeTo = annual, SelfEmployment, isAgent = true).url

  val manageObligationsShowUKPropertyUrl: String = manageObligationsController
    .showAgentUKProperty(changeTo = annual, taxYear = taxYear).url
  val manageObligationsShowForeignPropertyUrl: String = manageObligationsController
    .showAgentForeignProperty(changeTo = annual, taxYear = taxYear).url
  val manageObligationsShowSelfEmploymentUrl: String = manageObligationsController
    .showAgentSelfEmployment(id = testSelfEmploymentId,changeTo = annual, taxYear = taxYear).url

  val prefix: String = "incomeSources.manage.propertyReportingMethod"

  val continueButtonText: String = messagesAPI("base.confirm-and-continue")

  val pageTitle = messagesAPI(s"$prefix.heading.annual")

  s"calling GET $confirmReportingMethodShowUKPropertyUrl" should {
    "render the Confirm Reporting Method page" when {
      "all query parameters are valid" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $confirmReportingMethodShowUKPropertyUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.getConfirmUKPropertyReportingMethod(taxYear, annual, clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle),
          elementTextByID("confirm-and-continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $confirmReportingMethodShowForeignPropertyUrl" should {
    "render the Confirm Reporting Method page" when {
      "all query parameters are valid" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $confirmReportingMethodShowForeignPropertyUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.getConfirmForeignPropertyReportingMethod(taxYear, annual, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle),
          elementTextByID("confirm-and-continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $confirmReportingMethodShowSoleTraderBusinessUrl" should {
    "render the Confirm Reporting Method page" when {
      "all query parameters are valid" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.getConfirmSoleTraderBusinessReportingMethod(taxYear, annual, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle),
          elementTextByID("confirm-and-continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $confirmReportingMethodShowSoleTraderBusinessUrl" should {
    "redirect to home page" when {
      "Income Sources FS is Disabled" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is disabled")
        disable(IncomeSources)

        When(s"I call GET $confirmReportingMethodShowSoleTraderBusinessUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.getConfirmSoleTraderBusinessReportingMethod(taxYear, annual, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.showAgent.url)
        )
      }
    }
  }

  s"calling POST $confirmReportingMethodSubmitUKPropertyUrl" should {
    s"redirect to $manageObligationsShowUKPropertyUrl" when {
      "called with a valid form" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.postConfirmUKPropertyReportingMethod(taxYear, annual, clientDetailsWithConfirmation)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(manageObligationsShowUKPropertyUrl)
        )
      }
    }
  }

  s"calling POST $confirmReportingMethodSubmitForeignPropertyUrl" should {
    s"redirect to $manageObligationsShowForeignPropertyUrl" when {
      "called with a valid form" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.postConfirmForeignPropertyReportingMethod(taxYear, annual, clientDetailsWithConfirmation)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(manageObligationsShowForeignPropertyUrl)
        )
      }
    }
  }

  s"calling POST $confirmReportingMethodSubmitSoleTraderBusinessUrl" should {
    s"redirect to $manageObligationsShowSelfEmploymentUrl" when {
      "called with a valid form" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.postConfirmSoleTraderBusinessReportingMethod(taxYear, annual, clientDetailsWithConfirmation)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(manageObligationsShowSelfEmploymentUrl)
        )
      }
    }
  }

  s"calling POST $confirmReportingMethodSubmitSoleTraderBusinessUrl" should {
    s"return ${Status.BAD_REQUEST}" when {
      "called with a invalid form" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.postConfirmSoleTraderBusinessReportingMethod(taxYear, annual, clientDetailsWithConfirmation)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))
        )

        result should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }

  s"calling POST $confirmReportingMethodSubmitSoleTraderBusinessUrl" should {
    "redirect to home page" when {
      "Income Sources FS is disabled" in {

        stubAuthorisedAgentUser(authorised = true)

        disable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.postConfirmSoleTraderBusinessReportingMethod(taxYear, annual, clientDetailsWithConfirmation)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.showAgent.url)
        )
      }
    }
  }

  s"calling POST $confirmReportingMethodSubmitSoleTraderBusinessUrl" should {
    "redirect to the Sole Trader Business Reporting Method Change Error Page" when {
      "API 1771 returns an Error response" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing response")))

        val result = IncomeTaxViewChangeFrontend.postConfirmSoleTraderBusinessReportingMethod(taxYear, annual, clientDetailsWithConfirmation)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.incomeSources.manage.routes.ReportingMethodChangeErrorController.show(Some(testSelfEmploymentId), SelfEmployment, isAgent = true).url)
        )
      }
    }
  }

  s"calling POST $confirmReportingMethodSubmitForeignPropertyUrl" should {
    "redirect to the Foreign Property Reporting Method Change Error Page" when {
      "API 1771 returns an Error response" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing response")))

        val result = IncomeTaxViewChangeFrontend.postConfirmForeignPropertyReportingMethod(taxYear, annual, clientDetailsWithConfirmation)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.incomeSources.manage.routes.ReportingMethodChangeErrorController.show(None, ForeignProperty, isAgent = true).url)
        )
      }
    }
  }

  s"calling POST $confirmReportingMethodSubmitUKPropertyUrl" should {
    "redirect to the UK Property Reporting Method Change Error Page" when {
      "API 1771 returns an Error response" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing response")))

        val result = IncomeTaxViewChangeFrontend.postConfirmUKPropertyReportingMethod(taxYear, annual, clientDetailsWithConfirmation)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.incomeSources.manage.routes.ReportingMethodChangeErrorController.show(None, UkProperty, isAgent = true).url)
        )
      }
    }
  }
}
