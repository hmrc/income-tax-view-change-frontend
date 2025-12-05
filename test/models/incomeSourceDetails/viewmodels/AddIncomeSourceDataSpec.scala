/*
 * Copyright 2025 HM Revenue & Customs
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

package models.incomeSourceDetails.viewmodels

import models.incomeSourceDetails.{AddIncomeSourceData, Address, SensitiveAddIncomeSourceData}
import testUtils.UnitSpec

import java.time.LocalDate

class AddIncomeSourceDataSpec extends UnitSpec{

  "AddIncomeSourceData.encrypted and SensitiveAddIncomeSourceData.decrypted" should {
    "correctly encrypt and then decrypt back to the same object" in {

      val original = AddIncomeSourceData(
        businessName = Some("Test Biz"),
        businessTrade = Some("Consulting"),
        dateStarted = Some(LocalDate.of(2020, 1, 1)),
        accountingPeriodStartDate = Some(LocalDate.of(2020, 4, 6)),
        accountingPeriodEndDate = Some(LocalDate.of(2021, 4, 5)),
        incomeSourceId = Some("abc-123"),
        address = Some(Address(Seq("123 Test Street", "Testville"), Some("AB1 2CD"))),
        countryCode = Some("GB"),
        changeReportingFrequency = Some(true),
        reportingMethodTaxYear1 = Some("Quarterly"),
        reportingMethodTaxYear2 = Some("Annual"),
        incomeSourceAdded = Some(true),
        incomeSourceCreatedJourneyComplete = Some(false),
        incomeSourceRFJourneyComplete = Some(true)
      )

      val encrypted = original.encrypted

      //sanity check
      encrypted.businessName.map(_.decryptedValue) shouldBe Some("Test Biz")
      encrypted.address.flatMap(_.postcode.map(_.decryptedValue)) shouldBe Some("AB1 2CD")
      encrypted.changeReportingFrequency.map(_.decryptedValue) shouldBe Some(true)

      val decrypted = encrypted.decrypted

      decrypted shouldBe original
    }

    "handle empty AddIncomeSourceData gracefully" in {
      val empty = AddIncomeSourceData()
      val encrypted = empty.encrypted
      val decrypted = encrypted.decrypted

      encrypted shouldBe SensitiveAddIncomeSourceData()
      decrypted shouldBe empty
    }
  }
}
