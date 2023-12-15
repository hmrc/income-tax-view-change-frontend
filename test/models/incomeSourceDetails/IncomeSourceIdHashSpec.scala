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

import exceptions.{MultipleIncomeSourcesFound, NoIncomeSourceFound}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.IncomeSourceIdHash.mkIncomeSourceIdHash
import models.core.{IncomeSourceId, IncomeSourceIdHash}
import testConstants.BaseTestConstants.{testSelfEmploymentId, testSelfEmploymentId2, testSelfEmploymentIdValidation}
import testUtils.UnitSpec

class IncomeSourceIdHashSpec extends UnitSpec {

  val hashValue: String = "4154473711487316523"

  "IncomeSourceIdHash class" should {

    "return IncomeSourceIdHash objects" when {

      "calling both the mkFromIncomeSourceId and mkFromQueryString methods" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val incomeSourceIdHashFromIncomeSourceId: IncomeSourceIdHash = mkIncomeSourceIdHash(incomeSourceId)

        val incomeSourceIdHashMaybe = IncomeSourceIdHash.mkFromQueryString(hashValue)
        incomeSourceIdHashMaybe shouldBe Right(incomeSourceIdHashFromIncomeSourceId)
      }
    }

    "return the hash of the incomeSourceId" when {
      "supplied with an incomeSourceId object" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val hashObjectHash: IncomeSourceIdHash = incomeSourceId.toHash
        val hashOfString = "4154473711487316523"

        hashObjectHash.hash shouldBe hashOfString
      }
    }

    "return the overridden toString of the incomeSourceIdHash" when {
      "created with both mkIncomeSourceIdHash and mkIncomeSourceIdHashFromQueryString methods" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val incomeSourceIdHash = IncomeSourceIdHash.mkIncomeSourceIdHash(incomeSourceId)

        val incomeSourceIdHashMaybe = IncomeSourceIdHash.mkFromQueryString(hashValue)
        incomeSourceIdHashMaybe shouldBe Right(incomeSourceIdHash)
      }
    }

    "return a matching incomeSourceId" when {
      "given a wanted IncomeSourceId and a list of potential matching IncomeSourceIds, one of which matches" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val incomeSourceId2: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId2)
        val incomeSourceIdList: List[IncomeSourceId] = List(incomeSourceId, incomeSourceId2)

        val incomeSourceIdHash = mkIncomeSourceId(testSelfEmploymentId).toHash
        val incomeSourceIdMatchingList: Either[Throwable, IncomeSourceId] = incomeSourceIdHash.findIncomeSourceIdMatchingHash(incomeSourceIdList)

        incomeSourceIdMatchingList shouldBe Right(incomeSourceId)
      }
    }

    "return a NoIncomeSourceFound exception" when {
      "given a wanted IncomeSourceId and a list non-matching IncomeSourceIds" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val incomeSourceId2: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId2)
        val incomeSourceIdList: List[IncomeSourceId] = List(incomeSourceId, incomeSourceId2)

        val incomeSourceIdHash = mkIncomeSourceId(testSelfEmploymentIdValidation).toHash
        val incomeSourceIdMatchingList: Either[Throwable, IncomeSourceId] = incomeSourceIdHash.findIncomeSourceIdMatchingHash(incomeSourceIdList)

        incomeSourceIdMatchingList shouldBe Left(NoIncomeSourceFound(incomeSourceIdHash.hash))
      }
    }

    "return a MultipleIncomeSourcesFound exception" when {
      "given a wanted IncomeSourceId and a list non-matching IncomeSourceIds" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val incomeSourceIdList: List[IncomeSourceId] = List(incomeSourceId, incomeSourceId)

        val incomeSourceIdHash = mkIncomeSourceId(testSelfEmploymentId).toHash
        val incomeSourceIdMatchingList: Either[Throwable, IncomeSourceId] = incomeSourceIdHash.findIncomeSourceIdMatchingHash(incomeSourceIdList)

        incomeSourceIdMatchingList shouldBe Left(MultipleIncomeSourcesFound(incomeSourceIdHash.hash, incomeSourceIdList.map(_.value)))
      }
    }

    "return true" when {
      "automated testing findings: two ids returning same hash" in {
        val idA = mkIncomeSourceId("458G97M2iCklmno")
        val idB = mkIncomeSourceId("47829VOJ5Tklmn6")
        idA.toHash.hash should not be idB.toHash.hash
      }
    }
  }

}
