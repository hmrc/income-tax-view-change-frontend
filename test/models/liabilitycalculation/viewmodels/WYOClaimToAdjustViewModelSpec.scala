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
      "return the tax year" in {
        val testModel: WYOClaimToAdjustViewModel = WYOClaimToAdjustViewModel(Some(TaxYear(2023, 2024)))
        testModel.claimToAdjustTaxYear shouldBe Some(TaxYear(2023, 2024))
      }
    }
}
