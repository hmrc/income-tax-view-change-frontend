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

package utils

import play.api.mvc.Result

import scala.concurrent.Future
import scala.language.implicitConversions

object ImplicitsWithFunUtils {

  case class ErrorCode(message: String, code: Int = 0, showInternalServerError: Boolean = true) extends RuntimeException(message)

  type ItvcResponse[T]  = Either[ErrorCode, T]
  type ItvcResult  = Future[Result]

  implicit class ValueToEither[V](v: V) {
    def toRightE: Right[ErrorCode, V] = Right(v)
    def toLeftE[R]: Left[ErrorCode, R] = Left(v.asInstanceOf[ErrorCode])
  }

  implicit class EitherToFuture[T](either: Either[ErrorCode, T]) {
    def toFuture: Future[T] = either match {
      case Right(a) => Future.successful(a)
      case Left(errorCode) => Future.failed(errorCode)
    }
  }

  implicit class TypeToFuture[T](t: T) {
    def toFuture: Future[T] = Future.successful(t)
  }

}