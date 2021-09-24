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

import models.outstandingCharges.OutstandingChargesModel

import java.time.LocalDate

case class WhatYouOweChargesList(balanceDetails: BalanceDetails, overduePaymentList: List[DocumentDetailWithDueDate] = List(),
                                 dueInThirtyDaysList: List[DocumentDetailWithDueDate] = List(),
                                 futurePayments: List[DocumentDetailWithDueDate] = List(),
                                 outstandingChargesModel: Option[OutstandingChargesModel] = None) {

  lazy val allCharges: List[DocumentDetailWithDueDate] = overduePaymentList ++ dueInThirtyDaysList ++ futurePayments

  def bcdChargeTypeDefinedAndGreaterThanZero: Boolean =
    if (outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined
      && outstandingChargesModel.get.bcdChargeType.get.chargeAmount > 0) true
    else false

  def isChargesListEmpty: Boolean = allCharges.isEmpty && !bcdChargeTypeDefinedAndGreaterThanZero

  def hasDunningLock: Boolean = allCharges.exists(charge => charge.dunningLock)

  def interestOnOverdueCharges: Boolean = overduePaymentList.exists(_.documentDetail.hasAccruingInterest)

  def sortedOverduePaymentLists: List[DocumentDetailWithDueDate] = overduePaymentList.sortWith((charge1, charge2) =>
    charge1.currentDueDate.exists(date1 => charge2.currentDueDate.exists(_.isAfter(date1))))

  def getEarliestTaxYearAndAmountByDueDate: (Int, BigDecimal) = {

    implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

    val sortedListOfCharges: Option[DocumentDetailWithDueDate] = allCharges.sortBy(charge => charge.dueDate.get).headOption

    if (outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined && sortedListOfCharges.isDefined) {
      val bcdDueDate: LocalDate = LocalDate.parse(outstandingChargesModel.get.bcdChargeType.get.relevantDueDate.get)
      if (bcdDueDate.isBefore(sortedListOfCharges.get.dueDate.get)) {
        (bcdDueDate.getYear, outstandingChargesModel.get.bcdChargeType.get.chargeAmount)
      } else {
        (sortedListOfCharges.get.dueDate.get.getYear, sortedListOfCharges.get.documentDetail.remainingToPay)
      }
    } else {
      if (outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined) {
        (LocalDate.parse(outstandingChargesModel.get.bcdChargeType.get.relevantDueDate.get).getYear, outstandingChargesModel.get.bcdChargeType.get.chargeAmount)
      } else {
        (sortedListOfCharges.get.dueDate.get.getYear, sortedListOfCharges.get.documentDetail.remainingToPay)
      }
    }
  }
}
