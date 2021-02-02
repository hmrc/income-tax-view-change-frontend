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

package controllers.agent.predicates

import auth.BaseFrontendController
import controllers.predicates.AuthPredicate.AuthPredicate
import controllers.predicates.IncomeTaxAgentUser
import controllers.predicates.agent.AgentAuthenticationPredicate
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolments}

trait SelectClientController extends BaseFrontendController {

  val mcc: MessagesControllerComponents

  protected def baseAgentPredicates: AuthPredicate[IncomeTaxAgentUser] = AgentAuthenticationPredicate.defaultPredicates

  object Authenticated extends AuthenticatedActions[IncomeTaxAgentUser] {

    override def userApply: (Enrolments, Option[AffinityGroup], ConfidenceLevel) => IncomeTaxAgentUser = IncomeTaxAgentUser.apply

    override def async: AuthenticatedAction[IncomeTaxAgentUser] = asyncInternal(baseAgentPredicates)

  }

}
