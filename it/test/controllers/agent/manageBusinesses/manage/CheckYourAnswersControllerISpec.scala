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

import audit.models.ManageIncomeSourceCheckYourAnswersAuditModel
import auth.MtdItUser
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{JourneyType, Manage}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.admin.IncomeSources
import models.incomeSourceDetails.{LatencyDetails, ManageIncomeSourceData, UIJourneySessionData}
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate
import java.time.Month.APRIL

class CheckYourAnswersControllerISpec extends ComponentSpecBase {

  val annual = "annual"
  val quarterly = "quarterly"
  val quarterlyIndicator: String = "Q"
  val annuallyIndicator: String = "A"
  val taxYear = "2024"
  val timestamp = "2023-01-31T09:26:17Z"
  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd
  val taxYear1: Int = currentTaxYear
  val taxYear2: Int = currentTaxYear + 1
  val lastDayOfCurrentTaxYear: LocalDate = LocalDate.of(currentTaxYear, APRIL, 5)
  val latencyDetails: LatencyDetails =
    LatencyDetails(
      latencyEndDate = lastDayOfCurrentTaxYear.plusYears(2),
      taxYear1 = taxYear1.toString,
      latencyIndicator1 = quarterlyIndicator,
      taxYear2 = taxYear2.toString,
      latencyIndicator2 = annuallyIndicator
    )

  private lazy val manageObligationsController = controllers.manageBusinesses.manage.routes
    .ManageObligationsController
  private lazy val checkYourAnswersController = controllers.manageBusinesses.manage.routes
    .CheckYourAnswersController

  val checkYourAnswersShowUKPropertyUrl: String = checkYourAnswersController
    .show(isAgent = true, incomeSourceType = UkProperty).url
  val checkYourAnswersShowForeignPropertyUrl: String = checkYourAnswersController
    .show(isAgent = true, incomeSourceType = ForeignProperty).url
  val checkYourAnswersShowSoleTraderBusinessUrl: String = checkYourAnswersController
    .show(isAgent = true, incomeSourceType = SelfEmployment).url

  val checkYourAnswersSubmitUKPropertyUrl: String = checkYourAnswersController
    .submit(isAgent = true, incomeSourceType = UkProperty).url
  val checkYourAnswersSubmitForeignPropertyUrl: String = checkYourAnswersController
    .submit(isAgent = true, incomeSourceType = ForeignProperty).url
  val checkYourAnswersSubmitSoleTraderBusinessUrl: String = checkYourAnswersController
    .submit(isAgent = true, incomeSourceType = SelfEmployment).url

  val manageObligationsShowUKPropertyUrl: String = manageObligationsController
    .show(true, UkProperty).url
  val manageObligationsShowForeignPropertyUrl: String = manageObligationsController
    .show(true, ForeignProperty).url
  val manageObligationsShowSelfEmploymentUrl: String = manageObligationsController
    .show(true, SelfEmployment).url

  val prefix: String = "manageBusinesses.check-answers"

  val continueButtonText: String = messagesAPI("manageBusinesses.check-answers.confirm")

  val pageTitle = messagesAPI(s"$prefix.text")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), None, Some(Agent), None
  )(FakeRequest())

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Manage, incomeSourceType).toString,
    manageIncomeSourceData = Some(ManageIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), reportingMethod = Some(annual), taxYear = Some(2024))))

  s"calling GET $checkYourAnswersShowUKPropertyUrl" should {
    "render the Check your answers page" when {
      "all session parameters are valid" in {

        Given("Income Sources FS is enabled")
        enableFs(IncomeSources)

        stubAuthorisedAgentUser(authorised = true)

        When(s"I call GET $checkYourAnswersShowUKPropertyUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

        val result = IncomeTaxViewChangeFrontend.getCheckYourAnswersUKProperty()

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $checkYourAnswersShowForeignPropertyUrl" should {
    "render the Check your answers page" when {
      "all session parameters are valid" in {

        Given("Income Sources FS is enabled")
        enableFs(IncomeSources)

        stubAuthorisedAgentUser(authorised = true)

        When(s"I call GET $checkYourAnswersShowForeignPropertyUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.getCheckYourAnswersForeignProperty()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $checkYourAnswersShowSoleTraderBusinessUrl" should {
    "render the Check your answers page" when {
      "all session parameters are valid" in {

        Given("Income Sources FS is enabled")
        enableFs(IncomeSources)

        stubAuthorisedAgentUser(authorised = true)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId), Some(annual), Some(taxYear.toInt), Some(false))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod(latencyDetails))

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontend.getCheckYourAnswersSoleTrader()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $checkYourAnswersSubmitUKPropertyUrl" should {
    s"redirect to $manageObligationsShowUKPropertyUrl" when {
      "submitted with valid session data" in {

        Given("Income Sources FS is enabled")
        enableFs(IncomeSources)

        stubAuthorisedAgentUser(authorised = true)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-UK",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testPropertyIncomeId), Some(annual), Some(taxYear.toInt), Some(false))))))

        val result = IncomeTaxViewChangeFrontend.postCheckYourAnswersUKProperty()

        AuditStub.verifyAuditContainsDetail(ManageIncomeSourceCheckYourAnswersAuditModel(true, UkProperty.journeyType, "MANAGE", "Annually", "2023-2024", "UK property")(testUser).detail)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(manageObligationsShowUKPropertyUrl)
        )
      }
    }
  }

  s"calling POST $checkYourAnswersSubmitForeignPropertyUrl" should {
    s"redirect to $manageObligationsShowForeignPropertyUrl" when {
      "submitted with valid session data" in {

        Given("Income Sources FS is enabled")
        enableFs(IncomeSources)

        stubAuthorisedAgentUser(authorised = true)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.postCheckYourAnswersForeignProperty()

        AuditStub.verifyAuditContainsDetail(ManageIncomeSourceCheckYourAnswersAuditModel(true, ForeignProperty.journeyType, "MANAGE", "Annually", "2023-2024", "Foreign property")(testUser).detail)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(manageObligationsShowForeignPropertyUrl)
        )
      }
    }
  }

  s"calling POST $checkYourAnswersSubmitSoleTraderBusinessUrl" should {
    s"redirect to $manageObligationsShowSelfEmploymentUrl" when {
      "submitted with valid session data" in {

        Given("Income Sources FS is enabled")
        enableFs(IncomeSources)

        stubAuthorisedAgentUser(authorised = true)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.postCheckYourAnswersSoleTrader()

        AuditStub.verifyAuditContainsDetail(ManageIncomeSourceCheckYourAnswersAuditModel(true, SelfEmployment.journeyType, "MANAGE", "Annually", "2023-2024", "business")(testUser).detail)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(manageObligationsShowSelfEmploymentUrl)
        )
      }
    }
  }
}
