/*
 * Copyright 2025 HM Revenue & Customs
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

package utils

import mocks.services.MockDateService
import org.mockito.Mockito.when
import services.DateService
import testUtils.TestSupport

import java.time.LocalDate

class MtdConstantsSpec extends TestSupport with MockDateService {

  val mtdConstants = new MtdConstants {
    override val dateService: DateService = mockDateService
  }

  "mtdConstants" when {

    "getMtdThreshold is called" should {

      "return the base threshold before 6 April 2027" in {

        val fakeDate = LocalDate.of(2027, 4, 5)
        when(mockDateService.getCurrentDate).thenReturn(fakeDate)

        mtdConstants.getMtdThreshold() shouldBe "£50,000"
      }
      "return the threshold from 6 April 2027 to 5 April 2028" in {
        val fakeDate = LocalDate.of(2027, 4, 6)
        when(mockDateService.getCurrentDate).thenReturn(fakeDate)

        mtdConstants.getMtdThreshold() shouldBe "£30,000"
      }
      "return the threshold from 6 April 2028 onwards" in {
        val fakeDate = LocalDate.of(2028, 4, 6)
        when(mockDateService.getCurrentDate).thenReturn(fakeDate)

        mtdConstants.getMtdThreshold() shouldBe "£20,000"
      }
    }
  }
}
