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

package controllers.incomeSources.manage

import audit.models.SwitchReportingMethodAuditModel
import auth.MtdItUser
import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.incomeSources.manage.ConfirmReportingMethodForm
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.data.FormError
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.mvc.Http.Status
import testConstants.BaseIntegrationTestConstants.{credId, testMtditid, testNino, testPropertyIncomeId, testSaUtr, testSelfEmploymentId}
import testConstants.BusinessDetailsIntegrationTestConstants.{business1, business2, business3}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, multipleBusinessesWithBothPropertiesAndCeasedBusiness, ukPropertyOnlyResponse}
import testConstants.PropertyDetailsIntegrationTestConstants.{foreignProperty, ukProperty}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

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
    .show(None, taxYear = testPropertyIncomeId, changeTo = annual, incomeSourceType = UkProperty, isAgent = false).url
  val confirmReportingMethodShowForeignPropertyUrl: String = confirmReportingMethodSharedController
    .show(None, taxYear = testPropertyIncomeId, changeTo = annual, incomeSourceType = ForeignProperty, isAgent = false).url
  val confirmReportingMethodShowSoleTraderBusinessUrl: String = confirmReportingMethodSharedController
    .show(id = Some(testSelfEmploymentId), taxYear = taxYear, changeTo = annual, incomeSourceType = SelfEmployment, isAgent = false).url

  val confirmReportingMethodSubmitUKPropertyUrl: String = confirmReportingMethodSharedController
    .submit(id = testPropertyIncomeId, taxYear = taxYear, changeTo = annual, incomeSourceType = UkProperty, isAgent = false).url
  val confirmReportingMethodSubmitForeignPropertyUrl: String = confirmReportingMethodSharedController
    .submit(id = testPropertyIncomeId, taxYear = taxYear, changeTo = annual, incomeSourceType = ForeignProperty, isAgent = false).url
  val confirmReportingMethodSubmitSoleTraderBusinessUrl: String = confirmReportingMethodSharedController
    .submit(id = testSelfEmploymentId, taxYear = taxYear, changeTo = annual, incomeSourceType = SelfEmployment, isAgent = false).url

  val manageObligationsShowUKPropertyUrl: String = manageObligationsController
    .showUKProperty(changeTo = annual, taxYear = taxYear).url
  val manageObligationsShowForeignPropertyUrl: String = manageObligationsController
    .showForeignProperty(changeTo = annual, taxYear = taxYear).url
  val manageObligationsShowSelfEmploymentUrl: String = manageObligationsController
    .showSelfEmployment(id = testSelfEmploymentId,changeTo = annual, taxYear = taxYear).url

  val prefix: String = "incomeSources.manage.propertyReportingMethod"

  val continueButtonText: String = messagesAPI("base.confirm-this-change")

  val pageTitle = messagesAPI(s"$prefix.heading.annual")

  s"calling GET $confirmReportingMethodShowUKPropertyUrl" should {
    "render the Confirm Reporting Method page" when {
      "all query parameters are valid" in {

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $confirmReportingMethodShowUKPropertyUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.getConfirmUKPropertyReportingMethod(taxYear, annual)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitle),
          elementTextByID("confirm-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $confirmReportingMethodShowForeignPropertyUrl" should {
    "render the Confirm Reporting Method page" when {
      "all query parameters are valid" in {

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $confirmReportingMethodShowForeignPropertyUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.getConfirmForeignPropertyReportingMethod(taxYear, annual)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitle),
          elementTextByID("confirm-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $confirmReportingMethodShowSoleTraderBusinessUrl" should {
    "render the Confirm Reporting Method page" when {
      "all query parameters are valid" in {

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.getConfirmSoleTraderBusinessReportingMethod(taxYear, annual)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitle),
          elementTextByID("confirm-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $confirmReportingMethodShowSoleTraderBusinessUrl" should {
    "redirect to home page" when {
      "Income Sources FS is Disabled" in {

        Given("Income Sources FS is disabled")
        disable(IncomeSources)

        When(s"I call GET $confirmReportingMethodShowSoleTraderBusinessUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.getConfirmSoleTraderBusinessReportingMethod(taxYear, annual)
        verifyIncomeSourceDetailsCall(testMtditid)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.show().url)
        )
      }
    }
  }

  s"calling GET $confirmReportingMethodShowSoleTraderBusinessUrl" should {
    "trigger the audit event" when {
      "Income Sources FS is Enabled" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        IncomeTaxViewChangeFrontend.getConfirmSoleTraderBusinessReportingMethod(taxYear, annual)
        verifyIncomeSourceDetailsCall(testMtditid)

        AuditStub
          .verifyAuditEvent(
            SwitchReportingMethodAuditModel(
              journeyType = SelfEmployment.journeyType,
              reportingMethodChangeTo = annual,
              taxYear = taxYear,
              errorMessage = None
            )(
              MtdItUser(
                mtditid = testMtditid,
                nino = testNino,
                userName = None,
                incomeSources = IncomeSourceDetailsModel(
                  mtdbsa = testMtditid,
                  yearOfMigration = None,
                  businesses = List(business1, business2, business3),
                  properties = List(ukProperty, foreignProperty)
                ),
                btaNavPartial = None,
                saUtr = Some(testSaUtr),
                credId = Some(credId),
                userType = Some(Individual),
                arn = None
              )(
                FakeRequest()
              )
            )
          )
        }
      }
    }

  s"calling POST $confirmReportingMethodSubmitUKPropertyUrl" should {
    s"redirect to $manageObligationsShowUKPropertyUrl" when {
      "called with a valid form" in {

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.postConfirmUKPropertyReportingMethod(taxYear, annual)(
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

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.postConfirmForeignPropertyReportingMethod(taxYear, annual)(
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

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.postConfirmSoleTraderBusinessReportingMethod(taxYear, annual)(
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

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.postConfirmSoleTraderBusinessReportingMethod(taxYear, annual)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))
        )

        result should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }

  s"calling POST $confirmReportingMethodSubmitSoleTraderBusinessUrl" should {
    "trigger the audit event" when {
      "called with an empty form" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        IncomeTaxViewChangeFrontend.postConfirmSoleTraderBusinessReportingMethod(taxYear, annual)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq())
        )

        AuditStub
          .verifyAuditEvent(
            SwitchReportingMethodAuditModel(
              journeyType = SelfEmployment.journeyType,
              reportingMethodChangeTo = annual,
              taxYear = taxYear,
              errorMessage = Some(messagesAPI(ConfirmReportingMethodForm.noSelectionError(annual)))
            )(
              MtdItUser(
                mtditid = testMtditid,
                nino = testNino,
                userName = None,
                incomeSources = IncomeSourceDetailsModel(
                  mtdbsa = testMtditid,
                  yearOfMigration = None,
                  businesses = List(business1, business2, business3),
                  properties = List(ukProperty, foreignProperty)
                ),
                btaNavPartial = None,
                saUtr = Some(testSaUtr),
                credId = Some(credId),
                userType = Some(Individual),
                arn = None
              )(
                FakeRequest()
              )
            )
          )
      }
    }
  }


  s"calling POST $confirmReportingMethodSubmitSoleTraderBusinessUrl" should {
    "redirect to home page" when {
      "Income Sources FS is disabled" in {

        disable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.postConfirmSoleTraderBusinessReportingMethod(taxYear, annual)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.show().url)
        )
      }
    }
  }
}
