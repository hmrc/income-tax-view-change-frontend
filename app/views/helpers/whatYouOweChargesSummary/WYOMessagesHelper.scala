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

  def getMessage(key: String, args: String*)(implicit messages: Messages): String =
    messages(s"whatYouOwe.$key", args: _*)

  def getPrefix(key: String): String = s"whatYouOwe.$key"

  def getHeading(implicit user: MtdItUser[_], messages: Messages): String =
    getMessage(
      if(user.isAgent()) "heading-agent"
      else               "heading"
    )
}
