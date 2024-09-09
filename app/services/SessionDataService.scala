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
import models.sessionData.SessionDataModel
import models.sessionData.SessionDataPostResponse.SessionDataPostResponse
import testOnly.models.SessionDataGetResponse.SessionGetResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.Future

class SessionDataService @Inject()(sessionDataConnector: SessionDataConnector) {

  def getSessionData()(implicit hc: HeaderCarrier): Future[SessionGetResponse] = {
    sessionDataConnector.getSessionData()
  }

  def postSessionData(sessionDataModel: SessionDataModel)(implicit hc: HeaderCarrier): Future[SessionDataPostResponse] = {
    sessionDataConnector.postSessionData(sessionDataModel)
  }

}
