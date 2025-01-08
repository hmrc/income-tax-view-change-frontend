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

import helpers.{ComponentSpecBase, WiremockHelper}
import play.api.http.Status
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants._

object AuthStub extends ComponentSpecBase {

  val postAuthoriseUrl = "/auth/authorise"

  val requiredConfidenceLevel = appConfig.requiredConfidenceLevel

  def stubAuthorised(confidenceLevel: Option[Int] = None): Unit = {
    WiremockHelper.stubPost(
      postAuthoriseUrl,
      Status.OK,
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
           | "affinityGroup" : "Individual",
           | "optionalCredentials": {
           |  "providerId": "12345-credId",
           |  "providerType": "GovernmentGateway"
           | },
           | "confidenceLevel": ${confidenceLevel.getOrElse(requiredConfidenceLevel)}
           |}""".stripMargin
      ).toString()
    )
  }

  def stubUnauthorised(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.UNAUTHORIZED, "{}")
  }

  def stubAuthorisedAgent(mtdId: String = "mtdbsaId"): Unit = {
    WiremockHelper.stubPost(
      url = postAuthoriseUrl,
      status = Status.OK,
      responseBody = Json.stringify(Json.obj(
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
            "key" -> s"$testSaUtrEnrolmentKey",
            "identifiers" -> Json.arr(
              Json.obj(
                "key" -> s"$testSaUtrEnrolmentIdentifier",
                "value" -> s"$testSaUtr"
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
    )
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
