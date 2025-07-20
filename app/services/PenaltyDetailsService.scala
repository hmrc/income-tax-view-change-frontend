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

import auth.MtdItUser
import config.FrontendAppConfig
import connectors.GetPenaltyDetailsConnector
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.penalties.GetPenaltyDetailsParser._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PenaltyDetailsService @Inject()(getPenaltyDetailsConnector: GetPenaltyDetailsConnector,
                                      val appConfig: FrontendAppConfig) {

  def getPenaltySubmissionFrequency(status: ITSAStatus): String = {
    status match {
      case ITSAStatus.Mandated | ITSAStatus.Voluntary => "Quarterly"
      case ITSAStatus.Annual => "Annual"
      case _ => "Non Penalty Applicable Status"
    }
  }

  def getPenaltyDetails(mtdItId: String)(implicit hc: HeaderCarrier): Future[GetPenaltyDetailsResponse] = {
    getPenaltyDetailsConnector.getPenaltyDetails(mtdItId)
  }

  def getPenaltiesCount(penaltiesCallEnabled: Boolean)(implicit user: MtdItUser[_],
                                                       hc: HeaderCarrier,
                                                       ec: ExecutionContext): Future[Int] = {
    if (penaltiesCallEnabled) {
      getPenaltyDetails(user.mtditid).map(_.fold(
        {
          case error: GetPenaltyDetailsFailureResponse =>
            throw new Exception(s"Get penalty details call failed with status of : ${error.status}")
          case GetPenaltyDetailsMalformed =>
            throw new Exception("Get penalty details call failed with a malformed response body")
        },
        success => success.penaltyDetails.lateSubmissionPenalty.map(_.summary.activePenaltyPoints).getOrElse(0))
      )
    } else Future.successful(0)
  }
}
