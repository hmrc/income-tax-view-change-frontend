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

import java.io.Serializable
import javax.inject.{Inject, Singleton}

import connectors.ObligationDataConnector
import models._
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class ObligationsService @Inject()(val obligationDataConnector: ObligationDataConnector) {

  def getObligations(nino: String)(implicit hc: HeaderCarrier): Future[Any] = {

    Logger.debug(s"[ObligationsService][getObligations] - Requesting Obligation details from connectors for user with NINO: $nino")

    for {
      selfEmploymentId <- getSelfEmploymentId(nino)
      obligations <-  obligationDataConnector.getObligationData(nino, selfEmploymentId)
    } yield obligations
  }.recoverWith {
    //TODO catch appropriate set of exceptions here
    case s: Exception =>Future(InternalServerError(s.getMessage))
  }



  private[ObligationsService] def getSelfEmploymentId(nino: String) = {

    obligationDataConnector.getBusinessList(nino).map {
      case success: SuccessResponse =>
        Logger.debug(s"[ObligationsService][getObligations] - Retrieved business details for user with NINO: $nino")
        success.json.as[BusinessListModel].business.map {
          _.id match {
            case Some(id) =>
              Logger.debug("[ObligationsService][getObligations] - Retrieved Self Employment ID")
              id
            case _ =>
              //TODO handle no ID found for business exception and log appropriate message
              Logger.debug("[ObligationsService][getObligations] - Self employment ID not present.  Throwing exception")
              throw new Exception("")
          }
        }.head
      case error: ErrorResponse =>
        //TODO handle no ID found for business exception and log appropriate message
        Logger.debug(s"[ObligationsService][getObligations] Could not retrieve business details for user with NINO: $nino")
        throw new Exception("")
    }
  }

  private def getObligationData(nino: String, selfEmploymentId: String) = {
    obligationDataConnector.getObligationData(nino, selfEmploymentId).map {

    }
  }

}
