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

package helpers.servicemocks

import helpers.WiremockHelper
import play.api.http.Status
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSaUtr}

object SessionDataStub {

  def stubPostSessionDataResponseOkResponse(): Unit =
    WiremockHelper.stubPost("/income-tax-session-data/", Status.OK, "")

  def stubPostSessionDataResponseConflictResponse(): Unit =
    WiremockHelper.stubPost("/income-tax-session-data/", Status.CONFLICT, "")

  def stubPostSessionDataResponseFailure(): Unit =
    WiremockHelper.stubPost("/income-tax-session-data/", Status.INTERNAL_SERVER_ERROR, "")

  def stubGetSessionDataResponseSuccess(): Unit =
    WiremockHelper.stubGet("/income-tax-session-data/", Status.OK, testSessionResponse)

  def stubGetSessionDataResponseNotFound(): Unit =
    WiremockHelper.stubGet("/income-tax-session-data/", Status.NOT_FOUND, "")

  val testSessionResponse: String = Json.stringify(Json.obj(
    "mtditid" -> testMtditid,
    "nino" -> testNino,
    "utr" -> testSaUtr,
    "sessionId" -> "sessionId"
  ))

}
