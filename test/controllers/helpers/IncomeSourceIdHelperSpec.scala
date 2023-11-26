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

package controllers.helpers

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.IncomeSourceIdHash.mkFromQueryString
import models.core.{IncomeSourceId, IncomeSourceIdHash}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{dualBusinessIncome, noIncomeDetails, singleBusinessIncome, singleBusinessIncome2023}
import testUtils.TestSupport
import utils.IncomeSourceIdUtils

class IncomeSourceIdHelperSpec extends TestSupport with FeatureSwitching {


  val incomeSourceIdHelper: IncomeSourceIdUtils = new IncomeSourceIdUtils {}

  val testQueryString: String = mkIncomeSourceId("XA00001234").toHash.hash
  val testSelfEmploymentIdHash: Option[IncomeSourceIdHash] = mkFromQueryString(testQueryString)
  val testSelfEmploymentIdMaybe: Option[IncomeSourceId] = Option(mkIncomeSourceId("XA00001234"))
  val testSelfEmploymentIdHashValueMaybe: Option[String] = Option(testQueryString)

  ".mkIncomeSourceHashMaybe" when {
    "user has an incomeSourceHash query string in the url" should {
      "return an Option containing incomeSourceHash" in {
        val result = incomeSourceIdHelper.mkIncomeSourceHashMaybe(id = testSelfEmploymentIdHashValueMaybe)

        result shouldBe testSelfEmploymentIdHash
      }
    }
    "user has no incomeSourceHash query string in the url" should {
      "return None" in {
        val result = incomeSourceIdHelper.mkIncomeSourceHashMaybe(id = None)

        result shouldBe None
      }
    }
  }

  ".compare" when {
    "user has income incomeSourceIdHashes matching the url incomeSourceIdHash" should {
      "return the matching incomeSourceId inside an Option" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, singleBusinessIncome)

        val result = incomeSourceIdHelper.compare(incomeSourceIdHash = testSelfEmploymentIdHash)(user = user)

        result shouldBe testSelfEmploymentIdMaybe
      }
    }
    "user has multiple incomeSourceIdHashes matching the url incomeSourceIdHash" should {
      "return the matching incomeSourceId inside an Option" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, dualBusinessIncome)

        val result = incomeSourceIdHelper.compare(incomeSourceIdHash = testSelfEmploymentIdHash)(user = user)

        result shouldBe testSelfEmploymentIdMaybe
      }
    }
    "user has no incomeSourceIdHashes matching the url incomeSourceIdHash" should {
      "return None" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, singleBusinessIncome2023)

        val result = incomeSourceIdHelper.compare(incomeSourceIdHash = None)(user = user)

        result shouldBe None
      }
    }
    "user has no incomeSources" should {
      "return None" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, noIncomeDetails)

        val result = incomeSourceIdHelper.compare(incomeSourceIdHash = testSelfEmploymentIdHash)(user = user)

        result shouldBe None
      }
    }
  }
}
