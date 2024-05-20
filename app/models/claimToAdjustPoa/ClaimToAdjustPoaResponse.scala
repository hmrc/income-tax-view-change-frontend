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

package models.claimToAdjustPoa

import play.api.Logger
import play.api.http.Status.CREATED
import play.api.libs.json.{Format, JsSuccess, Json}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object ClaimToAdjustPoaResponse {

  case class ClaimToAdjustPoaSuccess(processingDate: String)

  trait ClaimToAdjustPoaFailure {
    val message: String
  }
  case class ClaimToAdjustPoaError(message: String) extends ClaimToAdjustPoaFailure

  object ClaimToAdjustPoaInvalidJson extends ClaimToAdjustPoaFailure {
    val message = "Invalid JSON"
  }

  object UnexpectedError extends ClaimToAdjustPoaFailure {
    val message = "Unexpected error"
  }

  type ClaimToAdjustPoaResponse = Either[ClaimToAdjustPoaFailure, ClaimToAdjustPoaSuccess]

  implicit val failureResponseFormat: Format[ClaimToAdjustPoaError] = Json.format[ClaimToAdjustPoaError]

  implicit val successResponseFormat: Format[ClaimToAdjustPoaSuccess] = Json.format[ClaimToAdjustPoaSuccess]

  implicit object ClaimToAdjustPoaResponseReads extends HttpReads[ClaimToAdjustPoaResponse] {

    override def read(method: String, url: String, response: HttpResponse): ClaimToAdjustPoaResponse = {
      response.status match {
        case CREATED =>
          response.json.validate[ClaimToAdjustPoaSuccess] match {
            case JsSuccess(model, _) =>
              Right(model)
            case _ =>
              Logger("application").warn(s"Unable to parse success response")
              Left(ClaimToAdjustPoaInvalidJson)
          }
        case _ =>
          response.json.validate[ClaimToAdjustPoaError] match {
            case JsSuccess(model, _) =>
              Left(model)
            case _ =>
              Logger("application").warn(s"Unable to parse failure response")
              Left(ClaimToAdjustPoaInvalidJson)
          }
      }
    }
  }
}
