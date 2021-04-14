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
import java.time.LocalDate

import play.api.Logger

case class Charge(taxYear: String,
                  transactionId: String,
                  transactionDate: Option[String] = None,
                  `type`: Option[String] = None,
                  totalAmount: Option[BigDecimal] = None,
                  originalAmount: Option[BigDecimal] = None,
                  outstandingAmount: Option[BigDecimal] =None,
                  clearedAmount: Option[BigDecimal] =None,
									chargeType: Option[String] = None,
                  mainType: Option[String] = None,
                  items: Option[Seq[SubItem]] = None
                 ) {
  val isPaid: Boolean = {
		outstandingAmount.fold(clearedAmount.exists(_ >= 0) && clearedAmount == originalAmount)(_ <= 0)
	}

	def remainingToPay: BigDecimal = {
		if (isPaid) BigDecimal(0)
		else outstandingAmount.getOrElse(originalAmount.get)
	}

  def charges(): Seq[SubItem] = items.getOrElse(Seq()).filter(_.dueDate.isDefined)

  def payments(): Seq[SubItem] = items.getOrElse(Seq()).filter(_.paymentReference.isDefined)

  def eligibleToPay(paymentEnabled: Boolean) :Boolean = {
    !isPaid && paymentEnabled
  }

  val due: Option[LocalDate] = charges().headOption.flatMap(_.dueDate).map(due => LocalDate.parse(due))

  def isOverdue: Boolean = due.exists(dueDate => dueDate.isBefore(LocalDate.now()))

	def getChargeTypeKey: String = mainType match {
		case Some("SA Payment on Account 1") => "paymentOnAccount1.text"
		case Some("SA Payment on Account 2") => "paymentOnAccount2.text"
		case Some("SA Balancing Charge") => "balancingCharge.text"
		case error => {
			Logger.error(s"[Charge][getChargeTypeKey] Missing or non-matching charge type: $error found")
			"unknownCharge"
		}
	}

  def getChargePaidStatus: String = {
    if(isPaid) "paid"
    else if(outstandingAmount.isDefined) "part-paid"
    else "unpaid"
  }
}

object Charge {

  implicit val format: Format[Charge] = Json.format[Charge]
}
