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
import play.mvc.Http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT}

object OptOutUpdateRequestModel {

  val itsaOptOutUpdateReason: Int = 10

  case class OptOutUpdateRequest(taxYear: String, updateReason: Int)
  sealed trait OptOutUpdateResponse {
    val statusCode: Int
  }
  case class OptOutUpdateResponseSuccess(correlationId: String, statusCode: Int = NO_CONTENT) extends OptOutUpdateResponse
  case class ErrorItem(code: String, reason: String)

  //todo check error page to user states at least some updates have failed
  case class OptOutUpdateResponseFailure(correlationId: String, statusCode: Int, failures: List[ErrorItem]) extends OptOutUpdateResponse

  object OptOutUpdateResponseFailure {
    def defaultFailure(correlationId: String = "unknown"): OptOutUpdateResponseFailure =
      OptOutUpdateResponseFailure(correlationId,
        INTERNAL_SERVER_ERROR,
        List(ErrorItem("INTERNAL_SERVER_ERROR", "Request failed due to unknown error"))
      )
  }

  implicit val formatSuccess: Format[OptOutUpdateResponseSuccess] = Json.format[OptOutUpdateResponseSuccess]
  implicit val formatErrorItem: Format[ErrorItem] = Json.format[ErrorItem]
  implicit val formatFailure: Format[OptOutUpdateResponseFailure] = Json.format[OptOutUpdateResponseFailure]
  implicit val format: Format[OptOutUpdateRequest] = Json.format[OptOutUpdateRequest]

}