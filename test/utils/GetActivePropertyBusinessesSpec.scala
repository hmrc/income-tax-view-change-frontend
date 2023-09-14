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

package utils

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import org.mockito.Mockito.mock
import services.IncomeSourceDetailsService
import testUtils.TestSupport

class GetActivePropertyBusinessesSpec extends TestSupport with FeatureSwitching with GetActivePropertyBusinesses {

  "getActiveForeignPropertyFromUserIncomeSources" when {
    "user has income sources" should {
      "return a PropertyDetailsModel when the user has one active property" in {
      }
    }
    "user has income sources" should {
      "return an exception when the user has more than one active property" in {
        implicit val user = getIndividualUserWithTwoActiveForeignProperties(fakeRequestWithActiveSession)

        val result = GetActivePropertyBusinesses.getActiveForeignPropertyFromUserIncomeSources(user = user)

        result shouldBe Left(new Error("Too many active foreign properties found. There should only be one."))
      }
    }
    "user has income sources" should {
      "return an exception when the user has no active property" in {

      }
    }
  }

  "getActiveUkPropertyFromUserIncomeSources" when {
    "user has income sources" should {
      "return a PropertyDetailsModel when the user has one active property" in {

      }
    }
    "user has income sources" should {
      "return an exception when the user has more than one active property" in {

      }
    }
    "user has income sources" should {
      "return an exception when the user has no active property" in {

      }
    }
  }
}
