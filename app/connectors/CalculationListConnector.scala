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
import models.calculationList.{CalculationListErrorModel, CalculationListModel, CalculationListResponseModel}
import models.core.Nino
import models.createIncomeSource._
import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, Ok}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationListConnector @Inject()(val http: HttpClient,
                                         val appConfig: FrontendAppConfig
                                        )(implicit val ec: ExecutionContext) extends RawResponseReads {

  def getLegacyCalculationListUrl(nino: String, taxYearEnd: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/list-of-calculation-results/$nino/$taxYearEnd"
  }

  def getCalculationListUrl(nino: String, taxYearRange: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/calculation-list/$nino/$taxYearRange"
  }

  def getOverwriteCalculationListUrl(nino: String, taxYearRange: String, crystallisationStatus: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/calculation-list/$nino/$taxYearRange/overwrite/$crystallisationStatus"
  }

  def getLegacyCalculationList(nino: Nino, taxYearEnd: String)
                              (implicit headerCarrier: HeaderCarrier): Future[CalculationListResponseModel] = {

    http.GET[HttpResponse](getLegacyCalculationListUrl(nino.value, taxYearEnd))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          response.json.validate[CalculationListModel].fold(
            invalid => {
              Logger("application").error("[IncomeTaxViewChangeConnector][getLegacyCalculationList] - " +
                s"Json validation error parsing legacy calculation list response, error $invalid")
              CalculationListErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing legacy calculation list response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getLegacyCalculationList] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getLegacyCalculationList] - Response status: ${response.status}, body: ${response.body}")
          }
          CalculationListErrorModel(response.status, response.body)
      }
    }
  }

  def getCalculationList(nino: Nino, taxYearRange: String)
                        (implicit headerCarrier: HeaderCarrier): Future[CalculationListResponseModel] = {

    http.GET[HttpResponse](getCalculationListUrl(nino.value, taxYearRange))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          response.json.validate[CalculationListModel].fold(
            invalid => {
              Logger("application").error("[IncomeTaxViewChangeConnector][getCalculationList] - " +
                s"Json validation error parsing calculation list response, error $invalid")
              CalculationListErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation list response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getCalculationList] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getCalculationList] - Response status: ${response.status}, body: ${response.body}")
          }
          CalculationListErrorModel(response.status, response.body)
      }
    }
  }

  def overwriteCalculationList(nino: Nino, taxYearRange: String, crystallisationStatus: String)
                              (implicit headerCarrier: HeaderCarrier): Future[Result] = {

    // TODO: remove
    val url = getOverwriteCalculationListUrl(nino.value, taxYearRange, crystallisationStatus)
    println("BBBBBBBBBB" + url)

    http.GET[HttpResponse](getOverwriteCalculationListUrl(nino.value, taxYearRange, crystallisationStatus))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          Ok("Overwrite successful")
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][overwriteCalculationList] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][overwriteCalculationList] - Response status: ${response.status}, body: ${response.body}")
          }
          InternalServerError("Overwrite unsuccessful")
      }
    }
  }
}