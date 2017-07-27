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

import models.{LastTaxCalculation, ObligationModel, Open, Overdue, Received}
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.ImplicitDateFormatter._
import utils.ImplicitCurrencyFormatter._

object BtaPartialHelper {
  
  def whichStatus(model: ObligationModel)(implicit messages: Messages): Html = model.getObligationStatus match {
    case open: Open =>
      Html(
        s"""
           |<p id="report-due">${messages("bta_partial.next_due", model.due.toLongDate)}</p>
           |<a id="obligations-link" href=${controllers.routes.ObligationsController.getObligations().url}>${messages("bta_partial.deadlines_link")}</a>
         """.stripMargin.trim
      )
    case Overdue =>
      Html(
        s"""
           |<p id="report-due">${messages("bta_partial.next_overdue")}</p>
           |<a id="obligations-link" href=${controllers.routes.ObligationsController.getObligations().url}>${messages("bta_partial.deadlines_link")}</a>
         """.stripMargin.trim
      )
    case Received =>
      Html(
        s"""
           |<p id="report-due">${messages("bta_partial.next_received")}</p>
           |<a id="obligations-link" href=${controllers.routes.ObligationsController.getObligations().url}>${messages("bta_partial.deadlines_link")}</a>
         """.stripMargin.trim
      )
  }

  def showLastEstimate(estimates: Option[List[LastTaxCalculation]])(implicit messages: Messages): Html = {
    if(estimates.isDefined) {
      estimates.get match {
        case estimate if estimate.length == 1 =>
          Html(
            s"""
               |<p id="current-estimate">${messages("bta_partial.estimated_tax", estimate.head.calcAmount.toCurrency)}</p>
               |<a id="estimates-link" href=${controllers.routes.FinancialDataController.redirectToEarliestEstimatedTaxLiability().url}>${messages("bta_partial.view_details_link")}</a>
             """.stripMargin.trim
          )
        case estimate if estimate.length == 2 =>
          Html(
            s"""
               |<p id="current-estimate-earliest">${messages("bta_partial.estimated_tax", estimate.head.calcAmount.toCurrency)}</p>
               |<a id="estimates-link" href=${controllers.routes.FinancialDataController.redirectToEarliestEstimatedTaxLiability().url}>${messages("bta_partial.view_details_link")}</a>
               |<p id="current-estimate-last">${messages("bta_partial.estimated_tax", estimate(1).calcAmount.toCurrency)}</p>
               |<a id="estimates-link" href=${controllers.routes.FinancialDataController.redirectToEarliestEstimatedTaxLiability().url}>${messages("bta_partial.view_details_link")}</a>
           """.stripMargin.trim
          )
      }
    } else Html("")
  }
}
