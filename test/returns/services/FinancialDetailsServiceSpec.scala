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

package returns.services

import common.config.featureswitch.FeatureSwitching
import common.models.incomeSourceDetails.TaxYear
import common.services.DateService
import common.testConstants.BaseTestConstants.*
import common.testUtils.TestSupport
import returns.mocks.connectors.MockFinancialDetailsConnector
import returns.models.*
import returns.testConstants.FinancialDetailsTestConstants.*
import shared.mocks.connectors.MockCalculationListConnector
import shared.models.calculationList.{CalculationListModel, CalculationListResponseModel}

import java.time.LocalDate

class FinancialDetailsServiceSpec extends TestSupport with MockFinancialDetailsConnector
  with MockCalculationListConnector with FeatureSwitching {

  def newFixture(date: LocalDate = LocalDate.of(2023, 8, 27)): FinancialDetailsService = {
    implicit val mockDateService: DateService = new DateService {
      override def getCurrentDate: LocalDate = date

      override def isBeforeLastDayOfTaxYear: Boolean = {
        val currentDate: LocalDate = getCurrentDate
        val lastDayOfTaxYear = LocalDate.of(date.getYear, 4, 5)
        currentDate.isBefore(lastDayOfTaxYear)
      }
    }
    val financialDetailsService = new FinancialDetailsService(
      mockFinancialDetailsConnector,
      mockCalculationListConnector,
      mockDateService
    )

    financialDetailsService
  }

  val calculationListSuccessResponseModelNonCrystallised: CalculationListResponseModel = CalculationListModel(crystallised = Some(false))

  val calculationListSuccessResponseModelCrystallisationMissing: CalculationListResponseModel = CalculationListModel(crystallised = None)
  val calculationListSuccessResponseModelCrystallised: CalculationListResponseModel =
    CalculationListModel(crystallised = Some(true))

  lazy val financialDetailsService = newFixture()

  "getFinancialDetails" when {
    "a successful financial details response is returned from the connector" should {
      "return a valid FinancialDetails model" in {
        setupMockGetFinancialDetails(testTaxYear, testNino)(financialDetailsModel(testTaxYear))
        financialDetailsService.getFinancialDetails(testTaxYear, testNino).futureValue shouldBe financialDetailsModel(testTaxYear)
      }
    }
    "a error model is returned from the connector" should {
      "return a FinancialDetailsError model" in {
        setupMockGetFinancialDetails(testTaxYear, testNino)(testFinancialDetailsErrorModel)
        financialDetailsService.getFinancialDetails(testTaxYear, testNino).futureValue shouldBe testFinancialDetailsErrorModel
      }
    }
  }

  "getPoaTaxYearForEntryPoint method" should {
    "return a future of a right with an option containing a TaxYear" when {
      "a user has two sets of document details relating to PoA data. The first year is a CTA amendable year and is non-crystallised" in {
        setupGetCalculationList(testNino, "22-23", testMtditid)(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24", testMtditid)(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(genericUserPoaDetails(2023, outstandingAmount = 250.00))

        val result = financialDetailsService.getPoaTaxYearForEntryPoint(testUserNino)

        result.futureValue shouldBe Right(Some(TaxYear(startYear = 2022, endYear = 2023)))

      }
      "a user has two sets of document details relating to PoA data. The second year is a CTA amendable year. Only the second year is non-crystallised" in {
        setupGetCalculationList(testNino, "22-23", testMtditid)(calculationListSuccessResponseModelCrystallised)
        setupGetCalculationList(testNino, "23-24", testMtditid)(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(genericUserPoaDetails(2023, outstandingAmount = 250.00))

        val result = financialDetailsService.getPoaTaxYearForEntryPoint(testUserNino)

        result.futureValue shouldBe Right(Some(TaxYear(startYear = 2023, endYear = 2024)))

      }
      "a user has only one CTA amendable year. This year has POA data and is not crystallised" in {
        setupGetCalculationList(testNino, "23-24", testMtditid)(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)

        val service = newFixture(LocalDate.of(2024, 4, 1))
        val result = service.getPoaTaxYearForEntryPoint(testUserNino)

        result.futureValue shouldBe Right(Some(TaxYear(startYear = 2023, endYear = 2024)))

      }
      "a user has only one CTA amendable year. They have signed up in CY, so CY-1 is not crystallised, but has no POAS, so only CY is amendable" in {
        setupGetCalculationList(testNino, "22-23", testMtditid)(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24", testMtditid)(calculationListSuccessResponseModelNonCrystallised)

        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(userNoPoaDetails)

        val service = newFixture(LocalDate.of(2023, 4, 20))
        val result = service.getPoaTaxYearForEntryPoint(testUserNino)

        result.futureValue shouldBe Right(Some(TaxYear(startYear = 2023, endYear = 2024)))
      }
      "a user has four sets of document details relating to PoA data. Two of them marked as credits." in {
        setupGetCalculationList(testNino, "22-23", testMtditid)(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24", testMtditid)(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(genericUserPoaDetailsWithPoaCredits(2023, outstandingAmount = 250.00))

        val service = newFixture(LocalDate.of(2023, 8, 27))
        val result = service.getPoaTaxYearForEntryPoint(testUserNino)

        result.futureValue shouldBe Right(Some(TaxYear(startYear = 2022, endYear = 2023)))

      }
    }
    
    "return a future right which is empty" when {
      "for amendable Poa years a user has non-crystallised tax years but no poa data" in {
        setupGetCalculationList(testNino, "22-23", testMtditid)(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24", testMtditid)(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userNoPoaDetails)
        setupMockGetFinancialDetails(2023, testNino)(userNoPoaDetails)

        val service = newFixture(LocalDate.of(2023, 8, 27))
        val result = service.getPoaTaxYearForEntryPoint(testUserNino)

        result.futureValue shouldBe Right(None)

      }
    }
    "return an exception" when {
      "financialDetailsConnector returns an error model" in {
        setupGetCalculationList(testNino, "22-23", testMtditid)(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24", testMtditid)(calculationListSuccessResponseModelNonCrystallised)

        setupMockGetFinancialDetails(2024, testNino)(financialDetailsErrorModel(500))
        setupMockGetFinancialDetails(2023, testNino)(financialDetailsErrorModel(500))

        val service = newFixture(LocalDate.of(2023, 8, 27))
        val result = service.getPoaTaxYearForEntryPoint(testUserNino)

        result.futureValue.toString shouldBe Left(new Exception("There was an error whilst fetching financial details data")).toString

      }
    }
  }
}
