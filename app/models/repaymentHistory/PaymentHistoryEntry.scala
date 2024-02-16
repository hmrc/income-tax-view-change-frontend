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

package models.repaymentHistory

import java.time.LocalDate
import java.time.Month.APRIL

case class PaymentHistoryEntry(date: LocalDate,
                               description: String,
                               amount: Option[BigDecimal],
                               transactionId: Option[String] = None,
                               linkUrl: String,
                               visuallyHiddenText: String) {

  def getTaxYearEndYear: Int = {
    val startDateYear = date.getYear
    val accountingPeriodEndDate = LocalDate.of(startDateYear, APRIL, 5)

    if (date.isBefore(accountingPeriodEndDate) || date.isEqual(accountingPeriodEndDate)) {
      startDateYear
    } else {
      startDateYear+1
    }
  }
}
