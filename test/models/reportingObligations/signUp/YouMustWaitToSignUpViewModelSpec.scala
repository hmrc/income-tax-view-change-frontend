/*
 * Copyright 2025 HM Revenue & Customs
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

package models.reportingObligations.signUp

import models.incomeSourceDetails.TaxYear
import models.reportingObligations.signUp.YouMustWaitToSignUpViewModel
import testUtils.UnitSpec

class YouMustWaitToSignUpViewModelSpec extends UnitSpec {

  val testDate = TaxYear(2024, 2025)

  //TODO this needs to be updated in MISUV-10514
  "YouMustWaitToSignUpViewModel" should {
    "have the correct back URL" in {
      val viewModel = YouMustWaitToSignUpViewModel(testDate)
      viewModel.backUrl shouldBe controllers.routes.HomeController.show().url
    }

    "have the correct next tax year" in {
      val viewModel = YouMustWaitToSignUpViewModel(testDate)
      viewModel.nextTaxYear shouldBe "2025"
    }
  }
}


