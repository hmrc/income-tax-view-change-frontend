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

package models.incomeSourceDetails.viewmodels

import enums.IncomeSourceJourney.SelfEmployment
import models.incomeSourceDetails.IncomeSourceDetailsModel
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.BusinessDetailsTestConstants.{business1, business1NoLatency, business2, ceasedBusiness}
import testUtils.UnitSpec

class IncomeSourceCeasedObligationsViewModelSpec extends UnitSpec {

  "IncomeSourceCeasedObligationsViewModel apply" when {
    "provided with Obligation view model" should {
      "return IncomeSourceCeasedObligationsViewModel when there are two businesses, one with latency" in {
        val incomeSourceDetailsModel = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business2, business1), Nil)

        val expectedViewModel = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          businessName = Some("Business Name"),
          isAgent = false,
          remainingLatentBusiness = false,
          allBusinessesCeased = false
        )

        val appliedViewModel = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          businessName = Some("Business Name"),
          isAgent = false,
          incomeSourceDetailsModel = incomeSourceDetailsModel
        )

        appliedViewModel shouldBe expectedViewModel
      }

      "return IncomeSourceCeasedObligationsViewModel when there is one business with no latency" in {
        val incomeSourceDetailsModel = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1NoLatency), Nil)

        val expectedViewModel = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          businessName = Some("Business Name"),
          isAgent = false,
          remainingLatentBusiness = false,
          allBusinessesCeased = false
        )

        val appliedViewModel = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          businessName = Some("Business Name"),
          isAgent = false,
          incomeSourceDetailsModel = incomeSourceDetailsModel
        )

        appliedViewModel shouldBe expectedViewModel
      }

      "return IncomeSourceCeasedObligationsViewModel when there is one business with latency" in {
        val incomeSourceDetailsModel = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)

        val expectedViewModel = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          businessName = Some("Business Name"),
          isAgent = false,
          remainingLatentBusiness = true,
          allBusinessesCeased = false
        )

        val appliedViewModel = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          businessName = Some("Business Name"),
          isAgent = false,
          incomeSourceDetailsModel = incomeSourceDetailsModel
        )

        appliedViewModel shouldBe expectedViewModel
      }

      "return IncomeSourceCeasedObligationsViewModel when there is one business left that is ceased" in {
        val incomeSourceDetailsModel = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(ceasedBusiness), Nil)

        val expectedViewModel = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          businessName = Some("Business Name"),
          isAgent = false,
          remainingLatentBusiness = false,
          allBusinessesCeased = true
        )

        val appliedViewModel = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          businessName = Some("Business Name"),
          isAgent = false,
          incomeSourceDetailsModel = incomeSourceDetailsModel
        )

        appliedViewModel shouldBe expectedViewModel
      }
    }
  }
}
