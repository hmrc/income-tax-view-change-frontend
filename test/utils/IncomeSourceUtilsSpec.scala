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

package utils

import auth.MtdItUser
import config.FrontendAppConfig
import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import testConstants.PropertyDetailsTestConstants.{foreignPropertyDetails, ukPropertyDetails}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._
import testUtils.TestSupport

class IncomeSourceUtilsSpec extends TestSupport {

  val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val TestIncomeSourcesUtils: IncomeSourcesUtils = new IncomeSourcesUtils {
    override val appConfig: FrontendAppConfig = mockAppConfig
  }

  "getActiveProperty where ForeignProperty" when {
    "user has income sources" should {
      "return a PropertyDetailsModel when the user has one active property" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, foreignPropertyIncome)

        val result = TestIncomeSourcesUtils.getActiveProperty(ForeignProperty)(user = user)

        result shouldBe Some(foreignPropertyDetails)
      }
    }
    "user has income sources" should {
      "return an exception when the user has more than one active property" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, twoActiveForeignPropertyIncomes)

        val result = TestIncomeSourcesUtils.getActiveProperty(ForeignProperty)(user = user)

        result shouldBe None
      }
    }
    "user has income sources" should {
      "return an exception when the user has no active property" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, noIncomeDetails)

        val result = TestIncomeSourcesUtils.getActiveProperty(ForeignProperty)(user = user)

        result shouldBe None
      }
    }
  }

  "getActiveProperty where UkProperty" when {
    "user has income sources" should {
      "return a PropertyDetailsModel when the user has one active property" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, ukPropertyIncome)

        val result = TestIncomeSourcesUtils.getActiveProperty(UkProperty)(user = user)

        result shouldBe Some(ukPropertyDetails)
      }
    }
    "user has income sources" should {
      "return an exception when the user has more than one active property" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, twoActiveUkPropertyBusinesses)

        val result = TestIncomeSourcesUtils.getActiveProperty(UkProperty)(user = user)

        result shouldBe None
      }
    }
    "user has income sources" should {
      "return an exception when the user has no active property" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, noIncomeDetails)

        val result = TestIncomeSourcesUtils.getActiveProperty(UkProperty)(user = user)

        result shouldBe None
      }
    }
  }
}
