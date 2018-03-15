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

import models.reportDeadlines._
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import utils.ImplicitDateFormatter.localDateOrdering
import utils.ImplicitListMethods
import views.html.templates.reportDeadlines.{reportDeadlines_error_template, reportDeadlines_template}

object ReportDeadlineRenderHelper extends ImplicitListMethods {

  def renderReportDeadlines(deadlines: ReportDeadlinesResponseModel, id: String)(implicit messages: Messages): HtmlFormat.Appendable =
    deadlines match {
      case rds: ReportDeadlinesModel => reportDeadlines_template(subsetReportDeadlines(rds), id)
      case _ => reportDeadlines_error_template(id)
    }

  def subsetReportDeadlines(rds: ReportDeadlinesModel): ReportDeadlinesModel =
    ReportDeadlinesModel(getLatestReceived(rds.obligations) ++ getAllOverdue(rds.obligations) ++ getNextDue(rds.obligations))

  private def getLatestReceived(obligations: List[ReportDeadlineModel]): List[ReportDeadlineModel] =
    obligations.filter(_.getReportDeadlineStatus == Received).maxItemBy(_.due)

  private def getAllOverdue(obligations: List[ReportDeadlineModel]): List[ReportDeadlineModel] =
    obligations.filter(_.getReportDeadlineStatus == Overdue)

  private def getNextDue(obligations: List[ReportDeadlineModel]): List[ReportDeadlineModel] =
    obligations.filter(_.getReportDeadlineStatus.isInstanceOf[Open]).minItemBy(_.due)

}
