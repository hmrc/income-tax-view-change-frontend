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

import helpers.WiremockHelper
import play.api.http.Status
import play.api.libs.json.Json
import helpers.IntegrationTestConstants._

object AuthStub {

  val postAuthoriseUrl = "/auth/authorise"

  def stubAuthorised(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.OK,
      Json.parse(
          s"""{
          | "authorisedEnrolments": [{
          | "key":"$testEnrolmentKey",
          | "identifiers": [{"key":"$testEnrolmentIdentifier", "value":"$testMtditid"}]
          | }]
          |}""".stripMargin).toString())
  }

  def stubUnauthorised():Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.OK, "{}")
  }

  def stubInsufficientEnrolments():Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.INTERNAL_SERVER_ERROR,
      Json.parse(
        s"""{
           |"authorisedEnrolments":[{}]
           |}
         """.stripMargin).toString()
    )
  }

  def stubWrongEnrolment(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.UNAUTHORIZED,
      Json.parse(
        s"""{
           | "authorisedEnrolments": [{
           | "key":"ANOTHER-KEY",
           | "identifiers": [{"key":"ANOTHER-ID", "value":"XA123456789"}]
           | }]
           |}""".stripMargin).toString())
  }
}


