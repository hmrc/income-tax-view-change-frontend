/*
 * Copyright 2026 HM Revenue & Customs
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

package hub.models.newHomePage

import play.api.i18n.Messages

case class ProofOfYourIncomeCardViewModel(taxYearStart: Int,
                                          calculationType: String,
                                          currentYear: Int,
                                          isLegacy: Boolean = false,
                                          saUtr: Option[String]) {

  val calculationTypeIsValid: Boolean = ("DF", "CA", "IF", "IA", "CR", "AM", "IC").toList.contains(calculationType)
  val itsCurrentTaxYear: Boolean = taxYearStart == currentYear

  def getCardHeader()(implicit messages: Messages): String = {
    if (!isLegacy) {
      if(itsCurrentTaxYear) {
        messages("newHome.overview.tax-year.proofOfIncome.card.currentTY.header", taxYearStart.toString, (taxYearStart + 1).toString)
      }else {
        messages("newHome.overview.tax-year.proofOfIncome.card.previousTY.header", taxYearStart.toString, (taxYearStart + 1).toString)
      }
    } else {
      messages("newHome.overview.tax-year.proofOfIncome.card.legacy.header", taxYearStart.toString, (taxYearStart + 1).toString)
    }
  }

  def getCardRows()(implicit messages: Messages): Seq[CardRows] = {
    if (!isLegacy) {
      val sa302CardBase = CardRows(messages("newHome.overview.tax-year.proofOfIncome.card.sa302key"),
        "",
        Some(messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.action")))

      val taxYearOverviewCardBase = CardRows(messages("newHome.overview.tax-year.proofOfIncome.card.taxYearOverviewKey"),
        "",
        Some(messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.action")))

      calculationType match {
        //CR, AM & IC -> #2150
        case "DF" | "CA" | "CR" | "AM" => Seq(
          sa302CardBase.copy(rowDescription = messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.proof")),
          taxYearOverviewCardBase.copy(rowDescription = messages("newHome.overview.tax-year.proofOfIncome.card.taxYearOverviewValue.proof"))
        )
        case "IF" | "IA" | "IC" => Seq(
          sa302CardBase.copy(rowDescription = messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.incomplete"), action = None),
          taxYearOverviewCardBase.copy(rowDescription = messages("newHome.overview.tax-year.proofOfIncome.card.taxYearOverviewValue.notAvailable"), action = None)
        )
        case _ => Seq.empty
      }
    }else {
      Seq(CardRows(messages("newHome.overview.tax-year.proofOfIncome.card.legacy"),
        messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.legacy"),
        Some(messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.action"))))
    }
  }
}
case class CardRows(rowHeader: String, rowDescription: String, action: Option[String])
