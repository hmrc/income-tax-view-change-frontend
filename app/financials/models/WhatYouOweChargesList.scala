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

package financials.models

import common.exceptions.MissingFieldException
import common.models.incomeSourceDetails.TaxYear
import common.services.DateServiceInterface
import financials.models.outstandingCharges.OutstandingChargesModel

import java.time.LocalDate

case class WhatYouOweChargesList(
                                  balanceDetails: BalanceDetails, chargesList: List[ChargeItem] = List(),
                                  outstandingChargesModel: Option[OutstandingChargesModel] = None,
                                  codedOutDetails: Option[CodingOutDetails] = None
                                )(implicit val dateService: DateServiceInterface) {

  lazy val overdueChargeList: List[ChargeItem] = chargesList.filter(x => x.isOverdue())

  val availableCredit: Option[BigDecimal] = this.balanceDetails.totalCredit.flatMap(v => if (v > 0) Some(v) else None)

  def sortedChargesList: List[ChargeItem] = chargesList.sortWith((charge1, charge2) =>
    charge1.dueDate.exists(date1 => charge2.dueDate.exists(_.isAfter(date1))))

  def bcdChargeTypeDefinedAndGreaterThanZero: Boolean =
    if (outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined
      && outstandingChargesModel.get.bcdChargeType.get.chargeAmount > 0) true
    else false

  def isChargesListEmpty: Boolean = chargesList.isEmpty && !bcdChargeTypeDefinedAndGreaterThanZero

  def hasDunningLock: Boolean = chargesList.exists(charge => charge.dunningLock)

  def hasLpiWithDunningLock: Boolean =
    if (overdueChargeList.exists(_.lpiWithDunningLock.isDefined)
      && overdueChargeList.exists(_.lpiWithDunningLock.getOrElse[BigDecimal](0) > 0)) true
    else false

  def getRelevantDueDate: LocalDate = {
    try {
      outstandingChargesModel.get.bcdChargeType.get.relevantDueDate
        .getOrElse(throw MissingFieldException("documentRelevantDueDate"))
    } catch {
      case _: NoSuchElementException => throw MissingFieldException("documentBcdChargeType")
    }
  }

  def getAciChargeWithTieBreakerChargeAmount: BigDecimal = {
    try {
      outstandingChargesModel.get.getAciChargeWithTieBreaker
        .getOrElse(throw MissingFieldException("documentAciChargeWithTieBreaker"))
        .chargeAmount
    } catch {
      case _: NoSuchElementException => throw MissingFieldException("documentAciChargeType")
    }
  }

  def getDefaultPaymentAmount: Option[BigDecimal] = {
    if (balanceDetails.overDueAmount != 0) Some(balanceDetails.overDueAmount)
    else if (balanceDetails.balanceDueWithin30Days != 0) Some(balanceDetails.balanceDueWithin30Days)
    else if (balanceDetails.balanceNotDuein30Days != 0) Some(balanceDetails.balanceNotDuein30Days)
    else {
      val payableChargesAmount = chargesList.map(_.remainingToPayOnCharge).filter(_ > 0).sum
      Option.when(payableChargesAmount > 0)(payableChargesAmount)
    }
  }
  def hasChargeWithAccruingInterest: Boolean = chargesList.exists(_.isAccruingInterest)
  def hasInterestAccruingAndNotApplicableFields: Boolean = chargesList.count(_.isAccruingInterest) < chargesList.size
}

case class CodingOutDetails(amountCodedOut: BigDecimal, codingTaxYear: TaxYear)
