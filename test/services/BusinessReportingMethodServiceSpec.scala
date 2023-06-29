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

import forms.incomeSources.add.AddBusinessReportingMethodForm
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.calculationList.CalculationListErrorModel
import models.incomeSourceDetails.IncomeSourceDetailsError
import models.incomeSourceDetails.viewmodels.BusinessReportingMethodViewModel
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{IM_A_TEAPOT, NOT_FOUND}
import testConstants.BaseTestConstants.{testMtdItUser, testNino, testSelfEmploymentId}
import testConstants.CalculationListTestConstants.{calculationListFalseFull, calculationListFull, calculationListMin, calculationListNone}
import testConstants.ITSAStatusTestConstants.{errorITSAStatusError, successITSAStatusResponseMTDMandatedModel, successITSAStatusResponseModel}
import testConstants.IncomeSourceDetailsTestConstants.{businessIncome, singleBusinessIncome, singleBusinessIncome2023}
import testUtils.TestSupport

import scala.concurrent.Future

class BusinessReportingMethodServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector {
  val mockDateService: DateService = mock(classOf[DateService])

  object TestBusinessReportingMethodService extends BusinessReportingMethodService(mockIncomeTaxViewChangeConnector, mockDateService, appConfig)

  "checkITSAStatusCurrentYear" when {
    val taxYearEnd = 2020
    val yearEnd = taxYearEnd.toString.substring(2).toInt
    val yearStart = yearEnd - 1
    val yearRange = s"$yearStart-$yearEnd"
    "ITSAStatus is returned " should {
      "return True if the status is in [MTD Mandated, MTD Voluntary ] " in {
        when(mockDateService.getCurrentTaxYearEnd(any())).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Right(List(successITSAStatusResponseMTDMandatedModel)))
        TestBusinessReportingMethodService.checkITSAStatusCurrentYear(testMtdItUser, headerCarrier, ec).futureValue shouldBe true
      }
      "return false" in {
        when(mockDateService.getCurrentTaxYearEnd(any())).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Right(List(successITSAStatusResponseModel)))
        TestBusinessReportingMethodService.checkITSAStatusCurrentYear(testMtdItUser, headerCarrier, ec).futureValue shouldBe false
      }
    }
    "ITSAStatus connector return an error" should {
      "return a failed future with error" in {
        when(mockDateService.getCurrentTaxYearEnd(any())).thenReturn(taxYearEnd)
        setupGetITSAStatusDetail(testNino, yearRange, false, false)(Left(errorITSAStatusError))
        TestBusinessReportingMethodService.checkITSAStatusCurrentYear(testMtdItUser, headerCarrier, ec).failed.futureValue.getMessage shouldBe "[BusinessReportingMethodService][checkITSAStatusCurrentYear] - Failed to retrieve ITSAStatus"
      }
    }
  }

  "isTaxYearCrystallised" when {
    "calculation list is returned for year 2022-23" should {
      val taxYearEnd = "2023"
      "returns Some(true)" in {
        when(mockDateService.getCurrentTaxYearEnd(any())).thenReturn(taxYearEnd.toInt)
        setupGetLegacyCalculationList(testNino, taxYearEnd)(calculationListFull)
        TestBusinessReportingMethodService.isTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe Some(true)
      }
      "returns Some(false)" in {
        when(mockDateService.getCurrentTaxYearEnd(any())).thenReturn(taxYearEnd.toInt)
        setupGetLegacyCalculationList(testNino, taxYearEnd)(calculationListFalseFull)
        TestBusinessReportingMethodService.isTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe Some(false)
      }
      "returns None" in {
        when(mockDateService.getCurrentTaxYearEnd(any())).thenReturn(taxYearEnd.toInt)
        setupGetLegacyCalculationList(testNino, taxYearEnd)(calculationListMin)
        TestBusinessReportingMethodService.isTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe None
      }
      "returns InternalServerException" in {
        when(mockDateService.getCurrentTaxYearEnd(any())).thenReturn(taxYearEnd.toInt)
        val error = CalculationListErrorModel(IM_A_TEAPOT, "I'm a teapot")
        setupGetLegacyCalculationList(testNino, taxYearEnd)(error)
        TestBusinessReportingMethodService.isTaxYearCrystallised(taxYearEnd.toInt).failed.futureValue.getMessage shouldBe error.message
      }
    }

    "calculation list is returned for year 2023-24" should {
      val taxYearRange = "23-24"
      "returns Some(true)" in {
        setupGetCalculationList(testNino, taxYearRange)(calculationListFull)
        TestBusinessReportingMethodService.isTaxYearCrystallised(2024).futureValue shouldBe Some(true)
      }
      "returns Some(false)" in {
        setupGetCalculationList(testNino, taxYearRange)(calculationListFalseFull)
        TestBusinessReportingMethodService.isTaxYearCrystallised(2024).futureValue shouldBe Some(false)
      }
      "returns None" in {
        setupGetCalculationList(testNino, taxYearRange)(calculationListMin)
        TestBusinessReportingMethodService.isTaxYearCrystallised(2024).futureValue shouldBe None
      }
      "returns InternalServerException" in {
        val error = CalculationListErrorModel(IM_A_TEAPOT, "I'm a teapot")
        setupGetCalculationList(testNino, taxYearRange)(error)
        TestBusinessReportingMethodService.isTaxYearCrystallised(2024).failed.futureValue.getMessage shouldBe error.message
      }
    }
  }

  "updateIncomeSourceTaxYearSpecific" when {
    "new reporting method are different w.r.t to latency" should {
      "submit the new methods giving timestamp as result" in {
        val taxYearSpecificUpdate = List(TaxYearSpecific("2023", true), TaxYearSpecific("2024", false))
        val result = UpdateIncomeSourceResponseModel(processingDate = "timestamp")
        val newReportingMethodDetails = AddBusinessReportingMethodForm(
          taxYear1 = Some("2023"),
          newTaxYear1ReportingMethod = Some("A"),
          taxYear2 = Some("2024"),
          newTaxYear2ReportingMethod = Some("Q"),
          taxYear1ReportingMethod = None,
          taxYear2ReportingMethod = None)

        setupUpdateIncomeSourceTaxYearSpecific(testNino, testSelfEmploymentId, taxYearSpecificUpdate)(result)
        TestBusinessReportingMethodService.updateIncomeSourceTaxYearSpecific(
          nino = testNino,
          incomeSourceId = testSelfEmploymentId,
          reportingMethod = newReportingMethodDetails).futureValue shouldBe Some(result)
      }
    }
    "new reporting method are same w.r.t to latency" should {
      "return None" in {
        val newReportingMethodDetails = AddBusinessReportingMethodForm(
          taxYear1 = Some("2023"),
          newTaxYear1ReportingMethod = Some("A"),
          taxYear2 = Some("2024"),
          newTaxYear2ReportingMethod = Some("Q"),
          taxYear1ReportingMethod = Some("A"),
          taxYear2ReportingMethod = Some("Q"))

        TestBusinessReportingMethodService.updateIncomeSourceTaxYearSpecific(
          nino = testNino,
          incomeSourceId = testSelfEmploymentId,
          reportingMethod = newReportingMethodDetails).futureValue shouldBe None

      }
    }
    "called with overloaded method with formData " should {
      "returns a result" in {
        val taxYearSpecificUpdate = List(TaxYearSpecific("2023", true), TaxYearSpecific("2024", false))
        val result = UpdateIncomeSourceResponseModel(processingDate = "timestamp")
        val formData = Map(
          AddBusinessReportingMethodForm.newTaxYear1ReportingMethod -> "A",
          AddBusinessReportingMethodForm.newTaxYear2ReportingMethod -> "Q",
          AddBusinessReportingMethodForm.taxYear1 -> "2023",
          AddBusinessReportingMethodForm.taxYear2 -> "2024"
        )

        setupUpdateIncomeSourceTaxYearSpecific(testNino, testSelfEmploymentId, taxYearSpecificUpdate)(result)
        TestBusinessReportingMethodService.updateIncomeSourceTaxYearSpecific(
          nino = testNino,
          incomeSourceId = testSelfEmploymentId,
          formData = formData).futureValue shouldBe Some(result)
      }
    }

  }

  "getBusinessReportingMethodDetails" when {
    "business details contain latency details and" when {
      "latency period is expired" should {
        "return None" in {
          setupBusinessDetails(testNino)(Future(singleBusinessIncome))
          when(mockDateService.getCurrentTaxYearEnd(any())).thenReturn(2020)
          TestBusinessReportingMethodService.getBusinessReportingMethodDetails(testNino).futureValue shouldBe None
        }
      }
      "tax year one is crystallised" should {
        "return view model with tax year two latency details" in {
          val taxYearEnd = "2023"
          val texYearStart = "2022"
          val result = Some(BusinessReportingMethodViewModel(taxYear2 = Some("2023"), latencyIndicator2 = Some("Q")))
          setupBusinessDetails(testNino)(Future(singleBusinessIncome2023))
          setupGetLegacyCalculationList(testNino, texYearStart)(calculationListFull)
          when(mockDateService.getCurrentTaxYearEnd(any())).thenReturn(taxYearEnd.toInt)
          TestBusinessReportingMethodService.getBusinessReportingMethodDetails(testSelfEmploymentId).futureValue shouldBe result
        }
      }
      "tax year one is not crystallised" should {
        "return view model with both tax year latency details" in {
          val taxYearEnd = "2023"
          val texYearStart = "2022"
          val result = Some(BusinessReportingMethodViewModel(
            taxYear1 = Some(texYearStart),
            latencyIndicator1 = Some("A"),
            taxYear2 = Some(taxYearEnd),
            latencyIndicator2 = Some("Q")))

          setupBusinessDetails(testNino)(Future(singleBusinessIncome2023))
          setupGetLegacyCalculationList(testNino, texYearStart)(calculationListFalseFull)
          when(mockDateService.getCurrentTaxYearEnd(any())).thenReturn(taxYearEnd.toInt)
          TestBusinessReportingMethodService.getBusinessReportingMethodDetails(testSelfEmploymentId).futureValue shouldBe result
        }
      }
      "crystallization status is not available " should {
        "returns a failed future with exception" in {
          val taxYearEnd = "2023"
          val texYearStart = "2022"
          val result = "[BusinessReportingMethodService][getBusinessReportingMethodDetails] Crystallisation status not found"

          setupBusinessDetails(testNino)(Future(singleBusinessIncome2023))
          setupGetLegacyCalculationList(testNino, texYearStart)(calculationListNone)
          when(mockDateService.getCurrentTaxYearEnd(any())).thenReturn(taxYearEnd.toInt)
          TestBusinessReportingMethodService.getBusinessReportingMethodDetails(testSelfEmploymentId).failed.futureValue.getMessage shouldBe result
        }
      }
      "failure while retrieving crystallization status" should {
        "returns a failed future with exception" in {
          val taxYearEnd = "2023"
          val texYearStart = "2022"
          val result = "error message"

          setupBusinessDetails(testNino)(Future(singleBusinessIncome2023))
          setupGetLegacyCalculationList(testNino, texYearStart)(CalculationListErrorModel(NOT_FOUND, result))
          when(mockDateService.getCurrentTaxYearEnd(any())).thenReturn(taxYearEnd.toInt)
          TestBusinessReportingMethodService.getBusinessReportingMethodDetails(testSelfEmploymentId).failed.futureValue.getMessage shouldBe result
        }

      }
    }
    "business details does not contain latency details " should {
      "returns None" in {
        setupBusinessDetails(testNino)(Future(businessIncome))
        TestBusinessReportingMethodService.getBusinessReportingMethodDetails(testSelfEmploymentId).futureValue shouldBe None
      }
    }
    "error while retrieving business details" should {
      "the exception" in {
        val errorMsg = "[BusinessReportingMethodService][getBusinessReportingMethodDetails] - Failed to retrieve IncomeSourceDetails"
        setupBusinessDetails(testNino)(Future(IncomeSourceDetailsError(NOT_FOUND, "")))
        TestBusinessReportingMethodService.getBusinessReportingMethodDetails(testSelfEmploymentId).failed.futureValue.getMessage shouldBe errorMsg
      }
    }
  }
}
