/*
 * Copyright 2017 HM Revenue & Customs
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

import models.{ErrorResponse, EstimatedTaxLiability, EstimatedTaxLiabilityError, SuccessResponse}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.TestSupport


class EstimatedTaxLiabilityServiceSpec extends TestSupport with MockEstimatedTaxLiabilityConnector {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mtditid = "1234"

  object TestEstimatedTaxLiabilityService extends EstimatedTaxLiabilityService(mockEstimatedTaxLiabilityConnector)

  "The EstimatedTaxLiabilityService.getEstimatedTaxLiability method" when {

    "a successful response is returned from the EstimatedTaxLiabilityConnector" should {

      val estimatedTaxLiabilityResponse = SuccessResponse(Json.parse(
        """
          |{
          |  "total":"1000",
          |  "nic2":"200",
          |  "nic4":"500",
          |  "incomeTax":"300"
          |}
        """.stripMargin
      ))

      "return a correctly formatted EstimateTaxLiability model" in {
        setupMockFinancialDataResult(mtditid)(estimatedTaxLiabilityResponse)
        await(TestEstimatedTaxLiabilityService.getEstimatedTaxLiability(mtditid)) shouldBe EstimatedTaxLiability(1000.0,200.0,500.0,300.0)
      }
    }

    "an Error Response is returned from the FinancialDataConnector" should {

      "return a correctly formatted EstimateTaxLiability model" in {
        val financialDataError = ErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error Message")
        setupMockFinancialDataResult(mtditid)(financialDataError)
        await(TestEstimatedTaxLiabilityService.getEstimatedTaxLiability(mtditid)) shouldBe
          EstimatedTaxLiabilityError(Status.INTERNAL_SERVER_ERROR, "Error Message")
      }
    }
  }
}
