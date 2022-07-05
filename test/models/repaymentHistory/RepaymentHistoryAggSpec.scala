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

import org.scalacheck.{Gen, Properties}

import java.time.LocalDate

object RepaymentHistoryAggSpec extends Properties("RepaymentHistory_aggregate") {

  import org.scalacheck.Prop.forAll

  def dateGen: Gen[LocalDate] = {
    val currentYear = LocalDate.now().getYear
    val rangeFrom = LocalDate.of(currentYear - 60, 1, 1).toEpochDay
    val rangeEnd = LocalDate.of(currentYear, 1, 1).toEpochDay
    Gen.choose(rangeFrom, rangeEnd).map(i => LocalDate.ofEpochDay(i))
  }

  def repaymentSupplementItemGen: Gen[RepaymentSupplementItem] = for {
    from <- dateGen
    addDays <- Gen.choose(30, 90)
    amount <- Gen.choose(100, 99999)
    rate <- Gen.choose(1, 200)
  } yield RepaymentSupplementItem(parentCreditReference = None,
    amount = Some(BigDecimal(amount.toDouble / 100)),
    fromDate = Some(from),
    toDate = Some(from.plusDays(addDays)),
    rate = Some(BigDecimal(rate.toDouble / 1000)))

  def repaymentHistoryGen: Gen[RepaymentHistory] = for {
    estimatedDate <- dateGen
    date <- dateGen
    items <- Gen.listOf(repaymentSupplementItemGen)
  } yield RepaymentHistory(
    amountApprovedforRepayment = None,
    amountRequested = BigDecimal("120.05"),
    repaymentMethod = "",
    totalRepaymentAmount = BigDecimal("20.05"),
    repaymentItems = Seq(RepaymentItem(repaymentSupplementItem = items)),
    estimatedRepaymentDate = estimatedDate,
    creationDate = date,
    repaymentRequestNumber = "04005005659")

  property("aggregate") = forAll(repaymentHistoryGen) { repaymentHistoryRecord =>
    println(repaymentHistoryRecord)
    val agg = repaymentHistoryRecord.aggregate

    val minDateOpt = repaymentHistoryRecord
      .repaymentItems.headOption.map(_.repaymentSupplementItem.map(_.fromDate)
      .collect {
        case Some(date) => (date.toEpochDay, date)
      })
      .map(ds => if (ds.nonEmpty) agg.map(rec => rec.fromDate == ds.minBy(_._1)._2).getOrElse(true) else true)

    val maxDateOpt = repaymentHistoryRecord
      .repaymentItems.headOption.map(_.repaymentSupplementItem.map(_.toDate)
      .collect {
        case Some(date) => (date.toEpochDay, date)
      })
      .map(ds => if (ds.nonEmpty) agg.map(rec => rec.toDate == ds.maxBy(_._1)._2).getOrElse(true) else true)

    val minRateOpt = repaymentHistoryRecord
      .repaymentItems.headOption.map(_.repaymentSupplementItem.map(_.rate)
      .collect {
        case Some(rate) => rate
      })
      .map(rs => if (rs.nonEmpty) agg.map(_.fromRate == rs.min).getOrElse(true) else true)

    val maxRateOpt = repaymentHistoryRecord
      .repaymentItems.headOption.map(_.repaymentSupplementItem.map(_.rate)
      .collect {
        case Some(rate) => rate
      })
      .map(rs => if (rs.nonEmpty) agg.map(_.toRate == rs.max).getOrElse(true) else true)

    val totalOpt = repaymentHistoryRecord
      .repaymentItems.headOption.map(_.repaymentSupplementItem.map(_.amount)
      .collect {
        case Some(amount) => amount
      }).map(ts => agg.map(_.total == ts.sum).getOrElse(true))

    {
      for {
        min <- minDateOpt
        max <- maxDateOpt
        fromRate <- minRateOpt
        toRate <- maxRateOpt
        total <- totalOpt
      } yield min && max && fromRate && toRate && total
    }.getOrElse(true)
  }

}
