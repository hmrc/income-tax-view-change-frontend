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
import helpers.WiremockHelper.{stubPostWithRequest, stubPostWithRequestAndResponseHeaders}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import testConstants.BaseIntegrationTestConstants.{credId, testArn, testMtditid}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment}

object MTDAgentAuthStub extends MTDAuthStub {

  def stubAuthorisedAndMTDEnrolled(isSupportingAgent: Boolean): Unit = {
    stubAuthorisedWithAgentEnrolment()
    if (isSupportingAgent) {
      stubMissingDelegatedEnrolment(false)
    }
    stubAuthorisedAgentWithDelegatedEnrolment(isSupportingAgent)
  }

  def stubAuthorisedButNotMTDEnrolled(): Unit = {
    stubAuthorisedWithAgentEnrolment()
    stubMissingDelegatedEnrolment(false)
    stubMissingDelegatedEnrolment(true)
  }

  override def stubUnauthorised(): Unit = {
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InvalidBearerToken\"")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = isAgentWithAgentEnrolmentPredicateRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  override def stubBearerTokenExpired(): Unit = {
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"BearerTokenExpired\"")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = isAgentWithAgentEnrolmentPredicateRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  def stubAuthorisedWithAgentEnrolment(): Unit = {
    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = isAgentWithAgentEnrolmentPredicateRequest,
      status = Status.OK,
      responseBody = agentSuccessResponse
    )
  }

  def stubAuthorisedAgentWithDelegatedEnrolment(isSupportingAgent: Boolean): Unit = {
    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = delegatedEnrolmentRequest(isSupportingAgent),
      status = Status.OK,
      responseBody = Json.arr().toString()
    )
  }

  def stubNotAnAgent(): Unit = {
    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = isAgentWithAgentEnrolmentPredicateRequest,
      status = Status.OK,
      responseBody = mtdNotAgentSuccessResponse()
    )
  }

  def stubNoAgentEnrolmentRequiredSuccess(): Unit =
    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = emptyPredicateRequest,
      status = Status.OK,
      responseBody = noAgentEnrolmentSuccessResponse
    )

  def stubNoAgentEnrolmentError(): Unit = {
    val responseHeaders =
      Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"", "Failing-Enrolment" -> "no arn enrolment")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = isAgentWithAgentEnrolmentPredicateRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  def stubMissingDelegatedEnrolment(isSupportingAgent: Boolean): Unit = {
    val enrolmentName = if (isSupportingAgent) "HMRC-MTD-IT-SUPP" else "HMRC-MTD-IT"
    val responseHeaders = Map(
      "WWW-Authenticate"  -> "MDTP detail=\"InsufficientEnrolments\"",
      "Failing-Enrolment" -> s"no $enrolmentName enrolment"
    )
    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = delegatedEnrolmentRequest(isSupportingAgent),
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  def stubAuthorisedWhenNoChecks(): Unit = stubNoAgentEnrolmentRequiredSuccess()

  lazy val isAgentWithAgentEnrolmentPredicateRequest = {
    lazy val isNotAgentPredicate = AffinityGroup.Individual or AffinityGroup.Organisation
    lazy val isAgentPredicate    = Enrolment("HMRC-AS-AGENT") and AffinityGroup.Agent
    val predicateJson            = Json.arr((isAgentPredicate or isNotAgentPredicate).toJson)
    Json.obj(
      "authorise" -> predicateJson,
      "retrieve"  -> retrivalsJson
    )
  }

  lazy val delegatedEnrolmentRequest: Boolean => JsObject = isSupportingAgent => {
    val predicate = if (isSupportingAgent) {
      Enrolment(secondaryAgentEnrolmentName)
        .withIdentifier(agentIdentifier, testMtditid)
        .withDelegatedAuthRule(secondaryAgentAuthRule)
        .toJson
    } else {
      Enrolment(mtdEnrolmentName)
        .withIdentifier(agentIdentifier, testMtditid)
        .withDelegatedAuthRule(primaryAgentAuthRule)
        .toJson
    }
    Json.obj(
      "authorise" -> Json.arr(predicate),
      "retrieve"  -> emptyRetrievalsJson
    )
  }

  lazy val noAgentEnrolmentSuccessResponse = Json.stringify(
    Json.obj(
      "allEnrolments" -> Json.arr(),
      "optionalCredentials" -> Json.obj(
        "providerId"   -> credId,
        "providerType" -> "GovernmentGateway"
      ),
      "affinityGroup"   -> "Agent",
      "confidenceLevel" -> 200
    )
  )

  lazy val agentSuccessResponse: String = {
    Json.stringify(
      Json.obj(
        "allEnrolments" -> Json.arr(
          Json.obj(
            "key" -> "HMRC-AS-AGENT",
            "identifiers" -> Json.arr(
              Json.obj(
                "key"   -> "AgentReferenceNumber",
                "value" -> testArn
              )
            )
          )
        ),
        "optionalCredentials" -> Json.obj(
          "providerId"   -> credId,
          "providerType" -> "GovernmentGateway"
        ),
        "affinityGroup"   -> "Agent",
        "confidenceLevel" -> requiredConfidenceLevel
      )
    )
  }

  def mtdNotAgentSuccessResponse(): String = {
    Json.stringify(
      Json.obj(
        "allEnrolments" -> Json.arr(),
        "optionalCredentials" -> Json.obj(
          "providerId"   -> credId,
          "providerType" -> "GovernmentGateway"
        ),
        "affinityGroup"   -> "Individual",
        "confidenceLevel" -> requiredConfidenceLevel
      )
    )
  }

}
