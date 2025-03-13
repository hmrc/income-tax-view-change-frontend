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

import exceptions.MissingFieldException
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import services.DateServiceInterface
import views.html.partials.chargeSummary.ChargeSummaryCodingOut

import java.time.LocalDate

case class WhatYouOweChargesList(balanceDetails: BalanceDetails, chargesList: List[ChargeItem] = List(),
                                 outstandingChargesModel: Option[OutstandingChargesModel] = None,
                                 codedOutDocumentDetail: Option[ChargeItem] = None)(implicit val dateService: DateServiceInterface) {

  lazy val overdueChargeList: List[ChargeItem] = chargesList.filter(x => x.isOverdue())
  println(s"DEBUG: Initializing WhatYouOweChargesList with balanceDueWithin30Days = ${balanceDetails.balanceDueWithin30Days}")


  lazy val overdueOrAccruingInterestChargeList: List[ChargeItem] = chargesList.filter(x => x.isOverdue() || x.hasAccruingInterest)
  lazy val overdueOutstandingCharges: List[OutstandingChargeModel] = outstandingChargesModel.toList.flatMap(_.outstandingCharges)
    .filter(_.relevantDueDate.getOrElse(LocalDate.MAX).isBefore(dateService.getCurrentDate))

  lazy val chargesDueWithin30DaysList: List[ChargeItem] = chargesList.filter(x => !x.isOverdue() && !x.hasAccruingInterest && isWithin30Days(x.dueDate))

  lazy val chargesDueAfter30DaysList: List[ChargeItem] = chargesList.filter(x => !x.isOverdue() && !x.hasAccruingInterest && !isWithin30Days(x.dueDate))

  def isWithin30Days(date: Option[LocalDate]): Boolean = {
    val currentDate = dateService.getCurrentDate
    date match {
      case Some(dueDate) => dueDate.minusDays(30).isBefore(currentDate)
      case None => false
    }
  }

  val availableCredit: Option[BigDecimal] = this.balanceDetails.availableCredit.flatMap(v => if (v > 0) Some(v) else None)

  def sortedChargesList: List[ChargeItem] = chargesList.sortWith((charge1, charge2) =>
    charge1.dueDate.exists(date1 => charge2.dueDate.exists(_.isAfter(date1))))

  def sortThisChargesListPlease(charges: List[ChargeItem]): List[ChargeItem] = charges.sortWith((charge1, charge2) =>
    charge2.getDisplayDueDate.isAfter(charge1.getDisplayDueDate)
  )

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
