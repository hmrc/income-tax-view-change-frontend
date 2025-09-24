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

package models.sessionData

import models.sessionData.SessionDataGetResponse._
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{JsObject, Json, JsonValidationError}
import testUtils.UnitSpec
import uk.gov.hmrc.http.HttpResponse

class SessionDataGetResponseSpec extends UnitSpec {

  val testHttpVerb = "GET"
  val testUri = "/test"

  val testValidSessionGetResponse: JsObject = Json.obj(
    "mtditid" -> "123abc",
    "nino" -> "AA123456A",
    "utr" -> "1234567890",
    "sessionId" -> "1234"
  )

  val sessionDataSuccess: SessionDataGetSuccess = SessionDataGetSuccess(
    mtditid = "123abc",
    nino = "AA123456A",
    utr = "1234567890",
    sessionId = "1234"
  )

  val testInvalidSessionGetResponse: JsObject = Json.obj(
    "mtditid" -> "1234",
    "utr" -> "1234567890",
    "sessionId" -> "1234",
  )

  val jsonValidationError: Seq[(String, Seq[JsonValidationError])] = Seq("/nino" -> Seq(JsonValidationError("error.path.missing")))

  "SessionDataGetResponseRead.read" should {

    "respond with an OK status and return a response when parsed successfully" in {
      val httpResponse = HttpResponse(status = OK, json = testValidSessionGetResponse, headers = Map.empty)
      val result = SessionGetResponseReads.read(testHttpVerb, testUri, httpResponse)

      result shouldBe Right(sessionDataSuccess)
    }

    "response an OK status and return SessionDataUnexpectedResponse when unable to parse successfully" in {
      val httpResponse = HttpResponse(status = OK, json = testInvalidSessionGetResponse, headers = Map.empty)
      val result = SessionGetResponseReads.read(testHttpVerb, testUri, httpResponse)

      result shouldBe Left(SessionDataUnexpectedResponse(s"Json validation error for SessionDataModel. Invalid: $jsonValidationError"))
    }

    "respond with a NotFound status and return SessionDataNotFound when no session data was found" in {
      val httpResponse = HttpResponse(status = NOT_FOUND, json = Json.toJson("{}"), headers = Map.empty)
      val result = SessionGetResponseReads.read(testHttpVerb, testUri, httpResponse)

      result shouldBe Left(SessionDataNotFound(s"No user session was found. status: $NOT_FOUND"))
    }

    "response with a InternalServerError and return SessionDataUnexpectedResponse when an unexpected error has occurred" in {
      val httpResponse = HttpResponse(status = INTERNAL_SERVER_ERROR, json = Json.toJson("{}"), headers = Map.empty)
      val result = SessionGetResponseReads.read(testHttpVerb, testUri, httpResponse)

      result shouldBe Left(SessionDataUnexpectedResponse("User session could not be retrieved. status: 500"))
    }

  }

  "SessionDataNotFound" should {
    "extend Exception and SessionDataGetFailure" in {
      val ex = SessionDataNotFound("not found message")

      ex shouldBe a [Exception]
      ex shouldBe a [SessionDataGetFailure]
    }

    "return the provided message from msg and getMessage" in {
      val ex = SessionDataNotFound("not found message")

      ex.msg shouldBe "not found message"
      ex.getMessage shouldBe "not found message"
    }
  }

  "SessionDataUnexpectedResponse" should {
    "extend Exception and SessionDataGetFailure" in {
      val ex = SessionDataUnexpectedResponse("unexpected response")

      ex shouldBe a [Exception]
      ex shouldBe a [SessionDataGetFailure]
    }
    "return the provided message from msg and getMessage" in {
      val ex = SessionDataUnexpectedResponse("unexpected response")

      ex.msg shouldBe "unexpected response"
      ex.getMessage shouldBe "unexpected response"
    }
  }
}
