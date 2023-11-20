/*
 * Copyright 2023 HM Revenue & Customs
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

package models.incomeSourceIds

import models.incomeSourceDetails.incomeSourceIds.IncomeSourceId.{mkIncomeSourceId, validateStringAsIncomeSourceId}
import models.incomeSourceDetails.incomeSourceIds.{IncomeSourceId, IncomeSourceIdHash}
import testConstants.BaseTestConstants.{testSelfEmploymentId, testSelfEmploymentIdValidation}
import testUtils.UnitSpec

class IncomeSourceIdSpec extends UnitSpec {

  "IncomeSourceId object" should {
    "return the overridden toString of the incomeSourceId" when {
      "on an incomeSourceId object" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val incomeSourceIdValue = incomeSourceId.value
        val incomeSourceIdToString: String = s"IncomeSourceId: $incomeSourceIdValue"

        incomeSourceId.toString shouldBe incomeSourceIdToString
      }
    }
    "return true" when {
      "validateStringAsIncomeSourceId method is called on a valid incomeSourceId value" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentIdValidation)
        val incomeSourceIdValidation: Boolean = validateStringAsIncomeSourceId(incomeSourceId.value)

        incomeSourceIdValidation shouldBe true
      }
    }
    "return false" when {
      "validateStringAsIncomeSourceId method is called on a valid incomeSourceId value" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val incomeSourceIdValidation: Boolean = validateStringAsIncomeSourceId(incomeSourceId.value)

        incomeSourceIdValidation shouldBe false
      }
    }
  }

}
