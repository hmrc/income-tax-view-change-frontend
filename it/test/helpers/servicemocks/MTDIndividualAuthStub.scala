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

import helpers.WiremockHelper._
import play.api.http.Status
import play.api.libs.json.{JsArray, JsString, JsValue, Json}
import testConstants.BaseIntegrationTestConstants._
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment}

object MTDIndividualAuthStub extends MTDAuthStub {

  lazy val enrolledIndividualRequest: JsValue = {
    lazy val isAgentPredicate = AffinityGroup.Agent
    lazy val isNotAgentPredicate = AffinityGroup.Organisation or AffinityGroup.Individual
    lazy val hasEnrolment = Enrolment("HMRC-MTD-IT")

    val predicateJson = {
      val predicate =  isAgentPredicate or (hasEnrolment and isNotAgentPredicate)
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

  def stubAuthorisedAndMTDEnrolled(confidenceLevel: Option[Int] = None): Unit = {

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = enrolledIndividualRequest,
      status = Status.OK,
      responseBody = mtdIndividualUserSuccessResponse(confidenceLevel)
    )
  }

  def stubAuthorisedWithNoName(): Unit = {

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = enrolledIndividualRequest,
      status = Status.OK,
      responseBody = mtdIndividualUserSuccessResponse(hasName = false)
    )
  }

  def stubAuthorisedNoNino(): Unit = {

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = enrolledIndividualRequest,
      status = Status.OK,
      responseBody = mtdIndividualUserSuccessResponse(hasNinoEnrolment = false)
    )
  }

  def stubAuthorisedButAgent(): Unit = {

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = enrolledIndividualRequest,
      status = Status.OK,
      responseBody = mtdAgentSuccessResponse
    )
  }


  def stubInsufficientEnrolments(): Unit = {
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
      "Failing-Enrolment" -> "no HMRC-MTD-IT enrolment")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = enrolledIndividualRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  override def stubUnauthorised(): Unit = {
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InvalidBearerToken\"")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = enrolledIndividualRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  override def stubBearerTokenExpired(): Unit = {
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"BearerTokenExpired\"")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = enrolledIndividualRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  override def stubAuthorisedWhenNoChecks(): Unit = {
    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = emptyPredicateRequest,
      status = Status.OK,
      responseBody = mtdIndividualUserSuccessResponse(Some(requiredConfidenceLevel))
    )
  }



  def mtdIndividualUserSuccessResponse(optConfidenceLevel: Option[Int] = None,
                                       hasNinoEnrolment: Boolean = true,
                                       hasSaEnrolment: Boolean = true,
                                       hasName: Boolean = true): String = {
    val confidenceLevel: Int = optConfidenceLevel.getOrElse(requiredConfidenceLevel)

    val userNameJsObj = if(hasName) {
      Json.obj(
        "optionalName" -> Json.obj(
          "name" -> "Albert",
          "lastName" -> "Einstein"
        )
      )
    } else Json.obj()

    Json.stringify(Json.obj(
      "allEnrolments" -> getEnrolmentsJson(hasNinoEnrolment, hasSaEnrolment)
    ) ++ userNameJsObj ++ Json.obj(
      "optionalCredentials" -> Json.obj(
        "providerId" -> credId,
        "providerType" -> "GovernmentGateway"
      ),
      "affinityGroup" -> "Individual",
      "confidenceLevel" -> confidenceLevel
    ))
  }

  def getEnrolmentsJson(hasNinoEnrolment: Boolean = true,
                        hasSaEnrolment: Boolean = true): JsArray = {
    lazy val ninoEnrolment = if(hasNinoEnrolment) {
      Json.obj(
        "key" -> testNinoEnrolmentKey,
        "identifiers" -> Json.arr(
          Json.obj(
            "key" -> testNinoEnrolmentIdentifier,
            "value" -> testNino
          )
        )
      )
    } else Json.obj()

    lazy val saEnrolment = if(hasSaEnrolment) {
      Json.obj(
        "key" -> testSaUtrEnrolmentKey,
        "identifiers" -> Json.arr(
          Json.obj(
            "key" -> testSaUtrEnrolmentIdentifier,
            "value" -> testSaUtr
          )
        )
      )
    } else {Json.obj()}

    Json.arr(
      Json.obj(
        "key" -> testMtditidEnrolmentKey,
        "identifiers" -> Json.arr(
          Json.obj(
            "key" -> testMtditidEnrolmentIdentifier,
            "value" -> testMtditid
          )
        )
      ), ninoEnrolment, saEnrolment
    )
  }

  val mtdAgentSuccessResponse: String = {

    Json.stringify(Json.obj(
      "allEnrolments" -> Json.arr(
        Json.obj(
          "key" -> "HMRC-AS-AGENT",
          "identifiers" -> Json.arr(
            Json.obj(
              "key" -> "AgentReferenceNumber",
              "value" -> testArn
            )
          )
        )
      ),
      "optionalCredentials" -> Json.obj(
        "providerId" -> credId,
        "providerType" -> "GovernmentGateway"
      ),
      "affinityGroup" -> "Agent",
      "confidenceLevel" -> requiredConfidenceLevel
    ))
  }

}
