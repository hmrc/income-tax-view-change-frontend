/*
 * Copyright 2019 HM Revenue & Customs
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

import models.calculation.BillsViewModel
import org.scalatest.Matchers
import testUtils.TestSupport

class BillsViewModelSpec extends TestSupport with Matchers {

  "eligibleForPayment" should {

    "return a false" when {

      "payments are not enabled" in {
        frontendAppConfig.features.paymentEnabled(false)
        BillsViewModel(10000, isPaid = false, 2019).eligibleForPayment(frontendAppConfig) shouldBe false
      }

      "bill is paid" in {
        frontendAppConfig.features.paymentEnabled(true)
        BillsViewModel(10000, isPaid = true, 2019).eligibleForPayment(frontendAppConfig) shouldBe false
      }
    }

    "return a true" when {

      "payments are enabled and the bill is not paid" in {
        frontendAppConfig.features.paymentEnabled(true)
        BillsViewModel(10000, isPaid = false, 2019).eligibleForPayment(frontendAppConfig) shouldBe true
      }
    }
  }
}
