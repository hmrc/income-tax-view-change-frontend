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
  val availableCredit: Option[BigDecimal] = unpaidCharges.collectFirst {
    case fdm: FinancialDetailsModel if creditsRefundsRepayEnabled => fdm.balanceDetails.getAbsoluteAvailableCreditAmount
  }.flatten
  def title()(implicit messages: Messages): String = (creditsRefundsRepayEnabled, paymentHistoryRefundsEnabled) match {
    case (_, true) => messages("home.paymentHistoryRefund.heading")
    case (_, _) => messages("home.paymentHistory.heading")
  }

}
