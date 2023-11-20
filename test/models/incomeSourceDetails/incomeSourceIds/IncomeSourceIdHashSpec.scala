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

import models.incomeSourceDetails.incomeSourceIds.{IncomeSourceId, IncomeSourceIdHash}
import models.incomeSourceDetails.incomeSourceIds.IncomeSourceId.mkIncomeSourceId
import testConstants.BaseTestConstants.testSelfEmploymentId
import testUtils.UnitSpec

class IncomeSourceIdHashSpec extends UnitSpec {

  "IncomeSourceIdHash object" should {
    "return the hash of the incomeSourceId" when {
      "supplied with an incomeSourceId object" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val hashObjectHash: IncomeSourceIdHash = incomeSourceId.toHash
        val hashOfString = testSelfEmploymentId.hashCode().abs.toString

        hashObjectHash.hash shouldBe hashOfString
      }
    }

    "return the overridden toString of the incomeSourceId" when {
      "supplied with an incomeSourceId object" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val hashObjectHash: IncomeSourceIdHash = incomeSourceId.toHash
        val hashOfString = testSelfEmploymentId.hashCode().abs.toString
        val incomeSourceIdHashToString: String = s"IncomeSourceIdHash: $hashOfString"

        hashObjectHash.toString shouldBe incomeSourceIdHashToString
      }
    }
  }

}
