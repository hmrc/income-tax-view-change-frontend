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

package models.creditDetailModel

import models.financialDetails.{BalanceDetails, DocumentDetail, FinancialDetail}

import java.time.LocalDate

case class CreditDetailModel(date: LocalDate , documentDetail: DocumentDetail, creditType: CreditType, balanceDetails: Option[BalanceDetails] = None, financialDetail: Option[FinancialDetail] = None)

sealed trait CreditType {
  val key: String
}

case object MfaCreditType extends CreditType {
  override val key = "paymentHistory.mfaCredit"
}

case object CutOverCreditType extends CreditType {
  override val key = "paymentHistory.paymentFromEarlierYear"
}

case object BalancingChargeCreditType extends CreditType {
  override val key = "paymentHistory.balancingChargeCredit"
}