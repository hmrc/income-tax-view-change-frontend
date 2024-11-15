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

import auth.{MtdItUser, MtdItUserOptionNino, MtdItUserWithNino}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.agent.sessionUtils.SessionKeys
import controllers.predicates.AuthPredicate.AuthPredicate
import controllers.predicates.IncomeTaxAgentUser
import controllers.predicates.agent.AgentAuthenticationPredicate
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.Future

trait ClientConfirmedController extends BaseAgentController {

  override protected def baseAgentPredicates: AuthPredicate[IncomeTaxAgentUser] = AgentAuthenticationPredicate.confirmedClientPredicates

  val mcc: MessagesControllerComponents

  def getClientUtr(implicit request: Request[_]): Option[String] = {
    request.session.get(SessionKeys.clientUTR)
  }

  def getClientMtditid(implicit request: Request[_]): String = {
    request.session.get(SessionKeys.clientMTDID)
      .getOrElse(throw new InternalServerException("client mtditid not found"))
  }

  def getClientNino(implicit request: Request[_]): String = {
    request.session.get(SessionKeys.clientNino)
      .getOrElse(throw new InternalServerException("client nino not found"))
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
    implicit user: IncomeTaxAgentUser, request: Request[AnyContent]): MtdItUserWithNino[AnyContent] = {
    MtdItUserWithNino(
      mtditid = getClientMtditid, nino = getClientNino, userName = None,
      saUtr = getClientUtr, credId = user.credId, userType = Some(Agent), arn = user.agentReferenceNumber, optClientName = getClientName
    )
  }

  def getMtdItUserWithIncomeSources(incomeSourceDetailsService: IncomeSourceDetailsService)(
    implicit user: IncomeTaxAgentUser, request: Request[AnyContent], hc: HeaderCarrier): Future[MtdItUser[AnyContent]] = {
    val userOptionNino: MtdItUserOptionNino[_] = MtdItUserOptionNino(
      getClientMtditid, Some(getClientNino), None, None, getClientUtr, user.credId, Some(Agent), user.agentReferenceNumber, getClientName
    )

    incomeSourceDetailsService.getIncomeSourceDetails()(hc = hc, mtdUser = userOptionNino) map {
      case model@IncomeSourceDetailsModel(nino, _, _, _, _) => MtdItUser(
        userOptionNino.mtditid, nino, userOptionNino.userName, model, None, userOptionNino.saUtr,
        userOptionNino.credId, userOptionNino.userType, userOptionNino.arn, userOptionNino.optClientName)
      case _ =>
        Logger("application").error("Failed to retrieve income sources for agent")
        throw new InternalServerException("IncomeSourceDetailsModel not created")
    }
  }

  def showInternalServerError(isAgent: Boolean)
                             (implicit user: MtdItUser[_],
                              itvcErrorHandler: ItvcErrorHandler,
                              itvcErrorHandlerAgent: AgentItvcErrorHandler
                             ): Result = {
    (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler)
      .showInternalServerError()
  }
}
