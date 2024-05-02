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
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.services.MockFinancialDetailsService
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import play.api.test.FakeRequest
import services.ClaimToAdjustService
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.claimToAdjustPOA.ClaimToAdjustPOATestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class ClaimToAdjustServiceSpec extends TestSupport with MockFinancialDetailsConnector with MockFinancialDetailsService with MockCalculationListConnector{

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


}
