/*
 * Copyright 2022 HM Revenue & Customs
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

import testConstants.BaseTestConstants._
import testConstants.CalcBreakdownTestConstants._
import testConstants.EstimatesTestConstants._
import config.featureswitch.FeatureSwitching
import mocks.connectors.{MockIncomeTaxCalculationConnector, MockIndividualCalculationsConnector}
import models.calculation._
import models.liabilitycalculation.{Inputs, LiabilityCalculationError, LiabilityCalculationResponse, Metadata, PersonalInformation}
import play.api.http.Status
import testUtils.TestSupport

class CalculationServiceSpec extends TestSupport with MockIndividualCalculationsConnector with MockIncomeTaxCalculationConnector with FeatureSwitching {

  val liabilityCalculationSuccessResponse: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = None
    )),
    messages = None,
    metadata = Metadata("2019-02-15T09:35:15.094Z", false),
    calculation = None)

  val liabilityCalculationNotFoundResponse: LiabilityCalculationError = LiabilityCalculationError(Status.NOT_FOUND, "not found")
  val liabilityCalculationErrorResponse: LiabilityCalculationError = LiabilityCalculationError(Status.INTERNAL_SERVER_ERROR, "Internal server error")


  object TestCalculationService extends CalculationService(
    mockIndividualCalculationsConnector,
    mockIncomeTaxCalculationConnector,
    appConfig
  )

  "The CalculationService.getCalculationDetail method" when {

    "when the  Calculation Data api feature switch is enabled" should {

      "successful response is returned from the IndividualCalculationConnector" should {

        "return a CalculationModel" in {
          mockGetLatestCalculationId(testNino, "2017-18")(Right("testIdOne"))
          mockGetCalculation(testNino, "testIdOne")(calculationDataSuccessModel)

          TestCalculationService.getCalculationDetail(testNino, testYear).futureValue shouldBe calculationDisplaySuccessModel(calculationDataSuccessModel)

        }
        "NOT_FOUND response is returned from the IndividualCalculationConnector" should {
          "return a CalculationErrorModel" in {
            mockGetLatestCalculationId(testNino, "2017-18")(
              Left(CalculationErrorModel(Status.NOT_FOUND, "not found"))
            )

            TestCalculationService.getCalculationDetail(testNino, testYear).futureValue shouldBe CalcDisplayNoDataFound
          }
        }
        "error response is returned from the IndividualCalculationConnector" should {
          "return a CalculationErrorModel" in {
            mockGetLatestCalculationId(testNino, "2017-18")(
              Left(CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, "Internal server error"))
            )

            TestCalculationService.getCalculationDetail(testNino, testYear).futureValue shouldBe CalcDisplayError
          }
        }
      }
      "successful response is returned from the IncomeTaxCalculationConnector" should {

        "return a LiabilityCalculationModel" in {
          mockGetCalculationResponse(testMtditid, testNino, "2018")(liabilityCalculationSuccessResponse)

          TestCalculationService.getLiabilityCalculationDetail(testMtditid, testNino, testYear).futureValue shouldBe liabilityCalculationSuccessResponse

        }
        "NOT_FOUND response is returned from the IncomeTaxCalculationConnector" should {
          "return a LiabilityCalculationError" in {
            mockGetCalculationResponse(testMtditid, testNino, "2018")(liabilityCalculationNotFoundResponse)

            TestCalculationService.getLiabilityCalculationDetail(testMtditid, testNino, testYear).futureValue shouldBe liabilityCalculationNotFoundResponse
          }
        }
        "error response is returned from the IncomeTaxCalculationConnector" should {
          "return a LiabilityCalculationError" in {
            mockGetCalculationResponse(testMtditid, testNino, "2018")(liabilityCalculationErrorResponse)

            TestCalculationService.getLiabilityCalculationDetail(testMtditid, testNino, testYear).futureValue shouldBe liabilityCalculationErrorResponse
          }
        }
      }
    }
  }
}
