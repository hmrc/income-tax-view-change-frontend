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
import testConstants.claimToAdjustPOA.ClaimToAdjustPOATestConstants.{empty1553Response, genericUserPOADetails, genericUserPOADetailsPOA1Only, genericUserPOADetailsPOA2Only, userPOADetails2018OnlyPOA1, userPOADetails2018OnlyPOA2, userPOADetails2024}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class ClaimToAdjustServiceSpec extends TestSupport with MockFinancialDetailsConnector with MockFinancialDetailsService {

  object TestClaimToAdjustService extends ClaimToAdjustService(mockFinancialDetailsConnector, mockFinancialDetailsService)

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    None,
    incomeSources = IncomeSourceDetailsModel(testNino, "123", None, List.empty, List.empty),
    None,
    Some("1234567890"),
    Some("12345-credId"),
    Some(Individual),
    None
  )(FakeRequest())

  "maybePoATaxYear method" should {
    "return a taxYear" when {
      "a user has document details relating to PoA data" in {
        mockGetAllFinancialDetails(List((2024, userPOADetails2024)))

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(Some(TaxYear(startYear = 2023, endYear = 2024)))
        }
      }
      "a user has document details relating to multiple years of PoA data" in {
        mockGetAllFinancialDetails(List(
          (2023, genericUserPOADetails(2023)),
          (2022, genericUserPOADetails(2022))
        ))

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(Some(TaxYear(startYear = 2022, endYear = 2023)))
        }
      }
      "a user has document details relating to more poa 1 than poa 2 but the most recent tax year of PoAs is normal" in {
        mockGetAllFinancialDetails(List(
          (2023, genericUserPOADetails(2023)),
          (2022, genericUserPOADetails(2022)),
          (2018, userPOADetails2018OnlyPOA1)
        ))

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(Some(TaxYear(startYear = 2022, endYear = 2023)))
        }
      }
      "a user has document details relating to more poa 2 than poa 1 but the most recent tax year of PoAs is normal" in {
        mockGetAllFinancialDetails(List(
          (2023, genericUserPOADetails(2023)),
          (2022, genericUserPOADetails(2022)),
          (2018, userPOADetails2018OnlyPOA2)
        ))

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(Some(TaxYear(startYear = 2022, endYear = 2023)))
        }
      }
    }
    "return None" when {
      "a user has no 1553 data related to POA" in {
        mockGetAllFinancialDetails(List((2024, empty1553Response)))

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe Right(None)
        }
      }
    }
    "return an Exception" when {
      "getAllFinancialDetails returns a failed future" in {
        mockGetAllFinancialDetails(List.empty)

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result.toString shouldBe Left(new Exception("User has no financial details")).toString
        }
      }
      "the most recent document for poa 1 is more recent than for poa 2" in {
        mockGetAllFinancialDetails(List(
          (2024, genericUserPOADetailsPOA1Only(2024)),
          (2023, genericUserPOADetails(2023))))

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result.toString shouldBe Left(new Exception("PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not.")).toString
        }
      }
      "the most recent document for poa 2 is more recent than for poa 1" in {
        mockGetAllFinancialDetails(List(
          (2024, genericUserPOADetailsPOA2Only(2024)),
          (2023, genericUserPOADetails(2023))))

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result.toString shouldBe Left(new Exception("PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not.")).toString
        }
      }
      "there is a poa document for poa 1 but nothing for poa 2" in {
        mockGetAllFinancialDetails(List(
          (2024, genericUserPOADetailsPOA1Only(2024))
        ))
        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result.toString shouldBe Left(new Exception("PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not.")).toString
        }
      }
      "there is a poa document for poa 2 but nothing for poa 1" in {
        mockGetAllFinancialDetails(List(
          (2024, genericUserPOADetailsPOA2Only(2024))
        ))

        val result = TestClaimToAdjustService.getPoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result.toString shouldBe Left(new Exception("PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not.")).toString
        }
      }
    }
  }
}
