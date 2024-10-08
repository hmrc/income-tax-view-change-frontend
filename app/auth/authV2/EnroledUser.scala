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

package auth.authV2

import controllers.predicates.agent.Constants
import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name}

// During the auth process, an MTD ID may not be present (e.g. an agent without a confirmed client)
// Existing User classes require MTD ID. Instead, defer creation of User object until after
// auth.
case class EnroledUser[A](enrolments: Enrolments,
                          userName: Option[Name],
                          affinityGroup: Option[AffinityGroup],
                          confidenceLevel: ConfidenceLevel,
                          credentials: Option[Credentials])(implicit request: Request[A]) extends WrappedRequest[A](request){

  private def getValueFromEnrolment(enrolment: String, identifier: String): Option[String] =
    enrolments.getEnrolment(enrolment)
      .flatMap(_.getIdentifier(identifier)).map(_.value)

  lazy val credId: Option[String] =
    credentials.map(credential => credential.providerId)

  lazy val mtdId: Option[String] =
    getValueFromEnrolment(Constants.mtdEnrolmentName, Constants.mtdEnrolmentIdentifierKey)

  lazy val nino: Option[String] =
    getValueFromEnrolment(Constants.ninoEnrolmentName, Constants.ninoEnrolmentIdentifierKey)

  lazy val saId: Option[String] =
    getValueFromEnrolment(Constants.saEnrolmentName, Constants.saEnrolmentIdentifierKey)

  lazy val arn: Option[String] =
    getValueFromEnrolment(Constants.agentServiceEnrolmentName, Constants.agentServiceIdentifierKey)

}
