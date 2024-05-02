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
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import play.api.test.FakeRequest
import services.ClaimToAdjustService
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.claimToAdjustPOA.ClaimToAdjustPOATestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class ClaimToAdjustServiceSpec extends TestSupport with MockFinancialDetailsConnector with MockFinancialDetailsService with MockCalculationListConnector {

  object TestClaimToAdjustService extends ClaimToAdjustService(mockFinancialDetailsConnector, mockCalculationListConnector, dateService)

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

  // TODO: implement UT with referring to correct method under test

  "getPoaTaxYearForEntryPoint method" should {
    "return an option containing a taxYear" when {
      "a user has document details relating to PoA data, for a CTA amendable year that is non-crystallised" in {
      }
    }
  }

  "arePoAPaymentsPresent method" should {
    "return an option containing a taxYear" when {
      "given a list of DocumentDetail models from the same tax year" when {
        "POA 1 or 2s are contained within these DocumentDetails" in {

        }
      }
    }
  }

  "getPoaAdjustableTaxYears method" should {
    "return a list containing just the current TaxYear" when {
      "the current date is between the POA deadline and tax year end date (31st Feb and 5th April)" in {

      }
    }
  }

  "checkCrystallisation method" should {
    "return a future of an option containing a tax year" when {
      "the user has not crystallised said given tax year" in {

      }
    }
  }

  "isFutureTaxYear method" should {
    "return true" when {
      "the tax year given is in a future tax year" in {

      }
    }
  }

  "isTaxYearNonCrystallised method" should {
    "return a future containing true" when {
      "the user has not crystallised a given tax year" in {

      }
    }
  }
}
