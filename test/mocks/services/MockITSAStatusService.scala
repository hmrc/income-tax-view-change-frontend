/*
 * Copyright 2024 HM Revenue & Customs
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

import models.incomeSourceDetails.{LatencyDetails, LatencyYearsAnnual, LatencyYearsQuarterly, LatencyYearsQuarterlyAndAnnualStatus, TaxYear}
import models.itsaStatus.StatusDetail
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import services.ITSAStatusService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockITSAStatusService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockITSAStatusService: ITSAStatusService = mock(classOf[ITSAStatusService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockITSAStatusService)
  }

  def setupMockGetStatusTillAvailableFutureYears(taxYear: TaxYear)(out: Future[Map[TaxYear, StatusDetail]]): Unit = {
    when(mockITSAStatusService.getStatusTillAvailableFutureYears(ArgumentMatchers.eq(taxYear))(any, any, any))
      .thenReturn(out)
  }

  def setupMockHasMandatedOrVoluntaryStatusCurrentYear(response: Boolean): Unit = {
    when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(
      ArgumentMatchers.any[StatusDetail => Boolean]())(any(), any(), any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockHasMandatedOrVoluntaryStatusCurrentYearDefaultParam(response: Boolean): Unit = {
    when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(ArgumentMatchers.eq(_.isMandatedOrVoluntary))(any(), any(), any())).thenReturn(Future.successful(response))
  }

  def setupMockLatencyYearsQuarterlyAndAnnualStatus(taxYear1Status: Boolean, taxYear2Status: Boolean): Unit = {
    when(mockITSAStatusService.latencyYearsQuarterlyAndAnnualStatus(any[Option[LatencyDetails]])(any(), any(), any()))
      .thenReturn(Future.successful(
        LatencyYearsQuarterlyAndAnnualStatus(LatencyYearsQuarterly(Some(taxYear1Status), Some(taxYear2Status)),
          LatencyYearsAnnual(Some(!taxYear1Status), Some(!taxYear2Status)))))
  }

}
