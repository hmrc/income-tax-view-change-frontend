/*
 * Copyright 2024 HM Revenue & Customs
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

package models.homePage

import models.financialDetails.{FinancialDetailsModel, FinancialDetailsResponseModel}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.obligations.NextUpdatesTileViewModel

import java.time.LocalDate

case class HomePageViewModel(utr: Option[String],
                             nextPaymentsTileViewModel: NextPaymentsTileViewModel,
                             returnsTileViewModel: ReturnsTileViewModel,
                             nextUpdatesTileViewModel: NextUpdatesTileViewModel,
                             paymentCreditAndRefundHistoryTileViewModel: PaymentCreditAndRefundHistoryTileViewModel,
                             yourBusinessesTileViewModel: YourBusinessesTileViewModel,
                             yourReportingObligationsTileViewModel: YourReportingObligationsTileViewModel,
                             penaltiesAndAppealsTileViewModel: PenaltiesAndAppealsTileViewModel,
                             dunningLockExists: Boolean = false,
                             origin: Option[String] = None)

case class NextPaymentsTileViewModel(nextPaymentDueDate: Option[LocalDate], overDuePaymentsCount: Int,
                                     paymentsAccruingInterestCount: Int, reviewAndReconcileEnabled: Boolean,
                                     yourSelfAssessmentChargesEnabled: Boolean) {

  def verify: Either[Throwable, NextPaymentsTileViewModel] = {
    if (!(overDuePaymentsCount == 0) && nextPaymentDueDate.isEmpty) {
      Left(new Exception("Error, overDuePaymentsCount was non-0 while nextPaymentDueDate was empty"))
    } else {
      Right(NextPaymentsTileViewModel(nextPaymentDueDate, overDuePaymentsCount, paymentsAccruingInterestCount,
        reviewAndReconcileEnabled, yourSelfAssessmentChargesEnabled))
    }
  }

}

object NextPaymentsTileViewModel {

  def paymentsAccruingInterestCount(unpaidCharges: List[FinancialDetailsResponseModel], currentDate: LocalDate): Int = {
    val financialDetailsModels = unpaidCharges collect {
      case fdm: FinancialDetailsModel => fdm
    }
    financialDetailsModels
      .foldLeft(0)((acc, c) => c.docDetailsNotDueWithInterest(currentDate) + acc)
  }
}

case class ReturnsTileViewModel(currentTaxYear: TaxYear, iTSASubmissionIntegrationEnabled: Boolean)

case class YourBusinessesTileViewModel(displayCeaseAnIncome: Boolean)

case class YourReportingObligationsTileViewModel(currentTaxYear: TaxYear, reportingObligationsEnabled: Boolean, currentYearITSAStatus: ITSAStatus)

case class PenaltiesAndAppealsTileViewModel(penaltiesAndAppealsIsEnabled: Boolean, submissionFrequency: String, penaltyPoints: Int) {

  private val annualPenaltyThreshold = 2

  private val quarterlyPenaltyThreshold = 4

  val penaltiesTagMessageKey: Option[String] = (submissionFrequency, penaltyPoints) match {
    case ("Annual", points) if points >= annualPenaltyThreshold => Some("home.penaltiesAndAppeals.twoPenaltiesTag")
    case ("Quarterly", points) if points >= quarterlyPenaltyThreshold => Some("home.penaltiesAndAppeals.fourPenaltiesTag")
    case _ => None
  }
}
