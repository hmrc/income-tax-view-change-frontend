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

package models.incomeSourceDetails.viewmodels

import java.time.LocalDate

case class CheckBusinessDetailsViewModel(businessName: Option[String],
                                         businessStartDate: Option[LocalDate],
                                         accountingPeriodEndDate: LocalDate,
                                         businessTrade: String,
                                         businessAddressLine1: String,
                                         businessAddressLine2: Option[String],
                                         businessAddressLine3: Option[String],
                                         businessAddressLine4: Option[String],
                                         businessPostalCode: Option[String],
                                         businessCountryCode: Option[String],
                                         incomeSourcesAccountingMethod: Option[String],
                                         cashOrAccrualsFlag: String,
                                         skippedAccountingMethod: Boolean) {

  def countryName: Option[String] = Some("United Kingdom")

}

case class CheckForeignPropertyViewModel(tradingStartDate: LocalDate, cashOrAccrualsFlag: String)

case class CheckUKPropertyViewModel(tradingStartDate: LocalDate, cashOrAccrualsFlag: String) {
  def getAccountingMethodMessageKey: String = {
    val cashAccountingSelected = cashOrAccrualsFlag.toLowerCase.equals("cash")

    if (cashAccountingSelected) {
      "incomeSources.add.accountingMethod.cash"
    } else {
      "incomeSources.add.accountingMethod.accruals"
    }
  }
}