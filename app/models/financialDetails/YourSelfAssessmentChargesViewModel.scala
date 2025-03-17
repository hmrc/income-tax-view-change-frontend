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

import models.financialDetails.YourSelfAssessmentChargesViewModel.getDisplayDueDate
import models.incomeSourceDetails.TaxYear
import models.nextPayments.viewmodels.WYOClaimToAdjustViewModel

import java.time.LocalDate

case class YourSelfAssessmentChargesViewModel(currentDate: LocalDate,
                                              hasOverdueOrAccruingInterestCharges: Boolean,
                                              whatYouOweChargesList: WhatYouOweChargesList,
                                              hasLpiWithDunningLock: Boolean,
                                              currentTaxYear: TaxYear,
                                              backUrl: String,
                                              dunningLock: Boolean,
                                              reviewAndReconcileEnabled: Boolean,
                                              creditAndRefundEnabled: Boolean,
                                              claimToAdjustViewModel: WYOClaimToAdjustViewModel) {


  def sortedOverdueOrAccruingInterestChargeList: List[ChargeItem] = whatYouOweChargesList.overdueOrAccruingInterestChargeList.sortWith((charge1, charge2) =>
    getDisplayDueDate(charge2).isAfter(getDisplayDueDate(charge1))
  )

}

object YourSelfAssessmentChargesViewModel {
  def getDisplayDueDate(chargeItem: ChargeItem): LocalDate = if (chargeItem.isLatePaymentInterest && chargeItem.isPaid) {
    chargeItem.getInterestEndDate
  } else {
    chargeItem.getDueDate
  }
}
