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
import testConstants.BusinessDetailsTestConstants.{testLatencyDetails3, testLatencyDetails4, testLatencyDetails5}
import testConstants.ITSAStatusTestConstants._
import testUtils.TestSupport

class ITSAStatusServiceSpec extends TestSupport with MockITSAStatusConnector {
  val mockDateService: DateService = mock(classOf[DateService])

  object TestITSAStatusService extends ITSAStatusService(mockITSAStatusConnector, mockDateService, appConfig)

  val taxYear = TaxYear.forYearEnd(2023)
  val taxYearEnd = taxYear.endYear
  val yearRange = taxYear.formatAsShortYearRange

  val taxYear2 = TaxYear.forYearEnd(2024)
  val secondYearRange = taxYear2.formatAsShortYearRange
  "hasMandatedOrVoluntaryStatusCurrentYear " when {

    "ITSAStatus is returned " should {
      "return True if the status is in [MTD Mandated, MTD Voluntary ] " in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Right(List(successITSAStatusResponseMTDMandatedModel)))
        TestITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(_.isMandatedOrVoluntary)(headerCarrier, ec, testMtdItUser).futureValue shouldBe true
      }
      "return False if the its NOT_FOUND " in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Right(List()))
        TestITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(_.isMandatedOrVoluntary)(headerCarrier, ec, testMtdItUser).futureValue shouldBe false
      }
      "return false" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Right(List(successITSAStatusResponseModel)))
        TestITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(_.isMandatedOrVoluntary)(headerCarrier, ec, testMtdItUser).futureValue shouldBe false
      }
    }
    "ITSAStatus connector return an error" should {
      "return a failed future with error" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Left(errorITSAStatusError))
        TestITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(_.isMandatedOrVoluntary)(headerCarrier, ec, testMtdItUser).failed.futureValue.getMessage shouldBe
          "Failed to retrieve ITSAStatus"
      }
    }
  }

  "get the ITSAStatus for each year from getStatusTillAvailableFutureYears" when {
    "the tax year contains a status" should {
      "return a Map of TaxYear and Status" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, true, false)(Right(successMultipleYearITSAStatusResponse))
        TestITSAStatusService.getStatusTillAvailableFutureYears(taxYear)(headerCarrier, ec, testMtdItUser).futureValue shouldBe yearToStatus
      }
    }

    "the tax years all have NO STATUS" should {
      "return a Map of TaxYear and Status" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, true, false)(Right(successMultipleYearITSAStatusWithUnknownResponse))
        TestITSAStatusService.getStatusTillAvailableFutureYears(taxYear)(headerCarrier, ec, testMtdItUser).futureValue shouldBe yearToUnknownStatus
      }
    }
  }


  "hasMandatedOrVoluntaryStatusForCurrentAndPreviousYear" when {

    "ITSAStatus is returned" should {

      "return (true, true) if both years have MTD Mandated or MTD Voluntary" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Right(List(successITSAStatusResponseMTDMandatedModel)))
        setupGetITSAStatusDetail(testNino, secondYearRange, false, false)(Right(List(successITSAStatusResponseMTDMandatedModel)))

        val result = TestITSAStatusService.hasMandatedOrVoluntaryStatusForLatencyYears(Some(testLatencyDetails3))(headerCarrier, ec, testMtdItUser).futureValue

        result shouldBe(true, true)
      }

      "return (true, true) if first year is MTD Mandated Or MTD Voluntary and second year has No Status (fall back to first year status)" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Right(List(successITSAStatusResponseMTDMandatedModel)))
        setupGetITSAStatusDetail(testNino, secondYearRange, false, false)(Right(List(successITSAStatusResponseModel)))


        val result = TestITSAStatusService.hasMandatedOrVoluntaryStatusForLatencyYears(Some(testLatencyDetails4))(headerCarrier, ec, testMtdItUser).futureValue
        result shouldBe(true, true)
      }

      "return (false, true) if only second year has MTD Mandated or MTD Voluntary" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Right(List(successITSAStatusResponseModel)))
        setupGetITSAStatusDetail(testNino, secondYearRange, false, false)(Right(List(successITSAStatusResponseMTDMandatedModel)))

        val result = TestITSAStatusService.hasMandatedOrVoluntaryStatusForLatencyYears(Some(testLatencyDetails3))(headerCarrier, ec, testMtdItUser).futureValue
        result shouldBe(false, true)
      }

      "return (false, false) if neither current nor previous years have MTD Mandated or MTD Voluntary" in {
        when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Right(List(successITSAStatusResponseModel)))
        setupGetITSAStatusDetail(testNino, secondYearRange, false, false)(Right(List(successITSAStatusResponseModel)))

        val result = TestITSAStatusService.hasMandatedOrVoluntaryStatusForLatencyYears(Some(testLatencyDetails5))(headerCarrier, ec, testMtdItUser).futureValue
        result shouldBe(false, false)
      }

    }
  }

}