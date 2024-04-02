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

package models.creditsandrefunds

import models.financialDetails._
import play.api.i18n.Messages

import java.time.LocalDate

sealed trait CreditRow {
  val amount: BigDecimal
  val creditType: CreditType
}
case class CreditViewRow(amount: BigDecimal, creditType: CreditType, taxYear: String) extends CreditRow
case class PaymentCreditRow(amount: BigDecimal, creditType: CreditType, date: LocalDate) extends CreditRow

case class CreditAndRefundViewModel(creditCharges: List[(DocumentDetailWithDueDate, FinancialDetail)]) {

  def creditViewRows()(implicit messages: Messages): List[CreditRow] = {
    val sortedCreditRows: Seq[Option[CreditRow]] = sortCreditsByYear.map(cc => {
      val maybeCreditRow = for {
        creditType <- cc._2.getCreditType
        amount <- cc._1.documentDetail.paymentOrChargeCredit
      } yield {
        creditType match {
          case PaymentType => PaymentCreditRow(amount = amount, creditType = creditType, date = cc._1.dueDate.get)
          case _ =>  CreditViewRow(amount = amount, creditType = creditType, taxYear = cc._2.taxYear)
        }
      }
      maybeCreditRow
    })
    sortedCreditRows.flatten.toList
  }

  def sortCreditsByYear: List[(DocumentDetailWithDueDate, FinancialDetail)] = {
    val sortedCredits = creditCharges.sortBy {
      case (_, financialDetails) => financialDetails.taxYear
    }
    sortedCredits.reverse
  }

  val sortedCreditCharges = sortCreditsByYear


}



