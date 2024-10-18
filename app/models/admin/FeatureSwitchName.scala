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

import play.api.libs.json._
import play.api.mvc.PathBindable

import scala.collection.immutable

case class FeatureSwitch(name: FeatureSwitchName, isEnabled: Boolean)

object FeatureSwitch {
  implicit val format: OFormat[FeatureSwitch] = Json.format[FeatureSwitch]
}

sealed trait FeatureSwitchName {
  val name: String
}

object FeatureSwitchName {
  implicit val writes: Writes[FeatureSwitchName] = (o: FeatureSwitchName) => JsString(o.name)

  implicit val reads: Reads[FeatureSwitchName] = {
    case name if name == JsString(ITSASubmissionIntegration.name) =>
      JsSuccess(ITSASubmissionIntegration)
    case name if name == JsString(ChargeHistory.name) =>
      JsSuccess(ChargeHistory)
    case name if name == JsString(NavBarFs.name) =>
      JsSuccess(NavBarFs)
    case name if name == JsString(CreditsRefundsRepay.name) =>
      JsSuccess(CreditsRefundsRepay)
    case name if name == JsString(PaymentHistoryRefunds.name) =>
      JsSuccess(PaymentHistoryRefunds)
    case name if name == JsString(CalendarQuarterTypes.name) =>
      JsSuccess(CalendarQuarterTypes)
    case name if name == JsString(IncomeSourcesNewJourney.name) =>
      JsSuccess(IncomeSourcesNewJourney)
    case name if name == JsString(IncomeSources.name) =>
      JsSuccess(IncomeSources)
    case name if name == JsString(OptOut.name) =>
      JsSuccess(OptOut)
    case name if name == JsString(AdjustPaymentsOnAccount.name) =>
      JsSuccess(AdjustPaymentsOnAccount)
    case name if name == JsString(ReviewAndReconcilePoa.name) =>
      JsSuccess(ReviewAndReconcilePoa)
    case _ => JsError("Invalid feature switch name")
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
    Set(ITSASubmissionIntegration, ChargeHistory, NavBarFs, CreditsRefundsRepay,
      PaymentHistoryRefunds, CalendarQuarterTypes, IncomeSourcesNewJourney, IncomeSources, OptOut, AdjustPaymentsOnAccount, ReviewAndReconcilePoa)

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

case object CalendarQuarterTypes extends FeatureSwitchName {
  override val name = "calendar-quarter-types"
  override val toString = "Calendar Quarter Types"
}

case object IncomeSourcesNewJourney extends FeatureSwitchName {
  override val name = "income-sources-new-journey"
  override val toString = "Income Sources New Journey"
}

case object IncomeSources extends FeatureSwitchName {
  override val name = "income-sources"
  override val toString = "Income Sources"
}

case object OptOut extends FeatureSwitchName {
  override val name = s"opt-out"
  override val toString = "Opt Out"
}

case object AdjustPaymentsOnAccount extends FeatureSwitchName {
  override val name: String = s"adjust-payments-on-account"
  override val toString: String = "Adjust Payments On Account"
}

case object ReviewAndReconcilePoa extends FeatureSwitchName {
  override val name: String = s"review-and-reconcile-poa"
  override val toString: String = "Review And Reconcile POA"
}

object FeatureSwitchMongoFormats {
  implicit val formats: Format[FeatureSwitch] =
    Json.format[FeatureSwitch]
}

