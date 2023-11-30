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

package services

import config.featureswitch.FeatureSwitching
import models.incomeSourceDetails.{AddIncomeSourceData, Address}
import testUtils.TestSupport

class EncryptionServiceSpec extends TestSupport with FeatureSwitching{
  "The getCurrentDate " should {
    "return ?" in {
//      val model = AddIncomeSourceData(
//        businessName = Some("businessName"),
//        businessTrade = Some("businessName"),
//        dateStarted = Some("2022-11-11"),
//        accountingPeriodStartDate = Some("2022-11-11"),
//        accountingPeriodEndDate = Some("2022-11-11"),
//        createdIncomeSourceId = Some("incomeSourceId"),
//        address = Some(Address( Seq("1231"), Some("SE13 3ER"))),
//        countryCode = Some("GB"),
//        incomeSourcesAccountingMethod = Some("method")
//      )
//      2 + 2 shouldBe 4
    }
  }
}
