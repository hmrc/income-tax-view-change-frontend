/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors

import config.FrontendAppConfig
import models.penalties.GetPenaltyDetailsParser.{GetPenaltyDetailsReads, GetPenaltyDetailsResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetPenaltyDetailsConnector @Inject() (val httpClientV2: HttpClientV2,
                                            val appConfig: FrontendAppConfig
                                           )(implicit val executionContext: ExecutionContext) {

  /*
    Connector to be updated in future to use NINO identifier
   */

  private val incomeTaxPenaltyDetailsUrl: String = appConfig.penaltiesBackendBase + s"/penalties/ITSA/etmp/penalties/MTDITID/"

  def getPenaltyDetails(mtdItId: String)(implicit headerCarrier: HeaderCarrier): Future[GetPenaltyDetailsResponse] = {
    val url = incomeTaxPenaltyDetailsUrl + mtdItId
    httpClientV2
      .get(url"$url")
      .execute[GetPenaltyDetailsResponse](GetPenaltyDetailsReads, executionContext)
  }

}
