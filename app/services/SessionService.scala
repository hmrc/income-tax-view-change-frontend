/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import auth.MtdItUser
import play.api.mvc.{AnyContent, RequestHeader, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionService @Inject()() {

  def get(key: String)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, Option[String]]] = {
    Future {
      Right(user.session.get(key))
    }
  }

  def set(key: String, value: String, result: Result)(implicit ec: ExecutionContext, request: RequestHeader): Future[Either[Throwable, Result]] = {
    Future {
      Right(result.addingToSession(key -> value))
    }
  }

  def remove(keys: Seq[String], result: Result)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, Result]] = {
    Future{
      val newSession = user.session -- keys
      Right(
        result.withSession(newSession)
      )
    }
  }
}

