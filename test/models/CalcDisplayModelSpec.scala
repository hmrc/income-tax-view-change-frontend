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

import assets.CalcBreakdownTestConstants
import enums.Estimate
import models.calculation.CalcDisplayModel
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec

class CalcDisplayModelSpec extends UnitSpec with Matchers {

  "whatYouOwe" should {

    "display the income tax total" when {

      "calculation data exists" in {
        CalcDisplayModel("", 2, Some(CalcBreakdownTestConstants.justBusinessCalcDataModel), Estimate).whatYouOwe shouldBe "&pound;149.86"
      }
    }

    "display the calculation amount" when {

      "calculation data does not exist" in {
        CalcDisplayModel("", 2, None, Estimate).whatYouOwe shouldBe "&pound;2"
      }
    }
  }
}
