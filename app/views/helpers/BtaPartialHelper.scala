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

import models.{ObligationModel, Overdue, Open}
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.ImplicitDateFormatter._

object BtaPartialHelper {
  
  def whichStatus(model: ObligationModel)(implicit messages: Messages) = model.getObligationStatus match {
    case open: Open =>
      Html(
        s"""
           |<p>${messages("bta_partial.next_due", model.due.toLongDate)}</p>
           |<p>${messages("bta_partial.deadlines_link")}</p>
         """.stripMargin
      )
    case overdue: Overdue.type =>
      Html(
        s"""
           |<p>${messages("bta_partial.next_due", model.due.toLongDate)}</p>
           |<a id="obligations-link" href=${controllers.routes.ObligationsController.getObligations().url}>${messages("bta_partial.deadlines_link")}</a>
         """.stripMargin
      )
    case _ =>
      //TODO something better than this
  }

  def showLastEstimate(estimate: Option[BigDecimal])(implicit messages: Messages) = estimate match {
    case Some(est) =>
      Html(
        s"""
           |<p>${messages("bta_partial.estimated_tax", est)}</p>
           |<a id="estimates-link" href=${controllers.routes.FinancialDataController.redirectToEarliestEstimatedTaxLiability().url}>${messages("bta_partial.view_details_link")}</a>
         """.stripMargin
      )
    case None =>
      Html(
        s"""
           |
         """.stripMargin
      )
  }

}
