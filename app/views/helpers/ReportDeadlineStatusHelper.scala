/*
 * Copyright 2018 HM Revenue & Customs
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

import models.reportDeadlines.{Open, Overdue, Received, ReportDeadlineStatus}
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.ImplicitDateFormatter._

object ReportDeadlineStatusHelper {

  def statusHtml(status: ReportDeadlineStatus)(implicit messages: Messages): Html = status match {
    case open: Open =>
      Html(s"""<span>${messages("status.open", open.dueDate.toLongDate)}</span>""")
    case _: Received.type =>
      Html(s"""<span>${messages("status.received")}</span>""")
    case overdue: Overdue =>
      Html(s"""<span>${overdue.dueDate.toLongDate} <strong class="task-overdue">${messages("status.overdue")}</strong></span>""")
  }

}
