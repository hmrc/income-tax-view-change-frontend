/*
 * Copyright 2021 HM Revenue & Customs
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
import models.core.NinoResponse
import models.financialDetails.{FinancialDetailsResponseModel, PaymentsResponse}
import models.incomeSourceDetails.IncomeSourceDetailsResponse
import models.reportDeadlines.ReportDeadlinesResponseModel
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

import java.time.LocalDate
import scala.concurrent.Future

trait MockIncomeTaxViewChangeConnector extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockIncomeTaxViewChangeConnector: IncomeTaxViewChangeConnector = mock[IncomeTaxViewChangeConnector]

  val testFromDate = LocalDate.of(2018, 4, 6)
  val testToDate = LocalDate.of(2018, 4, 5)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeTaxViewChangeConnector)
  }

  def setupMockIncomeSourceDetailsResponse(mtditid: String, nino: String,
                                           saUtr: Option[String], credId: Option[String],
                                           userType: Option[String])(response: IncomeSourceDetailsResponse): Unit =
    when(mockIncomeTaxViewChangeConnector.getIncomeSources()(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def setupNinoLookupResponse(mtdRef: String)(response: NinoResponse): Unit =
    when(mockIncomeTaxViewChangeConnector
      .getNino(
        ArgumentMatchers.eq(mtdRef))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def setupMockReportDeadlines(response: ReportDeadlinesResponseModel): Unit = {
    when(mockIncomeTaxViewChangeConnector.getReportDeadlines()(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockPreviousObligations(response: ReportDeadlinesResponseModel): Unit = {
    when(mockIncomeTaxViewChangeConnector.getPreviousObligations()(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockPreviousObligationsWithDates(from: LocalDate, to: LocalDate)(response: ReportDeadlinesResponseModel): Unit = {
    when(mockIncomeTaxViewChangeConnector.getPreviousObligations(
      fromDate = matches(from), toDate = matches(to))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockGetFinancialDetails(taxYear: Int, nino: String)(response: FinancialDetailsResponseModel): Unit = {
    when(mockIncomeTaxViewChangeConnector.getFinancialDetails(ArgumentMatchers.eq(taxYear), ArgumentMatchers.eq(nino))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def setupBusinessDetails(nino: String)(response: Future[IncomeSourceDetailsResponse]): Unit = {
    when(mockIncomeTaxViewChangeConnector.getBusinessDetails(ArgumentMatchers.eq(nino))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def setupGetPayments(taxYear: Int)(response: PaymentsResponse): Unit = {
    when(mockIncomeTaxViewChangeConnector.getPayments(ArgumentMatchers.eq(taxYear))(any(), any()))
      .thenReturn(Future.successful(response))
  }

}
