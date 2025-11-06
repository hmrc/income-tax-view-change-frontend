/*
 * Copyright 2023 HM Revenue & Customs
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

package models.itsaStatus

import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, NoStatus, Voluntary}
import models.itsaStatus.StatusReason.StatusReason
import play.api.libs.json.{Format, Json, Reads, Writes}

case class StatusDetail(submittedOn: String,
                        status: ITSAStatus,
                        statusReason: StatusReason,
                        businessIncomePriorTo2Years: Option[BigDecimal] = None) {

  def isVoluntary: Boolean = status == Voluntary

  def isMandated: Boolean = status == Mandated

  def isAnnual: Boolean = status == Annual

  def isUnknown: Boolean = status == NoStatus

  def isMandatedOrVoluntary: Boolean = isMandated || isVoluntary

  def statusReasonRollover: Boolean = statusReason == StatusReason.Rollover

}

object StatusDetail {
  implicit val format: Format[StatusDetail] = Json.format
}

object ITSAStatus extends Enumeration {

  type ITSAStatus = Value

  def fromString(string: String): ITSAStatus = {
    string match {
      case "UnknownStatus" => UnknownStatus
      case "No Status" => NoStatus
      case "MTD Mandated" => Mandated
      case "MTD Voluntary" => Voluntary
      case "Annual" => Annual
      case "Digitally Exempt" => DigitallyExempt
      case "Dormant" => Dormant
      case "MTD Exempt" => Exempt
    }
  }

  val UnknownStatus = Value("UnknownStatus")
  val NoStatus = Value("No Status")
  val Mandated = Value("MTD Mandated")
  val Voluntary = Value("MTD Voluntary")
  val Annual = Value("Annual")
  val DigitallyExempt = Value("Digitally Exempt")
  val Dormant = Value("Dormant")
  val Exempt = Value("MTD Exempt")

  def isExemptionStatus(status: ITSAStatus): Boolean =
    Set(Exempt, DigitallyExempt).contains(status)

  def isNonExemptStatus(status: ITSAStatus): Boolean =
    Set(Mandated, Voluntary, Annual, Dormant).contains(status)

  implicit val itsaStatusReads: Reads[ITSAStatus] = Reads.enumNameReads(ITSAStatus)
  implicit val itsaStatusWrite: Writes[ITSAStatus] = Writes.enumNameWrites
}

object StatusReason extends Enumeration {
  type StatusReason = Value
  val SignupReturnAvailable = Value("Sign up - return available")
  val SignupNoReturnAvailable = Value("Sign up - no return available")
  val ItsaFinalDeclaration = Value("ITSA final declaration")
  val ItsaQ4lDeclaration = Value("ITSA Q4 declaration")
  val CesaSaReturn = Value("CESA SA return")
  val Complex = Value("Complex")
  val CeasedIncomeSource = Value("Ceased income source")
  val ReinstatedIncomeSource = Value("Reinstated income source")
  val IncomeSourceLatencyChanges = Value("Income Source Latency Changes")
  val Rollover = Value("Rollover")
  val MtdItsaOptOut = Value("MTD ITSA Opt-Out")
  val MtdItsaOptIn = Value("MTD ITSA Opt-In")
  val DigitallyExempt = Value("Digitally Exempt")

  implicit val statusReasonReads: Reads[StatusReason] = Reads.enumNameReads(StatusReason)
  implicit val statusReasonWrite: Writes[StatusReason] = Writes.enumNameWrites
}