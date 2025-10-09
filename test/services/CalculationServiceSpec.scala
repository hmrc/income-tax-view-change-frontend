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

import config.featureswitch.FeatureSwitching
import enums.TaxYearSummary.CalculationRecord.{LATEST, PREVIOUS}
import mocks.connectors.MockIncomeTaxCalculationConnector
import models.admin.PostFinalisationAmendmentsR18
import models.liabilitycalculation._
import play.api.http.Status
import testConstants.BaseTestConstants._
import testUtils.TestSupport

class CalculationServiceSpec extends TestSupport with MockIncomeTaxCalculationConnector with FeatureSwitching {

  val liabilityCalculationSuccessResponse: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = None
    )),
    messages = None,
    metadata = Metadata(Some("2019-02-15T09:35:15.094Z"), "inYear", Some("customerRequest")),
    calculation = None)

  val calcResponseWithAmendmentCa = liabilityCalculationSuccessResponse.copy( metadata = Metadata(Some("2019-02-15T09:35:15.094Z"), "CA", Some("customerRequest")))
  val calcResponseWithAmendmentAm = liabilityCalculationSuccessResponse.copy( metadata = Metadata(Some("2019-02-15T09:35:15.094Z"), "AM", Some("customerRequest")))

  val liabilityCalculationNoContentResponse: LiabilityCalculationError = LiabilityCalculationError(Status.NO_CONTENT, "not found")
  val liabilityCalculationErrorResponse: LiabilityCalculationError = LiabilityCalculationError(Status.INTERNAL_SERVER_ERROR, "Internal server error")


  object TestCalculationService extends CalculationService(mockIncomeTaxCalculationConnector)

  "The CalculationService.getCalculationDetail method" when {
    "when the Calculation Data api feature switch is enabled" should {
      "successful response is returned from the IncomeTaxCalculationConnector" should {
        "return a LiabilityCalculationModel" in {
          mockGetCalculationResponse(testMtditid, testNino, "2018", None)(liabilityCalculationSuccessResponse)

          TestCalculationService.getLiabilityCalculationDetail(testMtditid, testNino, testTaxYear).futureValue shouldBe liabilityCalculationSuccessResponse
        }
      }
    }
    "NO_CONTENT response is returned from the IncomeTaxCalculationConnector" should {
      "return a LiabilityCalculationError" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", None)(liabilityCalculationNoContentResponse)

        TestCalculationService.getLiabilityCalculationDetail(testMtditid, testNino, testTaxYear).futureValue shouldBe liabilityCalculationNoContentResponse
      }
    }
    "error response is returned from the IncomeTaxCalculationConnector" should {
      "return a LiabilityCalculationError" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", None)(liabilityCalculationErrorResponse)

        TestCalculationService.getLiabilityCalculationDetail(testMtditid, testNino, testTaxYear).futureValue shouldBe liabilityCalculationErrorResponse
      }
    }
  }

  "The CalculationService.getLatestAndPreviousCalculationDetails method" when {
    "sending a request with PFA Enabled" should {
      "return a successful response when there are no amendments returned" in {
        enable(PostFinalisationAmendmentsR18)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationSuccessResponse)

        TestCalculationService.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe (liabilityCalculationSuccessResponse, None)
      }

      "return a successful response when the latest calculation is an amendment - Calc Type CA" in {
        enable(PostFinalisationAmendmentsR18)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(calcResponseWithAmendmentCa)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(PREVIOUS))(liabilityCalculationSuccessResponse)

        TestCalculationService.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe (calcResponseWithAmendmentCa, Some(liabilityCalculationSuccessResponse))
      }

      "return a successful response when the latest calculation is an amendment - Calc Type AM" in {
        enable(PostFinalisationAmendmentsR18)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(calcResponseWithAmendmentAm)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(PREVIOUS))(liabilityCalculationSuccessResponse)

        TestCalculationService.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe (calcResponseWithAmendmentAm, Some(liabilityCalculationSuccessResponse))
      }

      "return a LiabilityCalculationError when no calculation is returned" in {
        enable(PostFinalisationAmendmentsR18)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationNoContentResponse)

        TestCalculationService.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe (liabilityCalculationNoContentResponse, None)
      }

      "return a LiabilityCalculationError when an error response is returned from the connector" in {
        enable(PostFinalisationAmendmentsR18)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationErrorResponse)

        TestCalculationService.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe (liabilityCalculationErrorResponse, None)
      }
    }

    "sending a request with PFA Disabled" should {
      "return a successful response when there are no amendments returned" in {
        disable(PostFinalisationAmendmentsR18)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationSuccessResponse)

        TestCalculationService.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe (liabilityCalculationSuccessResponse, None)
      }

      "return a successful response when the latest calculation is an amendment - Calc Type CA" in {
        disable(PostFinalisationAmendmentsR18)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(calcResponseWithAmendmentCa)

        TestCalculationService.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe (calcResponseWithAmendmentCa, None)
      }

      "return a successful response when the latest calculation is an amendment - Calc Type AM" in {
        disable(PostFinalisationAmendmentsR18)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(calcResponseWithAmendmentAm)

        TestCalculationService.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe (calcResponseWithAmendmentAm, None)
      }

      "return a LiabilityCalculationError when no calculation is returned" in {
        disable(PostFinalisationAmendmentsR18)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationNoContentResponse)

        TestCalculationService.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe (liabilityCalculationNoContentResponse, None)
      }

      "return a LiabilityCalculationError when an error response is returned from the connector" in {
        disable(PostFinalisationAmendmentsR18)
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationErrorResponse)

        TestCalculationService.getLatestAndPreviousCalculationDetails(testMtditid, testNino, testTaxYear).futureValue shouldBe (liabilityCalculationErrorResponse, None)
      }
    }
  }

  "The CalculationService.getCalculationDetailsWithFlag method" when {
    "sending a request" should {
      "return a successful response when isPrevious is true" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(PREVIOUS))(liabilityCalculationSuccessResponse)

        TestCalculationService.getCalculationDetailsWithFlag(testMtditid, testNino, testTaxYear, isPrevious = true)
      }

      "return a successful response when isPrevious is false" in {
        mockGetCalculationResponse(testMtditid, testNino, "2018", Some(LATEST))(liabilityCalculationSuccessResponse)

        TestCalculationService.getCalculationDetailsWithFlag(testMtditid, testNino, testTaxYear, isPrevious = false)
      }
    }
  }
}
