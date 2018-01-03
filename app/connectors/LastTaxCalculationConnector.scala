/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import config.FrontendAppConfig
import models._
import play.api.Logger
import play.api.http.Status
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class LastTaxCalculationConnector @Inject()(val http: HttpClient,
                                            val config: FrontendAppConfig) extends RawResponseReads {

  lazy val getEstimatedTaxLiabilityUrl: (String, String) => String = (nino, year) =>
    s"${config.itvcProtectedService}/income-tax-view-change/estimated-tax-liability/$nino/$year/it"

  def getLastEstimatedTax(nino: String, year: Int)(implicit headerCarrier: HeaderCarrier): Future[LastTaxCalculationResponseModel] = {

    val url = getEstimatedTaxLiabilityUrl(nino, year.toString)
    Logger.debug(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - GET $url")

    http.GET[HttpResponse](url) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[LastTaxCalculation].fold(
              invalid => {
                Logger.warn(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - Json Validation Error. Parsing Latest Calc Response")
                LastTaxCalculationError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Latest Calc Response")
              },
              valid => valid
            )
          case NOT_FOUND =>
            Logger.debug(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - No Data Found response")
            NoLastTaxCalculation
          case _ =>
            Logger.debug(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.warn(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - Response status: [${response.status}] from Latest Calc call")
            LastTaxCalculationError(response.status, response.body)
        }
    } recover {
      case _ =>
        Logger.warn(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - Unexpected future failed error")
        LastTaxCalculationError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }
}
