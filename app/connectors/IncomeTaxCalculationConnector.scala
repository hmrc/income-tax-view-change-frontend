/*
 * Copyright 2022 HM Revenue & Customs
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
import models.calculation.{Calculation, CalculationErrorModel, CalculationResponseModel, ListCalculationItems}
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse, LiabilityCalculationResponseModel}
import play.api.Logger
import play.api.http.Status._
import testOnly.models.TestHeadersModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeTaxCalculationConnector @Inject()(http: HttpClient,
                                              config: FrontendAppConfig) extends RawResponseReads {
  val baseUrl: String = config.incomeTaxCalculationService

  def getCalculationResponseUrl(nino: String): String =  s"$baseUrl/income-tax/nino/$nino"

  def getCalculationResponse(nino: String, taxYear: String)(implicit headerCarrier: HeaderCarrier,
                                                            ec: ExecutionContext): Future[LiabilityCalculationResponseModel] = {

    http.GET[HttpResponse](getCalculationResponseUrl(nino), Seq(("taxYear", taxYear))) map { response =>
      response.status match {
        case OK =>
          response.json.validate[LiabilityCalculationResponse].fold(
            invalid => {
              Logger("application").error(
                s"[IncomeTaxCalculationConnector][getCalculationResponse] - Json validation error parsing calculation response, error $invalid")
              LiabilityCalculationError(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxCalculationConnector][getCalculationResponse] - Response status: ${response.status},body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxCalculationConnector][getCalculationResponse] - Response status: ${response.status}, body: ${response.body}")
          }
          LiabilityCalculationError(response.status, response.body)
      }
    }
  }

}
