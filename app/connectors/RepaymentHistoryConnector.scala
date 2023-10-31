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
import models.core.Nino
import models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseError, ITSAStatusResponseModel}
import models.repaymentHistory.{RepaymentHistoryErrorModel, RepaymentHistoryModel, RepaymentHistoryResponseModel}
import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RepaymentHistoryConnector @Inject()(val http: HttpClient,
                                          val appConfig: FrontendAppConfig
                                           )(implicit val ec: ExecutionContext) extends RawResponseReads {

  def getAllRepaymentHistoryUrl(nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/repayments/$nino"
  }

  def getRepaymentHistoryByIdUrl(nino: String, repaymentId: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/repayments/$nino/repaymentId/$repaymentId"
  }

  def getRepaymentHistoryByRepaymentId(nino: Nino, repaymentId: String)
                                      (implicit headerCarrier: HeaderCarrier): Future[RepaymentHistoryResponseModel] = {
    http.GET[HttpResponse](getRepaymentHistoryByIdUrl(nino.value, repaymentId))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          response.json.validate[RepaymentHistoryModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getRepaymentHistoryByRepaymentId] - Json validation error parsing repayment response, error $invalid")
              RepaymentHistoryErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing repayment response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getRepaymentHistoryByRepaymentId] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getPaymentHistoryByRepaymentId] - Response status: ${response.status}, body: ${response.body}")
          }
          RepaymentHistoryErrorModel(response.status, response.body)
      }
    }
  }

  def getRepaymentHistoryByNino(nino: Nino)
                               (implicit headerCarrier: HeaderCarrier): Future[RepaymentHistoryResponseModel] = {


    http.GET[HttpResponse](getAllRepaymentHistoryUrl(nino.value))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          response.json.validate[RepaymentHistoryModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getRepaymentHistoryByRepaymentDate] - Json validation error parsing repayment response, error $invalid")
              RepaymentHistoryErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing repayment response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getRepaymentHistoryByRepaymentDate] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getPaymentHistoryByRepaymentDate] - Response status: ${response.status}, body: ${response.body}")
          }
          RepaymentHistoryErrorModel(response.status, response.body)
      }
    }
  }

}