/*
 * Copyright 2023 HM Revenue & Customs
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

import models.financialDetails.{ChargeItem, BalancingCharge, ITSAReturnAmendment, Nics2}
import play.api.i18n.Messages


object ChargeNameLangHelper {

  def getHeading(chargeItem: ChargeItem)(implicit messages: Messages): String = {
    val messagePrefix = if (chargeItem.isOnlyInterest) "lpi." else ""
    messages(s"yourSelfAssessmentChargeSummary.$messagePrefix${chargeItem.getChargeTypeKey}.heading")
  }

  def chargeHistoryCaption(chargeItem: ChargeItem)(implicit messages: Messages): String = {
    if (List(BalancingCharge, ITSAReturnAmendment).contains(chargeItem.transactionType) && !chargeItem.codedOutStatus.contains(Nics2))
      messages(s"yourSelfAssessmentChargeSummary.chargeHistory.default.caption",
      chargeItem.taxYear.startYear.toString, chargeItem.taxYear.endYear.toString)
    else ""
  }
}
