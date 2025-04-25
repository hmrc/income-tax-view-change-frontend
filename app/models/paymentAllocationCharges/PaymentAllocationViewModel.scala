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

package models.paymentAllocationCharges

import exceptions.MissingFieldException
import implicits.ImplicitCurrencyFormatter._
import models.financialDetails.DocumentDetail
import models.paymentAllocations.AllocationDetail

import java.time.LocalDate

case class AllocationDetailWithClearingDate(allocationDetail: Option[AllocationDetail], clearingDate: Option[LocalDate]) {
  def allocationDetailActual: AllocationDetail = allocationDetail.getOrElse(throw MissingFieldException("Payment Allocation Detail"))
  def clearingDateActual: LocalDate = clearingDate.getOrElse(throw MissingFieldException("Payment Clearing Date"))

}

case class LatePaymentInterestPaymentAllocationDetails(documentDetail: DocumentDetail, amount: BigDecimal)

case class PaymentAllocationViewModel(paymentAllocationChargeModel: FinancialDetailsWithDocumentDetailsModel,
                                      originalPaymentAllocationWithClearingDate: Seq[AllocationDetailWithClearingDate] = Seq(),
                                      latePaymentInterestPaymentAllocationDetails: Option[LatePaymentInterestPaymentAllocationDetails] = None,
                                      isLpiPayment: Boolean = false) {

  def hasDocumentDetailWithCredit: Boolean =
    paymentAllocationChargeModel.documentDetails.exists(_.credit.isDefined)

  def getEffectiveDateOfPayment: LocalDate = {
    val details = paymentAllocationChargeModel.documentDetails.headOption
      .getOrElse(throw MissingFieldException("Payment Allocation charge document details"))

    details.effectiveDateOfPayment.getOrElse(throw MissingFieldException("Effective Date Of Payment"))
  }

  def getOriginalAmount: String =
    paymentAllocationChargeModel.filteredDocumentDetails.headOption.map(_.originalAmount.abs.toCurrencyString).getOrElse("")

  def showPaymentAllocationsTable(): Boolean =
    !(paymentAllocationChargeModel.documentDetails.exists(_.outstandingAmountZero) &&
      paymentAllocationChargeModel.documentDetails.exists(_.credit.isDefined))

}

case class PaymentAllocationError(status: Option[Int] = None)