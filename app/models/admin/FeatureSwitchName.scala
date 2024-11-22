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
    case JsString(IncomeSourcesNewJourney.name) =>
      JsSuccess(IncomeSourcesNewJourney)
    case JsString(IncomeSourcesFs.name) =>
      JsSuccess(IncomeSourcesFs)
    case JsString(OptOut.name) =>
      JsSuccess(OptOut)
    case JsString(AdjustPaymentsOnAccount.name) =>
      JsSuccess(AdjustPaymentsOnAccount)
    case JsString(ReviewAndReconcilePoa.name) =>
      JsSuccess(ReviewAndReconcilePoa)
    case JsString(FilterCodedOutPoas.name) =>
      JsSuccess(FilterCodedOutPoas)
    case JsString(ReportingFrequencyPage.name) =>
      JsSuccess(ReportingFrequencyPage)
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
      IncomeSourcesNewJourney,
      IncomeSourcesFs,
      OptOut,
      AdjustPaymentsOnAccount,
      ReviewAndReconcilePoa,
      FilterCodedOutPoas,
      ReportingFrequencyPage
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

case object IncomeSourcesNewJourney extends FeatureSwitchName {
  override val name = "income-sources-new-journey"
  override val toString = "Income Sources New Journey"
}

case object IncomeSourcesFs extends FeatureSwitchName {
  override val name = "income-sources"
  override val toString = "Income Sources"
}

case object OptOut extends FeatureSwitchName {
  override val name = "opt-out"
  override val toString = "Opt Out"
}

case object AdjustPaymentsOnAccount extends FeatureSwitchName {
  override val name: String = "adjust-payments-on-account"
  override val toString: String = "Adjust Payments On Account"
}

case object ReviewAndReconcilePoa extends FeatureSwitchName {
  override val name: String = "review-and-reconcile-poa"
  override val toString: String = "Review And Reconcile POA"
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


