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

import enums.IncomeSourceJourney.IncomeSourceType

import java.time.LocalDate


case class CheckDetailsViewModel(businessName: Option[String] = None,
                                 businessStartDate: Option[LocalDate],
                                 accountingPeriodEndDate: Option[LocalDate] = None,
                                 businessTrade: Option[String] = None,
                                 businessAddressLine1: Option[String] = None,
                                 businessAddressLine2: Option[String] = None,
                                 businessAddressLine3: Option[String] = None,
                                 businessAddressLine4: Option[String] = None,
                                 businessPostalCode: Option[String] = None,
                                 businessCountryCode: Option[String] = None,
                                 incomeSourcesAccountingMethod: Option[String] = None,
                                 cashOrAccrualsFlag: String,
                                 showedAccountingMethod: Boolean = true,
                                 incomeSourceType: IncomeSourceType
 ){

  def countryName: Option[String] = Some("United Kingdom")

  def getAccountingMethodMessageKey: String = {
    val cashAccountingSelected = cashOrAccrualsFlag.toLowerCase.equals("cash")

    if (cashAccountingSelected) {
      "incomeSources.add.accountingMethod.cash"
    } else {
      "incomeSources.add.accountingMethod.accruals"
    }
  }
}