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
import play.api.http.Status.{CONFLICT, OK}
import testOnly.connectors.SessionDataConnector
import testOnly.models.sessionData.SessionDataPostSuccess
import testOnly.models.{SessionDataModel, SessionDataRetrieval}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SessionDataService @Inject()(sessionDataConnector: SessionDataConnector) {

  def getSessionData()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, SessionDataRetrieval]] = {
    sessionDataConnector.getSessionData().map { response =>
      response.status match {
        case OK => response.json.validate[SessionDataRetrieval].fold(
          invalid => Left(new Exception(s"Json validation error for SessionDataModel. Invalid: $invalid")),
          valid => Right(valid)
        )
        case _ => Left(new Exception(s"Unknown exception. Status: ${response.status}, Json: ${response.json}"))
      }
    }
  }

  def postSessionData()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext)
  : Future[Either[Throwable, SessionDataPostSuccess]] = {

    user.saUtr match {
      case Some(utr) =>
        sessionDataConnector.postSessionData(postSessionDataModel(user, utr)).map { response =>
          response.status match {
            case status if status == OK || status == CONFLICT => Right(SessionDataPostSuccess(status, user.mtditid))
            case status => Left(new Exception(s"User session could not be saved. status: $status"))
          }
        }
      case None => Future.successful(Left(new Exception(s"User had no saUtr!")))
    }
  }

  private def postSessionDataModel(user: MtdItUser[_], utr: String): SessionDataModel = {
    SessionDataModel(
      mtditid = user.mtditid,
      nino = user.nino,
      utr = utr
    )
  }

}
