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

package services.helpers

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import testConstants.PropertyDetailsTestConstants.{foreignPropertyDetails, ukPropertyDetails}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{foreignPropertyIncome, noIncomeDetails, twoActiveForeignPropertyIncomes, twoActiveUkPropertyBusinesses, ukPropertyIncome}
import testUtils.TestSupport
import scala.language.reflectiveCalls

class ActivePropertyBusinessesHelperSpec extends TestSupport with FeatureSwitching {

  val fixture = new {
    val activePropertyBusinessesHelper: ActivePropertyBusinessesHelper = new ActivePropertyBusinessesHelper {}
  }

  "getActiveUkOrForeignPropertyBusinessFromUserIncomeSources where isUkProperty = false" when {
    "user has income sources" should {
      "return a PropertyDetailsModel when the user has one active property" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, foreignPropertyIncome)

        val result = fixture.activePropertyBusinessesHelper.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(isUkProperty = false)(user = user)

        result shouldBe Right(foreignPropertyDetails)
      }
    }
    "user has income sources" should {
      "return an exception when the user has more than one active property" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, twoActiveForeignPropertyIncomes)

        val result = fixture.activePropertyBusinessesHelper.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(isUkProperty = false)(user = user)

        result.toString shouldBe Left(new Error("More than one active foreign property found. There should only be one.")).toString
      }
    }
    "user has income sources" should {
      "return an exception when the user has no active property" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, noIncomeDetails)

        val result = fixture.activePropertyBusinessesHelper.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(isUkProperty = false)(user = user)

        result.toString shouldBe Left(new Error("No active foreign properties found.")).toString
      }
    }
  }

  "getActiveUkOrForeignPropertyBusinessFromUserIncomeSources where isUkProperty = true" when {
    "user has income sources" should {
      "return a PropertyDetailsModel when the user has one active property" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, ukPropertyIncome)

        val result = fixture.activePropertyBusinessesHelper.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(isUkProperty = true)(user = user)

        result shouldBe Right(ukPropertyDetails)
      }
    }
    "user has income sources" should {
      "return an exception when the user has more than one active property" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, twoActiveUkPropertyBusinesses)

        val result = fixture.activePropertyBusinessesHelper.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(isUkProperty = true)(user = user)

        result.toString shouldBe Left(new Error("More than one active UK property found. There should only be one.")).toString
      }
    }
    "user has income sources" should {
      "return an exception when the user has no active property" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, noIncomeDetails)

        val result = fixture.activePropertyBusinessesHelper.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(isUkProperty = true)(user = user)

        result.toString shouldBe Left(new Error("No active UK properties found.")).toString
      }
    }
  }
}
