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

package testConstants

import cats.implicits.catsSyntaxOptionId
import models.nrs.{NrsMetadata, NrsSubmission, NrsSuccessResponse, RawPayload, SearchKeys}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Headers
import play.api.test.FakeRequest
import testConstants.ChecksumUtils._
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, User}
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, ItmpAddress, ItmpName, LoginTimes, MdtpInformation, Name}

import java.time.{Instant, LocalDate}

object NrsUtils {

  val nrsMetadataBody: Array[Byte] = "payload".getBytes("UTF-8")
  val nrsMetadataRawPayload: RawPayload = RawPayload(nrsMetadataBody)

  val successResponseJson: String = Json.toJson(
    NrsSuccessResponse("submissionId")
  ).toString()

  val nrsMetadata: NrsMetadata = {
    val request =
      FakeRequest()
        .withHeaders(
          Headers(
            "Gov-Client-Public-IP" -> "198.51.100.0",
            "Gov-Client-Public-Port" -> "12345",
            "Authorization" -> "Bearer AbCdEf123456"
          ))
        .withBody(nrsMetadataBody)

    NrsMetadata(
      request,
      Instant.parse("2018-04-07T12:13:25.000Z"),
      SearchKeys("credId".some, "saUtr".some, "nino".some),
      request.body.calculateSha256)
  }

  val nrsSubmission: NrsSubmission = NrsSubmission(nrsMetadataRawPayload, nrsMetadata)

  val nrsMetadataJson: JsValue =
    Json.parse(
      s"""
         |{
         |    "businessId": "itsavc",
         |    "notableEvent": "your-event-name-here",
         |    "payloadContentType": "application/xml",
         |    "payloadSha256Checksum":"${nrsMetadataBody.calculateSha256}",
         |    "userSubmissionTimestamp": "2018-04-07T12:13:25.000Z",
         |    "userAuthToken": "Bearer AbCdEf123456",
         |    "headerData": {
         |      "Authorization": "Bearer AbCdEf123456",
         |      "Gov-Client-Public-IP": "198.51.100.0",
         |      "Gov-Client-Public-Port": "12345"
         |    },
         |    "searchKeys": {
         |      "submissionId": "3216783621-123873821-12332"
         |    }
         |}""".stripMargin)

}
