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

package models.repaymentHistory

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class TotalInterest(fromDate: LocalDate, fromRate: BigDecimal,
                         toDate: LocalDate, toRate: BigDecimal,
                         total: BigDecimal)
//TODO CHECK
case class RepaymentHistory(amountApprovedforRepayment: Option[BigDecimal],
                            amountRequested: BigDecimal,
                            repaymentMethod: String,
                            totalRepaymentAmount: BigDecimal,
                            repaymentItems: Seq[RepaymentItem],
                            estimatedRepaymentDate: LocalDate,
                            creationDate: LocalDate,
                            repaymentRequestNumber: String
                           ) {

  private val fromDateOpt = (repayments: Seq[RepaymentItem]) => {
    val items = repayments
      .flatMap(_.repaymentSupplementItem)
      .map(_.fromDate).collect {
        case Some(date) => (date.toEpochDay, date)
      }

    if (items.nonEmpty) {
      val (_, fromDates) = items.minBy(_._1)
      Some((fromDates))
    } else {
      None
    }

  }

  private val fromRateOpt = (repayments: Seq[RepaymentItem]) => {
    val items = repayments
      .flatMap(_.repaymentSupplementItem)
      .map(_.rate).collect {
        case Some(rate) => rate
      }
    if (items.nonEmpty) {
      Some(items.min)
    } else {
      None
    }
  }

  private val toDateOpt = (repayments: Seq[RepaymentItem]) => {
    val items = repayments
      .flatMap(_.repaymentSupplementItem)
      .map(_.toDate).collect {
        case Some(date) => (date.toEpochDay, date)
      }
    if (items.nonEmpty) {
      val (_, toDates) = items.maxBy(_._1)
      Some(toDates)
    } else {
      None
    }
  }

  private val toRateOpt = (repayments: Seq[RepaymentItem]) => {
    val items = repayments
      .flatMap(_.repaymentSupplementItem)
      .map(_.rate).collect {
        case Some(rate) => rate
      }
    if (items.nonEmpty) {
      Some(items.max)
    } else {
      None
    }
  }

  private val totalOpt = (repayments: Seq[RepaymentItem]) => {
    Some(repayments
      .flatMap(_.repaymentSupplementItem.map(_.amount))
      .collect{
        case Some(amount) => amount
      }.sum
    )
  }

  def aggregate: Option[TotalInterest] = {
    for {
      fromDate <- fromDateOpt(repaymentItems)
      fromRate <- fromRateOpt(repaymentItems)
      toDate <- toDateOpt(repaymentItems)
      toRate <- toRateOpt(repaymentItems)
      totalAmount <- totalOpt(repaymentItems)
    } yield TotalInterest(
      fromDate = fromDate,
      fromRate = fromRate,
      toDate = toDate,
      toRate = toRate,
      total = totalAmount)
  }
}

object RepaymentHistory {
  implicit val format: OFormat[RepaymentHistory] = Json.format[RepaymentHistory]

}
