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

package views.helpers

import play.api.i18n.Messages
import views.html.agent.EnterClientsUTR


object HtmlTitle {

  def apply(isAgent: Boolean = false, isErrorPage: Boolean = false, isInvalidInput: Boolean = false, h1Text: String, showServiceName: Boolean = true)
           (implicit messages: Messages): String = {

    (isInvalidInput, isErrorPage, isAgent, showServiceName) match {
      case (false, true, _, _) => messages("htmlTitle.errorPage", h1Text)
      case (true, false, _, _) => messages("htmlTitle.invalidInput", h1Text)
      case (_, _, true, true) => messages("htmlTitle.agent", h1Text)
      case(_, _, true, false) => messages("htmlTitle.confirmClient", h1Text)
      case (_, _, _, _) => messages("htmlTitle", h1Text)

    }
  }
}