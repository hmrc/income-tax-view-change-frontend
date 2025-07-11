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

import auth.MtdItUser
import models.incomeSourceDetails.TaxYear
import models.nextPayments.viewmodels.WYOClaimToAdjustViewModel
import models.taxYearAmount.EarliestDueCharge
import services.DateServiceInterface
import controllers.routes._

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
                                              selfServeTimeToPayStartUrl: String,
                                              claimToAdjustViewModel: WYOClaimToAdjustViewModel)(implicit val dateService: DateServiceInterface) {
  lazy val currentTaxYear: TaxYear = dateService.getCurrentTaxYear

  def chargesAndCodedOutDetailsAreEmpty: Boolean =
    whatYouOweChargesList.isChargesListEmpty && whatYouOweChargesList.codedOutDetails.isEmpty

  def chargeOrBcdExists: Boolean =
    whatYouOweChargesList.chargesList.nonEmpty || whatYouOweChargesList.bcdChargeTypeDefinedAndGreaterThanZero

  def creditAndRefundUrl(implicit user: MtdItUser[_]): String = {
    if(user.isAgent()) CreditAndRefundController.showAgent()
    else               CreditAndRefundController.show()
  }.url

  def creditAndRefundsControllerUrl(implicit user: MtdItUser[_]): String =
    (user.isAgent() match {
      case true if user.incomeSources.yearOfMigration.isDefined  => CreditAndRefundController.showAgent()
      case true                                                  => NotMigratedUserController.showAgent()
      case false if user.incomeSources.yearOfMigration.isDefined => CreditAndRefundController.show()
      case false                                                 => NotMigratedUserController.show()
    }).url

  def taxYearSummaryUrl(year: Int, origin: Option[String])(implicit user: MtdItUser[_]): String = {
    if(user.isAgent()) TaxYearSummaryController.renderAgentTaxYearSummaryPage(year)
    else               TaxYearSummaryController.renderTaxYearSummaryPage(year, origin)
  }.url

  def adjustPoaUrl(implicit user: MtdItUser[_]): String =
    controllers.claimToAdjustPoa.routes.AmendablePoaController.show(user.isAgent()).url

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
