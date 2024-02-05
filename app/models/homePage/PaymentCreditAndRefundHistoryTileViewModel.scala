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

package models.homePage

import models.financialDetails.{FinancialDetailsModel, FinancialDetailsResponseModel}
import play.api.i18n.Messages

case class PaymentCreditAndRefundHistoryTileViewModel(unpaidCharges: List[FinancialDetailsResponseModel],
                                                      creditsRefundsRepayEnabled: Boolean, paymentHistoryRefundsEnabled: Boolean) {
  val availableCredit: Option[BigDecimal] =
    if (creditsRefundsRepayEnabled) {
      Some(unpaidCharges.collectFirst {
        case fdm: FinancialDetailsModel =>
          fdm.balanceDetails.getAbsoluteAvailableCreditAmount.getOrElse(BigDecimal(0.00))
      }.getOrElse(BigDecimal(0.00)))
    } else None

  def title()(implicit messages: Messages): String = (creditsRefundsRepayEnabled, paymentHistoryRefundsEnabled) match {
    case (_, true) => messages("home.paymentHistoryRefund.heading")
    case (_, _) => messages("home.paymentHistory.heading")
  }

}
