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
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.MockClaimToAdjustService
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import org.mockito.Mockito.mock
import play.api.test.FakeRequest
import services.ClaimToAdjustService
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.claimToAdjustPOA.ClaimToAdjustPOATestConstants.{empty1553Response, userPOADetails2024}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import scala.concurrent.ExecutionContext

class ClaimToAdjustServiceSpec extends TestSupport with MockClaimToAdjustService with MockAuthenticationPredicate {

  override implicit val mockEC: ExecutionContext = mock(classOf[ExecutionContext])
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

        val result = TestClaimToAdjustService.maybePoATaxYear(user = testUser, hc = implicitly).futureValue

        result shouldBe Right(Some(TaxYear(startYear = 2023, endYear = 2024)))
      }
    }
    "return None" when {
      "a user has no 1553 data related to POA" in {
        mockGetAllFinancialDetails(List((2024, empty1553Response)))

        val result = TestClaimToAdjustService.maybePoATaxYear.futureValue

        result shouldBe Right(None)
      }
    }
  }

  "canCustomerClaimToAdjust method" should {
    "return true" when {
      "maybePoATaxYear returns a non empty value in its future" in {
        setupSpyMaybePoATaxYear(Some(TaxYear(2023, 2024)))
        val result = mockClaimToAdjustService.canCustomerClaimToAdjust.futureValue

        result shouldBe Right(Some(true))
      }
    }
    "return false" when {
      "maybePoATaxYear returns None in its future" in {
        setupSpyMaybePoATaxYear(None)
        val result = mockClaimToAdjustService.canCustomerClaimToAdjust.futureValue

        result shouldBe Right(Some(false))
      }
    }
  }

}
