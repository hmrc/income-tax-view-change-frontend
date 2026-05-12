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

package models.newHomePage

import play.api.i18n.Messages
import services.DateServiceInterface

case class ProofOfYourIncomeCardViewModel(taxYearStart: Int,
                                          calculationType: String,
                                          currentYear: Int,
                                          isLegacy: Boolean = false,
                                          saUtr: String) {

  val calculationTypeIsValid: Boolean = ("DF", "CA", "IF", "IA").toList.contains(calculationType)
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

  def getCardRows()(implicit messages: Messages): Option[CardRows] = {
    if (!isLegacy) {
      val cardBase = CardRows(messages("newHome.overview.tax-year.proofOfIncome.card.sa302key"),
        "",
        Some(messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.action"))) // TODO: Tax overview tax should have logic to check if action is needed
      calculationType match {
        case "DF" | "CA" => Some(cardBase.copy(rowDescription = messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.proof")))
        case "IF" | "IA" => Some(cardBase.copy(rowDescription = messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.incomplete")))
        case _ => None
      }
    }else {
     Some(CardRows(messages("newHome.overview.tax-year.proofOfIncome.card.legacy"),
       messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.legacy"),
       Some(messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.action"))))
    }
  }
}
case class CardRows(rowHeader: String, rowDescription: String, action: Option[String])
