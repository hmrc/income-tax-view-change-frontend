/*
 * Copyright 2021 HM Revenue & Customs
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
    Payment,
    IncomeBreakdown,
    DeductionBreakdown,
    TaxDue,
    ITSASubmissionIntegration,
    NewFinancialDetailsApi,
    AgentViewer,
    NextUpdates,
    TaxYearOverviewUpdate,
    PaymentHistory,
		IvUplift,
		ChargeHistory
  )

  def apply(str: String): FeatureSwitch =
    switches find (_.name == str) match {
      case Some(switch) => switch
      case None => throw new IllegalArgumentException("Invalid feature switch: " + str)
    }

  def get(str: String): Option[FeatureSwitch] = switches find (_.name == str)

}

case object Payment extends FeatureSwitch {
  override val name = s"$prefix.enable-payment"
  override val displayText = "Enable Payment functionality"
}

case object IncomeBreakdown extends FeatureSwitch {
  override val name: String = s"$prefix.enable-calculation-income-breakdown"
  override val displayText: String = "Enable Income Calc Breakdown"
}

case object DeductionBreakdown extends FeatureSwitch {
  override val name: String = s"$prefix.enable-calculation-deduction-breakdown"
  override val displayText: String = "Enable Deduction Calc Breakdown"
}

case object TaxDue extends FeatureSwitch {
  override val name: String = s"$prefix.enable-tax-due"
  override val displayText: String = "Enable Tax Due Feature"
}

case object ITSASubmissionIntegration extends FeatureSwitch {
  override val name = s"$prefix.enable-itsa-submission-integration"
  override val displayText = "ITSA Submission Integration"
}

case object NewFinancialDetailsApi extends FeatureSwitch {
  override val name = s"$prefix.enable-new-financial-details-api"
  override val displayText = "New Financial Details Api"
}

case object AgentViewer extends FeatureSwitch {
  override val name = s"$prefix.enable-agent-viewer"
  override val displayText = "Enable Agent Viewer"
}

case object NextUpdates extends FeatureSwitch {
  override val name = s"$prefix.enable-next-updates"
  override val displayText = "Enable Next Updates Feature"
}

case object TaxYearOverviewUpdate extends FeatureSwitch {
  override val name = s"$prefix.enable-tax-year-overview-update"
  override val displayText = "Tax Year Overview Update"
}

case object PaymentHistory extends FeatureSwitch {
	override val name = s"$prefix.enable-payment-history-page"
	override val displayText = "Payment History"
}

case object IvUplift extends FeatureSwitch {
	override val name = s"$prefix.enable-iv-uplift"
	override val displayText = "IV Uplift"
}

case object ChargeHistory extends FeatureSwitch {
	override val name: String = s"$prefix.enable-charge-history"
	override val displayText: String = "Charge History"
}
