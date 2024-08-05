/*
 * Copyright 2024 HM Revenue & Customs
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
import models.chargeHistory.ChargesHistoryResponse.{ChargesHistoryResponse, ChargesHistoryResponseReads}
import models.chargeHistory.{ChargeHistoryResponseModel, ChargesHistoryErrorModel, ChargesHistoryModel}
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, StringContextOps}

import javax.inject.Inject
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

class ChargeHistoryConnector @Inject()(val http: HttpClient,
                                       val httpV2: HttpClientV2,
                                       val appConfig: FrontendAppConfig
                                      )(implicit val ec: ExecutionContext) extends RawResponseReads {

  def getChargeHistoryUrl(nino: String, chargeReference: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/charge-history/$nino/chargeReference/$chargeReference"
  }

  def getChargeHistory(nino: String, chargeRef: Option[String])
                      (implicit headerCarrier: HeaderCarrier): Future[ChargeHistoryResponseModel] = {
    chargeRef match {
      case Some(chargeReference) => val url = getChargeHistoryUrl(nino, chargeReference)
        Logger("application").debug(s"GET $url")

        httpV2
          .get(url"$url")
          .execute[ChargesHistoryResponse]
          .recover {
            case ex =>
              Logger("application").error(s"Unexpected failure, ${ex.getMessage}", ex)
              ChargesHistoryErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
          }
      case None => Logger("application").info("No charge history found as no chargeReference value supplied")
        Future(ChargesHistoryModel("", "", "", None))
    }

  }

}
