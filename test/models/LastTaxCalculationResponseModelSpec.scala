/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.TestConstants.Estimates._
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec
import enums._

class LastTaxCalculationResponseModelSpec extends UnitSpec with Matchers {

  "The LastTaxCalculationWithYear model" when {

    "contains an Estimate calc" should {

      "return 'true' when the method matchesStatus is called with Estimate" in {
        LastTaxCalculationWithYear(lastTaxCalcSuccess, testYear).matchesStatus(Estimate) shouldBe true
      }

      "return 'false' when the method matchesStatus is called with Crystallised" in {
        LastTaxCalculationWithYear(lastTaxCalcSuccess, testYear).matchesStatus(Crystallised) shouldBe false
      }
    }

    "contains a Crystallised calc" should {

      "return 'true' when the method matchesStatus is called with Crystallised" in {
        LastTaxCalculationWithYear(lastTaxCalcCrystallisedSuccess, testYear).matchesStatus(Crystallised) shouldBe true
      }

      "return 'false' when the method matchesStatus is called with Estimate" in {
        LastTaxCalculationWithYear(lastTaxCalcCrystallisedSuccess, testYear).matchesStatus(Estimate) shouldBe false
      }
    }

    "return 'false' when the model passed in is not a LastTaxCalculation model" in {
      LastTaxCalculationWithYear(LastTaxCalculationError(1,""), testYear).matchesStatus(Crystallised) shouldBe false
    }
  }

}
