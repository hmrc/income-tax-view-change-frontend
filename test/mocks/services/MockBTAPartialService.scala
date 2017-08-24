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

package mocks.services

import models.{IncomeSourcesModel, LastTaxCalculationResponseModel, ObligationsResponseModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import services.BTAPartialService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

trait MockBTAPartialService extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockBTAPartialService: BTAPartialService = mock[BTAPartialService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockBTAPartialService)
  }

  def setupMockGetObligations(nino: String, incomeSources: IncomeSourcesModel)(response: ObligationsResponseModel): Unit =
    when(mockBTAPartialService
      .getObligations(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(incomeSources)
      )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def setupMockGetEstimate(nino: String, taxYear: Int)(response: LastTaxCalculationResponseModel): Unit =
    when(mockBTAPartialService
      .getEstimate(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(taxYear)
      )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

}
