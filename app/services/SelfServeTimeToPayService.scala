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

package services

import connectors.SelfServeTimeToPayConnector
import models.core.{SelfServeTimeToPayJourneyErrorResponse, SelfServeTimeToPayJourneyResponseModel}
import play.api.Logger
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class SelfServeTimeToPayService @Inject()(
                                           selfServeTimeToPayConnector: SelfServeTimeToPayConnector
                                         )(implicit ec: ExecutionContext) {

  def startSelfServeTimeToPayJourney()(implicit hc: HeaderCarrier): Future[Either[SelfServeTimeToPayJourneyErrorResponse,String]] = {
    selfServeTimeToPayConnector.startSelfServeTimeToPayJourney()
      .map {
        case SelfServeTimeToPayJourneyResponseModel(_, nextUrl) => Right(nextUrl)

        case SelfServeTimeToPayJourneyErrorResponse(status, message) =>
          Left(SelfServeTimeToPayJourneyErrorResponse(status, message))
      }
      .recover {
        case ex: Exception =>
          Left(SelfServeTimeToPayJourneyErrorResponse(INTERNAL_SERVER_ERROR, s"Unexpected future failed error"))
      }
  }
}

