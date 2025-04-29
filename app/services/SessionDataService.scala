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

package services

import connectors.SessionDataConnector
import controllers.agent.sessionUtils.SessionKeys
import models.sessionData.SessionDataGetResponse.{SessionDataGetSuccess, SessionDataNotFound, SessionGetResponse}
import models.sessionData.SessionDataModel
import models.sessionData.SessionDataPostResponse.SessionDataPostResponse
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SessionDataService @Inject()(sessionDataConnector: SessionDataConnector)
                                  (implicit ec: ExecutionContext){

  def getSessionData(useCookie: Boolean = false)
                    (implicit request: Request[_],
                     hc: HeaderCarrier): Future[SessionGetResponse] = {
    if(useCookie) {
      getSessionResponseFromCookie
    } else {
      sessionDataConnector.getSessionData()
    }
  }

  def postSessionData(sessionDataModel: SessionDataModel)(implicit hc: HeaderCarrier): Future[SessionDataPostResponse] = {
    sessionDataConnector.postSessionData(sessionDataModel)
  }

  private def getSessionResponseFromCookie(implicit request: Request[_]): Future[SessionGetResponse] = {
    val optMtdid = request.session.get(SessionKeys.clientMTDID)
    val optUtr = request.session.get(SessionKeys.clientUTR)
    val optNino = request.session.get(SessionKeys.clientNino)

    (optMtdid, optUtr, optNino) match {
      case (Some(mtdItId), Some(utr), Some(nino)) => Future(
        Right(
          SessionDataGetSuccess(
            mtditid = mtdItId,
            nino = nino,
            utr = utr,
            sessionId = "not required"
          )
        )
      )
      case _ => Future(Left(SessionDataNotFound("Cookie does not contain agent data")))
    }
  }

}
