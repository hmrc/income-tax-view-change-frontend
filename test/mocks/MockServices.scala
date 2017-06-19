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

package mocks

import models.{LastTaxCalculationResponseModel, ObligationsResponseModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import services.{EstimatedTaxLiabilityService, ObligationsService}
import uk.gov.hmrc.play.test.UnitSpec
import assets.TestConstants.Estimates._
import assets.TestConstants.Estimates
import assets.TestConstants._

import scala.concurrent.Future

sealed trait MockServices extends UnitSpec with MockitoSugar with BeforeAndAfterEach

trait MockEstimatedLiabilityService extends MockServices {

  val mockEstimatedLiabilityService: EstimatedTaxLiabilityService = mock[EstimatedTaxLiabilityService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEstimatedLiabilityService)
  }

  def setupMockLastTaxCalculationResult(nino: String, year: String, calcType: String)(response: LastTaxCalculationResponseModel): Unit =
    when(mockEstimatedLiabilityService
      .getLastEstimatedTaxCalculation(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(year),
        ArgumentMatchers.eq(calcType)
      )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
}


trait MockObligationsService extends MockServices {

  val mockObligationsService: ObligationsService = mock[ObligationsService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockObligationsService)
  }

  def setupMockObligationsResult(nino: String)(response: ObligationsResponseModel): Unit = {
    when(mockObligationsService.getObligations(ArgumentMatchers.eq(nino))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }
}
