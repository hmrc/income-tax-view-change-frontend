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

package services.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, Mandated, NoStatus, Voluntary}
import testUtils.TestSupport

class PreviousOptOutYearSpec extends TestSupport{

  val currentTaxYear: TaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  val previousTaxYear: TaxYear = currentTaxYear.addYears(-1)


  "return canOptOut as true" when {
    "Previous year status is Voluntary and NOT crystallised" in {
      val optOut = PreviousTaxYearOptOut(Voluntary, previousTaxYear, crystallised = false)
      optOut.canOptOut shouldBe true
    }
  }

  "return canOptOut as false" when {
    "Previous year status is Voluntary and IS crystallised" in {
      val optOut = PreviousTaxYearOptOut(Voluntary, previousTaxYear, crystallised = true)
      optOut.canOptOut shouldBe false
    }

    "Previous year status is NoStatus and IS crystallised" in {
      val optOut = PreviousTaxYearOptOut(NoStatus, previousTaxYear, crystallised = true)
      optOut.canOptOut shouldBe false
    }

    "Previous year status is Mandated and IS crystallised" in {
      val optOut = PreviousTaxYearOptOut(Mandated, previousTaxYear, crystallised = true)
      optOut.canOptOut shouldBe false
    }

    "Previous year status is Annual and IS crystallised" in {
      val optOut = PreviousTaxYearOptOut(Annual, previousTaxYear, crystallised = true)
      optOut.canOptOut shouldBe false
    }

    "Previous year status is NoStatus and NOT crystallised" in {
      val optOut = PreviousTaxYearOptOut(NoStatus, previousTaxYear, crystallised = false)
      optOut.canOptOut shouldBe false
    }

    "Previous year status is Mandated and NOT crystallised" in {
      val optOut = PreviousTaxYearOptOut(Mandated, previousTaxYear, crystallised = false)
      optOut.canOptOut shouldBe false
    }

    "Previous year status is Annual and NOT crystallised" in {
      val optOut = PreviousTaxYearOptOut(Annual, previousTaxYear, crystallised = false)
      optOut.canOptOut shouldBe false
    }
  }
}
