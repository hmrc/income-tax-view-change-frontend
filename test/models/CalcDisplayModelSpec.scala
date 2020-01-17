/*
 * Copyright 2020 HM Revenue & Customs
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

import assets.CalcBreakdownTestConstants
import enums.Estimate
import models.calculation.{CalcDisplayModel, Calculation}
import org.scalatest.Matchers
import testUtils.TestSupport

class CalcDisplayModelSpec extends TestSupport with Matchers {

  val calculation = Calculation(crystallised = false)

  "whatYouOwe" should {

    "display the income tax total" when {

      "calculation data exists" in {
        CalcDisplayModel("", 149.86, CalcBreakdownTestConstants.justBusinessCalcDataModel, Estimate).whatYouOwe shouldBe "&pound;149.86"
      }
    }

    "display the calculation amount" when {

      "calculation data does not exist" in {
        CalcDisplayModel("", 2, calculation, Estimate).whatYouOwe shouldBe "&pound;2"
      }
    }
  }

  "displayCalcBreakdown" should {

    "return a true " when {

      "breakdown is enabled and is not empty" in {
        frontendAppConfig.features.calcBreakdownEnabled(true)
        CalcDisplayModel("", 2, CalcBreakdownTestConstants.justBusinessCalcDataModel, Estimate).displayCalcBreakdown(frontendAppConfig) shouldBe true
      }
    }

    "return a false" when {

      "breakdown is disabled" in {
        frontendAppConfig.features.calcBreakdownEnabled(false)
        CalcDisplayModel("", 2, calculation, Estimate).displayCalcBreakdown(frontendAppConfig) shouldBe false
      }


      "breakdown is empty " in {
        frontendAppConfig.features.calcBreakdownEnabled(true)
        CalcDisplayModel("", 2, calculation, Estimate).displayCalcBreakdown(frontendAppConfig) shouldBe true
      }
    }
  }
}
