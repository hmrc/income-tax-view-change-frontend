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

import models.incomeSourceDetails.TaxYear
import models.nextPayments.viewmodels.WYOClaimToAdjustViewModel
import models.taxYearAmount.EarliestDueCharge
import services.DateServiceInterface

import java.time.LocalDate

case class YourSelfAssessmentChargesViewModel(hasOverdueOrAccruingInterestCharges: Boolean,
                                              whatYouOweChargesList: WhatYouOweChargesList,
                                              hasLpiWithDunningLock: Boolean,
                                              backUrl: String,
                                              dunningLock: Boolean,
                                              reviewAndReconcileEnabled: Boolean,
                                              penaltiesEnabled: Boolean,
                                              LPP2Url: String,
                                              creditAndRefundEnabled: Boolean,
                                              earliestTaxYearAndAmountByDueDate: Option[EarliestDueCharge],
                                              claimToAdjustViewModel: WYOClaimToAdjustViewModel)(implicit val dateService: DateServiceInterface) {
  lazy val currentTaxYear: TaxYear = dateService.getCurrentTaxYear

  lazy val currentDate: LocalDate = dateService.getCurrentDate

  def overdueChargesWithIndex: List[(ChargeItem, Int)] = {
    ChargeItem.sortedOverdueOrAccruingInterestChargeList(whatYouOweChargesList).zipWithIndex
  }

  def chargesDueWithin30DaysWithIndex: List[(ChargeItem, Int)] = {
    ChargeItem.chargesDueWithin30DaysList(whatYouOweChargesList).zipWithIndex
  }

  def chargesDueAfter30DaysWithIndex: List[(ChargeItem, Int)] = {
    ChargeItem.chargesDueAfter30DaysList(whatYouOweChargesList).zipWithIndex
  }

  def chargesDueAfter30DaysListNonEmpty: Boolean = {
    ChargeItem.chargesDueAfter30DaysList(whatYouOweChargesList).nonEmpty
  }

  def chargesDueWithin30DaysListNonEmpty: Boolean = {
    ChargeItem.chargesDueWithin30DaysList(whatYouOweChargesList).nonEmpty
  }

  def overdueAccruingInterestOrOutstandingChargesListNonEmpty: Boolean = {
    ChargeItem.overdueOrAccruingInterestChargeList(whatYouOweChargesList).nonEmpty ||
      whatYouOweChargesList.overdueOutstandingCharges.nonEmpty
  }
}

object YourSelfAssessmentChargesViewModel {
  def getDisplayDueDate(chargeItem: ChargeItem): LocalDate = if (chargeItem.isLatePaymentInterest && chargeItem.isPaid) {
    chargeItem.getInterestEndDate
  } else {
    chargeItem.getDueDate
  }
}
