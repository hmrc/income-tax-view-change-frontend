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
import mocks.connectors.MockIncomeTaxCalculationConnector
import models.liabilitycalculation._
import play.api.http.Status
import testConstants.BaseTestConstants._
import testUtils.TestSupport

import java.time.LocalDate

class CalculationServiceSpec extends TestSupport with MockIncomeTaxCalculationConnector with FeatureSwitching {

  val liabilityCalculationSuccessResponse: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = None
    )),
    messages = None,
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "IY",
      calculationReason = "customerRequest",
      periodFrom = LocalDate.of(2018, 1, 1),
      periodTo = LocalDate.of(2019, 1, 1)
    ),
    calculation = None)

  val liabilityCalculationNoContentResponse: LiabilityCalculationError = LiabilityCalculationError(Status.NO_CONTENT, "not found")
  val liabilityCalculationErrorResponse: LiabilityCalculationError = LiabilityCalculationError(Status.INTERNAL_SERVER_ERROR, "Internal server error")


  object TestCalculationService extends CalculationService(mockIncomeTaxCalculationConnector)

  "The CalculationService.getCalculationDetail method" when {

    "when the  Calculation Data api feature switch is enabled" should {
      "successful response is returned from the IncomeTaxCalculationConnector" should {

        "return a LiabilityCalculationModel" in {
          mockGetCalculationResponse(testMtditid, testNino, "2018")(liabilityCalculationSuccessResponse)

          TestCalculationService.getLiabilityCalculationDetail(testMtditid, testNino, testTaxYear).futureValue shouldBe liabilityCalculationSuccessResponse

        }
        "NO_CONTENT response is returned from the IncomeTaxCalculationConnector" should {
          "return a LiabilityCalculationError" in {
            mockGetCalculationResponse(testMtditid, testNino, "2018")(liabilityCalculationNoContentResponse)

            TestCalculationService.getLiabilityCalculationDetail(testMtditid, testNino, testTaxYear).futureValue shouldBe liabilityCalculationNoContentResponse
          }
        }
        "error response is returned from the IncomeTaxCalculationConnector" should {
          "return a LiabilityCalculationError" in {
            mockGetCalculationResponse(testMtditid, testNino, "2018")(liabilityCalculationErrorResponse)

            TestCalculationService.getLiabilityCalculationDetail(testMtditid, testNino, testTaxYear).futureValue shouldBe liabilityCalculationErrorResponse
          }
        }
      }
    }
  }
}
