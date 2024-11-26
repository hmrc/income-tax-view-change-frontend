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

  val postAuthoriseUrl = "/auth/authorise"
  val requiredConfidenceLevel = 250

  lazy val getAuthRequest: JsValue = {
    lazy val isAgentPredicate = AffinityGroup.Agent
    lazy val isNotAgentPredicate = AffinityGroup.Organisation or AffinityGroup.Individual
    lazy val hasEnrolment = Enrolment("HMRC-MTD-IT")

    val predicateJson = {
      val predicate = (hasEnrolment and isNotAgentPredicate) or isAgentPredicate
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


  override def stubAuthorised(confidenceLevel: Option[Int] = None): Unit = {

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = getAuthRequest,
      status = Status.OK,
      responseBody = mtdIndividualUserSuccessResponse(confidenceLevel)
    )
  }

  def stubAuthorisedWithName(): Unit = {

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = getAuthRequest,
      status = Status.OK,
      responseBody = mtdIndividualUserSuccessResponse(hasName = true)
    )
  }

  def stubAuthorisedNoNino(): Unit = {

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = getAuthRequest,
      status = Status.OK,
      responseBody = mtdIndividualUserSuccessResponse(hasNinoEnrolment = false)
    )
  }



  def stubAuthorisedButAgent(): Unit = {

    stubPostWithRequest(
      url = postAuthoriseUrl,
      requestBody = getAuthRequest,
      status = Status.OK,
      responseBody = mtdAgentSuccessResponse
    )
  }


  def stubInsufficientEnrolments(): Unit = {
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InsufficientEnrolments\"",
      "Failing-Enrolment" -> "no HMRC-MTD-IT enrolment")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = getAuthRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }

  override def stubUnauthorised(): Unit = {
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"InvalidBearerToken\"")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = getAuthRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }



  override def stubBearerTokenExpired(): Unit = {
    val responseHeaders = Map("WWW-Authenticate" -> "MDTP detail=\"BearerTokenExpired\"")

    stubPostWithRequestAndResponseHeaders(
      url = postAuthoriseUrl,
      requestBody = getAuthRequest,
      status = Status.UNAUTHORIZED,
      responseHeaders = responseHeaders
    )
  }


  def mtdIndividualUserSuccessResponse(optConfidenceLevel: Option[Int] = None,
                                       hasNinoEnrolment: Boolean = true,
                                       hasSaEnrolment: Boolean = true,
                                       hasName: Boolean = false): String = {
    val confidenceLevel: Int = optConfidenceLevel.getOrElse(requiredConfidenceLevel)

    val userNameJsObj = if(hasName) {
      Json.obj(
        "optionalName" -> Json.obj(
          "name" -> "John",
          "lastName" -> "Doe"
        )
      )
    } else Json.obj()

    Json.stringify(Json.obj(
      "allEnrolments" -> getEnrolmentsJson(hasNinoEnrolment, hasSaEnrolment)
    ) ++ userNameJsObj ++ Json.obj(
      "optionalCredentials" -> Json.obj(
        "providerId" -> "12345-credId",
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
              "value" -> "1"
            )
          )
        )
      ),
      "optionalCredentials" -> Json.obj(
        "providerId" -> "12345-credId",
        "providerType" -> "GovernmentGateway"
      ),
      "affinityGroup" -> "Agent",
      "confidenceLevel" -> requiredConfidenceLevel
    ))
  }

}
