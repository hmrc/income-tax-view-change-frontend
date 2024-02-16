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

import models.creditDetailModel.{BalancingChargeCreditType, CreditType, CutOverCreditType, MfaCreditType}
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetail}


case class CreditAndRefundViewModel(creditCharges: List[(DocumentDetailWithDueDate, FinancialDetail)]) {


  private val balancingChargeCredit = "BCC"
  private val mfaCredit = "MFA"
  private val cutOverCredit = "CutOver"
  private val payment = "Payment"

  def sortCreditsByTypeAndMonetaryValue(credits: List[(DocumentDetailWithDueDate, FinancialDetail)])
  : List[(DocumentDetailWithDueDate, FinancialDetail)] = {

    val creditTypeSortingOrder = Map(
      balancingChargeCredit -> 1,
      mfaCredit -> 2,
      cutOverCredit -> 3,
      payment -> 4
    )

    val sortedCredits = credits.groupBy[String] {
      credits => {
        getCreditType(credits)
      }
    }.toList.sortWith((p1, p2) => creditTypeSortingOrder(p1._1) < creditTypeSortingOrder(p2._1))
      .map {
        case (documentDetailWithDueDate, financialDetail) => (documentDetailWithDueDate, sortCreditsByMonetaryValue(financialDetail))
      }.flatMap {
      case (_, credits) => credits
    }
    sortedCredits
  }

  def getCreditType(credit: (DocumentDetailWithDueDate, FinancialDetail)): String = {
    val creditType: Option[CreditType] = credit._2.getCreditType
    val isPayment: Boolean = credit._1.documentDetail.paymentLot.isDefined

    (creditType, isPayment) match {
      case (Some(BalancingChargeCreditType), false) => balancingChargeCredit
      case (Some(MfaCreditType), false) => mfaCredit
      case (Some(CutOverCreditType), false) => cutOverCredit
      case (None, true) => payment
      case (_, _) => "Unknown credit"
    }
  }

  def sortCreditsByMonetaryValue(credits: List[(DocumentDetailWithDueDate, FinancialDetail)])
  : List[(DocumentDetailWithDueDate, FinancialDetail)] = {
    credits
      .sortBy(_._1.documentDetail.paymentOrChargeCredit).reverse
  }

}



