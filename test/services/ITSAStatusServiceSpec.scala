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

import mocks.connectors.MockITSAStatusConnector
import models.incomeSourceDetails.TaxYear
import org.mockito.Mockito.{mock, when}
import testConstants.BaseTestConstants.{testMtdItUser, testNino}
import testConstants.BusinessDetailsTestConstants.testLatencyDetails3
import testConstants.ITSAStatusTestConstants._
import testUtils.TestSupport

class ITSAStatusServiceSpec extends TestSupport with MockITSAStatusConnector {
  val mockDateService: DateService = mock(classOf[DateService])

  object TestITSAStatusService extends ITSAStatusService(mockITSAStatusConnector, mockDateService, appConfig)

  val taxYear = TaxYear.forYearEnd(2023)
  val taxYearEnd = taxYear.endYear
  val yearRange = taxYear.formatTaxYearRange
  "hasMandatedOrVoluntaryStatusCurrentYear " when {

    "ITSAStatus is returned " should {
      "return True if the status is in [MTD Mandated, MTD Voluntary ] " in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Right(List(successITSAStatusResponseMTDMandatedModel)))
        TestITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(testMtdItUser, headerCarrier, ec).futureValue shouldBe true
      }
      "return false" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Right(List(successITSAStatusResponseModel)))
        TestITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(testMtdItUser, headerCarrier, ec).futureValue shouldBe false
      }
    }
    "ITSAStatus connector return an error" should {
      "return a failed future with error" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Left(errorITSAStatusError))
        TestITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(testMtdItUser, headerCarrier, ec).failed.futureValue.getMessage shouldBe
          "Failed to retrieve ITSAStatus"
      }
    }
  }

  "get the ITSAStatus for each year from getStatusTillAvailableFutureYears" when {
    "the tax year contains a status" should {
      "return a Map of TaxYear and Status" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, true, false)(Right(successMultipleYearITSAStatusResponse))
        TestITSAStatusService.getStatusTillAvailableFutureYears(taxYear)(testMtdItUser, headerCarrier, ec).futureValue shouldBe yearToStatus
      }
    }

    "the tax years all have NO STATUS" should {
      "return a Map of TaxYear and Status" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, true, false)(Right(successMultipleYearITSAStatusWithUnknownResponse))
        TestITSAStatusService.getStatusTillAvailableFutureYears(taxYear)(testMtdItUser, headerCarrier, ec).futureValue shouldBe yearToUnknownStatus
      }
    }
  }

  "hasMandatedOrVoluntaryStatusForCurrentAndPreviousYear" when {

    "ITSAStatus is returned" should {

      "return (true, true) if both current and previous years have MTD Mandated or MTD Voluntary" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, true, false)(Right(successMultipleYearMandatedITSAStatusResponse))

        TestITSAStatusService.hasMandatedOrVoluntaryStatusForLatencyYears(Some(testLatencyDetails3))(testMtdItUser, headerCarrier, ec).futureValue shouldBe yearToUnknownStatus
      }

      "return (true, false) if only current year has MTD Mandated or MTD Voluntary" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, true, false)(Right(currentYearMandatedPreviousYearNoStatusResponse))

        TestITSAStatusService.hasMandatedOrVoluntaryStatusForLatencyYears(Some(testLatencyDetails3))(testMtdItUser, headerCarrier, ec).futureValue shouldBe (true, false)
      }

      "return (false, true) if only previous year has MTD Mandated or MTD Voluntary" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, true, false)(Right(previousYearMandatedCurrentYearNoStatusResponse))

        TestITSAStatusService.hasMandatedOrVoluntaryStatusForLatencyYears(Some(testLatencyDetails3))(testMtdItUser, headerCarrier, ec).futureValue shouldBe (false, true)
      }

      "return (false, false) if neither current nor previous years have MTD Mandated or MTD Voluntary" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, true, false)(Right(bothYearsNoStatusResponse))

        TestITSAStatusService.hasMandatedOrVoluntaryStatusForLatencyYears(Some(testLatencyDetails3))(testMtdItUser, headerCarrier, ec).futureValue shouldBe (false, false)
      }
    }
  }

}