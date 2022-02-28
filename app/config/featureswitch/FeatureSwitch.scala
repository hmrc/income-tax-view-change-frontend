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
    PaymentHistory,
    IvUplift,
    ChargeHistory,
    TxmEventsApproved,
    PaymentAllocation,
    CodingOut,
    WhatYouOweTotals,
    BtaNavBar,
    Class4UpliftEnabled
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

case object PaymentAllocation extends FeatureSwitch {
  override val name: String = s"$prefix.enable-payment-allocation"
  override val displayText: String = "Payment Allocation"
}

case object TxmEventsApproved extends FeatureSwitch {
  override val name: String = s"$prefix.enable-txm-events-approved"
  override val displayText: String = "Txm Events Approved"
}

case object CodingOut extends FeatureSwitch {
  override val name: String = s"$prefix.enable-coding-out"
  override val displayText: String = "Coding Out"
}

case object WhatYouOweTotals extends FeatureSwitch {
  override val name: String = s"$prefix.enable-whatyouowe-totals"
  override val displayText: String = "Display WhatYouOwe Totals"
}

case object BtaNavBar extends FeatureSwitch {
  override val name = s"$prefix.enable-bta-nav-bar"
  override val displayText = "BTA Nav Bar"
}

case object Class4UpliftEnabled extends FeatureSwitch {
  override val name = s"$prefix.class4-uplift-enabled"
  override val displayText = "Class4 Uplift Enabled"
}
