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

package services.claimToAdjust

import auth.MtdItUser
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.services.MockFinancialDetailsService
import models.calculationList.{CalculationListModel, CalculationListResponseModel}
import models.financialDetails.{BalanceDetails, FinancialDetailsModel}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import play.api.test.FakeRequest
import services.{ClaimToAdjustService, DateService}
import testConstants.BaseTestConstants.{testMtditid, testNino, testUserNino}
import testConstants.claimToAdjustPOA.ClaimToAdjustPOATestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import scala.language.reflectiveCalls
import java.time.LocalDate

class ClaimToAdjustServiceSpec extends TestSupport with MockFinancialDetailsConnector with MockFinancialDetailsService with MockCalculationListConnector {

  def fixture(date: LocalDate) = new {
    implicit val mockDateService: DateService = new DateService {
      override def getCurrentDate: LocalDate = date

      override def isBeforeLastDayOfTaxYear: Boolean = {
        val currentDate = getCurrentDate
        val lastDayOfTaxYear = LocalDate.of(date.getYear, 4, 5)
        currentDate.isBefore(lastDayOfTaxYear)
      }
    }
    val testClaimToAdjustService = new ClaimToAdjustService(mockFinancialDetailsConnector, mockCalculationListConnector, mockDateService)
  }

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    None,
    incomeSources = IncomeSourceDetailsModel(testNino, "123", Some("2023"), List.empty, List.empty),
    None,
    Some("1234567890"),
    Some("12345-credId"),
    Some(Individual),
    None
  )(FakeRequest())

  val calculationListSuccessResponseModelCrystallised: CalculationListResponseModel = CalculationListModel(
    calculationId = "TEST_ID",
    calculationTimestamp = "TEST_STAMP",
    calculationType = "TEST_TYPE",
    crystallised = Some(true)
  )

  val calculationListSuccessResponseModelNonCrystallised: CalculationListResponseModel = CalculationListModel(
    calculationId = "TEST_ID",
    calculationTimestamp = "TEST_STAMP",
    calculationType = "TEST_TYPE",
    crystallised = Some(false)
  )

  val calculationListSuccessResponseModelCrystallisationMissing: CalculationListResponseModel = CalculationListModel(
    calculationId = "TEST_ID",
    calculationTimestamp = "TEST_STAMP",
    calculationType = "TEST_TYPE",
    crystallised = None
  )

  val financialDetailsModelBothPoAs: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(0.0, 0.0, 0.0, None, None, None, None, None),
    documentDetails = List.empty,
    financialDetails = List.empty
  )

  "getPoaTaxYearForEntryPoint method" should {
    "return a future of a right with an option containing a taxYear" when {
      "a user has two sets of document details relating to PoA data. The first year is a CTA amendable year and is non-crystallised" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(genericUserPOADetails(2023))

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)(hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(Some(TaxYear(startYear = 2022, endYear = 2023)))
        }
      }
      "a user has two sets of document details relating to PoA data. The second year is a CTA amendable year. Only the second year is non-crystallised" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(genericUserPOADetails(2023))

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)(hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(Some(TaxYear(startYear = 2023, endYear = 2024)))
        }
      }
      "a user has only one CTA amendable year. This year has POA data and is not crystallised" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(genericUserPOADetails(2023))

        val f = fixture(LocalDate.of(2024, 4, 1))
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)(hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(Some(TaxYear(startYear = 2023, endYear = 2024)))
        }
      }
    }
    "return a future right which is empty" when {
      "for amendable Poa years a user has non-crystallised tax years but no poa data" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userNoPOADetails)
        setupMockGetFinancialDetails(2023, testNino)(userNoPOADetails)

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)(hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(None)
        }
      }
    }
    "return an exception" when {
      "financialDetailsConnector returns an error model" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)


        setupMockGetFinancialDetails(2024, testNino)(financialDetailsErrorModel(500))
        setupMockGetFinancialDetails(2023, testNino)(financialDetailsErrorModel(500))

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)(hc = implicitly)

        whenReady(result) {
          result => result.toString shouldBe Left(new Exception("There was an error whilst fetching financial details data")).toString
        }
      }
    }
  }

  "getPoaForNonCrystallisedTaxYear method" should {
    "return a future of an option containing a PaymentOnAccount object" when {
      "a user has document details relating to PoA data for a CTA amendable year that is non-crystallised" in {
      }
    }
  }

}
