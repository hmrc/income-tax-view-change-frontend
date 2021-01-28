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
    Bills,
    ReportDeadlines,
    ObligationsPage,
    IncomeBreakdown,
    DeductionBreakdown,
    TaxDue,
    API5,
    ITSASubmissionIntegration,
    NewFinancialDetailsApi,
    AgentViewer
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

case object Bills extends FeatureSwitch {
  override val name = s"$prefix.enable-bills"
  override val displayText = "Enable Bills Feature"
}

case object ReportDeadlines extends FeatureSwitch {
  override val name = s"$prefix.enable-report-deadlines"
  override val displayText = "Enable Report Deadlines Feature"
}

case object ObligationsPage extends FeatureSwitch {
  override val name = s"$prefix.enable-obligations-page"
  override val displayText = "Enable Obligations Page"
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

case object API5 extends FeatureSwitch {
  override val name = s"$prefix.enable-API5"
  override val displayText = "Enable API5 Calls"
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
