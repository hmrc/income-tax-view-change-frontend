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

import play.api.libs.json.{Json, Reads, Writes}

import scala.math.abs

case class BalanceDetails(balanceDueWithin30Days: BigDecimal,
                          overDueAmount: BigDecimal,
                          totalBalance: BigDecimal,
                          availableCredit: Option[BigDecimal],
                          firstPendingAmountRequested: Option[BigDecimal],
                          secondPendingAmountRequested: Option[BigDecimal],
                          unallocatedCredit: Option[BigDecimal]
                          ) {

  val refundInProgress: Boolean = firstPendingAmountRequested.isDefined || secondPendingAmountRequested.isDefined
  val total: Option[BigDecimal] = availableCredit.map{ total =>
    total - firstPendingAmountRequested.getOrElse(0) - secondPendingAmountRequested.getOrElse(0)
  }

  def getAbsoluteUnAllocatedCreditAmount: Option[BigDecimal] = {
    unallocatedCredit.map (credit => math.abs(credit.toDouble))
  }

  def getAbsoluteAvailableCreditAmount: Option[BigDecimal] = {
    availableCredit.map (credit => math.abs(credit.toDouble))
  }

}



object BalanceDetails {
  implicit val writes: Writes[BalanceDetails] = Json.writes[BalanceDetails]
  implicit val reads: Reads[BalanceDetails] = Json.reads[BalanceDetails]
}
