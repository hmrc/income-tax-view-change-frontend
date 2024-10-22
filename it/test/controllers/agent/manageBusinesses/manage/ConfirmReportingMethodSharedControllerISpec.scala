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

import auth.MtdItUser
import models.admin.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.Manage
import forms.incomeSources.manage.ConfirmReportingMethodForm
import helpers.agent.AgentComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.{LatencyDetails, ManageIncomeSourceData, UIJourneySessionData}
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.mvc.Http.Status
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate
import java.time.Month.APRIL

class ConfirmReportingMethodSharedControllerISpec extends AgentComponentSpecBase {

  val annual = "Annual"
  val quarterly = "Quarterly"
  val quarterlyIndicator: String = "Q"
  val annuallyIndicator: String = "A"
  val taxYear = "2023-2024"
  val timestamp = "2023-01-31T09:26:17Z"
  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd
  val taxYear1: Int = currentTaxYear
  val taxYear2: Int = currentTaxYear + 1
  val taxYear1YYtoYY: String = s"${(taxYear1 - 1).toString.takeRight(2)}-${taxYear1.toString.takeRight(2)}"
  val taxYear1YYYYtoYY: String = "20" + taxYear1YYtoYY
  val taxYearYYYYtoYYYY = s"${taxYear1 - 1}-$taxYear1"
  val lastDayOfCurrentTaxYear: LocalDate = LocalDate.of(currentTaxYear, APRIL, 5)
  val latencyDetails: LatencyDetails =
    LatencyDetails(
      latencyEndDate = lastDayOfCurrentTaxYear.plusYears(2),
      taxYear1 = taxYear1.toString,
      latencyIndicator1 = quarterlyIndicator,
      taxYear2 = taxYear2.toString,
      latencyIndicator2 = annuallyIndicator
    )

  private lazy val checkYourAnswersController = controllers.manageBusinesses.manage.routes
    .CheckYourAnswersController
  private lazy val confirmReportingMethodSharedController = controllers.manageBusinesses.manage.routes
    .ConfirmReportingMethodSharedController

  val confirmReportingMethodShowUKPropertyUrl: String = confirmReportingMethodSharedController
    .show(taxYear = testPropertyIncomeId, changeTo = annual, incomeSourceType = UkProperty, isAgent = true).url
  val confirmReportingMethodShowForeignPropertyUrl: String = confirmReportingMethodSharedController
    .show(taxYear = testPropertyIncomeId, changeTo = annual, incomeSourceType = ForeignProperty, isAgent = true).url
  val confirmReportingMethodShowSoleTraderBusinessUrl: String = confirmReportingMethodSharedController
    .show(taxYear = taxYear, changeTo = annual, incomeSourceType = SelfEmployment, isAgent = true).url

  val confirmReportingMethodSubmitUKPropertyUrl: String = confirmReportingMethodSharedController
    .submit(taxYear = taxYear, changeTo = annual, incomeSourceType = UkProperty, isAgent = true).url
  val confirmReportingMethodSubmitForeignPropertyUrl: String = confirmReportingMethodSharedController
    .submit(taxYear = taxYear, changeTo = annual, incomeSourceType = ForeignProperty, isAgent = true).url
  val confirmReportingMethodSubmitSoleTraderBusinessUrl: String = confirmReportingMethodSharedController
    .submit(taxYear = taxYear, changeTo = annual, incomeSourceType = SelfEmployment, isAgent = true).url

  val checkYourAnswersShowUKPropertyUrl: String = checkYourAnswersController
    .show(isAgent = true, UkProperty).url
  val checkYourAnswersShowForeignPropertyUrl: String = checkYourAnswersController
    .show(isAgent = true, ForeignProperty).url
  val checkYourAnswersShowSelfEmploymentUrl: String = checkYourAnswersController
    .show(isAgent = true, SelfEmployment).url

  val prefix: String = "incomeSources.manage.propertyReportingMethod"

  val continueButtonText: String = messagesAPI("base.confirm-this-change")

  val pageTitle = messagesAPI(s"$prefix.heading.annual")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), None, Some(Agent), None
  )(FakeRequest())

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Manage))
  }

  s"calling GET $confirmReportingMethodShowUKPropertyUrl" should {
    "render the Confirm Reporting Method page" when {
      "all query parameters are valid" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $confirmReportingMethodShowUKPropertyUrl")

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-UK",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testMtditid), Some(annual), Some(2024))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.getConfirmUKPropertyReportingMethod(taxYear, annual, clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle),
          elementTextByID("confirm-button")(continueButtonText)
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

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-FP",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testMtditid), Some(annual), Some(2024))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.getConfirmForeignPropertyReportingMethod(taxYear, annual, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle),
          elementTextByID("confirm-button")(continueButtonText)
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

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod(latencyDetails))

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.getConfirmSoleTraderBusinessReportingMethod(taxYear, annual, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle),
          elementTextByID("confirm-button")(continueButtonText)
        )
      }
    }
    "redirect to home page" when {
      "Income Sources FS is Disabled" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is disabled")
        disable(IncomeSources)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

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

  s"calling POST $confirmReportingMethodSubmitSoleTraderBusinessUrl" should {
    s"redirect to $checkYourAnswersShowSelfEmploymentUrl" when {
      "called with a valid form" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId), Some(annual), Some(2024))))))

        val result = IncomeTaxViewChangeFrontend.postConfirmSoleTraderBusinessReportingMethod(taxYear, annual, clientDetailsWithConfirmation)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersShowSelfEmploymentUrl)
        )
      }
    }
    s"return ${Status.BAD_REQUEST}" when {
      "called with a invalid form" in {

        stubAuthorisedAgentUser(authorised = true)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

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

    "redirect to home page" when {
      "Income Sources FS is disabled" in {

        stubAuthorisedAgentUser(authorised = true)

        disable(IncomeSources)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

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

  s"calling POST $confirmReportingMethodSubmitForeignPropertyUrl" should {
    s"redirect to $checkYourAnswersShowForeignPropertyUrl" when {
      "called with a valid form" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-FP",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testMtditid), Some(annual), Some(2024))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postConfirmForeignPropertyReportingMethod(taxYear, annual, clientDetailsWithConfirmation)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersShowForeignPropertyUrl)
        )
      }
    }
  }

  s"calling POST $confirmReportingMethodSubmitUKPropertyUrl" should {
    s"redirect to $checkYourAnswersShowUKPropertyUrl" when {
      "called with a valid form" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-UK",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testMtditid), Some(annual), Some(2024))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postConfirmUKPropertyReportingMethod(taxYear, annual, clientDetailsWithConfirmation)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersShowUKPropertyUrl)
        )
      }
    }
  }
}
