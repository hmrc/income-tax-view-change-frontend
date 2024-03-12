/*
 * Copyright 2024 HM Revenue & Customs
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

sealed trait CreditType {
  val key: String
}

case object MfaCreditType extends CreditType {
  override val key = "credit.description.mfaCredit"
}

case object CutOverCreditType extends CreditType {
  override val key = "credit.description.paymentFromEarlierYear"
}

case object BalancingChargeCreditType extends CreditType {
  override val key = "credit.description.balancingChargeCredit"
}

case object SetOffCharge extends CreditType {
  override val key = "credit.description.setOffCharge"
}

case object SetOffChargeInterest extends CreditType {
  override val key = "credit.description.setOffChargeInterest"
}

object CreditType {

  // values come from EPID #1138
  private val cutOver = "6110"
  private val balancingCharge = "4905"
  private val setOffCharge = "0060"
  private val setOffChargeInterest = "6020"
  private val mfaCredit = Range.inclusive(4004, 4025)
    .filterNot(_ == 4010).filterNot(_ == 4020).map(_.toString)
    .toList

  def apply(mainTransaction: String): Option[CreditType] = {
    mainTransaction match {
      case CreditType.cutOver =>
        Some(CutOverCreditType)
      case CreditType.balancingCharge =>
        Some(BalancingChargeCreditType)
      case CreditType.setOffCharge =>
        Some(SetOffCharge)
      case CreditType.setOffChargeInterest =>
        Some(SetOffChargeInterest)
      case x if mfaCredit.contains(x) =>
        Some(MfaCreditType)
      case _ => None
    }
  }
}
