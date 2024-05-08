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

package models.optOut

import play.api.libs.json.{Format, Json}
import play.mvc.Http.Status.NO_CONTENT

object OptOutUpdateRequestModel {

  private val defaultUpdateReason: Int = 10
  private val successfulStatusCode: Int = NO_CONTENT

  case class OptOutUpdateRequest(taxYear: String, updateReason: Int = defaultUpdateReason)
  sealed trait OptOutUpdateResponse {
    val statusCode: Int
  }
  case class OptOutUpdateResponseSuccess(correlationId: String, statusCode: Int = successfulStatusCode) extends OptOutUpdateResponse
  case class ErrorItem(code: String, reason: String)
  case class OptOutUpdateResponseFailure(correlationId: String, statusCode: Int, failures: List[ErrorItem]) extends OptOutUpdateResponse

  implicit val formatSuccess: Format[OptOutUpdateResponseSuccess] = Json.format[OptOutUpdateResponseSuccess]
  implicit val formatErrorItem: Format[ErrorItem] = Json.format[ErrorItem]
  implicit val formatFailure: Format[OptOutUpdateResponseFailure] = Json.format[OptOutUpdateResponseFailure]
  implicit val format: Format[OptOutUpdateRequest] = Json.format[OptOutUpdateRequest]

}