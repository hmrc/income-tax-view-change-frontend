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


  def sortCreditsByYear: List[(DocumentDetailWithDueDate, FinancialDetail)] = {
    val sortedCredits = creditCharges.sortBy {
      case (_, financialDetails) => financialDetails.taxYear
    }
    sortedCredits.reverse
  }

  val sortedCreditCharges = sortCreditsByYear


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

}



