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

package testOnly.models

import play.api.Logger
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object SessionDataGetResponse {

  case class SessionDataGetSuccess(
                                    mtditid: String,
                                    nino: String,
                                    utr: String,
                                    sessionId: String
                                  )

  object SessionDataGetSuccess {
    implicit val format: OFormat[SessionDataGetSuccess] = Json.format[SessionDataGetSuccess]
  }

  sealed trait SessionDataGetFailure

    case class SessionDataNotFound(msg: String) extends Exception with SessionDataGetFailure {
      override def getMessage: String = msg
    }

    case class SessionDataUnexpectedResponse(msg: String) extends Exception with SessionDataGetFailure {
      override def getMessage: String = msg
    }

  type SessionGetResponse = Either[SessionDataGetFailure, SessionDataGetSuccess]

  implicit object SessionGetResponseReads extends HttpReads[SessionGetResponse] {
    override def read(method: String, url: String, response: HttpResponse): SessionGetResponse = {
      response.status match {
        case OK =>
          Logger("application").info("Get session call successful. OK response was returned from the API")
          response.json.validate[SessionDataGetSuccess].fold(
            invalid => Left(SessionDataUnexpectedResponse(s"Json validation error for SessionDataModel. Invalid: $invalid")),
            valid => Right(valid)
          )
        case NOT_FOUND =>
          Logger("application").error(s"No user session was found. status: $NOT_FOUND")
          Left(SessionDataNotFound(s"No user session was found. status: $NOT_FOUND"))
        case status =>
          Logger("application").error(s"User session could not be saved. status: $status")
          Left(SessionDataUnexpectedResponse(s"User session could not be saved. status: $status"))
      }
    }
  }

}