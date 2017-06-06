/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import connectors.ObligationDataConnector
import models.{ErrorResponse, SuccessResponse}
import play.api.Logger
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class ObligationsService @Inject()(val obligationDataConnector: ObligationDataConnector) {

  def getObligations(nino: String)(implicit hc: HeaderCarrier) = Future {

    Logger.debug(s"[ObligationsService][getObligations] - Requesting Obligation details from connectors for user with NINO: $nino")

    for {

      selfEmploymentId <- obligationDataConnector.getBusinessList(nino).map {
        case success: SuccessResponse =>
          Logger.debug(s"[ObligationsService][getObligations] - Retrieved business details for user with NINO: $nino")
          //retrieve the business ID .map
            //success
            Logger.debug(s"[ObligationsService][getObligations] - Found Self Employment ID")

            //error
            Logger.debug(s"[ObligationsService][getObligations] - Could not find Self Employment ID")
        case error: ErrorResponse =>
      }
    }

      Future.successful(Ok)

  }

}
