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
import models.core.ErrorModel
import play.api.http.Status.INTERNAL_SERVER_ERROR
import testOnly.connectors.SessionDataConnector
import testOnly.models.sessionData.SessionDataPostResponse.{SessionDataPostFailure, SessionDataPostResponse}
import testOnly.models.{SessionDataModel, SessionDataRetrieval}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SessionDataService @Inject()(sessionDataConnector: SessionDataConnector) {

  def getSessionData()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ErrorModel, SessionDataRetrieval]] = {
    sessionDataConnector.getSessionData()
  }

  def postSessionData()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext)
  : Future[SessionDataPostResponse] = {

    user.saUtr match {
      case Some(utr) => sessionDataConnector.postSessionData(postSessionDataModel(user, utr))
      case None => Future.successful(Left(SessionDataPostFailure(INTERNAL_SERVER_ERROR, "User had no saUtr!")))
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
