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

package services.helpers

import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import org.apache.pekko.util.Timeout
import play.api.Logger
import play.api.http.Status
import services.CalculationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

trait CalculationServiceHelper {

  // TODO: ???
  implicit val timeout : Timeout = 1.second
  implicit val calculationService: CalculationService

  //Waits for polling interval time to complete and responds with response code from calculation service
  def getCalculationResponse(endTimeForEachInterval: Long,
                                     endTimeInMillis: Long,
                                     calcId: String,
                                     nino: String,
                                     taxYear: Int,
                                     mtditid: String
                                    )
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    for {
      result <-
        calculationService.getLatestCalculation(mtditid, nino, calcId, taxYear).map {
          case _: LiabilityCalculationResponse => Status.OK
          case error: LiabilityCalculationError =>
            if (System.currentTimeMillis() > endTimeInMillis) Status.INTERNAL_SERVER_ERROR
            else error.status
        }
    } yield result
  }
}
