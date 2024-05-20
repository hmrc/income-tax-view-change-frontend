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

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class OptOutPropositionTest extends AnyWordSpecLike with Matchers with BeforeAndAfter {

  "OptOutData" when {

    "only previous year is available for opt-out" should {

      "return true for isOneYearOptOut " in {
        val data = OptOutTestSupport.buildOneYearOptOutDataForPreviousYear()
        data.isOneYearOptOut shouldBe true
        data.isMultiYearOptOut shouldBe false
        data.isNoOptOutAvailable shouldBe false
      }
    }

    "only current year is available for opt-out" should {

      "return true for isOneYearOptOut " in {
        val data = OptOutTestSupport.buildOneYearOptOutDataForCurrentYear()
        data.isOneYearOptOut shouldBe true
        data.isMultiYearOptOut shouldBe false
        data.isNoOptOutAvailable shouldBe false
      }
    }

    "only next year is available for opt-out" should {

      "return true for isOneYearOptOut " in {
        val data = OptOutTestSupport.buildOneYearOptOutDataForNextYear()
        data.isOneYearOptOut shouldBe true
        data.isMultiYearOptOut shouldBe false
        data.isNoOptOutAvailable shouldBe false
      }
    }
  }

}