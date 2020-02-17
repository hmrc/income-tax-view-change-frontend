/*
 * Copyright 2020 HM Revenue & Customs
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

  val switches: Set[FeatureSwitch]  = Set(
    Payment,
    Statements,
    Estimates,
    Bills,
    ReportDeadlines,
    AccountDetails,
    CalcBreakdown,
    CalcDataApi,
    ObligationsPage
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

case object Statements extends FeatureSwitch {
  override val name = s"$prefix.enable-statements"
  override val displayText = "Enable Statements"
}

case object Estimates extends FeatureSwitch {
  override val name = s"$prefix.enable-estimates"
  override val displayText = "Enable Estimates"
}

case object Bills extends FeatureSwitch {
  override val name = s"$prefix.enable-bills"
  override val displayText = "Enable Bills Feature"
}

case object ReportDeadlines extends FeatureSwitch {
  override val name = s"$prefix.enable-report-deadlines"
  override val displayText = "Enable Report Deadlines Feature"
}

case object AccountDetails extends FeatureSwitch {
  override val name = s"$prefix.enable-account-details"
  override val displayText = "Enable Account Details & Business Details Pages"
}

case object CalcBreakdown extends FeatureSwitch {
  override val name = s"$prefix.enable-calc-breakdown"
  override val displayText = "Enable Calculation Breakdown"
}

case object CalcDataApi extends FeatureSwitch {
  override val name = s"$prefix.enable-calc-dataApi"
  override val displayText = "Enable Calculation Data API"
}

case object ObligationsPage extends FeatureSwitch {
  override val name = s"$prefix.enable-obligations-page"
  override val displayText = "Enable Obligations Page"
}
