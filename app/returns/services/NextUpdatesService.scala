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

package returns.services

import common.auth.MtdItUser
import common.models.obligations.{ObligationsErrorModel, ObligationsModel, ObligationsResponseModel}
import common.services.DateServiceInterface
import shared.connectors.ObligationsConnector
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NextUpdatesService @Inject()(
                                    val obligationsConnector: ObligationsConnector
                                  )(implicit ec: ExecutionContext, val dateService: DateServiceInterface) {


  def getAllObligationsWithinDateRange(fromDate: LocalDate, toDate: LocalDate)(
    implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ObligationsResponseModel] = {
    obligationsConnector.getAllObligationsDateRange(fromDate, toDate).map {
      case obligationsResponse: ObligationsModel =>
        ObligationsModel(obligationsResponse.obligations.filter(_.obligations.nonEmpty))
      case error: ObligationsErrorModel =>
        error
    }
  }
}


