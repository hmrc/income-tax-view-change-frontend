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

package utils

import config.{AgentItvcErrorHandler, FrontendAppConfig}
import models.sessionData.SessionCookieData
import models.sessionData.SessionDataPostResponse.{SessionDataPostFailure, SessionDataPostSuccess}
import play.api.Logger
import play.api.mvc.{Request, Result}
import services.SessionDataService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait SessionCookieUtil {
  self =>

  val sessionDataService: SessionDataService
  val itvcErrorHandler: AgentItvcErrorHandler
  val appConfig: FrontendAppConfig

  def getSessionDataStorageFS: Boolean = appConfig.isSessionDataStorageEnabled

  def handleSessionCookies(sessionCookieData: SessionCookieData)(codeBlock: Seq[(String, String)] => Future[Result])
                          (implicit hc: HeaderCarrier, request: Request[_], ec: ExecutionContext): Future[Result] = {
    if (getSessionDataStorageFS) {
      sessionDataService.postSessionData(sessionCookieData.toSessionDataModel).flatMap {
        case Left(value: SessionDataPostFailure) =>
          Logger("application").error(s"[Agent] Posting user session data was unsuccessful. Status: ${value.status}, error message: ${value.errorMessage}")
          Future.successful(itvcErrorHandler.showInternalServerError())
        case Right(value: SessionDataPostSuccess) =>
          Logger("application").info(s"[Agent] Posting user session data was successful. Status: ${value.status}")
          codeBlock(sessionCookieData.toSessionCookieSeq)
      }
    } else {
      Logger("application").info(s"[Agent] GetUserSessionApi feature switch was off so session data has not been posted to the session-data service")
      codeBlock(sessionCookieData.toSessionCookieSeq)
    }
  }
}
