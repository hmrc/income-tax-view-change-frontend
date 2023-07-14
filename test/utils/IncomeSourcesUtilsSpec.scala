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

import forms.utils.SessionKeys._
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckUKPropertyViewModel}
import testUtils.TestSupport

import java.time.LocalDate

class IncomeSourcesUtilsSpec extends TestSupport {

  val viewModelMax: CheckBusinessDetailsViewModel = CheckBusinessDetailsViewModel(
    businessName = Some("Test Business"),
    businessStartDate = Some(LocalDate.of(2022, 1, 1)),
    businessTrade = "Test Trade",
    businessAddressLine1 = "Test Business Address Line 1",
    businessPostalCode = Some("Test Business Postal Code"),
    businessAccountingMethod = Some("Cash"),
    accountingPeriodEndDate = LocalDate.of(2022, 1, 1),
    businessAddressLine2 = None,
    businessAddressLine3 = None,
    businessAddressLine4 = None,
    businessCountryCode = None,
    cashOrAccrualsFlag = "Cash"
  )

  val checkUKPropertyViewModel = CheckUKPropertyViewModel(
    tradingStartDate = LocalDate.of(2023, 5, 1),
    cashOrAccrualsFlag = "CASH")

  val fakeRequest = fakeRequestWithActiveSession.withSession(
    businessName -> viewModelMax.businessName.get,
    businessStartDate -> viewModelMax.businessStartDate.get.toString,
    businessTrade -> viewModelMax.businessTrade,
    addBusinessAddressLine1 -> viewModelMax.businessAddressLine1,
    addBusinessPostalCode -> viewModelMax.businessPostalCode.get,
    addBusinessAccountingMethod -> viewModelMax.businessAccountingMethod.get,
    addBusinessAccountingPeriodEndDate -> viewModelMax.accountingPeriodEndDate.toString,
    addUkPropertyStartDate -> checkUKPropertyViewModel.tradingStartDate.toString,
    addUkPropertyAccountingMethod -> checkUKPropertyViewModel.cashOrAccrualsFlag
  )

  "getBusinessDetailsFromSession" when {
    "user has business details in session" should {
      "return CheckBusinessDetailsViewModel" in {
        implicit val user = individualUser.copy()(fakeRequest)
        val result = IncomeSourcesUtils.getBusinessDetailsFromSession
        result shouldBe Right(viewModelMax)
      }
    }

    "user is missing business details in session" should {
      "returns an exception" in {
        val result = IncomeSourcesUtils.getBusinessDetailsFromSession
        result.isLeft shouldBe true
      }
    }

  }

  "getUKPropertyDetailsFromSession" when {
    "user has uk property details in session" should {
      "return CheckBusinessDetailsViewModel" in {
        implicit val user = individualUser.copy()(fakeRequest)
        val result = IncomeSourcesUtils.getUKPropertyDetailsFromSession
        result shouldBe Right(checkUKPropertyViewModel)
      }
    }

    "user is missing uk property details in session" should {
      "returns an exception" in {
        val result = IncomeSourcesUtils.getUKPropertyDetailsFromSession
        result.isLeft shouldBe true
      }
    }

  }

  "removeIncomeSourceDetailsFromSession" when {
    "user has session data" should {
      "remove session data" in {
        implicit val user = individualUser.copy()(fakeRequest)
        val newSession = IncomeSourcesUtils.removeIncomeSourceDetailsFromSession

        newSession.get("addUkPropertyStartDate") shouldBe None
        newSession.get("addBusinessName") shouldBe None
        newSession.get("addBusinessTrade") shouldBe None
        newSession.get("addBusinessAccountingMethod") shouldBe None
        newSession.get("addBusinessStartDate") shouldBe None
        newSession.get("addBusinessAccountingPeriodStartDate") shouldBe None
        newSession.get("addBusinessAccountingPeriodEndDate") shouldBe None
        newSession.get("addBusinessStartDate") shouldBe None
        newSession.get("addBusinessAddressLine1") shouldBe None
        newSession.get("addBusinessAddressLine2") shouldBe None
        newSession.get("addBusinessAddressLine3") shouldBe None
        newSession.get("addBusinessAddressLine4") shouldBe None
        newSession.get("addBusinessPostalCode") shouldBe None
        newSession.get("addBusinessCountryCode") shouldBe None
        newSession.get("ceaseForeignPropertyDeclare") shouldBe None
        newSession.get("ceaseForeignPropertyEndDate") shouldBe None
        newSession.get("ceaseUKPropertyDeclare") shouldBe None
        newSession.get("ceaseUKPropertyEndDate") shouldBe None
      }
    }
  }


}
