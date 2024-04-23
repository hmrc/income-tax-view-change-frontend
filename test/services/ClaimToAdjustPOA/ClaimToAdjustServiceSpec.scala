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

package services.ClaimToAdjustPOA

import auth.MtdItUser
import mocks.connectors.MockFinancialDetailsConnector
import mocks.services.MockFinancialDetailsService
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import play.api.test.FakeRequest
import services.ClaimToAdjustService
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.claimToAdjustPOA.ClaimToAdjustPOATestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class ClaimToAdjustServiceSpec extends TestSupport with MockFinancialDetailsConnector with MockFinancialDetailsService {

  object TestClaimToAdjustService extends ClaimToAdjustService(mockFinancialDetailsConnector, dateService)

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

  "getPoATaxYear method" should {
    "return a taxYear" when {
      "a user has document details relating to PoA data" in {
        setupMockGetFinancialDetails(2024, testNino)(userPOADetails2024)
        setupMockGetFinancialDetailsError(2023, testNino)(financialDetailsErrorModel(2023))

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(Some(TaxYear(startYear = 2023, endYear = 2024)))
        }
      }
      "a user has document details relating to multiple years of PoA data" in {
        setupMockGetFinancialDetails(2024, testNino)(genericUserPOADetails(2024))
        setupMockGetFinancialDetails(2023, testNino)(genericUserPOADetails(2023))

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(Some(TaxYear(startYear = 2023, endYear = 2024)))
        }
      }
      "a user has document details relating to more poa 1 than poa 2 but the most recent tax year of PoAs is normal" in {
        setupMockGetFinancialDetails(2024, testNino)(genericUserPOADetails(2024))
        setupMockGetFinancialDetails(2023, testNino)(userPOADetails2023OnlyPOA1)

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(Some(TaxYear(startYear = 2023, endYear = 2024)))
        }
      }
      "a user has document details relating to more poa 2 than poa 1 but the most recent tax year of PoAs is normal" in {
        setupMockGetFinancialDetails(2024, testNino)(genericUserPOADetails(2024))
        setupMockGetFinancialDetails(2023, testNino)(userPOADetails2023OnlyPOA1)

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(Some(TaxYear(startYear = 2023, endYear = 2024)))
        }
      }
    }

    // TODO: fix this unit test
//    "return None" when {
//      "a user has no 1553 data related to POA" in {
//        setupMockGetFinancialDetailsError(2024, testNino)(financialDetailsErrorModel())
//        setupMockGetFinancialDetailsError(2023, testNino)(financialDetailsErrorModel())
//
//        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)
//
//        whenReady(result) {
//          result => result shouldBe Right(None)
//        }
//      }
//    }

    "return an Exception" when {
      "the most recent document for poa 1 is more recent than for poa 2" in {
        setupMockGetFinancialDetails(2024, testNino)(genericUserPOADetailsPOA1Only(2024))
        setupMockGetFinancialDetails(2023, testNino)(genericUserPOADetails(2023))

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result.toString shouldBe Left(new Exception("PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not.")).toString
        }
      }
      "the most recent document for poa 2 is more recent than for poa 1" in {
        setupMockGetFinancialDetails(2024, testNino)(genericUserPOADetailsPOA2Only(2024))
        setupMockGetFinancialDetails(2023, testNino)(genericUserPOADetails(2023))

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result.toString shouldBe Left(new Exception("PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not.")).toString
        }
      }
      "there is a poa document for poa 1 but nothing for poa 2" in {
        setupMockGetFinancialDetails(2024, testNino)(genericUserPOADetailsPOA1Only(2024))
        setupMockGetFinancialDetailsError(2023, testNino)(financialDetailsErrorModel())
        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result.toString shouldBe Left(new Exception("PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not.")).toString
        }
      }
      "there is a poa document for poa 2 but nothing for poa 1" in {
        setupMockGetFinancialDetails(2024, testNino)(genericUserPOADetailsPOA2Only(2024))
        setupMockGetFinancialDetailsError(2023, testNino)(financialDetailsErrorModel())

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result.toString shouldBe Left(new Exception("PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not.")).toString
        }
      }
    }
  }
}
