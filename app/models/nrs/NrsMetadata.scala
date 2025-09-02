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
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, LoginTimes}

import java.time.Instant

case class NrsMetadata(
  businessId:               String,
  notableEvent:             String,
  payloadContentType:       String,
  payloadSha256Checksum:    String,
  userSubmissionTimestamp:  Instant,
  identityData:             IdentityData,
  userAuthToken:            String,
  headerData:               JsValue,
  searchKeys:               SearchKeys
)

object NrsMetadata extends InstantFormatter {
  implicit val writes: Writes[NrsMetadata] = Json.writes[NrsMetadata]

  def apply(
      request:                 Request[_],
      userSubmissionTimestamp: Instant,
      searchKeys:              SearchKeys,
      checkSum:                String
    ): NrsMetadata =
    NrsMetadata(
      businessId              = "income-tax-view-change",
      notableEvent            = "adjust-payment-on-account",
      payloadContentType      = MimeTypes.XML,
      payloadSha256Checksum   = checkSum,
      userSubmissionTimestamp = userSubmissionTimestamp,
      userAuthToken           = request.headers.get("Authorization").getOrElse(""),
      headerData              = JsObject(request.headers.toMap.map(x => x._1 -> JsString(x._2 mkString ","))),
      searchKeys              = searchKeys,
      identityData            = IdentityData(
        confidenceLevel   = ConfidenceLevel.L250,
        agentInformation  = AgentInformation(None, None, None),
        enrolments        = Enrolments(Set()),
        loginTimes        = LoginTimes(userSubmissionTimestamp, None)
      )
    )
}
