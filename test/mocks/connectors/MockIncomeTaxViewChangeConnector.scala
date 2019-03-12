/*
 * Copyright 2019 HM Revenue & Customs
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

package mocks.connectors

import connectors.IncomeTaxViewChangeConnector
import models.calculation.{CalculationResponseModel, LastTaxCalculationResponseModel}
import models.core.NinoResponse
import models.incomeSourceDetails.IncomeSourceDetailsResponse
import models.reportDeadlines.ReportDeadlinesResponseModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

trait MockIncomeTaxViewChangeConnector extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockIncomeTaxViewChangeConnector: IncomeTaxViewChangeConnector = mock[IncomeTaxViewChangeConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeTaxViewChangeConnector)
  }

  def setUpLatestCalculationResponse(nino: String, taxYear: Int)(response: CalculationResponseModel): Unit =
    when(mockIncomeTaxViewChangeConnector
      .getLatestCalculation(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(taxYear))(ArgumentMatchers.any()))
    .thenReturn(Future.successful(response))

  def setupMockIncomeSourceDetailsResponse(mtditid: String, nino: String)(response: IncomeSourceDetailsResponse): Unit =
    when(mockIncomeTaxViewChangeConnector.getIncomeSources(
      ArgumentMatchers.eq(mtditid),
      ArgumentMatchers.eq(nino)
    )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def setupLastTaxCalculationResponse(nino: String, year: Int)(response: LastTaxCalculationResponseModel): Unit =
    when(mockIncomeTaxViewChangeConnector
      .getLastEstimatedTax(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(year))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def setupNinoLookupResponse(mtdRef: String)(response: NinoResponse): Unit =
    when(mockIncomeTaxViewChangeConnector
      .getNino(
        ArgumentMatchers.eq(mtdRef))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def setupMockReportDeadlines(incomeSourceId: String)(response: ReportDeadlinesResponseModel): Unit = {
    when(mockIncomeTaxViewChangeConnector.getReportDeadlines(ArgumentMatchers.eq(incomeSourceId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

}
