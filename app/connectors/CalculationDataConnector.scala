/*
 * Copyright 2019 HM Revenue & Customs
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
import models.calculation._
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class CalculationDataConnector @Inject()(val http: HttpClient,
                                         val config: FrontendAppConfig) extends RawResponseReads {

  private[connectors] lazy val getCalculationDataUrl: (String, String) => String =
    (nino, taxCalculationId) => s"${config.saApiService}/ni/$nino/calculations/$taxCalculationId"

  private[connectors] lazy val getLatestCalculationUrl: (String, String) => String =
    (nino, taxYear) => s"${config.itvcProtectedService}/income-tax-view-change/previous-tax-calculation/$nino/$taxYear"

  def getCalculationData(nino: String, taxCalculationId: String)(implicit headerCarrier: HeaderCarrier): Future[CalculationDataResponseModel] = {

    val url = getCalculationDataUrl(nino, taxCalculationId)
    Logger.debug(s"[CalculationDataConnector][getCalculationData] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"), implicitly) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[CalculationDataConnector][getCalculationData] - RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[CalculationDataModel].fold(
              invalid => {
                Logger.error(s"[CalculationDataConnector][getCalculationData] - Json Validation Error. Parsing Calc Breakdown Response. Invalid=$invalid")
                CalculationDataErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Calc Breakdown Response")
              },
              valid => valid
            )
          case _ =>
            Logger.error(s"[CalculationDataConnector][getCalculationData] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.error(s"[CalculationDataConnector][getCalculationData] - Response status: [${response.status}] returned from Calc Breakdown call")
            CalculationDataErrorModel(response.status, response.body)
        }
    } recover {
      case ex =>
        Logger.error(s"[CalculationDataConnector][getCalculationData] - Unexpected future failed error, ${ex.getMessage}")
        CalculationDataErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
      case _ =>
        Logger.error(s"[CalculationDataConnector][getCalculationData] - Unexpected future failed error")
        CalculationDataErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }

  def getLatestCalculation(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[CalculationResponseModel] = {

    val url = getLatestCalculationUrl(nino, taxYear.toString)
    Logger.debug(s"[CalculationDataConnector][getLatestCalculation] - GET $url")

    http.GET[HttpResponse](url)(httpReads, hc.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"), implicitly) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[CalculationDataConnector][getLatestCalculation] - Response status: ${response.status}, json: ${response.json}")
            response.json.validate[CalculationModel].fold(
              invalid => {
                Logger.error(s"[CalculationDataConnector][getLatestCalculation] - Json validation error parsing calculation model response. Invalid=$invalid")
                CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, "Json validation error parsing calculation model response")
              },
              valid => valid
            )
          case _ =>
            Logger.error(s"[CalculationDataConnector][getLatestCalculation] - Response status: ${response.status}, json: ${response.body}")
            Logger.error(s"[CalculationDataConnector][getLatestCalculation] - Response status: [${response.status}] returned from Latest Calculation call")
            CalculationErrorModel(response.status, response.body)
        }
    } recover {
      case ex =>
        Logger.error(s"[CalculationDataConnector][getLatestCalculation] - Unexpected future failed error, ${ex.getMessage}")
        CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
      case _ =>
        Logger.error(s"[CalculationDataConnector][getLatestCalculation] - Unexpected future failed error")
        CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error")
    }
  }
}
