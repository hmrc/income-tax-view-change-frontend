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
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockClaimToAdjustService, MockFinancialDetailsService}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{mock, reset, spy, when}
import play.api.test.FakeRequest
import services.ClaimToAdjustService
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.claimToAdjustPOA.ClaimToAdjustPOATestConstants.{empty1553Response, userPOADetails2024}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import scala.concurrent.{ExecutionContext, Future}

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

        val result = TestClaimToAdjustService.maybePoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe Some(TaxYear(startYear = 2023, endYear = 2024))
        }
      }
    }
    "return None" when {
      "a user has no 1553 data related to POA" in {
        mockGetAllFinancialDetails(List((2024, empty1553Response)))

        val result = TestClaimToAdjustService.maybePoATaxYear(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe None
        }
      }
    }
  }

  "canCustomerClaimToAdjust method" should {
    "return true" when {
      "maybePoATaxYear returns a non empty value in its future" in {
        mockGetAllFinancialDetails(List((2024, userPOADetails2024)))

        val result = TestClaimToAdjustService.canCustomerClaimToAdjust(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe true
        }
      }
    }
    "return false" when {
      "maybePoATaxYear returns None in its future" in {
        mockGetAllFinancialDetails(List((2024, empty1553Response)))

        val result = TestClaimToAdjustService.canCustomerClaimToAdjust(user = testUser, hc = implicitly)

        whenReady(result) {
          result => result shouldBe false
        }
      }
    }
  }
}
