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

package models.chargeSummary

import models.financialDetails.{FinancialDetail, MfaDebitUtils}

import java.time.LocalDate

case class PaymentHistoryAllocations (allocations: Seq[PaymentHistoryAllocation], chargeMainType: Option[String], chargeType: Option[String]) {
  def getPaymentAllocationTextInChargeSummary: Option[String] = {
    if (MfaDebitUtils.isMFADebitMainType(chargeMainType)) {
      Some("chargeSummary.paymentAllocations.mfaDebit")
    } else {
      FinancialDetail.getMessageKeyByTypes(chargeMainType, chargeType)
        .map(typesKey => s"chargeSummary.paymentAllocations.$typesKey")
    }
  }
}

case class PaymentHistoryAllocation (dueDate: Option[LocalDate], amount: Option[BigDecimal], clearingSAPDocument: Option[String], clearingId: Option[String])
