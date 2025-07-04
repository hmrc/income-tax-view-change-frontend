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

package views.helpers.whatYouOweChargesSummary

import auth.MtdItUser
import play.api.i18n.Messages

object WYOMessagesHelper {

  def getMessage(key: String, args: String*)(implicit messages: Messages) =
    messages(s"whatYouOwe.$key", args: _*)

  def getPrefix(key: String) = s"whatYouOwe.$key"

  def getPaymentsMadeBulletLinkText(implicit user: MtdItUser[_], messages: Messages): String =
    getMessage(
      if (user.isAgent()) "payments-made-bullet-agent-1.2"
      else                "payments-made-bullet-1.2"
    )

  def getPaymentsMadeBulletText(implicit user: MtdItUser[_], messages: Messages): String =
    getMessage(
      if (user.isAgent()) "whatYouOwe.payments-made-bullet-agent-2"
      else                "whatYouOwe.payments-made-bullet-2"
    )

  def getNoPaymentsDueText(implicit user: MtdItUser[_], messages: Messages): String =
    getMessage(
      if (user.isAgent()) "no-payments-due-agent"
      else                "no-payments-due"
    )

  def getPaymentsMadeText(implicit user: MtdItUser[_], messages: Messages): String =
    getMessage(
      if (user.isAgent()) "payments-made-agent"
      else                "payments-made"
    )


}
