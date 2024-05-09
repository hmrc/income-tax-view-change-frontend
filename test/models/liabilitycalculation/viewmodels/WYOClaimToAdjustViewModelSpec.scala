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

package models.liabilitycalculation.viewmodels

import models.incomeSourceDetails.TaxYear
import models.nextPayments.viewmodels.WYOClaimToAdjustViewModel
import testUtils.UnitSpec

class WYOClaimToAdjustViewModelSpec extends UnitSpec {

  "WYOClaimToAdjustViewModel claimToAdjustTaxYear val" when {
    "adjustPaymentsOnAccountFSEnabled is false" should {
      "return None" in {
        val testModel: WYOClaimToAdjustViewModel = WYOClaimToAdjustViewModel(adjustPaymentsOnAccountFSEnabled = false, Some(TaxYear(2023, 2024)))

        testModel.claimToAdjustTaxYear shouldBe None
      }
    }
    "adjustPaymentsOnAccountFSEnabled is true" should {
      "return None" in {
        val testModel: WYOClaimToAdjustViewModel = WYOClaimToAdjustViewModel(adjustPaymentsOnAccountFSEnabled = true, Some(TaxYear(2023, 2024)))

        testModel.claimToAdjustTaxYear shouldBe Some(TaxYear(2023, 2024))
      }
    }
  }
  "WYOClaimToAdjustViewModel object" when {
    "ctaLink val is called for an individual" should {
      "return the correct redirect link" in {
        WYOClaimToAdjustViewModel.ctaLink(false) shouldBe "/report-quarterly/income-and-expenses/view/adjust-poa/start"
      }
    }
    "ctaLink val is called for an agent" should {
      "return the correct redirect link" in {
        WYOClaimToAdjustViewModel.ctaLink(true) shouldBe "/report-quarterly/income-and-expenses/view/agents/adjust-poa/start"
      }
    }
  }

}
