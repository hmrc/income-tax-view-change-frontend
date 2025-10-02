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

package controllers.optIn.oldJourney

import controllers.ControllerISpecHelper
import enums.JourneyType.{Opt, OptInJourney}
import enums.MTDIndividual
import forms.optIn.SingleTaxYearOptInWarningForm
import helpers.WiremockHelper
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDIndividualAuthStub}
import models.UIJourneySessionData
import models.admin.{NavBarFs, ReportingFrequencyPage, SignUpFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, Voluntary}
import models.optin.{OptInContextData, OptInSessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import repositories.ITSAStatusRepositorySupport.statusToString
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.ITSAStatusTestConstants.successITSAStatusResponseJson
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

import scala.concurrent.Future

class SingleTaxYearOptInWarningControllerISpec extends ControllerISpecHelper {

  private val isAgent: Boolean = false

  private val confirmTaxYearPage = controllers.optIn.oldJourney.routes.ConfirmTaxYearController.show(isAgent).url

  private val optInCancelledPageUrl = controllers.optIn.oldJourney.routes.OptInCancelledController.show().url

  private val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  private def getPageForm(value: String): Map[String, Seq[String]] =
    Map(
      SingleTaxYearOptInWarningForm.choiceField -> Seq(value)
    )

  private def setupOptInSessionData(
                                     currentTaxYear: TaxYear,
                                     currentYearStatus: ITSAStatus.Value,
                                     nextYearStatus: ITSAStatus.Value,
                                     intent: TaxYear
                                   ): Future[Boolean] = {
    repository.set(
      UIJourneySessionData(testSessionId,
        Opt(OptInJourney).toString,
        optInSessionData =
          Some(OptInSessionData(
            optInContextData = Some(OptInContextData(
              currentTaxYear = currentTaxYear.toString,
              currentYearITSAStatus = statusToString(status = currentYearStatus),
              nextYearITSAStatus = statusToString(status = nextYearStatus)
            )),
            selectedOptInYear = Some(intent.toString))))
    )
  }

  private val path = "/opt-in/single-taxyear-warning"

  private val validYesForm = getPageForm("true")
  private val validNoForm = getPageForm("false")
  private val inValidForm = getPageForm("")

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  s"GET - /opt-in/single-taxyear-warning - OK (200)" should {

    "render SingleYearOptInWarning page" when {

      "user is authorised" in {
        enable(ReportingFrequencyPage, SignUpFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val responseBody = Json.arr(successITSAStatusResponseJson)
        val url = s"/income-tax-view-change/itsa-status/status/AA123456A/21-22?futureYears=true&history=false"

        val currentTaxYear: TaxYear = TaxYear(2020, 2021)

        val intent = currentTaxYear
        setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Voluntary, intent).futureValue shouldBe true

        WiremockHelper.stubGet(url, OK, responseBody.toString())

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val result = buildGETMTDClient(path).futureValue
        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleCustom("Voluntarily opting in to reporting quarterly for a single tax year - Manage your Self Assessment - GOV.UK")
        )
      }
    }

    testAuthFailures(path, MTDIndividual)
  }

  s"POST - /opt-in/single-taxyear-warning" should {

    s"render SingleYearOptInWarning page with error summary - BAD_REQUEST (400)" when {

      "user answers with invalid data" in {
        enable(ReportingFrequencyPage, SignUpFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val responseBody = Json.arr(successITSAStatusResponseJson)
        val url = s"/income-tax-view-change/itsa-status/status/AA123456A/21-22?futureYears=true&history=false"

        WiremockHelper.stubGet(url, OK, responseBody.toString())

        val currentTaxYear: TaxYear = TaxYear(2020, 2021)

        val intent = currentTaxYear
        setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Voluntary, intent).futureValue shouldBe true

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val result = buildPOSTMTDPostClient(path, body = inValidForm).futureValue
        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleCustom("Voluntarily opting in to reporting quarterly for a single tax year - Manage your Self Assessment - GOV.UK")
        )

      }
    }
    s"redirect to ConfirmTaxYear page with status - SEE_OTHER (303)" when {

      "user answers Yes" in {
        enable(ReportingFrequencyPage, SignUpFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val responseBody = Json.arr(successITSAStatusResponseJson)
        val url = s"/income-tax-view-change/itsa-status/status/AA123456A/21-22?futureYears=true&history=false"

        WiremockHelper.stubGet(url, OK, responseBody.toString())

        val currentTaxYear: TaxYear = TaxYear(2020, 2021)

        val intent = currentTaxYear
        setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Voluntary, intent).futureValue shouldBe true

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val result = buildPOSTMTDPostClient(path, body = validYesForm).futureValue
        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(confirmTaxYearPage)
        )
      }
    }

    s"redirect to Home page - with status - SEE_OTHER (303)" when {

      "user answers No" in {
        enable(ReportingFrequencyPage, SignUpFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val responseBody = Json.arr(successITSAStatusResponseJson)
        val url = s"/income-tax-view-change/itsa-status/status/AA123456A/21-22?futureYears=true&history=false"

        WiremockHelper.stubGet(url, OK, responseBody.toString())

        val currentTaxYear: TaxYear = TaxYear(2020, 2021)

        val intent = currentTaxYear
        setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Voluntary, intent).futureValue shouldBe true

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val result = buildPOSTMTDPostClient(path, body = validNoForm).futureValue

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(optInCancelledPageUrl)
        )
      }
    }
  }
}
