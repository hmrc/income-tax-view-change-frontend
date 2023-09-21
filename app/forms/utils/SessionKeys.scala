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

package forms.utils

object SessionKeys {

  val calculationId: String = "calculationId"

  val summaryData: String = "summaryData"

  val gatewayPage: String = "gatewayPage"

  val calcPagesBackPage: String = "calcPagesBackPage"

  val origin: String = "origin"

  val ceaseUKPropertyDeclare: String = "ceaseUKPropertyDeclare"

  val ceaseForeignPropertyDeclare: String = "ceaseForeignPropertyDeclare"

  val addUkPropertyStartDate: String = "addUkPropertyStartDate"

  val addForeignPropertyStartDate: String = "addForeignPropertyStartDate"

  val businessName: String = "addBusinessName"

  val businessTrade: String = "addBusinessTrade"

  val addIncomeSourcesAccountingMethod: String = "addIncomeSourcesAccountingMethod"

  val ceaseUKPropertyEndDate: String = "ceaseUKPropertyEndDate"

  val addBusinessStartDate: String = "addBusinessStartDate"

  val addBusinessAccountingPeriodStartDate: String = "addBusinessAccountingPeriodStartDate"

  val addBusinessAccountingPeriodEndDate: String = "addBusinessAccountingPeriodEndDate"

  val ceaseForeignPropertyEndDate: String = "ceaseForeignPropertyEndDate"

  val businessStartDate: String = "addBusinessStartDate"

  val addBusinessAddressLine1: String = "addBusinessAddressLine1"
  val addBusinessAddressLine2: String = "addBusinessAddressLine2"
  val addBusinessAddressLine3: String = "addBusinessAddressLine3"
  val addBusinessAddressLine4: String = "addBusinessAddressLine4"
  val addBusinessPostalCode: String = "addBusinessPostalCode"
  val addBusinessCountryCode: String = "addBusinessCountryCode"

  val ceaseBusinessEndDate: String = "ceaseBusinessEndDate"

  val ceaseBusinessIncomeSourceId: String = "ceaseBusinessIncomeSourceId"

  val incomeSourceId: String = "incomeSourceId"

  val foreignPropertyStartDate: String = "addForeignPropertyStartDate"

  val incomeSourcesSessionKeys: Seq[String] = Seq(
    addUkPropertyStartDate,
    addForeignPropertyStartDate,
    businessName,
    businessTrade,
    addIncomeSourcesAccountingMethod,
    addBusinessStartDate,
    addBusinessAccountingPeriodStartDate,
    addBusinessAccountingPeriodEndDate,
    addBusinessStartDate,
    addBusinessAddressLine1,
    addBusinessAddressLine2,
    addBusinessAddressLine3,
    addBusinessAddressLine4,
    addBusinessPostalCode,
    addBusinessCountryCode,
    ceaseForeignPropertyDeclare,
    ceaseForeignPropertyEndDate,
    ceaseUKPropertyDeclare,
    ceaseUKPropertyEndDate
  )
}
