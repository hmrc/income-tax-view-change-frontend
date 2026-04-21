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

package services

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import enums.TaxYearSummary.CalculationRecord.{LATEST, PREVIOUS}
import mocks.connectors.MockIncomeTaxCalculationConnector
import models.admin.FeatureSwitchName
import models.liabilitycalculation.*
import play.api.http.Status
import testConstants.BaseTestConstants.*
import testUtils.TestSupport

class CalculationServiceSpec extends TestSupport with MockIncomeTaxCalculationConnector with FeatureSwitching {

  val liabilityCalculationSuccessResponse: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = None
    )),
    messages = None,
    metadata = Metadata(Some("2019-02-15T09:35:15.094Z"), "inYear", Some("customerRequest")),
    calculation = None,
    submissionChannel = None
  )

  val calcResponseWithAmendmentCa = liabilityCalculationSuccessResponse.copy(metadata = Metadata(Some("2019-02-15T09:35:15.094Z"), "CA", Some("customerRequest")))
  val calcResponseWithAmendmentAm = liabilityCalculationSuccessResponse.copy(metadata = Metadata(Some("2019-02-15T09:35:15.094Z"), "AM", Some("customerRequest")))

  val liabilityCalculationNoContentResponse: LiabilityCalculationError = LiabilityCalculationError(Status.NO_CONTENT, "not found")
  val liabilityCalculationErrorResponse: LiabilityCalculationError = LiabilityCalculationError(Status.INTERNAL_SERVER_ERROR, "Internal server error")


  object TestCalculationServiceFeatureEnabled extends CalculationService(mockIncomeTaxCalculationConnector) {
    override def isEnabled(featureSwitch: FeatureSwitchName)(implicit user: MtdItUser[_]): Boolean = true
  }

  object TestCalculationServiceFeatureDisabled extends CalculationService(mockIncomeTaxCalculationConnector) {
    override def isEnabled(featureSwitch: FeatureSwitchName)(implicit user: MtdItUser[_]): Boolean = false
  }

  "The CalculationService.getCalculationDetail method" when {
    "when the Calculation Data api feature switch is enabled" should {
      "successful response is returned from the IncomeTaxCalculationConnector" should {
        "return a LiabilityCalculationModel" in {
          mockGetCalculationResponse(testMtditid, testNino, "2018", None)(liabilityCalculationSuccessResponse)

          TestCalculationServiceFeatureDisabled.getLiabilityCalculationDetail(testMtditid, testNino, testTaxYear).futureValue shouldBe liabilityCalculationSuccessResponse
        }
      }
    }
    "NO_CONTENT response is returned from the IncomeTaxCalculationConnector" should {
      "return a LiabilityCalculationError" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", None)(liabilityCalculationNoContentResponse)

        TestCalculationServiceFeatureDisabled.getLiabilityCalculationDetail(testMtditid, testNino, testTaxYear).futureValue shouldBe liabilityCalculationNoContentResponse
      }
    }
    "error response is returned from the IncomeTaxCalculationConnector" should {
      "return a LiabilityCalculationError" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", None)(liabilityCalculationErrorResponse)

        TestCalculationServiceFeatureDisabled.getLiabilityCalculationDetail(testMtditid, testNino, testTaxYear).futureValue shouldBe liabilityCalculationErrorResponse
      }
    }
  }

  "The CalculationService.getLatestAndPreviousCalculationDetails method" when {
    "sending a request with PFA Enabled" should {
      "return a successful response when there are no amendments returned" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationSuccessResponse)

        TestCalculationServiceFeatureEnabled.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe(liabilityCalculationSuccessResponse, None)
      }

      "return a successful response when the latest calculation is an amendment - Calc Type CA" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(calcResponseWithAmendmentCa)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(PREVIOUS))(liabilityCalculationSuccessResponse)

        TestCalculationServiceFeatureEnabled.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe(calcResponseWithAmendmentCa, Some(liabilityCalculationSuccessResponse))
      }

      "return a successful response when the latest calculation is an amendment - Calc Type AM" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(calcResponseWithAmendmentAm)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(PREVIOUS))(liabilityCalculationSuccessResponse)

        TestCalculationServiceFeatureEnabled.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe(calcResponseWithAmendmentAm, Some(liabilityCalculationSuccessResponse))
      }

      "return a LiabilityCalculationError when no calculation is returned" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationNoContentResponse)

        TestCalculationServiceFeatureEnabled.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe(liabilityCalculationNoContentResponse, None)
      }

      "return a LiabilityCalculationError when an error response is returned from the connector" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationErrorResponse)

        TestCalculationServiceFeatureEnabled.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe(liabilityCalculationErrorResponse, None)
      }
    }

    "sending a request with PFA Disabled" should {
      "return a successful response when there are no amendments returned" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationSuccessResponse)

        TestCalculationServiceFeatureDisabled.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe(liabilityCalculationSuccessResponse, None)
      }

      "return a successful response when the latest calculation is an amendment - Calc Type CA" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(calcResponseWithAmendmentCa)

        TestCalculationServiceFeatureDisabled.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe(calcResponseWithAmendmentCa, None)
      }

      "return a successful response when the latest calculation is an amendment - Calc Type AM" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(calcResponseWithAmendmentAm)

        TestCalculationServiceFeatureDisabled.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe(calcResponseWithAmendmentAm, None)
      }

      "return a LiabilityCalculationError when no calculation is returned" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationNoContentResponse)

        TestCalculationServiceFeatureDisabled.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe(liabilityCalculationNoContentResponse, None)
      }

      "return a LiabilityCalculationError when an error response is returned from the connector" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationErrorResponse)

        TestCalculationServiceFeatureDisabled.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe(liabilityCalculationErrorResponse, None)
      }
    }
  }

  "The CalculationService.getCalculationDetailsWithFlag method" when {
    "sending a request" should {
      "return a successful response when isPrevious is true" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(PREVIOUS))(liabilityCalculationSuccessResponse)

        TestCalculationServiceFeatureDisabled.getCalculationDetailsWithFlag(testMtditid, testNino, testTaxYear, isPrevious = true)
      }

      "return a successful response when isPrevious is false" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationSuccessResponse)

        TestCalculationServiceFeatureDisabled.getCalculationDetailsWithFlag(testMtditid, testNino, testTaxYear, isPrevious = false)
      }
    }
  }
}
