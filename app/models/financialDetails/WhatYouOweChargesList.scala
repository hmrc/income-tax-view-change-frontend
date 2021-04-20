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
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class WhatYouOweChargesList(overduePaymentList: List[Charge] = List(), dueInThirtyDaysList: List[Charge] = List(),
                                 futurePayments: List[Charge] = List(), outstandingChargesModel: Option[OutstandingChargesModel] = None) {

  private def getAllCharges: List[Charge] = overduePaymentList ++ dueInThirtyDaysList ++ futurePayments

  def bcdChargeTypeDefinedAndGreaterThanZero: Boolean =
    if(outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined
      && outstandingChargesModel.get.bcdChargeType.get.chargeAmount > 0) true
    else false

  def isChargesListEmpty: Boolean = getAllCharges.isEmpty && !bcdChargeTypeDefinedAndGreaterThanZero


  def getEarliestTaxYearAndAmountByDueDate: (Int, BigDecimal) = {

    implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

    val sortedListOfCharges = getAllCharges.sortBy(charge => charge.due.get).headOption

    if(outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined && sortedListOfCharges.isDefined) {
      val bcdDueDate: LocalDate = LocalDate.parse(outstandingChargesModel.get.bcdChargeType.get.relevantDueDate.get)
      if(bcdDueDate.isBefore(sortedListOfCharges.get.due.get)) {
        (bcdDueDate.getYear, outstandingChargesModel.get.bcdChargeType.get.chargeAmount)
      } else {
        (sortedListOfCharges.get.due.get.getYear, sortedListOfCharges.get.remainingToPay)
      }
    } else {
      if(outstandingChargesModel.isDefined && outstandingChargesModel.get.bcdChargeType.isDefined) {
        (LocalDate.parse(outstandingChargesModel.get.bcdChargeType.get.relevantDueDate.get).getYear, outstandingChargesModel.get.bcdChargeType.get.chargeAmount)
      } else {
        (sortedListOfCharges.get.due.get.getYear, sortedListOfCharges.get.remainingToPay)
      }
    }
  }
}
