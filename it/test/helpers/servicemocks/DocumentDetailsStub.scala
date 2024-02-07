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

import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}
import services.DateService
import testConstants.BaseIntegrationTestConstants.testTaxYear

import java.time.LocalDate

object DocumentDetailsStub {
  def docDetail(documentDescription: String): DocumentDetail = DocumentDetail(
    taxYear = testTaxYear,
    transactionId = "1040000124",
    documentDescription = Some(documentDescription),
    documentText = Some("Class 2 National Insurance"),
    originalAmount = Some(10.34),
    outstandingAmount = Some(1.2),
    documentDate = LocalDate.of(2018, 3, 29),
    amountCodedOut = Some(2500),
    effectiveDateOfPayment = Some(LocalDate.parse("2018-02-14")),
    documentDueDate = Some(LocalDate.parse("2018-02-14"))
  )

  def docDateDetail(dueDate: String, chargeType: String)(implicit dateService: DateService): DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetail(chargeType),
    dueDate = Some(LocalDate.parse(dueDate))
  )


  def docDetailWithInterest(documentDescription: String): DocumentDetail = DocumentDetail(
    taxYear = testTaxYear,
    transactionId = "1040000124",
    documentDescription = Some(documentDescription),
    documentText = Some("documentText"),
    originalAmount = Some(123.45),
    outstandingAmount = Some(1.2),
    documentDate = LocalDate.of(2018, 3, 29),
    latePaymentInterestAmount = Some(54.32),
    interestOutstandingAmount = Some(42.50),
    interestFromDate = Some(LocalDate.of(2018, 4, 14)),
    interestEndDate = Some(LocalDate.of(2019, 1, 1)),
    amountCodedOut = Some(2500),
    effectiveDateOfPayment = Some(LocalDate.of(2023, 7, 1)),
    documentDueDate = Some(LocalDate.of(2023, 7, 1))
  )

  def docDateDetailWithInterest(dueDate: String, chargeType: String)(implicit dateService: DateService): DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetailWithInterest(chargeType),
    dueDate = Some(LocalDate.parse(dueDate))
  )

  def docDetailWithInterestAndOverdue(documentDescription: String): DocumentDetail = DocumentDetail(
    taxYear = testTaxYear,
    transactionId = "1040000124",
    documentDescription = Some(documentDescription),
    documentText = Some("documentText"),
    originalAmount = Some(123.45),
    outstandingAmount = Some(1.2),
    documentDate = LocalDate.of(2018, 3, 29),
    latePaymentInterestAmount = Some(54.32),
    interestOutstandingAmount = Some(42.50),
    interestFromDate = Some(LocalDate.of(2018, 4, 14)),
    interestEndDate = Some(LocalDate.of(2019, 1, 1)),
    amountCodedOut = Some(2500),
    effectiveDateOfPayment = Some(LocalDate.of(2017, 7, 1)),
    documentDueDate = Some(LocalDate.of(2017, 7, 1))
  )

  def docDateDetailWithInterestAndOverdue(dueDate: String, chargeType: String)(implicit dateService: DateService): DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetailWithInterestAndOverdue(chargeType),
    dueDate = Some(LocalDate.parse(dueDate))
  )
}
