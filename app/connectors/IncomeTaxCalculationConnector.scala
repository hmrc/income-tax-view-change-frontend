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

package connectors

import config.FrontendAppConfig
import enums.TaxYearSummary.CalculationRecord
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse, LiabilityCalculationResponseModel}
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeTaxCalculationConnector @Inject()(http: HttpClientV2, config: FrontendAppConfig) extends RawResponseReads {

  val baseUrl: String = config.incomeTaxCalculationService

  def getCalculationResponseUrl(nino: String): String = s"$baseUrl/income-tax-calculation/income-tax/nino/$nino/calculation-details"

  def getCalculationResponseByCalcIdUrl(nino: String, calcId: String): String =
    s"$baseUrl/income-tax-calculation/income-tax/nino/$nino/calc-id/$calcId/calculation-details"

  def getCalculationResponse(
                              mtditid: String,
                              nino: String,
                              taxYear: String,
                              calculationRecord: Option[CalculationRecord]
                            )(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[LiabilityCalculationResponseModel] = {

    val queryParams = Map("taxYear" -> taxYear) ++ calculationRecord.map("calculationRecord" -> _)
    val url = url"${getCalculationResponseUrl(nino)}?$queryParams"

    http.get(url)
      .setHeader("mtditid" -> mtditid)
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          response.json.validate[LiabilityCalculationResponse].fold(
            invalid => {
              Logger("application").error(
                s"Json validation error parsing calculation response, error $invalid")
              LiabilityCalculationError(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"Response status: ${response.status},body: ${response.body}")
          } else {
            Logger("application").warn(s"Response status: ${response.status}, body: ${response.body}")
          }
          LiabilityCalculationError(response.status, response.body)
      }
    }
  }

  def getCalculationResponseByCalcId(mtditid: String, nino: String, calcId: String, taxYear: Int)
                                    (implicit headerCarrier: HeaderCarrier,
                                     ec: ExecutionContext): Future[LiabilityCalculationResponseModel] = {
    http.get(url"${getCalculationResponseByCalcIdUrl(nino, calcId)}?taxYear=${taxYear.toString}")
      .setHeader("mtditid" -> mtditid)
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          response.json.validate[LiabilityCalculationResponse].fold(
            invalid => {
              Logger("application").error(
                s"Json validation error parsing calculation response, error $invalid")
              LiabilityCalculationError(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(
              s"Response status: ${response.status},body: ${response.body}")
          } else {
            Logger("application").warn(
              s"Response status: ${response.status}, body: ${response.body}")
          }
          LiabilityCalculationError(response.status, response.body)
      }
    }
  }
}
