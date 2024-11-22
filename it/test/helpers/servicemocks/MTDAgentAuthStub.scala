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
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment}

object MTDAgentAuthStub {

  val postAuthoriseUrl = "/auth/authorise"
  val requiredConfidenceLevel = 200

  def stubAuthorisedMTDAgent(mtdItId: String = "mtdbsaId", isSupportingAgent: Boolean): Unit = {
    val jsonRequest = getMTDAgentAuthRequest(mtdItId, isSupportingAgent)

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.OK,
      responseBody = mtdAgentSuccessResponse(mtdItId = mtdItId, isSupportingAgent = isSupportingAgent)
    )
  }

  def stubNotAnAgent(mtdItId: String = "mtdbsaId", isSupportingAgent: Boolean): Unit = {
    val jsonRequest = getMTDAgentAuthRequest(mtdItId, isSupportingAgent)

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.OK,
      responseBody = mtdNotAgentSuccessResponse(mtdItId = mtdItId)
    )
  }

  def stubNoAgentEnrolmentRequiredSuccess(mtdItId: String = "mtdbsaId"): Unit = {
    val jsonRequest = emptyPredicateRequest
    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.OK,
      responseBody = mtdAgentSuccessResponse(mtdItId = mtdItId, isSupportingAgent = false)
    )
  }

  def stubNoAgentEnrolmentError(mtdItId: String = "mtdbsaId", isSupportingAgent: Boolean): Unit = {
    val jsonRequest = getMTDAgentAuthRequest(mtdItId, isSupportingAgent)
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
      "Failing-Enrolment" -> "no arn enrolment")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  def stubUnauthorised(mtdItId: String = "mtdbsaId", isSupportingAgent: Boolean): Unit = {
    val jsonRequest = getMTDAgentAuthRequest(mtdItId, isSupportingAgent)
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InvalidBearerToken\"")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }



  def stubBearerTokenExpired(mtdItId: String = "mtdbsaId", isSupportingAgent: Boolean): Unit = {
    val jsonRequest = getMTDAgentAuthRequest(mtdItId, isSupportingAgent)
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"BearerTokenExpired\"")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  def stubMissingDelegatedEnrolment(mtdItId: String = "MtdItId", isSupportingAgent: Boolean): Unit = {
    if(isSupportingAgent) notASecondaryAgent(mtdItId) else notAPrimaryAgent(mtdItId)
  }

  def notAPrimaryAgent(mtdItId: String = "MtdItId"): Unit = {
    val jsonRequest = getMTDAgentAuthRequest(mtdItId, false)

    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
      "Failing-Enrolment" -> "no HMRC-MTD-IT enrolment")
    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }


  def notASecondaryAgent(mtdItId: String = "MtdItId"): Unit = {
    val jsonRequest = getMTDAgentAuthRequest(mtdItId, true)
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
      "Failing-Enrolment" -> "no HMRC-MTD-IT-SUPP enrolment")
    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = jsonRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  lazy val emptyPredicateRequest: JsValue = {
    val predicateJson = {
      EmptyPredicate.toJson
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


  def getMTDAgentAuthRequest(mtdItId: String, isSupportingAgent: Boolean): JsValue = {
    lazy val isAgentPredicate = Enrolment("HMRC-AS-AGENT") and AffinityGroup.Agent
    lazy val isNotAgentPredicate = AffinityGroup.Individual or AffinityGroup.Organisation
    lazy val isPrimaryAgentPredicate = Enrolment(mtdEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
      .withDelegatedAuthRule(primaryAgentAuthRule)
    lazy val isSupportingAgentPredicate = Enrolment(secondaryAgentEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
      .withDelegatedAuthRule(secondaryAgentAuthRule)
    lazy val hasDelegatedEnrolment = if(isSupportingAgent) isSupportingAgentPredicate else isPrimaryAgentPredicate


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

  def mtdAgentSuccessResponse(mtdItId: String, isSupportingAgent: Boolean): String = {
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
      "optionalCredentials" -> Json.obj(
        "providerId" -> "12345-credId",
        "providerType" -> "GovernmentGateway"
      ),
      "affinityGroup" -> "Agent",
      "confidenceLevel" -> requiredConfidenceLevel
    ))
  }

  def mtdNotAgentSuccessResponse(mtdItId: String): String = {
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
