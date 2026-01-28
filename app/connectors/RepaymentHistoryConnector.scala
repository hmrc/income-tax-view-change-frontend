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
import models.repaymentHistory._
import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RepaymentHistoryConnector @Inject()(val http: HttpClientV2,
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
    http.get(url"${getRepaymentHistoryByIdUrl(nino.value, repaymentId)}")
      .setHeader("Accept" -> "application/vnd.hmrc.2.0+json")
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          response.json.validate[HipRepaymentHistoryResponse]
            .map(hip =>
              RepaymentHistoryModel(hip.etmp_Response_Details.repaymentsViewerDetails)
            )
            .orElse(
              response.json.validate[RepaymentHistoryModel]
            )
            .fold(
              invalid => {
                Logger("application").error(s"Json validation error parsing repayment response, error $invalid")
                RepaymentHistoryErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing repayment response")
              },
              valid => valid
            )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"Response status: ${response.status}, body: ${response.body}")
          }
          RepaymentHistoryErrorModel(response.status, response.body)
      }
    }
  }

  def getRepaymentHistoryByNino(nino: Nino)
                               (implicit headerCarrier: HeaderCarrier): Future[RepaymentHistoryResponseModel] = {


    http.get(url"${getAllRepaymentHistoryUrl(nino.value)}")
      .setHeader("Accept" -> "application/vnd.hmrc.2.0+json")
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          response.json.validate[HipRepaymentHistoryResponse]
            .map(hip =>
              RepaymentHistoryModel(hip.etmp_Response_Details.repaymentsViewerDetails)
            )
            .orElse(
              response.json.validate[RepaymentHistoryModel]
            )
            .fold(
              invalid => {
                Logger("application").error(s"Json validation error parsing repayment response, error $invalid")
                RepaymentHistoryErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing repayment response")
              },
              valid => valid
            )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"Response status: ${response.status}, body: ${response.body}")
          }
          RepaymentHistoryErrorModel(response.status, response.body)
      }
    }
  }

}