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

class NextTaxYearOptOutSpec extends TestSupport{

  val currentTaxYear: TaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  val nextTaxYear: TaxYear = currentTaxYear.addYears(1)


  "return canOptOut as true" when {
    "Next tax year status is Voluntary" in {
      val optOut = NextTaxYearOptOut(Voluntary, nextTaxYear, CurrentTaxYearOptOut(NoStatus, currentTaxYear))
      optOut.canOptOut shouldBe true
    }

    "Next tax year status is Unknown but current tax year status is Voluntary" in {
      val optOut = NextTaxYearOptOut(NoStatus, nextTaxYear, CurrentTaxYearOptOut(Voluntary, currentTaxYear))
      optOut.canOptOut shouldBe true
    }
  }

  "return canOptOut as false" when {
    "Next tax year status is no status AND current year status is not voluntary" in {
      val optOut = NextTaxYearOptOut(NoStatus, nextTaxYear, CurrentTaxYearOptOut(NoStatus, currentTaxYear))
      optOut.canOptOut shouldBe false
    }

    "Next tax year status is Mandated" in {
      val optOut = NextTaxYearOptOut(Mandated, nextTaxYear, CurrentTaxYearOptOut(Voluntary, currentTaxYear))
      optOut.canOptOut shouldBe false
    }

    "Next tax year status is Annual" in {
      val optOut = NextTaxYearOptOut(Annual, nextTaxYear, CurrentTaxYearOptOut(Voluntary, currentTaxYear))
      optOut.canOptOut shouldBe false
    }
  }

}
