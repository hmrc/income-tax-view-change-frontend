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

import models.sessionData.SessionDataPostResponse.{SessionDataPostFailure, SessionDataPostResponseReads, SessionDataPostSuccess}
import play.api.http.Status.{CONFLICT, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import testUtils.UnitSpec
import uk.gov.hmrc.http.HttpResponse

class SessionDataPostResponseSpec extends UnitSpec {

  val testHttpVerbPost = "POST"
  val testUri = "/test"

  "SessionDataPostResponse.read" should {
    "parse OK status and return a success response" in {
      val httpResponse = HttpResponse(OK, json = Json.obj(), headers = Map.empty)
      val result = SessionDataPostResponseReads.read(testHttpVerbPost, testUri, httpResponse)

      result shouldBe Right(SessionDataPostSuccess(OK))
    }
    "parse CONFLICT status and return a success response" in {
      val httpResponse = HttpResponse(CONFLICT, json = Json.obj(), headers = Map.empty)
      val result = SessionDataPostResponseReads.read(testHttpVerbPost, testUri, httpResponse)

      result shouldBe Right(SessionDataPostSuccess(CONFLICT))
    }
    "parse INTERNAL SERVER ERROR status and return an error response" in {
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, json = Json.obj(), headers = Map.empty)
      val result = SessionDataPostResponseReads.read(testHttpVerbPost, testUri, httpResponse)

      result shouldBe Left(SessionDataPostFailure(INTERNAL_SERVER_ERROR, "User session could not be saved. status: 500"))
    }
  }

}
