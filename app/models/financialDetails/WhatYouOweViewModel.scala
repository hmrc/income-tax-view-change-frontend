/*
 * Copyright 2025 HM Revenue & Customs
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

package models.financialDetails

import models.nextPayments.viewmodels.WYOClaimToAdjustViewModel
import services.DateServiceInterface
import auth.MtdItUser

import java.time.LocalDate

case class WhatYouOweViewModel(currentDate: LocalDate,
                               hasOverdueOrAccruingInterestCharges: Boolean,
                               whatYouOweChargesList: WhatYouOweChargesList,
                               hasLpiWithDunningLock: Boolean,
                               currentTaxYear: Int,
                               backUrl: String,
                               utr: Option[String],
                               dunningLock: Boolean,
                               creditAndRefundUrl: String,
                               creditAndRefundEnabled: Boolean,
                               taxYearSummaryUrl: Int => String,
                               claimToAdjustViewModel: WYOClaimToAdjustViewModel,
                               lpp2Url: String,
                               adjustPoaUrl: String,
                               chargeSummaryUrl: (Int, String, Boolean, Option[String]) => String,
                               paymentHandOffUrl: Long => String,
                               selfServeTimeToPayEnabled: Boolean,
                               selfServeTimeToPayStartUrl: String)(implicit val dateServiceInterface: DateServiceInterface) {

  val chargesListAndCodedOutDetailsAreEmpty: Boolean = whatYouOweChargesList.isChargesListEmpty && whatYouOweChargesList.codedOutDetails.isEmpty

  val chargesListIsNonEmptyOrBcdChargeTypeDefinedAndGreaterThanZero: Boolean = whatYouOweChargesList.chargesList.nonEmpty || whatYouOweChargesList.bcdChargeTypeDefinedAndGreaterThanZero

  val chargesListIsNotEmptyAndDunningLock: Boolean = !whatYouOweChargesList.isChargesListEmpty && (dunningLock || hasLpiWithDunningLock)

  val chargeIsNotDueAndChargesDefinedAndGreaterThanZero: Boolean = whatYouOweChargesList.bcdChargeTypeDefinedAndGreaterThanZero &&
    whatYouOweChargesList.outstandingChargesModel.get.getAciChargeWithTieBreaker.isDefined &&
    whatYouOweChargesList.getRelevantDueDate.isBefore(currentDate)
}
