/*
 * Copyright 2020 HM Revenue & Customs
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

import implicits.ImplicitDateFormatter.localDateOrdering
import models.reportDeadlines._
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.html.templates.reportDeadlines.{reportDeadlines_error_template, reportDeadlines_template}

object ReportDeadlineRenderHelper {

  def renderReportDeadlines(deadlines: ReportDeadlinesResponseModel, id: String, caption: String)(implicit messages: Messages): HtmlFormat.Appendable =
    deadlines match {
      case rds: ReportDeadlinesModel => reportDeadlines_template(rds.obligations.sortBy(_.due), id, caption)
      case _ => reportDeadlines_error_template(id)
    }


}
