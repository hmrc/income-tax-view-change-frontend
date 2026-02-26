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

package services.reportingObligations.optOut.optouttaxyear

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import services.reportingObligations.optOut.CurrentOptOutTaxYear
import testUtils.UnitSpec

class CurrentTaxYearOptOutSpec extends UnitSpec {

  "CurrentTaxYearOptOut" should {
    "offer opt-out" when {
      s"current year is ${ITSAStatus.Voluntary}" in {
        CurrentOptOutTaxYear(ITSAStatus.Voluntary, TaxYear.forYearEnd(2024)).canOptOut shouldBe true
      }
    }

    "not offer opt-out" when {
      s"current year is ${ITSAStatus.Mandated}" in {
        CurrentOptOutTaxYear(ITSAStatus.Mandated, TaxYear.forYearEnd(2024)).canOptOut shouldBe false
      }

      s"current year is ${ITSAStatus.Annual}" in {
        CurrentOptOutTaxYear(ITSAStatus.Annual, TaxYear.forYearEnd(2024)).canOptOut shouldBe false
      }

      s"current year is ${ITSAStatus.NoStatus}" in {
        CurrentOptOutTaxYear(ITSAStatus.NoStatus, TaxYear.forYearEnd(2024)).canOptOut shouldBe false
      }
    }
  }
}
