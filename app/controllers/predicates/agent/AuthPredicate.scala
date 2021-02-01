/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.predicates.agent

import play.api.mvc.{AnyContent, Request, Result}
import cats.Monoid


import scala.concurrent.Future

object AuthPredicate {

  case object AuthPredicateSuccess

  type AuthPredicateSuccess = AuthPredicateSuccess.type

  implicit object AuthPredicateSuccessMonoid extends Monoid[AuthPredicateSuccess] {
    override def empty: AuthPredicateSuccess = AuthPredicateSuccess

    override def combine(x: AuthPredicateSuccess, y: AuthPredicateSuccess): AuthPredicateSuccess = AuthPredicateSuccess
  }

  type AuthPredicate[User <: IncomeTaxAgentUser] = Request[AnyContent] => User => Either[Future[Result], AuthPredicateSuccess]

}
