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

package models.incomeSourceDetails

import exceptions.NoIncomeSourceFound
import forms.IncomeSourcesFormsSpec.individualUser
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.{IncomeSourceId, IncomeSourceIdHash}
import testConstants.BaseTestConstants.{testMtdItUser, testSelfEmploymentId}
import testUtils.UnitSpec

class IncomeSourceIdSpec extends UnitSpec {

  val incomeSourceIdHash: IncomeSourceIdHash = mkIncomeSourceId("1234").toHash

  "IncomeSourceId class" should {
    "return an IncomeSourceIdHash" when {
      "the .toHash method is called" in {

        val result: IncomeSourceIdHash = mkIncomeSourceId("1234").toHash

        result shouldBe incomeSourceIdHash
      }
    }
  }

  "IncomeSourceId object" should {
    "return an Option containing an incomeSourceId" when {
      "provided an input of type Option[Right[IncomeSourceId]]" in {

        val user = testMtdItUser

        val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = Option(mkIncomeSourceId(testSelfEmploymentId).toHash)

        val hashCompareResult: Option[Either[Throwable, IncomeSourceId]] = incomeSourceIdHashMaybe.map(x => user.incomeSources.compareHashToQueryString(x))

        val result: Option[IncomeSourceId] = IncomeSourceId.toOption(hashCompareResult)

        result shouldBe Some(mkIncomeSourceId(testSelfEmploymentId))

      }
    }

    "return None" when {
      "provided an input of None" in {
        val result = IncomeSourceId.toOption(None)

        result shouldBe None
      }
      "provided an input of type Option[Left[Throwable]]" in {

        val result = IncomeSourceId.toOption(Some(Left(NoIncomeSourceFound(""))))

        result shouldBe None
      }
    }
  }


  }
