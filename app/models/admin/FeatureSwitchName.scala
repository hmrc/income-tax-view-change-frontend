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

package models.admin

import play.api.Logger
import play.api.libs.json._
import play.api.mvc.PathBindable

import scala.collection.immutable

case class FeatureSwitch(name: FeatureSwitchName, isEnabled: Boolean)

object FeatureSwitchMongoFormats {
  implicit val formats: Format[FeatureSwitch] =
    Json.format[FeatureSwitch]
}

object FeatureSwitch {
  implicit val format: OFormat[FeatureSwitch] = Json.format[FeatureSwitch]
}

sealed trait FeatureSwitchName {
  val name: String
}

object FeatureSwitchName {

  implicit val writes: Writes[FeatureSwitchName] = (o: FeatureSwitchName) => JsString(o.name)

  implicit val reads: Reads[FeatureSwitchName] = {
    case JsString(ITSASubmissionIntegration.name) =>
      JsSuccess(ITSASubmissionIntegration)
    case JsString(ChargeHistory.name) =>
      JsSuccess(ChargeHistory)
    case JsString(NavBarFs.name) =>
      JsSuccess(NavBarFs)
    case JsString(CreditsRefundsRepay.name) =>
      JsSuccess(CreditsRefundsRepay)
    case JsString(PaymentHistoryRefunds.name) =>
      JsSuccess(PaymentHistoryRefunds)
    case JsString(OptOutFs.name) =>
      JsSuccess(OptOutFs)
    case JsString(FilterCodedOutPoas.name) =>
      JsSuccess(FilterCodedOutPoas)
    case JsString(ReportingFrequencyPage.name) =>
      JsSuccess(ReportingFrequencyPage)
    case JsString(DisplayBusinessStartDate.name) =>
      JsSuccess(DisplayBusinessStartDate)
    case JsString(AccountingMethodJourney.name) =>
      JsSuccess(AccountingMethodJourney)
    case JsString(PenaltiesAndAppeals.name) =>
      JsSuccess(PenaltiesAndAppeals)
    case JsString(PenaltiesBackendEnabled.name) =>
      JsSuccess(PenaltiesBackendEnabled)
    case JsString(YourSelfAssessmentCharges.name) =>
      JsSuccess(YourSelfAssessmentCharges)
    case JsString(OptInOptOutContentUpdateR17.name) =>
      JsSuccess(OptInOptOutContentUpdateR17)
    case JsString(SelfServeTimeToPayR17.name) =>
      JsSuccess(SelfServeTimeToPayR17)
    case invalidName =>
      Logger("application").error(s"Invalid feature switch Json found: $invalidName")
      JsSuccess(InvalidFS)
  }


  implicit val formats: Format[FeatureSwitchName] =
    Format(reads, writes)

  implicit def pathBindable: PathBindable[FeatureSwitchName] = new PathBindable[FeatureSwitchName] {

    override def bind(key: String, value: String): Either[String, FeatureSwitchName] =
      JsString(value).validate[FeatureSwitchName] match {
        case JsSuccess(name, _) =>
          Right(name)
        case _ =>
          Left(s"The feature switch `$value` does not exist")
      }

    override def unbind(key: String, value: FeatureSwitchName): String =
      value.name
  }

  val allFeatureSwitches: immutable.Set[FeatureSwitchName] =
    Set(
      ITSASubmissionIntegration,
      ChargeHistory,
      NavBarFs,
      CreditsRefundsRepay,
      PaymentHistoryRefunds,
      OptOutFs,
      FilterCodedOutPoas,
      ReportingFrequencyPage,
      DisplayBusinessStartDate,
      AccountingMethodJourney,
      PenaltiesAndAppeals,
      PenaltiesBackendEnabled,
      YourSelfAssessmentCharges,
      OptInOptOutContentUpdateR17,
      SelfServeTimeToPayR17
    )

  def get(str: String): Option[FeatureSwitchName] = allFeatureSwitches find (_.name == str)
}

case object ITSASubmissionIntegration extends FeatureSwitchName {
  override val name: String = "itsa-submission-integration"

  override def toString: String = "ITSA Submission Integration"
}

case object ChargeHistory extends FeatureSwitchName {
  override val name: String = "charge-history"

  override def toString: String = "Charge History"
}

case object NavBarFs extends FeatureSwitchName {
  override val name = "nav-bar"

  override def toString: String = "Nav Bar"
}

case object CreditsRefundsRepay extends FeatureSwitchName {
  override val name = "credits-refunds-repay"

  override def toString: String = "Credits/Refunds Repayment"
}

case object PaymentHistoryRefunds extends FeatureSwitchName {
  override val name = "payment-history-refunds"

  override def toString: String = "Payment History Refunds"
}

case object OptOutFs extends FeatureSwitchName {
  override val name = "opt-out"
  override val toString = "Opt Out"
}

case object FilterCodedOutPoas extends FeatureSwitchName {
  override val name: String = s"filter-coded-out-poas"
  override val toString: String = "Filter Coded Out Poas"
}

case object InvalidFS extends FeatureSwitchName {
  override val name: String = "invalid-feature-switch"
  override val toString: String = "Invalid feature Switch"
}

case object ReportingFrequencyPage extends FeatureSwitchName {
  override val name: String = "reporting-frequency-page"
  override val toString: String = "Reporting Frequency page"
}

case object DisplayBusinessStartDate extends FeatureSwitchName {
  override val name: String = "display-business-start-date"
  override val toString: String = "Display Business Start Date"
}

case object AccountingMethodJourney extends FeatureSwitchName {
  override val name: String = "accounting-method-journey"
  override val toString: String = "Accounting Method Journey"
}

case object PenaltiesAndAppeals extends FeatureSwitchName {
  override val name: String = "penalties-and-appeals"
  override val toString: String = "Penalties and Appeals"
}

case object PenaltiesBackendEnabled extends FeatureSwitchName {
  override val name: String = "penalties-backend"
  override val toString: String = "Penalties Backend"
}

case object YourSelfAssessmentCharges extends FeatureSwitchName {
  override val name: String = "your-self-assessment-charges"
  override val toString: String = "Your Self Assessment Charges page"
}

case object OptInOptOutContentUpdateR17 extends FeatureSwitchName {
  override val name = "opt-in-opt-out-content-update-r17"
  override val toString = "Opt In Opt Out Content Update R17"
}

case object SelfServeTimeToPayR17 extends FeatureSwitchName {
  override val name: String = "self-serve-time-to-pay-r17"
  override val toString: String = "Self Serve Time To Pay R17"
}
