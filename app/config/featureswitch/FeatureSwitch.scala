/*
 * Copyright 2022 HM Revenue & Customs
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

package config.featureswitch

import config.featureswitch.FeatureSwitch.prefix

sealed trait FeatureSwitch {
  val name: String
  val displayText: String
}

object FeatureSwitch {
  val prefix = "feature-switch"

  val switches: Set[FeatureSwitch] = Set(
    ITSASubmissionIntegration,
    IvUplift,
    ChargeHistory,
    PaymentAllocation,
    CodingOut,
    NavBarFs,
    Class4UpliftEnabled,
    ForecastCalculation,
    CutOverCredits,
    CreditsRefundsRepay,
    WhatYouOweCreditAmount,
    R7bTxmEvents,
    NewStateBenefitIncome,
    MFACreditsAndDebits,
    PaymentHistoryRefunds
  )

  def apply(str: String): FeatureSwitch =
    switches find (_.name == str) match {
      case Some(switch) => switch
      case None => throw new IllegalArgumentException("Invalid feature switch: " + str)
    }

  def get(str: String): Option[FeatureSwitch] = switches find (_.name == str)

}

case object ITSASubmissionIntegration extends FeatureSwitch {
  override val name = s"$prefix.enable-itsa-submission-integration"
  override val displayText = "ITSA Submission Integration"
}

case object IvUplift extends FeatureSwitch {
  override val name = s"$prefix.enable-iv-uplift"
  override val displayText = "IV Uplift"
}

case object ChargeHistory extends FeatureSwitch {
  override val name: String = s"$prefix.enable-charge-history"
  override val displayText: String = "Charge History"
}

case object PaymentAllocation extends FeatureSwitch {
  override val name: String = s"$prefix.enable-payment-allocation"
  override val displayText: String = "Payment Allocation"
}

case object CodingOut extends FeatureSwitch {
  override val name: String = s"$prefix.enable-coding-out"
  override val displayText: String = "Coding Out"
}

case object NavBarFs extends FeatureSwitch {
  override val name = s"$prefix.enable-nav-bar"
  override val displayText = "Nav Bar"
}

case object Class4UpliftEnabled extends FeatureSwitch {
  override val name = s"$prefix.class4-uplift-enabled"
  override val displayText = "Class4 Uplift Enabled"
}

case object ForecastCalculation extends FeatureSwitch {
  override val name = s"$prefix.enable-forecast-calculation"
  override val displayText = "Forecast Calculation"
}

case object CutOverCredits extends FeatureSwitch {
  override val name = s"$prefix.enable-cut-over-credit"
  override val displayText = "Cut-Over Credit (CESA to ETMP)"
}

case object CreditsRefundsRepay extends FeatureSwitch {
  override val name = s"$prefix.enable-credits-refunds-repay"
  override val displayText = "Credits/Refunds Repayment"
}

case object WhatYouOweCreditAmount extends FeatureSwitch {
  override val name = s"$prefix.enable-what-you-owe-credit-amount"
  override val displayText = "What You Owe Credit Amount"
}

case object R7bTxmEvents extends FeatureSwitch {
  override val name = s"$prefix.enable-R7b-Txm-Events"
  override val displayText = "R7b Viewer TXM Events"
}

case object NewStateBenefitIncome extends FeatureSwitch {
  override val name = s"$prefix.enable-new-state-benefit-income"
  override val displayText = "New State Benefit Income"
}

case object MFACreditsAndDebits extends FeatureSwitch {
  override val name = s"$prefix.enable-mfa-credits-and-debits"
  override val displayText = "MFA Credits and Debits"
}

case object PaymentHistoryRefunds extends FeatureSwitch {
  override val name = s"$prefix.enable-payment-history-refunds"
  override val displayText = "Payment History Refunds"
}


