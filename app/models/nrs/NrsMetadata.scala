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

import play.api.http.MimeTypes
import play.api.libs.json._
import play.api.mvc.{Request, RequestHeader}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolments, MissingBearerToken}
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, LoginTimes}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.time.Instant

case class NrsMetadata(
  businessId:               String,
  notableEvent:             String,
  payloadContentType:       String,
  payloadSha256Checksum:    String,
  userSubmissionTimestamp:  Instant,
  identityData:             IdentityData,
  userAuthToken:            String,
  headerData:               Map[String, String],
  searchKeys:               SearchKeys
)

object NrsMetadata extends InstantFormatter {
  implicit val writes: Writes[NrsMetadata] = Json.writes[NrsMetadata]

  // TODO: Could be required to populate this in the future - but not required as part of MISUV-10030
  private val emptyIdentityData = IdentityData(
    confidenceLevel = ConfidenceLevel.L250,
    agentInformation = AgentInformation(None, None, None),
    enrolments = Enrolments(Set()),
    loginTimes = LoginTimes(Instant.now(), None)
  )

  def apply(
      request:                 Request[_],
      userSubmissionTimestamp: Instant,
      identityData:            IdentityData = emptyIdentityData,
      searchKeys:              SearchKeys,
      checkSum:                String
    ): NrsMetadata = {
    val hc = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    NrsMetadata(
      businessId              = "income-tax-view-change",
      notableEvent            = "income-tax-view-change-adjust-payment-on-account",
      payloadContentType      = MimeTypes.XML,
      payloadSha256Checksum   = checkSum,
      userSubmissionTimestamp = userSubmissionTimestamp,
      userAuthToken           = request.session.get(SessionKeys.authToken).orElse(request.headers.get("Authorization")).getOrElse(throw MissingBearerToken("missing authorisation token")),
      headerData              = request.headers.toMap.map(x => x._1 -> (x._2 mkString ",")),
      searchKeys              = searchKeys,
      identityData            = identityData
    )
  }

}
