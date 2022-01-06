/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.agent.predicates

import controllers.agent.utils.SessionKeys
import controllers.predicates.AuthPredicate.AuthPredicate
import controllers.predicates.IncomeTaxAgentUser
import controllers.predicates.agent.AgentAuthenticationPredicate
import play.api.mvc.{AnyContent, Request}

trait ConfirmClientController extends BaseAgentController {

  override protected def baseAgentPredicates: AuthPredicate[IncomeTaxAgentUser] = AgentAuthenticationPredicate.clientDetailsPredicates

  def fetchClientName(implicit request: Request[AnyContent]): Option[String] =
    (request.session.get(SessionKeys.clientFirstName),
      request.session.get(SessionKeys.clientLastName)) match {
      case (Some(f), Some(l)) =>
        Some(f + " " + l)
      case _ => None
    }

  def fetchClientUTR(implicit request: Request[AnyContent]): Option[String] =
    request.session.get(SessionKeys.clientUTR)

}
