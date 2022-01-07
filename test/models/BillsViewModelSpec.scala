/*
 * Copyright 2022 HM Revenue & Customs
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

package models

import config.featureswitch.FeatureSwitching
import models.calculation.BillsViewModel
import org.scalatest.Matchers
import testUtils.TestSupport

class BillsViewModelSpec extends TestSupport with Matchers with FeatureSwitching {

  "eligibleForPayment" should {

    "return a false" when {


      "bill is paid" in {
        BillsViewModel(10000, isPaid = true, 2019).eligibleForPayment() shouldBe false
      }
    }

    "return a true" when {

      "The bill is not paid" in {
        BillsViewModel(10000, isPaid = false, 2019).eligibleForPayment() shouldBe true
      }
    }
  }
}
