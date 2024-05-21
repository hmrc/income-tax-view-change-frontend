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

package models.optout

import models.incomeSourceDetails.TaxYear
import play.api.mvc.Call
import testUtils.UnitSpec

class OptOutOneYearViewModelSpec extends UnitSpec {

  val singleYearOptOutConfirmationIndividual: Call = controllers.optout.routes.SingleYearOptOutWarningController.show(isAgent = false)
  val singleYearOptOutConfirmationAgent: Call = controllers.optout.routes.SingleYearOptOutWarningController.show(isAgent = true)
  val confirmOptOutIndividual: Call = controllers.optout.routes.ConfirmOptOutController.show()
  val confirmOptOutAgent: Call = controllers.optout.routes.ConfirmOptOutController.showAgent()

  val taxYear: TaxYear = TaxYear.forYearEnd(2022)
  "OptOutOneYearViewModel.optOutConfirmationLink" when {
    "show warning is true" should {
      s"Individual - return a $singleYearOptOutConfirmationIndividual " in {
        OptOutOneYearViewModel(taxYear, showWarning = true).optOutConfirmationLink(isAgent = false) shouldBe singleYearOptOutConfirmationIndividual
      }
      s"Agent - return a $singleYearOptOutConfirmationAgent" in {
        OptOutOneYearViewModel(taxYear, showWarning = true).optOutConfirmationLink(isAgent = true) shouldBe singleYearOptOutConfirmationAgent
      }
    }

    "show warning is false" should {
      s"Individual - return a $confirmOptOutIndividual" in {
        OptOutOneYearViewModel(taxYear).optOutConfirmationLink(isAgent = false) shouldBe confirmOptOutIndividual
      }
      s"Agent - return a $confirmOptOutAgent" in {
        OptOutOneYearViewModel(taxYear).optOutConfirmationLink(isAgent = true) shouldBe confirmOptOutAgent
      }
    }

  }

}
