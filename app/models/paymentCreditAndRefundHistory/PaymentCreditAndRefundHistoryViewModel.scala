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

package models.paymentCreditAndRefundHistory

import play.api.i18n.Messages

case class PaymentCreditAndRefundHistoryViewModel(
    creditsRefundsRepayEnabled:      Boolean,
    paymentHistoryAndRefundsEnabled: Boolean) {
  def title()(implicit messages: Messages): String =
    (creditsRefundsRepayEnabled, paymentHistoryAndRefundsEnabled) match {
      case (false, true)  => messages("paymentHistory.paymentAndRefundHistory.heading")
      case (true, false)  => messages("paymentHistory.paymentAndCreditHistory")
      case (true, true)   => messages("paymentHistory.paymentCreditAndRefundHistory.heading")
      case (false, false) => messages("paymentHistory.heading")
    }
}
