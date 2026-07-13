/*
 * Copyright 2026 HM Revenue & Customs
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

package financials.models

case class MakingPaymentViewModel(
                                   backUrl: String,
                                   paymentHandoffUrl: String,
                                   whatYouOweUrl: String,
                                   moneyInYourAccountUrl: String,
                                   payPenaltyUrl: String,
                                   hasInterest: Boolean,
                                   hasPenalty: Boolean,
                                   unallocatedCredit: Option[BigDecimal],
                                   hasOverdue: Boolean
                                 ) {

  val hasMoneyInAccount: Boolean = unallocatedCredit.exists(_ > 0)

  val hasAdditionalSections: Boolean = hasInterest || hasPenalty || hasMoneyInAccount
}
