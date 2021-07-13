/*
 * Copyright 2021 HM Revenue & Customs
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

case class Payment( reference: Option[String],
                    amount: Option[BigDecimal],
                    method: Option[String],
                    lot: Option[String],
                    lotItem: Option[String],
                    date: Option[String],
                    transactionId: Option[String])

object Payment {
  implicit val format: Format[Payment] = Json.format[Payment]
}

case class PaymentsWithChargeType(payments: Seq[Payment], mainType: Option[String], chargeType: Option[String]){
  def getPaymentAllocationTextInChargeSummary: Option[String] = (mainType, chargeType) match {
    case (Some("SA Payment on Account 1"), Some(chargeType)) if chargeType.contains("NIC4") => Some("chargeSummary.paymentAllocations.poa1.nic4")
    case (Some("SA Payment on Account 1"), Some(chargeType)) if chargeType.contains("ITSA") => Some("chargeSummary.paymentAllocations.poa1.incomeTax")
    case (Some("SA Payment on Account 2"), Some(chargeType)) if chargeType.contains("NIC4") => Some("chargeSummary.paymentAllocations.poa2.nic4")
    case (Some("SA Payment on Account 2"), Some(chargeType)) if chargeType.contains("ITSA") => Some("chargeSummary.paymentAllocations.poa2.incomeTax")
    case (Some("SA Balancing Charge"), Some(chargeType)) if chargeType.contains("ITSA") => Some("chargeSummary.paymentAllocations.bcd.incomeTax")
    case (Some("SA Balancing Charge"), Some(chargeType)) if chargeType.contains("NIC2") => Some("chargeSummary.paymentAllocations.bcd.nic2")
    case (Some("SA Balancing Charge"), Some(chargeType)) if chargeType.contains("Voluntary Class 2 NIC") => Some("chargeSummary.paymentAllocations.bcd.vcnic2")
    case (Some("SA Balancing Charge"), Some(chargeType)) if chargeType.contains("NIC4") => Some("chargeSummary.paymentAllocations.bcd.nic4")
    case (Some("SA Balancing Charge"), Some(chargeType)) if chargeType.contains("SL") => Some("chargeSummary.paymentAllocations.bcd.sl")
    case (Some("SA Balancing Charge"), Some(chargeType)) if chargeType.contains("CGT") => Some("chargeSummary.paymentAllocations.bcd.cgt")
    case _ => None
  }
}
