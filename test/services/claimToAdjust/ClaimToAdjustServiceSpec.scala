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
import mocks.connectors.{MockCalculationListConnector, MockChargeHistoryConnector, MockFinancialDetailsConnector}
import mocks.services.MockFinancialDetailsService
import models.calculationList.{CalculationListModel, CalculationListResponseModel}
import models.chargeHistory.{ChargeHistoryModel, ChargesHistoryModel}
import models.claimToAdjustPoa.{AmendablePoaViewModel, PaymentOnAccountViewModel, PoAAmountViewModel}
import models.financialDetails.{BalanceDetails, FinancialDetailsModel}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import play.api.test.FakeRequest
import services.{ClaimToAdjustService, DateService}
import testConstants.BaseTestConstants.{testMtditid, testNino, testUserNino}
import testConstants.claimToAdjustPOA.ClaimToAdjustPOATestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate
import scala.language.reflectiveCalls

class ClaimToAdjustServiceSpec extends TestSupport with MockFinancialDetailsConnector with MockChargeHistoryConnector with MockFinancialDetailsService with MockCalculationListConnector {

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

   def financialDetailsModelBothPoAsWithOutstandingAmount(taxYear: Int, outstandingAmount: BigDecimal) = {
     val poaFinancialDetailsModel = genericUserPOADetails(taxYear)
     poaFinancialDetailsModel
       .copy(
         documentDetails = poaFinancialDetailsModel.documentDetails.map(_.copy(outstandingAmount = outstandingAmount)),
         financialDetails = List(genericFinancialDetailPOA1(taxYear, outstandingAmount), genericFinancialDetailPOA2(taxYear, outstandingAmount)))
   }

  "getPoaTaxYearForEntryPoint method" should {
    "return a future of a right with an option containing a TaxYear" when {
      "a user has two sets of document details relating to PoA data. The first year is a CTA amendable year and is non-crystallised" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(genericUserPOADetails(2023))

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)

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
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)

