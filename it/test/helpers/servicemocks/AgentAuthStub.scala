/*
 * Copyright 2025 HM Revenue & Customs
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

///*
// * Copyright 2017 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package helpers.servicemocks
//
//import controllers.agent.AuthUtils._
//import enums.{MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
//import helpers.WiremockHelper._
//import play.api.http.Status
//import play.api.libs.json.{JsString, JsValue, Json}
//import testConstants.BaseIntegrationTestConstants.testMtditid
//import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
//import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment}
//
//object AgentAuthStub {
//
//  val postAuthoriseUrl = "/auth/authorise"
//  val requiredConfidenceLevel = 200
//
//
//  def stubPrimaryAuthorisedAgent(mtdItId: String = "mtdbsaId"): Unit = {
//    val jsonRequest = getAgentAuthRequest(Some(MTDPrimaryAgent), mtdItId)
//
//    stubPostWithRequest(
//      url = postAuthoriseUrl,
//      requestBody = jsonRequest,
//      status = Status.OK,
//      responseBody = Json.arr()
//    )
//  }
//
//  def stubSecondaryAuthorisedAgent(mtdItId: String = "mtdbsaId"): Unit = {
//    val jsonRequest = getAgentAuthRequest(Some(MTDSupportingAgent), mtdItId)
//
//    stubPostWithRequest(
//      url = postAuthoriseUrl,
//      requestBody = jsonRequest,
//      status = Status.OK,
//      responseBody = Json.arr()
//    )
//  }
//
//  def stubAuthAgent(mtdItId: String = "mtdbsaId"): Unit = {
//    val jsonRequest = getAgentAuthRequest(None, mtdItId)
//
//    stubPostWithRequest(
//      url = postAuthoriseUrl,
//      requestBody = jsonRequest,
//      status = Status.OK,
//      responseBody = agentSuccessResponse(mtdItId = mtdItId)
//    )
//  }
//
//  def stubNotAnAgent(mtdItId: String = "mtdbsaId"): Unit = {
//    val jsonRequest = getAgentAuthRequest(None, mtdItId)
//    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"UnsupportedAffinityGroup\"")
//
//    stubPostWithRequestAndResponseHeaders(
//      url = postAuthoriseUrl,
//      requestBody = jsonRequest,
//      status = Status.UNAUTHORIZED,
//      responseHeaders = responseHeaders
//    )
//  }
//
//  def stubNoAgentEnrolment(mtdItId: String = "mtdbsaId"): Unit = {
//    val jsonRequest = getAgentAuthRequest(None, mtdItId)
//    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
//      "Failing-Enrolment" -> "no arn enrolment")
//
//    stubPostWithRequestAndResponseHeaders(
//      url = postAuthoriseUrl,
//      requestBody = jsonRequest,
//      status = Status.UNAUTHORIZED,
//      responseHeaders = responseHeaders
//    )
//  }
//
//  def failedPrimaryAgent(mtdItId: String = "MtdItId"): Unit = {
//    val jsonRequest = getAgentAuthRequest(Some(MTDPrimaryAgent), mtdItId)
//
//    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
//      "Failing-Enrolment" -> "no mtditid enrolment")
//    stubPostWithRequestAndResponseHeaders(
//      url = postAuthoriseUrl,
//      requestBody = jsonRequest,
//      status = Status.UNAUTHORIZED,
//      responseHeaders = responseHeaders
//    )
//  }
//
//
//  def failedSecondaryAgent(mtdItId: String = "MtdItId"): Unit = {
//    val jsonRequest = getAgentAuthRequest(Some(MTDSupportingAgent), mtdItId)
//    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
//      "Failing-Enrolment" -> "no mtditid enrolment")
//    stubPostWithRequestAndResponseHeaders(
//      url = postAuthoriseUrl,
//      requestBody = jsonRequest,
//      status = Status.UNAUTHORIZED,
//      responseHeaders = responseHeaders
//    )
//  }
//
//  def getDelegatedEnrolmentRequest(isSupportingAgent: Boolean): JsValue = {
//    val predicate = if(isSupportingAgent) {
//      Enrolment(secondaryAgentEnrolmentName).withIdentifier(agentIdentifier, testMtditid)
//        .withDelegatedAuthRule(secondaryAgentAuthRule).toJson
//    } else {
//      Enrolment(mtdEnrolmentName).withIdentifier(agentIdentifier, testMtditid)
//        .withDelegatedAuthRule(primaryAgentAuthRule).toJson
//    }
//    Json.obj(
//      "authorise" -> Json.arr(predicate),
//      "retrieve" -> Json.arr()
//    )
//  }
//
//
//  def getAgentAuthRequest(): JsValue = {
//    lazy val isNotAgentPredicate = AffinityGroup.Individual or AffinityGroup.Organisation
//    lazy val isAgentPredicate = Enrolment("HMRC-AS-AGENT") and AffinityGroup.Agent
//    val predicateJson = Json.arr((isAgentPredicate or isNotAgentPredicate).toJson)
//
//    Json.obj(
//      "authorise" -> predicateJson,
//      "retrieve" -> Json.arr(
//        JsString("allEnrolments"),
//        JsString("optionalName"),
//        JsString("optionalCredentials"),
//        JsString("affinityGroup"),
//        JsString("confidenceLevel")
//      )
//    )
//  }
//
//  def agentSuccessResponse(mtdItId: String): String = {
//    Json.stringify(Json.obj(
//      "allEnrolments" -> Json.arr(
//        Json.obj(
//          "key" -> "HMRC-AS-AGENT",
//          "identifiers" -> Json.arr(
//            Json.obj(
//              "key" -> "AgentReferenceNumber",
//              "value" -> "1"
//            )
//          )
//        )
//      ),
//      "affinityGroup" -> "Agent",
//      "confidenceLevel" -> requiredConfidenceLevel
//    ))
//  }
//
//}
