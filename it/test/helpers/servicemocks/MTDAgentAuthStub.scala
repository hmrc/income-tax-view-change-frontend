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

import helpers.WiremockHelper.stubPostWithRequest
import play.api.http.Status
import play.api.libs.json.Json

trait MTDAgentAuthStub extends MTDAuthStub {

  def stubAuthorisedAndMTDEnrolled(confidenceLevel: Option[Int] = None): Unit

  def stubUnauthorised(): Unit

  def stubBearerTokenExpired(): Unit

  def stubNotAnAgent(): Unit

  def stubNoAgentEnrolmentRequiredSuccess(): Unit = stubPostWithRequest(
    url = postAuthoriseUrl,
    requestBody = emptyPredicateRequest,
    status = Status.OK,
    responseBody = noAgentEnrolmentSuccessResponse
  )

  def stubNoAgentEnrolmentError(): Unit

  def stubMissingDelegatedEnrolment(): Unit

  def stubAuthorisedWhenNoChecks(): Unit = stubNoAgentEnrolmentRequiredSuccess()

  lazy val noAgentEnrolmentSuccessResponse = Json.stringify(Json.obj(
    "allEnrolments" -> Json.arr(),
    "optionalCredentials" -> Json.obj(
      "providerId" -> "12345-credId",
      "providerType" -> "GovernmentGateway"
    ),
    "affinityGroup" -> "Agent",
    "confidenceLevel" -> 200
  ))

}
