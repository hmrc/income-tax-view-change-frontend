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
import enums.{MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import helpers.WiremockHelper._
import play.api.http.Status
import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment}

object AgentAuthStub {

  val postAuthoriseUrl = "/auth/authorise"
  val requiredConfidenceLevel = 200


  def stubPrimaryAuthorisedAgent(mtdItId: String = "mtdbsaId"): Unit = {
    val jsonRequest = getAgentAuthRequest(Some(MTDPrimaryAgent), mtdItId)

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.OK,
      responseBody = agentSuccessResponse(mtdItId = mtdItId, isSupportingAgent = false)
    )
  }

  def stubSecondaryAuthorisedAgent(mtdItId: String = "mtdbsaId"): Unit = {
    val jsonRequest = getAgentAuthRequest(Some(MTDSupportingAgent), mtdItId)

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.OK,
      responseBody = agentSuccessResponse(mtdItId = mtdItId, true)
    )
  }

  def stubAuthAgent(mtdItId: String = "mtdbsaId", isSupportingAgent: Boolean = false): Unit = {
    val jsonRequest = getAgentAuthRequest(None, mtdItId)

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.OK,
      responseBody = agentSuccessResponse(mtdItId = mtdItId, isSupportingAgent)
    )
  }

  def stubNotAnAgent(mtdItId: String = "mtdbsaId"): Unit = {
    val jsonRequest = getAgentAuthRequest(None, mtdItId)
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"UnsupportedAffinityGroup\"")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  def stubNoAgentEnrolment(mtdItId: String = "mtdbsaId"): Unit = {
    val jsonRequest = getAgentAuthRequest(None, mtdItId)
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
      "Failing-Enrolment" -> "no arn enrolment")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  def failedPrimaryAgent(mtdItId: String = "MtdItId"): Unit = {
    val jsonRequest = getAgentAuthRequest(Some(MTDPrimaryAgent), mtdItId)

    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
      "Failing-Enrolment" -> "no mtditid enrolment")
    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }


  def failedSecondaryAgent(mtdItId: String = "MtdItId"): Unit = {
    val jsonRequest = getAgentAuthRequest(Some(MTDSupportingAgent), mtdItId)
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
      "Failing-Enrolment" -> "no mtditid enrolment")
    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }


  def getAgentAuthRequest(role: Option[MTDUserRole], mtdItId: String): JsValue = {
    lazy val isNotAgentPredicate = AffinityGroup.Individual or AffinityGroup.Organisation
    lazy val isAgentPredicate = Enrolment("HMRC-AS-AGENT") and AffinityGroup.Agent
    val predicateJson = role match {
      case Some(MTDPrimaryAgent) => Json.arr(Enrolment(mtdEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
        .withDelegatedAuthRule(primaryAgentAuthRule).toJson)
      case Some(MTDSupportingAgent) => Json.arr(Enrolment(secondaryAgentEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
        .withDelegatedAuthRule(secondaryAgentAuthRule).toJson)
      case _ => Json.arr((isAgentPredicate or isNotAgentPredicate).toJson)
    }

    Json.obj(
      "authorise" -> predicateJson,
      "retrieve" -> Json.arr(
        JsString("allEnrolments"),
        JsString("optionalCredentials"),
        JsString("affinityGroup"),
        JsString("confidenceLevel")
      )
    )
  }

  def agentSuccessResponse(mtdItId: String, isSupportingAgent: Boolean): String = {
    val mtdItAgentEnrolment = {
      val (key, delAuthRule) = if (isSupportingAgent) {
        ("HMRC-MTD-IT-SUPP", "mtd-it-auth-supp")
      } else {
        ("HMRC-MTD-IT", "mtd-it-auth")
      }
      Json.obj(
        "key" -> key,
        "identifiers" -> Json.arr(
          Json.obj(
            "key" -> "MTDITID",
            "value" -> mtdItId
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
      "affinityGroup" -> "Agent",
      "confidenceLevel" -> requiredConfidenceLevel
    ))
  }

}
