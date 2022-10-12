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


object HtmlTitle {

  def apply(isAgent: Boolean = false, isErrorPage: Boolean = false, isInvalidInput: Boolean = false, h1Text: String)
           (implicit messages: Messages): String = {

    (isInvalidInput, isErrorPage, isAgent) match {
      case (false, true, _) => messages("htmlTitle.errorPage", h1Text)
      case (true, false, _) => messages("htmlTitle.invalidInput", h1Text)
      case (_, _, true) => h1Text + " - " + messages("htmlTitle.agent") + " - " + messages("base.govUk")
      case (_, _, _) => h1Text + " - " + messages("titlePattern.serviceName.govUk") + " - " + messages("base.govUk")
    }
  }

  def messageTitle(serviceName: String, pageTitle: String)(implicit messages: Messages): String = {
    pageTitle + " - " + messages(serviceName) + " - " + messages("base.govUk")
  }
}