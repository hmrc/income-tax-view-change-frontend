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

package models.core

import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{Format, JsSuccess, JsValue}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.util.Try


object ResponseModel {

  trait SuccessModel

  object InvalidJson extends ErrorModel(INTERNAL_SERVER_ERROR, "Invalid JSON" )

  object NotFound extends ErrorModel(NOT_FOUND, "Not found" )

  object UnexpectedError extends ErrorModel(INTERNAL_SERVER_ERROR, "Unexpected error")

  type ResponseModel[T <: SuccessModel] = Either[ErrorModel, T]

  abstract class AResponseReads[T <: SuccessModel] extends HttpReads[ResponseModel[T]] {

    implicit val format: Format[T]

    override def read(method: String, url: String, response: HttpResponse): ResponseModel[T] = {

      def logInvalidResponse(): ResponseModel[T] = {
        Logger("application").warn(s"Unable to parse response")
        Left(InvalidJson)
      }

      def validateAndReturnSuccess(jsValue: JsValue) = {
        jsValue.validate[T] match {
          case JsSuccess(model, _) =>
            Right(model)
          case _ =>
            logInvalidResponse()
        }
      }

      def validateAndReturnError(jsValue: JsValue) = {
        jsValue.validate[ErrorModel] match {
          case JsSuccess(model, _) =>
            Left(model)
          case _ =>
            logInvalidResponse()
        }
      }

      def applyToJsonBody(func: JsValue => ResponseModel[T]) = {
        for {
          jsValue <- Try(response.json)
        } yield {
          func(jsValue)
        }
      }

      val responseTry: Try[ResponseModel[T]] = response.status match {
        case OK =>
          applyToJsonBody(validateAndReturnSuccess)
        case NOT_FOUND =>
          Try[ResponseModel[T]](Left(NotFound))
        case _ =>
          applyToJsonBody(validateAndReturnError)
      }

      responseTry.getOrElse(logInvalidResponse())
    }
  }
}
