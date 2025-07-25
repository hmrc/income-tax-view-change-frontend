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

package helpers.servicemocks

import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import testConstants.BaseIntegrationTestConstants.testTaxYear

import java.time.LocalDate

object ChargeItemStub {
  def docDetail(chargeType: ChargeType): ChargeItem = ChargeItem(
    transactionId = "1040000124",
    taxYear = TaxYear.forYearEnd(testTaxYear),
    transactionType = chargeType,
    codedOutStatus = None,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(LocalDate.parse("2018-02-14")),
    originalAmount = 10.34,
    outstandingAmount = 1.2,
    interestOutstandingAmount = None,
    latePaymentInterestAmount = None,
    interestFromDate = None,
    interestEndDate = None,
    interestRate = None,
    lpiWithDunningLock = None,
    amountCodedOut = Some(2500),
    dunningLock = false,
    poaRelevantAmount = None,
    chargeReference = Some("chargeRef")
  )

  def chargeItemWithInterest(chargeType: ChargeType = PoaOneDebit,
                             codedOutStatus: Option[CodedOutStatusType] = None): ChargeItem = ChargeItem(
    transactionId = "1040000124",
    taxYear = TaxYear.forYearEnd(testTaxYear),
    transactionType = chargeType,
    codedOutStatus = codedOutStatus,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(LocalDate.of(2023, 7, 1)),
    originalAmount = 123.45,
    outstandingAmount = 1.2,
    interestOutstandingAmount = Some(42.50),
    latePaymentInterestAmount = Some(54.32),
    interestFromDate = Some(LocalDate.of(2018, 4, 14)),
    interestEndDate = Some(LocalDate.of(2019, 1, 1)),
    interestRate = None,
    lpiWithDunningLock = None,
    amountCodedOut = Some(2500),
    dunningLock = false,
    poaRelevantAmount = None,
    chargeReference = Some("chargeRef")
  )

  def chargeItemWithInterestAndOverdue(chargeType: ChargeType = PoaOneDebit,
                                       codedOutStatus: Option[CodedOutStatusType] = None,
                                       dueDate: Option[LocalDate] = Some(LocalDate.of(2017, 7, 1))): ChargeItem = ChargeItem(
    transactionId = "1040000124",
    taxYear = TaxYear.forYearEnd(testTaxYear),
    transactionType = chargeType,
    codedOutStatus = codedOutStatus,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = dueDate,
    originalAmount = 123.45,
    outstandingAmount = 1.2,
    latePaymentInterestAmount = Some(54.32),
    interestOutstandingAmount = Some(42.50),
    interestFromDate = Some(LocalDate.of(2018, 4, 14)),
    interestEndDate = Some(LocalDate.of(2019, 1, 1)),
    interestRate = None,
    lpiWithDunningLock = None,
    amountCodedOut = Some(2500),
    dunningLock = false,
    poaRelevantAmount = None,
    chargeReference = Some("chargeRef")
  )

}
