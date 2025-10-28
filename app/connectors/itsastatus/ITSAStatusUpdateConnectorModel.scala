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

package connectors.itsastatus

import play.api.libs.json.{Format, Json, OFormat}
import play.mvc.Http.Status.NO_CONTENT

case class ITSAStatusUpdateRequest(taxYear: String, updateReason: String)

object ITSAStatusUpdateRequest {
  implicit val format: OFormat[ITSAStatusUpdateRequest] = Json.format[ITSAStatusUpdateRequest]
}

object ITSAStatusUpdateConnectorModel {

  val optOutUpdateReason: String = "10"
  val optInUpdateReason: String = "11"

  sealed trait ITSAStatusUpdateResponse

  sealed trait ITSAStatusUpdateError

  case class ErrorItem(code: String, reason: String)

  case class ITSAStatusUpdateResponseSuccess(statusCode: Int = NO_CONTENT) extends ITSAStatusUpdateResponse

  case class ITSAStatusUpdateResponseFailure(failures: List[ErrorItem]) extends ITSAStatusUpdateResponse

  case object NoOptOutRequestsMade extends ITSAStatusUpdateError

  object ITSAStatusUpdateResponseFailure {

    def defaultFailure(message: String = "unknown reason"): ITSAStatusUpdateResponseFailure =
      ITSAStatusUpdateResponseFailure(
        List(ErrorItem("INTERNAL_SERVER_ERROR", s"Request failed due to $message"))
      )
  }

  implicit val formatSuccess: Format[ITSAStatusUpdateResponseSuccess] = Json.format[ITSAStatusUpdateResponseSuccess]
  implicit val formatErrorItem: Format[ErrorItem] = Json.format[ErrorItem]
  implicit val formatFailure: Format[ITSAStatusUpdateResponseFailure] = Json.format[ITSAStatusUpdateResponseFailure]
}