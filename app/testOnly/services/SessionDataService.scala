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

package testOnly.services

import auth.MtdItUser
import play.api.http.Status.OK
import testOnly.connectors.SessionDataConnector
import testOnly.models.SessionDataModel
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import utils.Utilities.ToFutureSuccessful

class SessionDataService @Inject()(sessionDataConnector: SessionDataConnector) {

  def getSessionData(sessionId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, SessionDataModel]] =
    sessionDataConnector.getSessionData(sessionId).map { response =>
      response.status match {
        case OK => response.json.validate[SessionDataModel].fold(
          invalid => Left(new Exception(s"Json validation error for SessionDataModel. Invalid: $invalid")),
          valid => Right(valid)
        )
        case _ => Left(new Exception(s"Unknown exception. Status: ${response.status}, Json: ${response.json}"))
      }
    }

  def postSessionData(isAgent: Boolean, sessionId: SessionId)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext)
  : Future[Either[Throwable, String]] = {

    user.saUtr match {
      case Some(saUtr) =>
        sessionDataConnector.postSessionData(postSessionDataModel(isAgent, user, sessionId, saUtr)).map { response =>
          response.status match {
            case OK => Right(sessionId.value)
            case _ => Left(new Exception(s"Unknown exception. Status: ${response.status}, Json: ${response.json}"))
          }
        }
      case None => ( (Left(new Exception(s"User had no saUtr!"))) ).asFuture 
    }
  }

  private def postSessionDataModel(isAgent: Boolean, user: MtdItUser[_], sessionId: SessionId, saUtr: String): SessionDataModel = {

    val affinityGroup = if (isAgent) "Agent" else "Individual"

    SessionDataModel(
      sessionID = sessionId.value,
      mtditid = user.mtditid,
      nino = user.nino,
      saUtr = saUtr,
      clientFirstName = user.userName.flatMap(_.name),
      clientLastName = user.userName.flatMap(_.lastName),
      userType = affinityGroup
    )
  }

}
