/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import models.calculation.{Calculation, CalculationErrorModel, CalculationResponseModel, ListCalculationItems}
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IndividualCalculationsConnector @Inject()(val http: HttpClient,
                                                val config: FrontendAppConfig) extends RawResponseReads {
  val baseUrl: String = config.individualCalculationsService

  def listCalculationsUrl(nino: String): String = s"$baseUrl/$nino/self-assessment"

  def getLatestCalculationId(nino: String, taxYear: String)(implicit headerCarrier: HeaderCarrier,
                                                            ec: ExecutionContext): Future[Either[CalculationResponseModel, String]] = {
    http.GET[HttpResponse](listCalculationsUrl(nino), Seq(("taxYear", taxYear)))(httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"), ec) map {
      response =>
        response.status match {
          case OK => response.json.validate[ListCalculationItems].fold(
            invalid => {
              Logger.error(s"[IndividualCalculationsConnector][getLatestCalculationId] - Json validation error parsing calculation list response, error: $invalid.")
              Left(CalculationErrorModel(INTERNAL_SERVER_ERROR, s"Json validation error parsing calculation list response"))
            },
            valid => {
              Logger.debug("[IndividualCalculationsConnector][getLatestCalculationId] - Successfully parsed calculation list response")
              Right(valid.calculations.sortWith((x, y) => x.calculationTimestamp.isAfter(y.calculationTimestamp)).head.id)
            }
          )
          case NOT_FOUND =>
            Logger.warn(s"[IndividualCalculationsConnector][getLatestCalculationId] - No calculation found for tax year $taxYear")
            Left(CalculationErrorModel(NOT_FOUND, s"No calculation found for tax year $taxYear"))
          case status =>
            if (status >= 500) {
              Logger.error(s"[IndividualCalculationsConnector][getLatestCalculationId] - Response status: ${response.status}, json: ${response.body}")
            } else {
              Logger.warn(s"[IndividualCalculationsConnector][getLatestCalculationId] - Response status: ${response.status}, json: ${response.body}")
            }
            Left(CalculationErrorModel(response.status, response.body))
        }
    }
  }

  def getCalculationUrl(nino: String, calculationId: String) = s"$baseUrl/$nino/self-assessment/$calculationId"

  def getCalculation(nino: String, calculationId: String)(implicit headerCarrier: HeaderCarrier,
                                                          ec: ExecutionContext): Future[Either[CalculationErrorModel, Calculation]] = {
    http.GET[HttpResponse](getCalculationUrl(nino, calculationId))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK => response.json.validate[Calculation].fold(
          invalid => {
            Logger.error(s"[IndividualCalculationsConnector][getCalculation] - Json validation error parsing calculation response, error $invalid")
            Left(CalculationErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation response"))
          },
          valid => Right(valid)
        )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger.error(s"[IndividualCalculationsConnector][getCalculation] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger.warn(s"[IndividualCalculationsConnector][getCalculation] - Response status: ${response.status}, body: ${response.body}")
          }
          Left(CalculationErrorModel(response.status, response.body))
      }
    }
  }

}
