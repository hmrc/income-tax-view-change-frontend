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

package testOnly.connectors

import connectors.RawResponseReads
import testOnly.TestOnlyAppConfig
import testOnly.models.PenaltiesDataModel
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PenaltiesStubConnector @Inject()(val appConfig: TestOnlyAppConfig,
                                       val http: HttpClientV2
                                      )(implicit ec: ExecutionContext) extends RawResponseReads {

  def addPenaltiesData(dataModel: PenaltiesDataModel)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.penaltiesStubUrl}/penalties-stub/insert/penalty/details/ITSA/NINO/${dataModel.nino}"
    http.post(url"$url")
      .withBody(dataModel.response)
      .execute[HttpResponse]
  }

  def deletePenaltiesData(nino: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.penaltiesStubUrl}/penalties-stub/insert/penalty/details/ITSA/NINO/$nino"
    http.delete(url"$url")
      .execute[HttpResponse]
  }

  def addPenaltiesFinancialData(dataModel: PenaltiesDataModel)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.penaltiesStubUrl}/penalties-stub/insert/financial-data/details/ITSA/NINO/${dataModel.nino}"
    http.post(url"$url")
      .withBody(dataModel.response)
      .execute[HttpResponse]
  }

  def deletePenaltiesFinancialData(nino: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.penaltiesStubUrl}/penalties-stub/insert/financial-data/details/ITSA/NINO/$nino"
    http.delete(url"$url")
      .execute[HttpResponse]
  }
}
