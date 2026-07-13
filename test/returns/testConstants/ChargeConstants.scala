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

package returns.testConstants

import common.models.incomeSourceDetails.TaxYear
import common.services.DateServiceInterface
import returns.models.*
import returns.testConstants.FinancialDetailsTestConstants.*

import java.time.LocalDate

trait ChargeConstants {

  implicit def dateService: DateServiceInterface

  def chargeItemModel(taxYear: TaxYear = TaxYear.forYearEnd(2018),
                      transactionId: String = id1040000123,
                      transactionType: TransactionType = PoaOneDebit,
                      codedOutStatus: Option[CodedOutStatusType] = None,
                      documentDate: LocalDate = LocalDate.of(2018, 3, 29),
                      dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                      originalAmount: BigDecimal = 1400.00,
                      outstandingAmount: BigDecimal = 1400.00,
                      amountCodedOut: Option[BigDecimal] = None,
                      interestOutstandingAmount: Option[BigDecimal] = Some(80.0),
                      accruingInterestAmount: Option[BigDecimal] = Some(100.0),
                      latePaymentInterestAmount: Option[BigDecimal] = Some(100.0),
                      interestRate: Option[BigDecimal] = Some(1.0),
                      interestFromDate: Option[LocalDate] = Some(LocalDate.of(2018, 3, 29)),
                      interestEndDate: Option[LocalDate] = Some(LocalDate.of(2018, 6, 15)),
                      lpiWithDunningLock: Option[BigDecimal] = Some(100.0),
                      dunningLock: Boolean = false,
                      poaRelevantAmount: Option[BigDecimal] = None,
                      chargeReference: Option[String] = None): ChargeItem = ChargeItem(
    transactionId = transactionId,
    taxYear = taxYear,
    transactionType = transactionType,
    codedOutStatus = codedOutStatus,
    documentDate = documentDate,
    dueDate = dueDate,
    originalAmount = originalAmount,
    outstandingAmount = outstandingAmount,
    interestOutstandingAmount = interestOutstandingAmount,
    accruingInterestAmount = accruingInterestAmount,
    latePaymentInterestAmount = latePaymentInterestAmount,
    interestFromDate = interestFromDate,
    interestEndDate = interestEndDate,
    lpiWithDunningLock = lpiWithDunningLock,
    dunningLock = dunningLock,
    interestRate = interestRate,
    amountCodedOut = amountCodedOut,
    poaRelevantAmount = poaRelevantAmount,
    chargeReference = chargeReference
  )
}