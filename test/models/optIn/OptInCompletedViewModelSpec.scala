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

package models.optIn

import models.incomeSourceDetails.TaxYear
import models.optin.OptInCompletedViewModel
import testUtils.UnitSpec

class OptInCompletedViewModelSpec extends UnitSpec {

  val endTaxYear = 2023
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(endTaxYear)

  s"OptInCompletedViewModel - for year ${currentTaxYear.toString}" should {

    val optInTaxYear = currentTaxYear

    s"Individual" in {
      val model = OptInCompletedViewModel(isAgent = false, optInTaxYear = optInTaxYear, isCurrentYear = false)

      model.startYear shouldBe "2022"
      model.endYear shouldBe "2023"
      model.isAgent shouldBe false
      model.nextYear shouldBe "2024"
    }

    s"Agent" in {
      val model = OptInCompletedViewModel(isAgent = true, optInTaxYear = optInTaxYear, isCurrentYear = true)

      model.startYear shouldBe "2022"
      model.endYear shouldBe "2023"
      model.isAgent shouldBe true
      model.nextYear shouldBe "2024"
    }
  }

  s"OptInCompletedViewModel - for year ${currentTaxYear.nextYear.toString}" should {

    val optInTaxYear = currentTaxYear.nextYear

    s"Individual" in {
      val model = OptInCompletedViewModel(isAgent = false, optInTaxYear = optInTaxYear, isCurrentYear = true)

      model.startYear shouldBe "2023"
      model.endYear shouldBe "2024"
      model.isAgent shouldBe false
      model.nextYear shouldBe "2025"
    }

    s"Agent" in {
      val model = OptInCompletedViewModel(isAgent = true, optInTaxYear = optInTaxYear, isCurrentYear = true)

      model.startYear shouldBe "2023"
      model.endYear shouldBe "2024"
      model.isAgent shouldBe true
      model.nextYear shouldBe "2025"
    }
  }

}
