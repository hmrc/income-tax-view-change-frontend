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

package controllers.agent.predicates

import auth.{MtdItUser, MtdItUserWithNino}
import controllers.agent.utils.SessionKeys
import controllers.predicates.AuthPredicate.AuthPredicate
import controllers.predicates.IncomeTaxAgentUser
import controllers.predicates.agent.AgentAuthenticationPredicate
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, InternalServerException}

import scala.concurrent.Future

trait ClientConfirmedController extends BaseAgentController {

  override protected def baseAgentPredicates: AuthPredicate[IncomeTaxAgentUser] = AgentAuthenticationPredicate.confirmedClientPredicates

  val mcc: MessagesControllerComponents

  def getClientUtr(implicit request: Request[_]): Option[String] = {
    request.session.get(SessionKeys.clientUTR)
  }

  def getClientMtditid(implicit request: Request[_]): String = {
    request.session.get(SessionKeys.clientMTDID)
      .getOrElse(throw new InternalServerException("[ClientConfirmedController][getClientMtditid] client mtditid not found"))
  }

  def getClientNino(implicit request: Request[_]): String = {
    request.session.get(SessionKeys.clientNino)
      .getOrElse(throw new InternalServerException("[ClientConfirmedController][getClientNino] client nino not found"))
  }

  def getClientName(implicit request: Request[_]): Option[Name] = {
    val firstName = request.session.get(SessionKeys.clientFirstName)
    val lastName = request.session.get(SessionKeys.clientLastName)

    if (firstName.isDefined && lastName.isDefined) {
      Some(Name(firstName, lastName))
    } else {
      None
    }
  }

  def getMtdItUserWithNino()(
    implicit user: IncomeTaxAgentUser, request: Request[AnyContent], hc: HeaderCarrier): MtdItUserWithNino[AnyContent] = {
    MtdItUserWithNino(
      mtditid = getClientMtditid, nino = getClientNino, userName = getClientName,
      saUtr = getClientUtr, credId = user.credId, userType = Some(Agent), arn = user.agentReferenceNumber
    )
  }

  def getMtdItUserWithIncomeSources(incomeSourceDetailsService: IncomeSourceDetailsService)(
    implicit user: IncomeTaxAgentUser, request: Request[AnyContent], hc: HeaderCarrier): Future[MtdItUser[AnyContent]] = {
    val userWithNino: MtdItUserWithNino[_] = MtdItUserWithNino(
      getClientMtditid, getClientNino, getClientName, None, getClientUtr, user.credId, Some(Agent), user.agentReferenceNumber
    )

    val cacheKey = None
    incomeSourceDetailsService.getIncomeSourceDetails(cacheKey)(hc = hc, mtdUser = userWithNino) map {
      case model@IncomeSourceDetailsModel(_, _, _, _) => MtdItUser(
        userWithNino.mtditid, userWithNino.nino, userWithNino.userName, model, None, userWithNino.saUtr,
        userWithNino.credId, userWithNino.userType, userWithNino.arn)
      case _ =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getIncomeSources] - Failed to retrieve income sources for agent")
        throw new InternalServerException("[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created")
    }
  }
}
