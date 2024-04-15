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

package models

import models.homePage.NextPaymentsTileViewModel
import org.scalatest.matchers.should.Matchers
import testUtils.UnitSpec

import java.time.LocalDate

class NextPaymentsTileViewModelSpec extends UnitSpec with Matchers{

  val mockDate = Some(LocalDate.parse("2022-08-16"))
  val mockNextPaymentsTileViewModel: NextPaymentsTileViewModel = NextPaymentsTileViewModel(mockDate, 1)

  "The verify method" when {
    "overdue payment exists" should {
      "create the NextPaymentsTileViewModel" in {
        NextPaymentsTileViewModel(mockDate, 1) shouldBe mockNextPaymentsTileViewModel
      }
    }

    "overdue payment doesn't exist" should {
      "throw an Exception" in {
        NextPaymentsTileViewModel(None, 1).verify.isLeft shouldBe true
      }
    }
  }
}
