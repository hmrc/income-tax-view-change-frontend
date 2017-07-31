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

import models._
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.ImplicitDateFormatter._
import utils.ImplicitCurrencyFormatter._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BtaPartialHelper {
  
  def whichStatus(model: ObligationModel)(implicit messages: Messages): Html = model.getObligationStatus match {
    case open: Open =>
      Html(
        s"""
           |<p id="report-due-open">${messages("bta_partial.next_due", model.due.toLongDate)}</p>
           |<a id="obligations-link" href=${controllers.routes.ObligationsController.getObligations().url}>${messages("bta_partial.deadlines_link")}</a>
         """.stripMargin.trim
      )
    case Overdue =>
      Html(
        s"""
           |<p id="report-due-overdue">${messages("bta_partial.next_overdue")}</p>
           |<a id="obligations-link" href=${controllers.routes.ObligationsController.getObligations().url}>${messages("bta_partial.deadlines_link")}</a>
         """.stripMargin.trim
      )
    case Received =>
      Html(
        s"""
           |<p id="report-due-received">${messages("bta_partial.next_received")}</p>
           |<a id="obligations-link" href=${controllers.routes.ObligationsController.getObligations().url}>${messages("bta_partial.deadlines_link")}</a>
         """.stripMargin.trim
      )
  }

  def showLastEstimate(estimates: List[LastTaxCalculationWithYear])(implicit messages: Messages): List[Html] =
    estimates.map {
      estimate => estimate.calculation match {
        case calc: LastTaxCalculation =>
          Html(
            s"""
               |<p id="current-estimate-${estimate.taxYear}">${messages("bta_partial.estimated_tax", calc.calcAmount.toCurrency)}</p>
               |<a id="estimates-link" href=${controllers.routes.FinancialDataController.getFinancialData(estimate.taxYear).url}>${messages("bta_partial.view_details_link")}</a>
             """.stripMargin.trim
          )
        case NoLastTaxCalculation =>
          Html(
            s"""
               <p>${messages("bta_partial.no_estimate", (estimate.taxYear - 1).toString, estimate.taxYear.toString)}</p>
             """.stripMargin.trim
          )
        case calc: LastTaxCalculationError =>
          Html(
            s"""
               Error
             """.stripMargin.trim
          )
      }
    }
}
