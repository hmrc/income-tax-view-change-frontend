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

import assets.TestConstants.{Estimates, _}
import mocks.MockEstimatedTaxLiabilityConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.TestSupport

class EstimatedTaxLiabilityServiceSpec extends TestSupport with MockEstimatedTaxLiabilityConnector {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  object TestEstimatedTaxLiabilityService extends EstimatedTaxLiabilityService(mockEstimatedTaxLiabilityConnector)

  "The EstimatedTaxLiabilityService.getEstimatedTaxLiability method" when {

    "a successful response is returned from the EstimatedTaxLiabilityConnector" should {

      "return a correctly formatted EstimateTaxLiability model" in {
        setupGetEstimatedTaxLiabilityResult(testMtditid)(Estimates.successModel)
        await(TestEstimatedTaxLiabilityService.getEstimatedTaxLiability(testMtditid)) shouldBe Estimates.successModel
      }
    }

    "an Error Response is returned from the FinancialDataConnector" should {

      "return a correctly formatted EstimateTaxLiability model" in {
        setupGetEstimatedTaxLiabilityResult(testMtditid)(Estimates.errorModel)
        await(TestEstimatedTaxLiabilityService.getEstimatedTaxLiability(testMtditid)) shouldBe Estimates.errorModel
      }
    }
  }
}
