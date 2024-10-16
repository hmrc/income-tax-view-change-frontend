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
import models.itsaStatus.ITSAStatus._
import testUtils.TestSupport

class CurrentTaxYearOptOutSpec extends TestSupport{

  val currentTaxYear: TaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)

  "return canOptOut as true" when {
    "status is Voluntary" in {
      val optOut = CurrentTaxYearOptOut(Voluntary, currentTaxYear)
      optOut.canOptOut shouldBe true
    }
  }

  "return canOptOut as false" when {
    "status is NoStatus" in {
      val optOut = CurrentTaxYearOptOut(NoStatus, currentTaxYear)
      optOut.canOptOut shouldBe false
    }

    "status is Mandated" in {
      val optOut = CurrentTaxYearOptOut(Mandated, currentTaxYear)
      optOut.canOptOut shouldBe false
    }

    "status is Annual" in {
      val optOut = CurrentTaxYearOptOut(Annual, currentTaxYear)
      optOut.canOptOut shouldBe false
    }
  }

}
