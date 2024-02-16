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
import models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseError, ITSAStatusResponseModel}
import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ITSAStatusConnector @Inject()(val http: HttpClient,
                                    val appConfig: FrontendAppConfig
                                           )(implicit val ec: ExecutionContext) extends RawResponseReads {

  def getITSAStatusDetailUrl(taxableEntityId: String, taxYear: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/itsa-status/status/$taxableEntityId/$taxYear"
  }

  def getITSAStatusDetail(nino: String, taxYear: String, futureYears: Boolean, history: Boolean)
                         (implicit headerCarrier: HeaderCarrier): Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] = {
    val url = getITSAStatusDetailUrl(nino, taxYear)
    val queryParams = Seq("futureYears" -> futureYears.toString, "history" -> history.toString)
    http.GET[HttpResponse](url = url, queryParams = queryParams)(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          response.json.validate[List[ITSAStatusResponseModel]].fold(
            invalid => {
              Logger("application").error(s"[ITSAStatusConnector][getITSAStatusDetail] - Json validation error parsing repayment response, error $invalid")
              Left(ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing ITSA Status response"))
            },
            valid => Right(valid)
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[ITSAStatusConnector][getITSAStatusDetail]xxx - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[ITSAStatusConnector][getITSAStatusDetail] - Response status: ${response.status}, body: ${response.body}")
          }
          Left(ITSAStatusResponseError(response.status, response.body))
      }
    }
  }

  def getOverwriteItsaStatusUrl(nino: String, taxYearRange: String, itsaStatus: String): String = {
    s"${appConfig.itvcDynamicStubUrl}/income-tax-view-change/itsa-status/$nino/$taxYearRange/overwrite/$itsaStatus"
  }


  def overwriteItsaStatus(nino: Nino, taxYearRange: String, itsaStatus: String)
                              (implicit headerCarrier: HeaderCarrier): Future[Either[Throwable, Result]] = {


    http.GET[HttpResponse](getOverwriteItsaStatusUrl(nino.value, taxYearRange, itsaStatus))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          Right(Ok("Overwrite successful"))
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[ITSAStatusConnector][overwriteItsaStatus] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[ITSAStatusConnector][overwriteItsaStatus] - Response status: ${response.status}, body: ${response.body}")
          }
          Left(new Exception(s"Overwrite unsuccessful. ~ Response status: ${response.status} ~. < Response body: ${response.body} >"))
      }
    }
  }
}