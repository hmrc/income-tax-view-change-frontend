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

import models.outstandingCharges.OutstandingChargesModel

import java.time.LocalDate

case class WhatYouOweChargesList(balanceDetails: BalanceDetails, chargesList: List[DocumentDetailWithDueDate] = List(),
                                 outstandingChargesModel: Option[OutstandingChargesModel] = None,
                                 codedOutDocumentDetail: Option[DocumentDetailWithCodingDetails] = None) {

  lazy val overdueChargeList: List[DocumentDetailWithDueDate] = chargesList.filter(_.isOverdue)

  def sortedChargesList: List[DocumentDetailWithDueDate] = chargesList.sortWith((charge1, charge2) =>
    charge1.dueDate.exists(date1 => charge2.dueDate.exists(_.isAfter(date1))))

  def bcdChargeTypeDefinedAndGreaterThanZero: Boolean =
    if (outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined
      && outstandingChargesModel.get.bcdChargeType.get.chargeAmount > 0) true
    else false

  def isChargesListEmpty: Boolean = chargesList.isEmpty && !bcdChargeTypeDefinedAndGreaterThanZero

  def hasDunningLock: Boolean = chargesList.exists(charge => charge.dunningLock)

  def hasLpiWithDunningBlock: Boolean =
    if (overdueChargeList.exists(_.documentDetail.lpiWithDunningBlock.isDefined)
      && overdueChargeList.exists(_.documentDetail.lpiWithDunningBlock.getOrElse[BigDecimal](0) > 0)) true
    else false


  def interestOnOverdueCharges: Boolean =
    if (overdueChargeList.exists(_.documentDetail.interestOutstandingAmount.isDefined)
      && overdueChargeList.exists(_.documentDetail.latePaymentInterestAmount.getOrElse[BigDecimal](0) <= 0)) true
    else false

  def getEarliestTaxYearAndAmountByDueDate: Option[(Int, BigDecimal)] = {

    implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

    val sortedListOfCharges: Option[DocumentDetailWithDueDate] = chargesList.sortBy(charge => charge.dueDate.get).headOption

    if (outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined && sortedListOfCharges.isDefined) {
      val bcdDueDate: LocalDate = LocalDate.parse(outstandingChargesModel.get.bcdChargeType.get.relevantDueDate.get)
      if (bcdDueDate.isBefore(sortedListOfCharges.get.dueDate.get)) {
        Some((bcdDueDate.getYear, outstandingChargesModel.get.bcdChargeType.get.chargeAmount))
      } else if (sortedListOfCharges.isDefined) {
          Some((sortedListOfCharges.get.dueDate.get.getYear, sortedListOfCharges.get.documentDetail.remainingToPayByChargeOrLpi))
      } else {
        None
      }
    } else {
      if (outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined) {
        Some((LocalDate.parse(outstandingChargesModel.get.bcdChargeType.get.relevantDueDate.get).getYear, outstandingChargesModel.get.bcdChargeType.get.chargeAmount))
      } else if (sortedListOfCharges.isDefined) {
          Some((sortedListOfCharges.get.dueDate.get.getYear, sortedListOfCharges.get.documentDetail.remainingToPayByChargeOrLpi))
      } else {
        None
      }
    }
  }
}
