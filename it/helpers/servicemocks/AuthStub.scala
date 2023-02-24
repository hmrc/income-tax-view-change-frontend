/*
 * Copyright 2017 HM Revenue & Customs
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

package helpers.servicemocks

import testConstants.BaseIntegrationTestConstants._
import helpers.{ComponentSpecBase, WiremockHelper}
import play.api.http.Status
import play.api.libs.json.Json

object AuthStub extends ComponentSpecBase {

  val postAuthoriseUrl = "/auth/authorise"

  val requiredConfidenceLevel = appConfig.requiredConfidenceLevel

  def stubAuthorised(confidenceLevel: Option[Int] = None): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.OK,
      Json.parse(
        s"""{
           | "allEnrolments": [{
           | "key":"$testMtditidEnrolmentKey",
           | "identifiers": [{"key":"$testMtditidEnrolmentIdentifier", "value":"$testMtditid"}]
           | },
           | {
           | "key":"$testNinoEnrolmentKey",
           | "identifiers": [{"key":"$testNinoEnrolmentIdentifier", "value":"$testNino"}]
           | },
           | {
           | "key":"$testSaUtrEnrolmentKey",
           | "identifiers": [{"key":"$testSaUtrEnrolmentIdentifier", "value":"$testSaUtr"}]
           | }
           | ],
           | "userDetailsUri":"$testUserDetailsWiremockUrl",
           | "affinityGroup" : "Individual",
           | "optionalCredentials": {
           |  "providerId": "12345-credId",
           |  "providerType": "GovernmentGateway"
           | },
           | "confidenceLevel": ${confidenceLevel.getOrElse(requiredConfidenceLevel)}
           |}""".stripMargin).toString())
  }

  def stubAuthorisedWithName(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.OK,
      Json.parse(
        s"""{
           | "allEnrolments": [{
           | "key":"$testMtditidEnrolmentKey",
           | "identifiers": [{"key":"$testMtditidEnrolmentIdentifier", "value":"$testMtditid"}]
           | },
           | {
           | "key":"$testNinoEnrolmentKey",
           | "identifiers": [{"key":"$testNinoEnrolmentIdentifier", "value":"$testNino"}]
           | },
           | {
           | "key":"$testSaUtrEnrolmentKey",
           | "identifiers": [{"key":"$testSaUtrEnrolmentIdentifier", "value":"$testSaUtr"}]
           | }
           | ],
           | "userDetailsUri":"$testUserDetailsWiremockUrl",
           | "affinityGroup" : "Individual",
           | "optionalCredentials": {
           |  "providerId": "12345-credId",
           |  "providerType": "GovernmentGateway"
           | },
           | "optionalName": {
           |  "name": "John",
           |  "lastName": "Doe"
           | },
           | "confidenceLevel": $requiredConfidenceLevel
           |}""".stripMargin).toString())
  }

  def stubAuthorisedNoNino(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.OK,
      Json.parse(
        s"""
           |{
           |"allEnrolments": [{
           |  "key":"$testMtditidEnrolmentKey",
           |  "identifiers": [{"key":"$testMtditidEnrolmentIdentifier", "value":"$testMtditid"}]
           |}],
           | "userDetailsUri":"$testUserDetailsWiremockUrl",
           | "affinityGroup" : "Individual"
           |}
       """.stripMargin).toString
    )
  }

  def stubUnauthorised(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.UNAUTHORIZED, "{}")
  }

  def stubInsufficientEnrolments(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.INTERNAL_SERVER_ERROR,
      Json.parse(
        s"""{
           |"allEnrolments":[{}],
           |"userDetailsUri":"$testUserDetailsWiremockUrl"
           |}
         """.stripMargin).toString()
    )
  }

  def stubWrongEnrolment(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.UNAUTHORIZED,
      Json.parse(
        s"""{
           | "allEnrolments": [{
           | "key":"ANOTHER-KEY",
           | "identifiers": [{"key":"ANOTHER-ID", "value":"XA123456789"}]
           | }],
           | "userDetailsUri":"$testUserDetailsWiremockUrl"
           |}""".stripMargin).toString())
  }

  private def defaultResponseBody(mtdId: String): String = Json.stringify(Json.obj(
    "allEnrolments" -> Json.arr(
      Json.obj(
        "key" -> "HMRC-AS-AGENT",
        "identifiers" -> Json.arr(
          Json.obj(
            "key" -> "AgentReferenceNumber",
            "value" -> "1"
          )
        )
      ),
      Json.obj(
        "key" -> "HMRC-MTD-IT",
        "identifiers" -> Json.arr(
          Json.obj(
            "key" -> "MTDITID",
            "value" -> mtdId
          )
        ),
        "delegatedAuthRule" -> "mtd-it-auth"
      )
    ),
    "affinityGroup" -> "Agent",
    "confidenceLevel" -> requiredConfidenceLevel
  ))

  def stubAuthorisedAgent(mtdId: String = "mtdbsaId", overrideResponseBody: Option[String] = None): Unit = {
    overrideResponseBody match {
      case Some(value) => WiremockHelper.stubPost(
        url = postAuthoriseUrl,
        status = Status.OK,
        responseBody = value)
      case None => WiremockHelper.stubPost(
        url = postAuthoriseUrl,
        status = Status.OK,
        responseBody = defaultResponseBody(mtdId))
    }
  }

  def stubAuthorisedAgentNoARN(): Unit = {
    WiremockHelper.stubPost(
      url = postAuthoriseUrl,
      status = Status.OK,
      responseBody = Json.stringify(Json.obj(
        "allEnrolments" -> Json.arr(),
        "affinityGroup" -> "Agent",
        "confidenceLevel" -> requiredConfidenceLevel
      ))
    )
  }

}
