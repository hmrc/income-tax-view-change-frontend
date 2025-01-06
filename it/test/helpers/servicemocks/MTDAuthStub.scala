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

import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate

trait MTDAuthStub {

  lazy val postAuthoriseUrl = "/auth/authorise"

  def stubAuthorisedAndMTDEnrolled(confidenceLevel: Option[Int] = None): Unit

  def stubUnauthorised(): Unit

  def stubBearerTokenExpired(): Unit

  def stubAuthorisedWhenNoChecks(): Unit

  lazy val emptyPredicateRequest: JsValue = {
    val predicateJson = {
      EmptyPredicate.toJson
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
}
