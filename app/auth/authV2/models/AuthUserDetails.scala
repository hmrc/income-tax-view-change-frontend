/*
 * Copyright 2025 HM Revenue & Customs
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

package auth.authV2.models

import auth.authV2.Constants
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, ItmpAddress, ItmpName, LoginTimes, MdtpInformation, Name}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole, Enrolment, Enrolments}

import java.time.LocalDate

case class AuthUserDetails(enrolments: Enrolments,
                           affinityGroup: Option[AffinityGroup],
                           credentials: Option[Credentials],
                           name: Option[Name] = None,
                           internalId: Option[String] = None,
                           externalId: Option[String] = None,
                           agentCode: Option[String] = None,
                           nino: Option[String] = None,
                           dateOfBirth: Option[LocalDate] = None,
                           email: Option[String] = None,
                           agentInformation: Option[AgentInformation] = None,
                           groupIdentifier: Option[String] = None,
                           credentialRole: Option[CredentialRole] = None,
                           mdtpInformation: Option[MdtpInformation] = None,
                           itmpName: Option[ItmpName] = None,
                           itmpDateOfBirth: Option[LocalDate] = None,
                           itmpAddress: Option[ItmpAddress] = None,
                           credentialStrength: Option[String] = None,
                           loginTimes: Option[LoginTimes] = None,
                           confidenceLevel: Option[ConfidenceLevel] = None
                          ){
  lazy val agentReferenceNumber: Option[String] = getEnrolment(Constants.agentServiceEnrolmentName)

  lazy val credId = credentials.map(credential => credential.providerId)

  val saUtr = getValueFromEnrolment(Constants.saEnrolmentName, Constants.saEnrolmentIdentifierKey)
  val optNino = getValueFromEnrolment(Constants.ninoEnrolmentName, Constants.ninoEnrolmentIdentifierKey)
  private def getEnrolment(key: String): Option[String] = {
    enrolments.enrolments.find(e => e.key == key && e.identifiers.nonEmpty) map { (enr: Enrolment) => enr.identifiers.head.value }
  }

  private def getValueFromEnrolment(enrolment: String, identifier: String): Option[String] =
    enrolments.getEnrolment(enrolment)
      .flatMap(_.getIdentifier(identifier)).map(_.value)
}
