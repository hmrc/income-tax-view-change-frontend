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

package models.repaymentHistory

import play.api.libs.json._

import java.time.LocalDate

sealed abstract class RepaymentHistoryStatus(indicator: String) {
  override def toString: String = this.indicator
}

final case class Approved (indicator: String) extends RepaymentHistoryStatus(indicator) {
  def isApprovedByRisking: Boolean = indicator.equals("A")

  def isApprovedManually: Boolean = indicator.equals("M")
}

final case class Rejected (indicator: String) extends RepaymentHistoryStatus(indicator)

object SentForRisking extends RepaymentHistoryStatus("I")

object RepaymentHistoryStatus {
  def apply(indicator: String): RepaymentHistoryStatus = {
    indicator match {
      case "A" | "M" | "Approved" => Approved(indicator)
      case "I" | "Sent for Risking" => SentForRisking
      case "C" | "Rejected" => Rejected(indicator)
      case _ => Rejected(indicator)
    }
  }

  implicit val writes: Writes[RepaymentHistoryStatus] = (o: RepaymentHistoryStatus) => JsString(o.toString)
  implicit val reads: Reads[RepaymentHistoryStatus] = __.read[String].map(RepaymentHistoryStatus(_))
}


case class TotalInterest(fromDate: LocalDate, fromRate: BigDecimal,
                         toDate: LocalDate, toRate: BigDecimal,
                         total: BigDecimal)

case class RepaymentHistory(amountApprovedforRepayment: Option[BigDecimal],
                            amountRequested: BigDecimal,
                            repaymentMethod: Option[String],
                            totalRepaymentAmount: Option[BigDecimal],
                            repaymentItems: Option[Seq[RepaymentItem]],
                            estimatedRepaymentDate: Option[LocalDate],
                            creationDate: Option[LocalDate],
                            repaymentRequestNumber: String,
                            status: RepaymentHistoryStatus
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
      .collect {
        case Some(amount) => amount
      }.sum
    )
  }

  def aggregate: Option[TotalInterest] = {
    repaymentItems.flatMap { repaymentItems =>
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
}

object RepaymentHistory {
  implicit val format: OFormat[RepaymentHistory] = Json.format[RepaymentHistory]
}
