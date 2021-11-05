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

import play.api.Logger
import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class DocumentDetail(taxYear: String,
													transactionId: String,
													documentDescription: Option[String],
													documentText: Option[String],
													outstandingAmount: Option[BigDecimal],
													originalAmount: Option[BigDecimal],
													documentDate: LocalDate,
													interestOutstandingAmount: Option[BigDecimal] = None,
													interestRate: Option[BigDecimal] = None,
													latePaymentInterestId: Option[String] = None,
													interestFromDate: Option[LocalDate] = None,
													interestEndDate: Option[LocalDate] = None,
													latePaymentInterestAmount: Option[BigDecimal] = None,
													lpiWithDunningBlock: Option[BigDecimal] = None,
													paymentLotItem: Option[String] = None,
													paymentLot: Option[String] = None,
													amountCodedOut: Option[BigDecimal] = None
												 ) {

	lazy val hasLpiWithDunningBlock: Boolean =
		lpiWithDunningBlock.isDefined && lpiWithDunningBlock.getOrElse[BigDecimal](0) > 0

  lazy val hasAccruingInterest: Boolean =
    interestOutstandingAmount.isDefined && latePaymentInterestAmount.getOrElse[BigDecimal](0) <= 0

	val isPaid: Boolean = outstandingAmount match {
		case Some(amount) if amount == 0 => true
		case _ => false
	}

	val interestIsPaid: Boolean = interestOutstandingAmount match {
		case Some(amount) if amount == 0 => true
		case _ => false
	}

	val interestIsPartPaid: Boolean = interestOutstandingAmount.getOrElse[BigDecimal](0) != latePaymentInterestAmount.getOrElse[BigDecimal](0)

	def getInterestPaidStatus: String = {
		if (interestIsPaid) "paid"
		else if (interestIsPartPaid) "part-paid"
		else "unpaid"
	}

	def checkIsPaid(isInterestCharge: Boolean): Boolean = {
		if (isInterestCharge) interestIsPaid
		else isPaid
	}

	val isPartPaid: Boolean = outstandingAmount.getOrElse[BigDecimal](0) != originalAmount.getOrElse[BigDecimal](0)

	def remainingToPay: BigDecimal = {
		if (isPaid) BigDecimal(0)
		else outstandingAmount.getOrElse(originalAmount.get)
	}

	def interestRemainingToPay: BigDecimal = {
		if (interestIsPaid) BigDecimal(0)
		else interestOutstandingAmount.getOrElse(latePaymentInterestAmount.get)
	}

	def checkIfEitherChargeOrLpiHasRemainingToPay: Boolean = {
		if(latePaymentInterestAmount.isDefined) interestRemainingToPay > 0
		else remainingToPay > 0
	}

	def getChargePaidStatus: String = {
		if (isPaid) "paid"
		else if (isPartPaid) "part-paid"
		else "unpaid"
	}

	val isClass2Nic: Boolean = documentText match {
		case Some(documentText) if documentText == "Class 2 National Insurance" => true
		case _ => false
	}

	val isCodingOut: Boolean = amountCodedOut match {
		case Some(amountCodedOut) if amountCodedOut > 0 => true
		case _ => false
	}

	def getChargeTypeKey(codedOutEnabled: Boolean = false): String = documentDescription match {
		case Some("ITSA- POA 1") => "paymentOnAccount1.text" //todo: fix the actual document descriptions
		case Some("ITSA - POA 2") => "paymentOnAccount2.text"
		case Some("TRM New Charge") | Some("TRM Amend Charge") => if (isClass2Nic && codedOutEnabled) {
			if (isCodingOut) "codingOut.text" else "class2Nic.text"
		} else "balancingCharge.text"
		case error =>
			Logger("application").error(s"[DocumentDetail][getChargeTypeKey] Missing or non-matching charge type: $error found")
			"unknownCharge"
	}
}

case class DocumentDetailWithDueDate(documentDetail: DocumentDetail, dueDate: Option[LocalDate],
                                     isLatePaymentInterest: Boolean = false, dunningLock: Boolean = false) {
	val isOverdue: Boolean = dueDate.exists(_ isBefore LocalDate.now)
	val currentDueDate: Option[LocalDate] = if (documentDetail.latePaymentInterestAmount.isDefined) documentDetail.interestEndDate else dueDate
}

object DocumentDetail {
	implicit val format: Format[DocumentDetail] = Json.format[DocumentDetail]
}
