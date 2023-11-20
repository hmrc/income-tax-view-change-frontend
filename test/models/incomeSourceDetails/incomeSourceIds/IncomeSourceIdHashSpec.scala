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

package models.incomeSourceDetails.incomeSourceIds

import models.incomeSourceDetails.incomeSourceIds.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.incomeSourceIds.{IncomeSourceId, IncomeSourceIdHash}
import org.scalatest.Failed
import testConstants.BaseTestConstants.{testSelfEmploymentId, testSelfEmploymentId2, testSelfEmploymentIdValidation}
import testUtils.UnitSpec

class IncomeSourceIdHashSpec extends UnitSpec {

  val hashValue: String = "1487316523"

  "IncomeSourceIdHash class" should {

    "return IncomeSourceIdHash objects" when {

      "calling both the mkFromIncomeSourceId and mkFromQueryString methods" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val incomeSourceIdHashFromIncomeSourceId: IncomeSourceIdHash = IncomeSourceIdHash.mkIncomeSourceIdHash(incomeSourceId)

        val incomeSourceIdHashFromQueryString: Either[Throwable, IncomeSourceIdHash] =
          IncomeSourceIdHash.mkIncomeSourceIdHashFromQueryString(hashValue)

        incomeSourceIdHashFromQueryString match {
          case Left(error) => Failed(s"IncomeSourceIdHash.mkFromQueryString returned an Either[Left[Throwable]] with value: $error")
          case Right(incomeSourceIdHash) => assert(incomeSourceIdHashFromIncomeSourceId == incomeSourceIdHash)
        }

      }
    }

    "return the hash of the incomeSourceId" when {
      "supplied with an incomeSourceId object" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val hashObjectHash: IncomeSourceIdHash = incomeSourceId.toHash
        val hashOfString = testSelfEmploymentId.hashCode().abs.toString

        hashObjectHash.hash shouldBe hashOfString
      }
    }

    "return the overridden toString of the incomeSourceIdHash" when {
      "created with both mkIncomeSourceIdHash and mkIncomeSourceIdHashFromQueryString methods" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val incomeSourceIdHashFromIncomeSourceIdToString: String = IncomeSourceIdHash.mkIncomeSourceIdHash(incomeSourceId).toString

        val incomeSourceIdHashFromQueryString: Either[Throwable, IncomeSourceIdHash] =
          IncomeSourceIdHash.mkIncomeSourceIdHashFromQueryString(hashValue)

        incomeSourceIdHashFromQueryString match {
          case Left(error) => Failed(s"IncomeSourceIdHash.mkFromQueryString returned an Either[Left[Throwable]] with value: $error")
          case Right(incomeSourceIdHash) => assert(incomeSourceIdHashFromIncomeSourceIdToString == incomeSourceIdHash.toString)
        }
      }
    }

    "return a matching incomeSourceId" when {
      "given a wanted IncomeSourceId and a list of potential matching IncomeSourceIds, one of which matches" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val incomeSourceId2: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId2)
        val incomeSourceIdList: List[IncomeSourceId] = List(incomeSourceId, incomeSourceId2)

        val incomeSourceIdMatchingList: Option[IncomeSourceId] = mkIncomeSourceId(testSelfEmploymentId).toHash.oneOf(incomeSourceIdList)

        incomeSourceIdMatchingList shouldBe Option(incomeSourceId)
      }
    }

    "return None" when {
      "given a wanted IncomeSourceId and a list non-matching IncomeSourceIds" in {
        val incomeSourceId: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId)
        val incomeSourceId2: IncomeSourceId = mkIncomeSourceId(testSelfEmploymentId2)
        val incomeSourceIdList: List[IncomeSourceId] = List(incomeSourceId, incomeSourceId2)

        val incomeSourceIdMatchingList: Option[IncomeSourceId] = mkIncomeSourceId(testSelfEmploymentIdValidation).toHash.oneOf(incomeSourceIdList)

        incomeSourceIdMatchingList shouldBe None
      }
    }
  }

}
