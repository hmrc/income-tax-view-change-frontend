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

import play.api.http.Status.{CONFLICT, OK}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object SessionDataPostResponse {

  type SessionDataPostResponse = Either[SessionDataPostFailure, SessionDataPostSuccess]

  case class SessionDataPostSuccess(status: Int)

  object SessionDataPostSuccess {
    implicit val format: OFormat[SessionDataPostSuccess] = Json.format[SessionDataPostSuccess]
  }
  case class SessionDataPostFailure(status: Int, errorMessage: String)

  implicit object SessionDataPostResponseReads extends HttpReads[SessionDataPostResponse] {
    override def read(method: String, url: String, response: HttpResponse): SessionDataPostResponse = {
      response.status match {
        case status if status == OK || status == CONFLICT => Right(SessionDataPostSuccess(status))
        case status => Left(SessionDataPostFailure(status, s"User session could not be saved. status: $status"))
      }
    }
  }
}
