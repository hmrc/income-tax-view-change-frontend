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

import auth.MtdItUser
import models.incomeSourceDetails.UIJourneySessionData
import play.api.mvc.Result
import services.SessionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait OptOutJourney {
  self =>
  val sessionService: SessionService

  implicit val ec: ExecutionContext

  def withSessionData(codeBlock: UIJourneySessionData => Future[Result])(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    val journeyType = "OPTOUT"
    sessionService.getMongo(journeyType).flatMap {
      case Right(Some(data: UIJourneySessionData)) => codeBlock(data)
      case Right(None) =>
        sessionService.createSession(journeyType).flatMap { _ =>
          val data = UIJourneySessionData(hc.sessionId.get.value, journeyType)
          codeBlock(data)
        }
      case Left(ex) => ???
    }
  }
}
