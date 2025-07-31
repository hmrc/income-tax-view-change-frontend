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

package models.nrs

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole, Enrolments}

import java.time.{Instant, LocalDate}

case class IdentityData(
  internalId:         Option[String]          = None,
  externalId:         Option[String]          = None,
  agentCode:          Option[String]          = None,
  credentials:        Option[Credentials]     = None,
  confidenceLevel:    ConfidenceLevel,
  nino:               Option[String]          = None,
  saUtr:              Option[String]          = None,
  name:               Option[Name]            = None,
  dateOfBirth:        Option[LocalDate]       = None,
  email:              Option[String]          = None,
  agentInformation:   AgentInformation,
  groupIdentifier:    Option[String]          = None,
  credentialRole:     Option[CredentialRole]  = None,
  mdtpInformation:    Option[MdtpInformation] = None,
  itmpName:           Option[ItmpName]        = None,
  itmpDateOfBirth:    Option[LocalDate]       = None,
  itmpAddress:        Option[ItmpAddress]     = None,
  affinityGroup:      Option[AffinityGroup]   = None,
  credentialStrength: Option[String]          = None,
  enrolments:         Enrolments              = Enrolments(Set()),
  loginTimes:         LoginTimes
)

object IdentityData {
  implicit val localDateTimeWrites: Writes[LocalDate]        = Writes.DefaultLocalDateWrites
  implicit val instantWrites:       Writes[Instant]          = InstantFormatter.instantWrites
  implicit val credWrites:          Writes[Credentials]      = Json.writes[Credentials]
  implicit val nameWrites:          Writes[Name]             = Json.writes[Name]
  implicit val agentInfoWrites:     Writes[AgentInformation] = Json.writes[AgentInformation]
  implicit val mdtpInfoWrites:      Writes[MdtpInformation]  = Json.writes[MdtpInformation]
  implicit val itmpNameWrites:      Writes[ItmpName]         = Json.writes[ItmpName]
  implicit val itmpAddressWrites:   Writes[ItmpAddress]      = Json.writes[ItmpAddress]
  implicit val loginTimesWrites:    Writes[LoginTimes]       = Json.writes[LoginTimes]
  implicit val enrolmentWrites:     Writes[Enrolments]       = Json.writes[Enrolments]
  implicit val writes:              Writes[IdentityData]     = Json.writes[IdentityData]
}
