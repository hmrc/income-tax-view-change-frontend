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

package models.financialDetails

import models.outstandingCharges.OutstandingChargesModel
import services.DateServiceInterface

import java.time.LocalDate

case class WhatYouOweChargesList(balanceDetails: BalanceDetails, chargesList: List[ChargeItem] = List(),
                                 outstandingChargesModel: Option[OutstandingChargesModel] = None,
                                 codedOutDocumentDetail: Option[ChargeItem] = None)(implicit val dateService: DateServiceInterface) {

  private lazy val overdueChargeList: List[ChargeItem] = chargesList.filter(_.isOverdue())

  val availableCredit: Option[BigDecimal] = this.balanceDetails.availableCredit.flatMap(v => if (v > 0) Some(v) else None)

  def sortedChargesList: List[ChargeItem] = chargesList.sortWith((charge1, charge2) =>
    charge1.dueDate.exists(date1 => charge2.dueDate.exists(_.isAfter(date1))))

  def bcdChargeTypeDefinedAndGreaterThanZero: Boolean =
    if (outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined
      && outstandingChargesModel.get.bcdChargeType.get.chargeAmount > 0) true
    else false

  def isChargesListEmpty: Boolean = chargesList.isEmpty && !bcdChargeTypeDefinedAndGreaterThanZero

  def hasUnpaidPOAs: Boolean = chargesList.exists(chargeItem =>
    Seq(PoaOneDebit, PoaTwoDebit).contains(chargeItem.transactionType) && chargeItem.outstandingAmount > 0.0)

  def hasDunningLock: Boolean = chargesList.exists(charge => charge.dunningLock)

  def hasLpiWithDunningLock: Boolean =
    if (overdueChargeList.exists(_.lpiWithDunningLock.isDefined)
      && overdueChargeList.exists(_.lpiWithDunningLock.getOrElse[BigDecimal](0) > 0)) true
    else false


  def interestOnOverdueCharges: Boolean =
    if (overdueChargeList.exists(_.interestOutstandingAmount.isDefined)
      && overdueChargeList.exists(_.latePaymentInterestAmount.getOrElse[BigDecimal](0) <= 0)) true
    else false

  def getEarliestTaxYearAndAmountByDueDate: Option[(Int, BigDecimal)] = {

    implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

    val sortedListOfCharges: Option[ChargeItem] = chargesList.sortBy(charge => charge.dueDate.get).headOption

    if (outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined && sortedListOfCharges.isDefined) {
      val bcdDueDate: LocalDate = outstandingChargesModel.get.bcdChargeType.get.relevantDueDate.get
      if (bcdDueDate.isBefore(sortedListOfCharges.get.dueDate.get)) {
        Some((bcdDueDate.getYear, outstandingChargesModel.get.bcdChargeType.get.chargeAmount))
      } else if (sortedListOfCharges.isDefined) {
          Some((sortedListOfCharges.get.dueDate.get.getYear, sortedListOfCharges.get.remainingToPayByChargeOrInterest))
      } else {
        None
      }
    } else {
      if (outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined) {
        Some((outstandingChargesModel.get.bcdChargeType.get.relevantDueDate.get.getYear, outstandingChargesModel.get.bcdChargeType.get.chargeAmount))
      } else if (sortedListOfCharges.isDefined) {
          Some((sortedListOfCharges.get.dueDate.get.getYear, sortedListOfCharges.get.remainingToPayByChargeOrInterest))
      } else {
        None
      }
    }
  }
}