        whenReady(result) {
          result => result shouldBe Right(Some(TaxYear(startYear = 2023, endYear = 2024)))
        }
      }
      "a user has only one CTA amendable year. This year has POA data and is not crystallised" in {
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)

        val f = fixture(LocalDate.of(2024, 4, 1))
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)

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
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)

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
        val result = f.testClaimToAdjustService.getPoaTaxYearForEntryPoint(testUserNino)

        whenReady(result) {
          result => result.toString shouldBe Left(new Exception("There was an error whilst fetching financial details data")).toString
        }
      }
    }
  }

  "getPoaForNonCrystallisedTaxYear method" should {
    "return a future of a right with an option containing a PaymentOnAccount object" when {
      "a user has two sets of document details relating to PoA data. The first year is a CTA amendable year and is non-crystallised" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(genericUserPOADetails(2023))

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaForNonCrystallisedTaxYear(testUserNino)

        whenReady(result) {
          result => result shouldBe Right(Some(PaymentOnAccountViewModel("DOCID01", "DOCID02", TaxYear(2022, 2023), 150.00, 250.00, 100.00, 100.00, false)))
        }
      }
      "a user has two sets of document details relating to PoA data. The second year is a CTA amendable year. Only the second year is non-crystallised" in {
        setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelCrystallised)
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetails(2023, testNino)(genericUserPOADetails(2023))

        val f = fixture(LocalDate.of(2023, 8, 27))
        val result = f.testClaimToAdjustService.getPoaForNonCrystallisedTaxYear(testUserNino)

        whenReady(result) {
          result => result shouldBe Right(Some(PaymentOnAccountViewModel("DOCID01", "DOCID02", TaxYear(2023, 2024), 150.00, 250.00, 100.00, 100.00, false)))
        }
      }
      "a user has only one CTA amendable year. This year has POA data and is not crystallised" in {
        setupGetCalculationList(testNino, "23-24")(calculationListSuccessResponseModelNonCrystallised)
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)

        val f = fixture(LocalDate.of(2024, 4, 1))
        val result = f.testClaimToAdjustService.getPoaForNonCrystallisedTaxYear(testUserNino)
        whenReady(result) {
          result => result shouldBe Right(Some(PaymentOnAccountViewModel("DOCID01", "DOCID02", TaxYear(2023, 2024), 150.00, 250.00, 100.00, 100.00, false)))
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
        val result = f.testClaimToAdjustService.getPoaForNonCrystallisedTaxYear(testUserNino)

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
        val result = f.testClaimToAdjustService.getPoaForNonCrystallisedTaxYear(testUserNino)

        whenReady(result) {
          result => result.toString shouldBe Left(new Exception("There was an error whilst fetching financial details data")).toString
        }
      }
    }
  }

  "getAmendablePoaViewModel" should {
    "return a future right with AmendablePoaViewModel when POAs are unpaid" in {

      val taxYear:Int = 2023

      val outstandingAmount:BigDecimal = 1250.0

      val chargeHistories: List[ChargeHistoryModel] = List(
        ChargeHistoryModel(s"$taxYear", "1040000124", LocalDate.of(taxYear, 2, 14), "ITSA- POA 1", 2500, LocalDate.of(taxYear + 1, 2, 14), "Customer Request", Some("001")),
        ChargeHistoryModel(s"$taxYear", "1040000125", LocalDate.of(taxYear, 2, 14), "ITSA - POA 2", 2500, LocalDate.of(taxYear + 1, 2, 14), "Customer Request", Some("002")))

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(chargeHistories)
      ))

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsModelBothPoAsWithOutstandingAmount(taxYear, outstandingAmount))

      val f = fixture(LocalDate.of(taxYear, 8, 27))
      val result = f.testClaimToAdjustService.getAmendablePoaViewModel(testUserNino)

      whenReady(result) {
        result => result shouldBe Right(
          AmendablePoaViewModel(
            poaOneTransactionId = "DOCID01",
            poaTwoTransactionId = "DOCID02",
            taxYear = TaxYear(taxYear - 1, taxYear),
            poasHaveBeenAdjustedPreviously = true,
            paymentOnAccountOne = 150.00,
            paymentOnAccountTwo = 250.00,
            poARelevantAmountOne = 100.00,
            poARelevantAmountTwo = 100.00,
            poAPartiallyPaid = true,
            poAFullyPaid = false))
      }
    }

    "return a future right with AmendablePoaViewModel when POAs are paid" in {

      val taxYear:Int = 2023

      val outstandingAmount:BigDecimal = 0.0

      val chargeHistories: List[ChargeHistoryModel] = List(
        ChargeHistoryModel(s"$taxYear", "1040000124", LocalDate.of(taxYear, 2, 14), "ITSA- POA 1", 2500, LocalDate.of(taxYear + 1, 2, 14), "Customer Request", Some("001")),
        ChargeHistoryModel(s"$taxYear", "1040000125", LocalDate.of(taxYear, 2, 14), "ITSA - POA 2", 2500, LocalDate.of(taxYear + 1, 2, 14), "Customer Request", Some("002")))

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(chargeHistories)
      ))

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsModelBothPoAsWithOutstandingAmount(taxYear, outstandingAmount))

      val f = fixture(LocalDate.of(taxYear, 8, 27))
      val result = f.testClaimToAdjustService.getAmendablePoaViewModel(testUserNino)

      whenReady(result) {
        result => result shouldBe Right(
          AmendablePoaViewModel(
            poaOneTransactionId = "DOCID01",
            poaTwoTransactionId = "DOCID02",
            taxYear = TaxYear(taxYear - 1, taxYear),
            poasHaveBeenAdjustedPreviously = true,
            paymentOnAccountOne = 150.00,
            paymentOnAccountTwo = 250.00,
            poARelevantAmountOne = 100.00,
            poARelevantAmountTwo = 100.00,
            poAPartiallyPaid = true,
            poAFullyPaid = true))
      }
    }

    "return a future left with an exception when an exception is thrown" in {

      val taxYear:Int = 2023

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      val financialDetailsModelWithoutDocumentDetails = financialDetailsModelBothPoAs

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsModelWithoutDocumentDetails)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(List.empty)
      ))

      val f = fixture(LocalDate.of(taxYear, 8, 27))
      val result = f.testClaimToAdjustService.getAmendablePoaViewModel(testUserNino)

      whenReady(result) {
        result => {
          result.isLeft shouldBe true
          result.left.exists(_.getMessage == "Failed to retrieve non-crystallised financial details") shouldBe true
        }
      }
    }
  }

  "getEnterPoAAmountViewModel" should {

    "return a future right with PoAAmountViewModel when POAs are unpaid" in {

      val taxYear:Int = 2023

      val chargeHistories: List[ChargeHistoryModel] = List(
        ChargeHistoryModel(s"$taxYear", "1040000124", LocalDate.of(taxYear, 2, 14), "ITSA- POA 1", 2500, LocalDate.of(taxYear + 1, 2, 14), "Customer Request", Some("001")),
        ChargeHistoryModel(s"$taxYear", "1040000125", LocalDate.of(taxYear, 2, 14), "ITSA - POA 2", 2500, LocalDate.of(taxYear + 1, 2, 14), "Customer Request", Some("002")))

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(chargeHistories)
      ))

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsWithUnpaidPoAs(taxYear))

      val f = fixture(LocalDate.of(taxYear, 8, 27))

      val result = f.testClaimToAdjustService.getEnterPoAAmountViewModel(testUserNino)

      whenReady(result) {
        result => result shouldBe Right(
          PoAAmountViewModel(false, true, TaxYear(2022, 2023), 150.00, 250.00, 100.00, 100.00))
      }
    }

    "return a future right with PoAAmountViewModel when POAs are paid" in {

      val taxYear:Int = 2023

      val outstandingAmount:BigDecimal = 0.0

      val chargeHistories: List[ChargeHistoryModel] = List(
        ChargeHistoryModel(s"$taxYear", "1040000124", LocalDate.of(taxYear, 2, 14), "ITSA- POA 1", 2500, LocalDate.of(taxYear + 1, 2, 14), "Customer Request", Some("001")),
        ChargeHistoryModel(s"$taxYear", "1040000125", LocalDate.of(taxYear, 2, 14), "ITSA - POA 2", 2500, LocalDate.of(taxYear + 1, 2, 14), "Customer Request", Some("002")))

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(chargeHistories)
      ))

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsModelBothPoAsWithOutstandingAmount(taxYear, outstandingAmount))

      val f = fixture(LocalDate.of(taxYear, 8, 27))

      val result = f.testClaimToAdjustService.getEnterPoAAmountViewModel(testUserNino)

      whenReady(result) {
        result => result shouldBe Right(
          PoAAmountViewModel(true, true, TaxYear(2022, 2023), 150.00, 250.00, 100.00, 100.00))
      }
    }

    "return a future left with an exception when an exception is thrown" in {

      val taxYear: Int = 2023

      setupGetCalculationList(testNino, "22-23")(calculationListSuccessResponseModelNonCrystallised)

      val financialDetailsModelWithoutDocumentDetails = financialDetailsModelBothPoAs

      setupMockGetFinancialDetails(taxYear, testNino)(financialDetailsModelWithoutDocumentDetails)

      setupGetChargeHistory(testNino, Some("ABCD1234"))(ChargesHistoryModel(
        idType = "NINO",
        idValue = testNino,
        regimeType = "ITSA",
        chargeHistoryDetails = Some(List.empty)
      ))

      val f = fixture(LocalDate.of(taxYear, 8, 27))
      val result = f.testClaimToAdjustService.getEnterPoAAmountViewModel(testUserNino)

      whenReady(result) {
        result => {
          result.isLeft shouldBe true
          result.left.exists(_.getMessage == "No financial details found for this charge") shouldBe true
        }
      }
    }
  }
}
