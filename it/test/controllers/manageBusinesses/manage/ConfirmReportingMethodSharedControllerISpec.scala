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

package controllers.manageBusinesses.manage

import audit.models.IncomeSourceReportingMethodAuditModel
import auth.MtdItUser
import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{JourneyType, Manage}
import forms.incomeSources.manage.ConfirmReportingMethodForm
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
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
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate
import java.time.Month.APRIL

class ConfirmReportingMethodSharedControllerISpec extends ComponentSpecBase {

  val annual = "Annual"
  val quarterly = "Quarterly"
  val quarterlyIndicator: String = "Q"
  val annuallyIndicator: String = "A"
  val taxYear = "2023-2024"
  val timestamp = "2023-01-31T09:26:17Z"
  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd()
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

  private lazy val manageObligationsController = controllers.manageBusinesses.manage.routes
    .ManageObligationsController
  private lazy val confirmReportingMethodSharedController = controllers.manageBusinesses.manage.routes
    .ConfirmReportingMethodSharedController

  val confirmReportingMethodShowUKPropertyUrl: String = confirmReportingMethodSharedController
    .show(taxYear = testPropertyIncomeId, changeTo = annual, incomeSourceType = UkProperty, isAgent = false).url
  val confirmReportingMethodShowForeignPropertyUrl: String = confirmReportingMethodSharedController
    .show(taxYear = testPropertyIncomeId, changeTo = annual, incomeSourceType = ForeignProperty, isAgent = false).url
  val confirmReportingMethodShowSoleTraderBusinessUrl: String = confirmReportingMethodSharedController
    .show(taxYear = taxYear, changeTo = annual, incomeSourceType = SelfEmployment, isAgent = false).url

  val confirmReportingMethodSubmitUKPropertyUrl: String = confirmReportingMethodSharedController
    .submit(taxYear = taxYear, changeTo = annual, incomeSourceType = UkProperty, isAgent = false).url
  val confirmReportingMethodSubmitForeignPropertyUrl: String = confirmReportingMethodSharedController
    .submit(taxYear = taxYear, changeTo = annual, incomeSourceType = ForeignProperty, isAgent = false).url
  val confirmReportingMethodSubmitSoleTraderBusinessUrl: String = confirmReportingMethodSharedController
    .submit(taxYear = taxYear, changeTo = annual, incomeSourceType = SelfEmployment, isAgent = false).url

  val manageObligationsShowUKPropertyUrl: String = manageObligationsController
    .showUKProperty(changeTo = annual, taxYear = taxYear).url
  val manageObligationsShowForeignPropertyUrl: String = manageObligationsController
    .showForeignProperty(changeTo = annual, taxYear = taxYear).url
  val manageObligationsShowSelfEmploymentUrl: String = manageObligationsController
    .showSelfEmployment(changeTo = annual, taxYear = taxYear).url

  val prefix: String = "incomeSources.manage.propertyReportingMethod"

  val continueButtonText: String = messagesAPI("base.confirm-this-change")

  val pageTitle = messagesAPI(s"$prefix.heading.annual")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Manage, incomeSourceType).toString,
    manageIncomeSourceData = Some(ManageIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), reportingMethod = Some(annual), taxYear = Some(2024))))

  s"calling GET $confirmReportingMethodShowUKPropertyUrl" should {
    "render the Confirm Reporting Method page" when {
      "all query parameters are valid" in {

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $confirmReportingMethodShowUKPropertyUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getConfirmUKPropertyReportingMethod(taxYear, annual)

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
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getConfirmForeignPropertyReportingMethod(taxYear, annual)
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

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod(latencyDetails))

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getConfirmSoleTraderBusinessReportingMethod(taxYear, annual)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitle),
          elementTextByID("confirm-button")(continueButtonText)
        )
      }
    }

    "redirect to home page" when {
      "Income Sources FS is Disabled" in {

        Given("Income Sources FS is disabled")
        disable(IncomeSources)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

        When(s"I call GET $confirmReportingMethodShowSoleTraderBusinessUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getConfirmSoleTraderBusinessReportingMethod(taxYear, annual)
        verifyIncomeSourceDetailsCall(testMtditid)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.show().url)
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

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmUKPropertyReportingMethod(taxYear, annual)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        AuditStub.verifyAuditContainsDetail(IncomeSourceReportingMethodAuditModel(true, UkProperty.journeyType, "MANAGE", "Annually", "2023-2024", "UK property")(testUser).detail)

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

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmForeignPropertyReportingMethod(taxYear, annual)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        AuditStub.verifyAuditContainsDetail(IncomeSourceReportingMethodAuditModel(true, ForeignProperty.journeyType, "MANAGE", "Annually", "2023-2024", "Foreign property")(testUser).detail)

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

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmSoleTraderBusinessReportingMethod(taxYear, annual)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))
        )

        AuditStub.verifyAuditContainsDetail(IncomeSourceReportingMethodAuditModel(true, SelfEmployment.journeyType, "MANAGE", "Annually", "2023-2024", "business")(testUser).detail)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(manageObligationsShowSelfEmploymentUrl)
        )
      }
    }
    s"return ${Status.BAD_REQUEST}" when {
      "called with a invalid form" in {

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmSoleTraderBusinessReportingMethod(taxYear, annual)(
          Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))
        )

        result should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }

    "redirect to home page" when {
      "Income Sources FS is disabled" in {

        disable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1776 updateTaxYearSpecific returns a success response")
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmSoleTraderBusinessReportingMethod(taxYear, annual)(
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
