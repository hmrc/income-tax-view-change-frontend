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


import config.AppConfig
import models._
import play.api.i18n.Messages
import utils.ImplicitCurrencyFormatter._
import utils.ImplicitDateFormatter._

object BtaPartialHelper {

  def whichStatus(model: ObligationModel)(implicit messages: Messages, config: AppConfig): String = {
    val obligationMessage = model.getObligationStatus match {
      case _: Open => messages("bta_partial.next_due", model.due.toLongDate)
      case Overdue => messages("bta_partial.next_overdue")
      case Received => messages("bta_partial.next_received")
    }

    applyFormGroup {
      s"""
        |<p id="report-due">$obligationMessage</p>
        |<a data-journey-click="mtdPartial:clickedLink:View Obligations" id="obligations-link" href="${config.itvcFrontendEnvironment + controllers.routes.ObligationsController.getObligations().url}">
        |${messages("bta_partial.deadlines_link")}</a>"""
    }.stripMargin.replaceAll("\n","")
  }

  def showLastEstimate(estimates: List[LastTaxCalculationWithYear])(implicit messages: Messages, config: AppConfig): List[String] = {
    def estimatesMessage(taxYear: Int, calcAmount: BigDecimal): String = {
      if (estimates.length > 1) {
        messages("bta_partial.estimated_tax_with_year", (taxYear - 1).toString, taxYear.toString, calcAmount.toCurrency)
      } else {
        messages("bta_partial.estimated_tax_no_year", calcAmount.toCurrency)
      }
    }

    estimates.map {
      estimate =>
        applyFormGroup {
          estimate.calculation match {
            case calc: LastTaxCalculation =>
              val taxYear = estimate.taxYear
              s"""<p id="current-estimate-$taxYear">${estimatesMessage(taxYear, calc.calcAmount)}</p>
                 |<a data-journey-click="mtdPartial:clickedLink:View Estimated Tax Liability ${taxYear}" id="estimates-link-$taxYear" href="${config.itvcFrontendEnvironment + controllers.routes.FinancialDataController.getFinancialData(taxYear).url}">
                 |${messages("bta_partial.view_details_link")}</a>"""
            case NoLastTaxCalculation =>
              val taxYear = estimate.taxYear
              s"""<p id="current-estimate-$taxYear">${messages("bta_partial.no_estimate", (taxYear - 1).toString, taxYear.toString)}</p>"""
            case _: LastTaxCalculationError => ""
          }
        }.stripMargin.replaceAll("\n","")
    }
  }

  private def applyFormGroup(content: String): String = """<div class="form-group">""" + content + "</div>"
}

