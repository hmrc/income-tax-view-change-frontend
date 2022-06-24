/*
 * Copyright 2022 HM Revenue & Customs
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

package models.financialDetails

import play.api.libs.json.{Format, Json}



sealed trait PaymentsResponse

case class Payments(payments: Seq[Payment]) extends PaymentsResponse

case class PaymentsError(status: Int, error: String) extends PaymentsResponse

sealed trait PaymentAllocationStatus

case object FullyAllocatedPaymentStatus extends PaymentAllocationStatus
case object NotYetAllocatedPaymentStatus extends PaymentAllocationStatus
case object PartiallyAllocatedPaymentStatus extends PaymentAllocationStatus

case class Payment(reference: Option[String],
                   amount: Option[BigDecimal],
                   outstandingAmount: Option[BigDecimal],
                   method: Option[String],
                   documentDescription: Option[String],
                   lot: Option[String],
                   lotItem: Option[String],
                   date: Option[String],
                   transactionId: Option[String]) {

  def credit: Option[BigDecimal] = amount match {
    case None => None
    case _ if(lotItem.isDefined && lot.isDefined) => None
    case Some(_) if(amount.get > 0.00) => None
    case Some(credit) => Some(credit)
  }

  def validMFACreditDescription() : Boolean = MfaCreditUtils.validMFACreditDescription(this.documentDescription)

  def allocationStatus() : Option[PaymentAllocationStatus] = (outstandingAmount, amount) match {
    case (Some(outstandingAmountValue), _) if outstandingAmountValue.equals(BigDecimal(0.0)) =>
      Some(FullyAllocatedPaymentStatus)
    case (Some(outstandingAmountValue), Some(originalAmountValue)) if outstandingAmountValue.equals(originalAmountValue) =>
      Some(NotYetAllocatedPaymentStatus)
    case (Some(_), Some(_)) =>
      Some(PartiallyAllocatedPaymentStatus)
    case _ => None
  }

}



object Payment {
  implicit val format: Format[Payment] = Json.format[Payment]
}

case class PaymentsWithChargeType(payments: Seq[Payment], mainType: Option[String], chargeType: Option[String]) {
  def getPaymentAllocationTextInChargeSummary: Option[String] = {
    FinancialDetail.getMessageKeyByTypes(mainType, chargeType)
      .map(typesKey => s"chargeSummary.paymentAllocations.$typesKey")
  }
}
