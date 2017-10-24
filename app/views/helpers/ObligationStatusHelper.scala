/*
 * Copyright 2017 HM Revenue & Customs
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

import models.{ObligationStatus, Open, Overdue, Received}
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.ImplicitDateFormatter._

object ObligationStatusHelper {

  def statusHtml(status: ObligationStatus)(implicit messages: Messages): Html = status match {
    case open: Open =>
      Html(
        s"""
           |<p class="flush--bottom  alert  soft-half--ends soft--right">
           |  <span class='bold-xsmall'>
           |    ${messages("status.open", open.dueDate.toLongDate)}
           |  </span>
           |</p>
         """.stripMargin)
    case _: Received.type =>
      Html(
        s"""
           |<p class="flush--bottom  alert  soft-half--ends soft--right" style="color: #005ea5;">
           |  <span class='bold-xsmall'>
           |    ${messages("status.received")}
           |  </span>
           |</p>
           """.stripMargin)
    case _: Overdue.type   =>
      Html(
        s"""
           |<p class="flush--bottom  alert  soft-half--ends soft--right" style="color: #b10e1e;">
           |  <span class='bold-xsmall'>
           |    ${messages("status.overdue")}
           |  </span>
           |</p>
           """.stripMargin)
  }

}
