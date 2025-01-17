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

import controllers.agent.AuthUtils._
import helpers.WiremockHelper._
import play.api.http.Status
import play.api.libs.json.{JsString, JsValue, Json}
import testConstants.BaseIntegrationTestConstants.testMtditid
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment}

object MTDPrimaryAgentAuthStub extends MTDAgentAuthStub {

  val requiredConfidenceLevel = 250

  override def stubAuthorisedAndMTDEnrolled(confidenceLevel: Option[Int] = None): Unit = {
    val jsonRequest = getMTDAgentAuthRequest()

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.OK,
      responseBody = mtdAgentSuccessResponse()
    )
  }

  override def stubNotAnAgent(): Unit = {
    val jsonRequest = getMTDAgentAuthRequest()

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.OK,
      responseBody = mtdNotAgentSuccessResponse()
    )
  }

  override def stubNoAgentEnrolmentError(): Unit = {
    val jsonRequest = getMTDAgentAuthRequest()
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
      "Failing-Enrolment" -> "no arn enrolment")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  override def stubUnauthorised(): Unit = {
    val jsonRequest = getMTDAgentAuthRequest()
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InvalidBearerToken\"")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }


  override def stubBearerTokenExpired(): Unit = {
    val jsonRequest = getMTDAgentAuthRequest()
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"BearerTokenExpired\"")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  override def stubMissingDelegatedEnrolment(): Unit = {
    val jsonRequest = getMTDAgentAuthRequest()

    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
      "Failing-Enrolment" -> "no HMRC-MTD-IT enrolment")
    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  def getMTDAgentAuthRequest(): JsValue = {
    lazy val isAgentPredicate = Enrolment("HMRC-AS-AGENT") and AffinityGroup.Agent
    lazy val isNotAgentPredicate = AffinityGroup.Individual or AffinityGroup.Organisation
    lazy val hasDelegatedEnrolment = Enrolment(mtdEnrolmentName).withIdentifier(agentIdentifier, testMtditid)
      .withDelegatedAuthRule(primaryAgentAuthRule)


    val predicateJson = {
      val predicate = (isAgentPredicate and hasDelegatedEnrolment) or isNotAgentPredicate
      Json.arr(predicate.toJson)
    }

    Json.obj(
      "authorise" -> predicateJson,
      "retrieve" -> Json.arr(
        JsString("allEnrolments"),
        JsString("optionalName"),
        JsString("optionalCredentials"),
        JsString("affinityGroup"),
        JsString("confidenceLevel")
      )
    )
  }

  def mtdAgentSuccessResponse(): String = {
    val mtdItAgentEnrolment = {
      val (key, delAuthRule) = ("HMRC-MTD-IT", "mtd-it-auth")
      Json.obj(
        "key" -> key,
        "identifiers" -> Json.arr(
          Json.obj(
            "key" -> "MTDITID",
            "value" -> testMtditid
          )
        ),
        "delegatedAuthRule" -> delAuthRule
      )
    }
    Json.stringify(Json.obj(
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
        mtdItAgentEnrolment
      ),
      "optionalCredentials" -> Json.obj(
        "providerId" -> "12345-credId",
        "providerType" -> "GovernmentGateway"
      ),
      "affinityGroup" -> "Agent",
      "confidenceLevel" -> requiredConfidenceLevel
    ))
  }

  def mtdNotAgentSuccessResponse(): String = {
    Json.stringify(Json.obj(
      "allEnrolments" -> Json.arr(),
      "optionalCredentials" -> Json.obj(
        "providerId" -> "12345-credId",
        "providerType" -> "GovernmentGateway"
      ),
      "affinityGroup" -> "Individual",
      "confidenceLevel" -> requiredConfidenceLevel
    ))
  }
}
