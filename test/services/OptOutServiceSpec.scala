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

package services

import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService, MockOptOutConnector}
import models.incomeSourceDetails.TaxYear
import models.optOut.NextUpdatesQuarterlyReportingContentChecks
import services.optout.OptOutService
import testConstants.ITSAStatusTestConstants.yearToStatus
import testUtils.TestSupport

import scala.concurrent.Future

class OptOutServiceSpec extends TestSupport
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with MockOptOutConnector {

  val taxYear: TaxYear = TaxYear.forYearEnd(2021)
  val previousTaxYear: TaxYear = taxYear.addYears(-1)
  val crystallised: Boolean = true

  val error = new RuntimeException("Some Error")

  object TestOptOutService extends OptOutService(mockOptOutConnector, mockITSAStatusService, mockCalculationListService, mockDateService)

  "GetNextUpdatesQuarterlyReportingContentChecks" when {
    "ITSA Status from CY-1 till future years and Calculation State for CY-1 is available" should {
      "return NextUpdatesQuarterlyReportingContentCheck" in {
        setupMockIsTaxYearCrystallisedCall(previousTaxYear.endYear)(Future.successful(Some(crystallised)))
        setupMockGetStatusTillAvailableFutureYears(previousTaxYear)(Future.successful(yearToStatus))
        setupMockGetCurrentTaxYearEnd(taxYear.endYear)

        val expected = NextUpdatesQuarterlyReportingContentChecks(
          currentYearItsaStatus = true,
          previousYearItsaStatus = false,
          previousYearCrystallisedStatus = Some(crystallised))

        TestOptOutService.getNextUpdatesQuarterlyReportingContentChecks.futureValue shouldBe expected
      }
    }

    "Calculation State for CY-1 is unavailable" should {
      "return NextUpdatesQuarterlyReportingContentCheck" in {
        setupMockIsTaxYearCrystallisedCall(previousTaxYear.endYear)(Future.failed(error))
        setupMockGetStatusTillAvailableFutureYears(previousTaxYear)(Future.successful(yearToStatus))
        setupMockGetCurrentTaxYearEnd(taxYear.endYear)

        TestOptOutService.getNextUpdatesQuarterlyReportingContentChecks.failed.map { ex =>
          ex shouldBe a[RuntimeException]
          ex.getMessage shouldBe error.getMessage
        }
      }
    }

    "ITSA Status from CY-1 till future years is unavailable" should {
      "return NextUpdatesQuarterlyReportingContentCheck" in {
        setupMockIsTaxYearCrystallisedCall(previousTaxYear.endYear)(Future.successful(Some(crystallised)))
        setupMockGetStatusTillAvailableFutureYears(previousTaxYear)(Future.failed(error))
        setupMockGetCurrentTaxYearEnd(taxYear.endYear)

        TestOptOutService.getNextUpdatesQuarterlyReportingContentChecks.failed.map { ex =>
          ex shouldBe a[RuntimeException]
          ex.getMessage shouldBe error.getMessage
        }
      }
    }

  }
}
