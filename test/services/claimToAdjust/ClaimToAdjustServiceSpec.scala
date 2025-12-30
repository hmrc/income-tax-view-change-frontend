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
import authV2.AuthActionsTestData.defaultMTDITUser
import mocks.connectors.{MockCalculationListConnector, MockChargeHistoryConnector, MockFinancialDetailsConnector}
import mocks.services.MockFinancialDetailsService
import models.calculationList.{CalculationListModel, CalculationListResponseModel}
import models.chargeHistory.{ChargeHistoryModel, ChargesHistoryModel}
import models.claimToAdjustPoa.PaymentOnAccountViewModel
import models.financialDetails.{BalanceDetails, FinancialDetailsModel}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import services.{ClaimToAdjustService, DateService}
import testConstants.BaseTestConstants.{testNino, testUserNino}
import testConstants.claimToAdjustPoa.ClaimToAdjustPoaTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.{LocalDate, LocalDateTime, LocalTime}
import scala.language.reflectiveCalls

class ClaimToAdjustServiceSpec extends TestSupport with MockFinancialDetailsConnector with MockChargeHistoryConnector
  with MockFinancialDetailsService with MockCalculationListConnector {

  def fixture(date: LocalDate) = new {
    implicit val mockDateService: DateService = new DateService {
      override def getCurrentDate: LocalDate = date

      override def isBeforeLastDayOfTaxYear: Boolean = {
        val currentDate: LocalDate = getCurrentDate
        val lastDayOfTaxYear = LocalDate.of(date.getYear, 4, 5)
        currentDate.isBefore(lastDayOfTaxYear)
      }
    }
    val testClaimToAdjustService = new ClaimToAdjustService(mockFinancialDetailsConnector, mockChargeHistoryConnector, mockCalculationListConnector, mockDateService)
  }

  val testUser: MtdItUser[_] = defaultMTDITUser(Some(Individual), IncomeSourceDetailsModel(testNino, "123", Some("2023"), List.empty, List.empty))

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

  val financialDetailsModelBothPoas: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(0.0, 0.0, 0.0, None, None, None, None, None, None, None),
    documentDetails = List.empty,
    financialDetails = List.empty
  )

  def financialDetailsModelBothPoasWithOutstandingAmount(taxYear: Int, outstandingAmount: BigDecimal): FinancialDetailsModel = {
    val poaFinancialDetailsModel = genericUserPoaDetails(taxYear, outstandingAmount)
    poaFinancialDetailsModel
      .copy(
        financialDetails = List(genericFinancialDetailPOA1(taxYear, outstandingAmount), genericFinancialDetailPOA2(taxYear, outstandingAmount)))
  }

  "getPoaTaxYearForEntryPoint method" should {
    "return a future of a right with an option containing a TaxYear" when {
      "a user has two sets of document details relating to PoA data. The first year is a CTA amendable year and is non-crystallised" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(genericUserPoaDetails(2023, outstandingAmount = 250.00))

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)

        result.futureValue shouldBe Some(TaxYear(startYear = 2022, endYear = 2023))

      }
      "a user has two sets of document details relating to PoA data. The second year is a CTA amendable year. Only the second year is non-crystallised" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(genericUserPoaDetails(2023, outstandingAmount = 250.00))

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)

        result.futureValue shouldBe Some(TaxYear(startYear = 2023, endYear = 2024))

      }
      "a user has only one CTA amendable year. This year has POA data and is not crystallised" in {
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)

        val f = fixture(LocalDate.of(2024, 4, 1))
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)

        result.futureValue shouldBe Some(TaxYear(startYear = 2023, endYear = 2024))

      }
    }
    "return a future right which is empty" when {
      "for amendable Poa years a user has non-crystallised tax years but no poa data" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userNoPoaDetails)
        setupMockGetFinancialDetails(2023, testNino)(userNoPoaDetails)

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)

        result.futureValue shouldBe None

      }
    }

  }

  "getPoaForNonCrystallisedTaxYear method" should {
    "return a future of a right with an option containing a PaymentOnAccount object" when {
      "a user has two sets of document details relating to PoA data. The first year is a CTA amendable year and is non-crystallised" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(genericUserPoaDetails(2023, outstandingAmount = 250.00))
        setupMockGetFinancialDetails(2023, testNino)(userPOADetails2023)

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaForNonCrystallisedTaxYear(testUserNino)

        result.futureValue shouldBe Right(Some(PaymentOnAccountViewModel("DOCID01", "DOCID02", TaxYear(2022, 2023), 150.00, 250.00, 100.00, 100.00, None, partiallyPaid = false, fullyPaid = false)))

      }
      "a user has two sets of document details relating to PoA data. The second year is a CTA amendable year. Only the second year is non-crystallised" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(genericUserPoaDetails(2023, outstandingAmount = 250.00))

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaForNonCrystallisedTaxYear(testUserNino)

        result.futureValue shouldBe Right(Some(PaymentOnAccountViewModel("DOCID01", "DOCID02", TaxYear(2023, 2024), 150.00, 250.00, 100.00, 100.00, None, partiallyPaid = false, fullyPaid = false)))

      }
      "a user has only one CTA amendable year. This year has POA data and is not crystallised" in {
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)

        val f = fixture(LocalDate.of(2024, 4, 1))
        val result = f.testClaimToAdjustService.getPoaForNonCrystallisedTaxYear(testUserNino)
        result.futureValue shouldBe Right(Some(PaymentOnAccountViewModel("DOCID01", "DOCID02", TaxYear(2023, 2024), 150.00, 250.00, 100.00, 100.00, None, partiallyPaid = false, fullyPaid = false)))

      }
    }
    "return a future right which is empty" when {
      "for amendable Poa years a user has non-crystallised tax years but no poa data" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userNoPoaDetails)
        setupMockGetFinancialDetails(2023, testNino)(userNoPoaDetails)

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaForNonCrystallisedTaxYear(testUserNino)

        result.futureValue shouldBe Right(None)

      }
    }
    "return an exception" when {
      "financialDetailsConnector returns an error model" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)

        setupMockGetFinancialDetails(2024, testNino)(financialDetailsErrorModel(500))
        setupMockGetFinancialDetails(2023, testNino)(financialDetailsErrorModel(500))

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaForNonCrystallisedTaxYear(testUserNino)

        result.futureValue.toString shouldBe Left(new Exception("There was an error whilst fetching financial details data")).toString

      }
    }
  }

  "getAmendablePoaViewModel" should {
    "return a future right with AmendablePoaViewModel when POAs are unpaid" in {

      val taxYear: Int = 2023

      val outstandingAmount: BigDecimal = 1250.0

      val chargeHistories: List[ChargeHistoryModel] = List(
        ChargeHistoryModel(s"$taxYear", "1040000124", LocalDate.of(taxYear, 2, 14), "ITSA- POA 1", 2500, LocalDateTime.of(LocalDate.of(taxYear + 1, 2, 14), LocalTime.of(9, 30, 45)), "Customer Request", Some("001")),
        ChargeHistoryModel(s"$taxYear", "1040000125", LocalDate.of(taxYear, 2, 14), "ITSA- POA 2", 2500, LocalDateTime.of(LocalDate.of(taxYear + 1, 2, 14), LocalTime.of(10, 30, 45)), "Customer Request", Some("002")))

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(chargeHistories)
      ))

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsModelBothPoasWithOutstandingAmount(taxYear, outstandingAmount))

      val f = fixture(LocalDate.of(taxYear, 8, 27))
      val result = f.testClaimToAdjustService.getAmendablePoaViewModel(testUserNino)

      result.futureValue shouldBe Right(
        PaymentOnAccountViewModel(
          poaOneTransactionId = "DOCID01",
          poaTwoTransactionId = "DOCID02",
          taxYear = TaxYear(taxYear - 1, taxYear),
          previouslyAdjusted = Some(true),
          totalAmountOne = 150.00,
          totalAmountTwo = 250.00,
          relevantAmountOne = 100.00,
          relevantAmountTwo = 100.00,
          partiallyPaid = true,
          fullyPaid = false))
    }


    "return a future right with AmendablePoaViewModel when POAs are paid" in {

      val taxYear: Int = 2023

      val outstandingAmount: BigDecimal = 0.0

      val chargeHistories: List[ChargeHistoryModel] = List(
        ChargeHistoryModel(s"$taxYear", "1040000124", LocalDate.of(taxYear, 2, 14), "ITSA- POA 1", 2500, LocalDateTime.of(LocalDate.of(taxYear + 1, 2, 14), LocalTime.of(9, 30, 45)), "Customer Request", Some("001")),
        ChargeHistoryModel(s"$taxYear", "1040000125", LocalDate.of(taxYear, 2, 14), "ITSA- POA 2", 2500, LocalDateTime.of(LocalDate.of(taxYear + 1, 2, 14), LocalTime.of(10, 30, 45)), "Customer Request", Some("002")))

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(chargeHistories)
      ))

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsModelBothPoasWithOutstandingAmount(taxYear, outstandingAmount))

      val f = fixture(LocalDate.of(taxYear, 8, 27))
      val result = f.testClaimToAdjustService.getAmendablePoaViewModel(testUserNino)

      result.futureValue shouldBe Right(
        PaymentOnAccountViewModel(
          poaOneTransactionId = "DOCID01",
          poaTwoTransactionId = "DOCID02",
          taxYear = TaxYear(taxYear - 1, taxYear),
          previouslyAdjusted = Some(true),
          totalAmountOne = 150.00,
          totalAmountTwo = 250.00,
          relevantAmountOne = 100.00,
          relevantAmountTwo = 100.00,
          partiallyPaid = true,
          fullyPaid = true))

    }

    "return a future left with an exception when an exception is thrown" in {

      val taxYear: Int = 2023

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      val financialDetailsModelWithoutDocumentDetails = financialDetailsModelBothPoas

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsModelWithoutDocumentDetails)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(List.empty)
      ))

      val f = fixture(LocalDate.of(taxYear, 8, 27))
      val futureResult = f.testClaimToAdjustService.getAmendablePoaViewModel(testUserNino)

      val result = futureResult.futureValue

      result.isLeft shouldBe true
      result.left.exists(_.getMessage == "Failed to retrieve non-crystallised financial details") shouldBe true

    }
  }

  "getPoaViewModelWithAdjustmentReason" should {

    "return a future right with PoaAmountViewModel when POAs are unpaid" in {

      val taxYear: Int = 2023

      val chargeHistories: List[ChargeHistoryModel] = List(
        ChargeHistoryModel(s"$taxYear", "1040000124", LocalDate.of(taxYear, 2, 14), "ITSA- POA 1", 2500, LocalDateTime.of(LocalDate.of(taxYear + 1, 2, 14), LocalTime.of(9, 30, 45)), "Customer Request", Some("001")),
        ChargeHistoryModel(s"$taxYear", "1040000125", LocalDate.of(taxYear, 2, 14), "ITSA- POA 2", 2500, LocalDateTime.of(LocalDate.of(taxYear + 1, 2, 14), LocalTime.of(10, 30, 45)), "Customer Request", Some("002")))

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(chargeHistories)
      ))

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsWithUnpaidPoas(taxYear))

      val f = fixture(LocalDate.of(taxYear, 8, 27))

      val result = f.testClaimToAdjustService.getPoaViewModelWithAdjustmentReason(testUserNino)

      result.futureValue shouldBe Right(PaymentOnAccountViewModel("DOCID01", "DOCID02", TaxYear(2022, 2023), 150.00, 250.00, 100.00, 100.00, Some(true), partiallyPaid = false, fullyPaid = false))

    }

    "return a future right with PoAAmountViewModel when POAs are paid" in {

      val taxYear: Int = 2023

      val outstandingAmount: BigDecimal = 0.0

      val chargeHistories: List[ChargeHistoryModel] = List(
        ChargeHistoryModel(s"$taxYear", "1040000124", LocalDate.of(taxYear, 2, 14), "ITSA- POA 1", 2500, LocalDateTime.of(LocalDate.of(taxYear + 1, 2, 14), LocalTime.of(9, 30, 45)), "Customer Request", Some("001")),
        ChargeHistoryModel(s"$taxYear", "1040000125", LocalDate.of(taxYear, 2, 14), "ITSA- POA 2", 2500, LocalDateTime.of(LocalDate.of(taxYear + 1, 2, 14), LocalTime.of(10, 30, 45)), "Customer Request", Some("002")))

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(chargeHistories)
      ))

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsModelBothPoasWithOutstandingAmount(taxYear, outstandingAmount))

      val f = fixture(LocalDate.of(taxYear, 8, 27))

      val result = f.testClaimToAdjustService.getPoaViewModelWithAdjustmentReason(testUserNino)

      result.futureValue shouldBe Right(PaymentOnAccountViewModel("DOCID01", "DOCID02", TaxYear(2022, 2023), 150.00, 250.00, 100.00, 100.00, Some(true), partiallyPaid = true, fullyPaid = true))

    }

    "return a future left with an exception when an exception is thrown" in {

      val taxYear: Int = 2023

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      val financialDetailsModelWithoutDocumentDetails = financialDetailsModelBothPoas

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsModelWithoutDocumentDetails)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(List.empty)
      ))

      val f = fixture(LocalDate.of(taxYear, 8, 27))
      val futureResult = f.testClaimToAdjustService.getPoaViewModelWithAdjustmentReason(testUserNino)

      val result = futureResult.futureValue

      result.isLeft shouldBe true
      result.left.exists(_.getMessage == "No financial details found for this charge") shouldBe true

    }

    "return a future right with PoAAmountViewModel when POA 1 is not at the head of the list of charges" in {

      val taxYear: Int = 2023

      val chargeHistoriesNoPoaAtHead: List[ChargeHistoryModel] = List(
        chargeHistoryModelNoPOA(taxYear),
        ChargeHistoryModel(s"$taxYear", "1040000124", LocalDate.of(taxYear, 2, 14), "ITSA- POA 1", 2500, LocalDateTime.of(LocalDate.of(taxYear + 1, 2, 14), LocalTime.of(9, 30, 45)), "Customer Request", Some("001")))

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(chargeHistoriesNoPoaAtHead)
      ))

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsWithUnpaidPoas(taxYear))

      val f = fixture(LocalDate.of(taxYear, 8, 27))

      val result = f.testClaimToAdjustService.getPoaViewModelWithAdjustmentReason(testUserNino)

      result.futureValue shouldBe Right(PaymentOnAccountViewModel("DOCID01", "DOCID02", TaxYear(2022, 2023), 150.00, 250.00, 100.00, 100.00, Some(true), partiallyPaid = false, fullyPaid = false))

    }
    "return a future right with PoAAmountViewModel when POA 2 is not at the head of the list of charges" in {

      val taxYear: Int = 2023

      val chargeHistoriesNoPoaAtHead: List[ChargeHistoryModel] = List(
        chargeHistoryModelNoPOA(taxYear),
        ChargeHistoryModel(s"$taxYear", "1040000125", LocalDate.of(taxYear, 2, 14), "ITSA- POA 2", 2500, LocalDateTime.of(LocalDate.of(taxYear + 1, 2, 14), LocalTime.of(9, 30, 45)), "Customer Request", Some("002")))

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(chargeHistoriesNoPoaAtHead)
      ))

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsWithUnpaidPoas(taxYear))

      val f = fixture(LocalDate.of(taxYear, 8, 27))

      val result = f.testClaimToAdjustService.getPoaViewModelWithAdjustmentReason(testUserNino)

      result.futureValue shouldBe Right(PaymentOnAccountViewModel("DOCID01", "DOCID02", TaxYear(2022, 2023), 150.00, 250.00, 100.00, 100.00, Some(true), partiallyPaid = false, fullyPaid = false))
    }
  }
}
