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
import play.api.libs.json.{Format, Json, Reads, Writes}

case class StatusDetail(submittedOn: String,
                        status: String,
                        statusReason: String,
                        businessIncomePriorTo2Years: Option[BigDecimal] = None) {

  def isVoluntary: Boolean = status == Voluntary
  def isMandated: Boolean = status == Mandated
  def isAnnual: Boolean = status == Annual
  def isUnknown: Boolean = status == NoStatus

  def isMandatedOrVoluntary: Boolean = isMandated || isVoluntary

}

object StatusDetail {
  implicit val format: Format[StatusDetail] = Json.format
}

object ITSAStatus extends Enumeration {
  type ITSAStatus = Value
  val NoStatus = Value("No Status")
  val Mandated = Value("MTD Mandated")
  val Voluntary = Value("MTD Voluntary")
  val Annual = Value("Annual")
  val NonDigital = Value("Non Digital")
  val Dormant = Value("Dormant")
  val Exempt = Value("MTD Exempt")

  implicit val itsaStatusReads: Reads[ITSAStatus] = Reads.enumNameReads(ITSAStatus)
  implicit val itsaStatusWrite: Writes[ITSAStatus] = Writes.enumNameWrites
}
