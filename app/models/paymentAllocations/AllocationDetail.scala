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

package models.paymentAllocations

import implicits.ImplicitDateFormatter
import play.api.Logger
import play.api.libs.json.{Format, Json}

case class AllocationDetail(transactionId: Option[String],
														from: Option[String],
														to: Option[String],
														chargeType: Option[String],
														mainType: Option[String],
														amount: Option[BigDecimal],
														clearedAmount: Option[BigDecimal]) {

	def getPaymentAllocationTextInChargeSummary: String = (mainType, chargeType) match {
		case (Some("SA Payment on Account 1"), Some(chargeType)) if chargeType.contains("NIC4") => "paymentAllocation.paymentAllocations.poa1.nic4"
		case (Some("SA Payment on Account 1"), Some(chargeType)) if chargeType.contains("ITSA") => "paymentAllocation.paymentAllocations.poa1.incomeTax"
		case (Some("SA Payment on Account 2"), Some(chargeType)) if chargeType.contains("NIC4") => "paymentAllocation.paymentAllocations.poa2.nic4"
		case (Some("SA Payment on Account 2"), Some(chargeType)) if chargeType.contains("ITSA") => "paymentAllocation.paymentAllocations.poa2.incomeTax"
		case (Some("SA Balancing Charge"), Some(chargeType)) if chargeType.contains("ITSA") => "paymentAllocation.paymentAllocations.bcd.incomeTax"
		case (Some("SA Balancing Charge"), Some(chargeType)) if chargeType.contains("NIC4") => "paymentAllocation.paymentAllocations.bcd.nic4"
		case (Some("SA Balancing Charge"), Some(chargeType)) if chargeType.contains("Voluntary NIC2") => "paymentAllocation.paymentAllocations.bcd.vcnic2"
		case (Some("SA Balancing Charge"), Some(chargeType)) if chargeType.contains("NIC2") => "paymentAllocation.paymentAllocations.bcd.nic2"
		case (Some("SA Balancing Charge"), Some(chargeType)) if chargeType.contains("SL") => "paymentAllocation.paymentAllocations.bcd.sl"
		case (Some("SA Balancing Charge"), Some(chargeType)) if chargeType.contains("CGT") => "paymentAllocation.paymentAllocations.bcd.cgt"
		case _ =>
			Logger.error(s"[PaymentAllocations] Non-matching document/charge found with main charge: ${mainType} and sub-charge: ${chargeType}")
			""
	}

	def getTaxYear(implicit implicitDateFormatter: ImplicitDateFormatter): Int = {
		import implicitDateFormatter.localDate

		to.getOrElse(throw new Exception("Missing tax period end date")).toLocalDate.getYear
	}
}

object AllocationDetail {

  val emptyAllocation: AllocationDetail = AllocationDetail(None, None, None, None, None, None, None)

  implicit val format: Format[AllocationDetail] = Json.format[AllocationDetail]
}
