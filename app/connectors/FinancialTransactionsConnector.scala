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
import models.{FinancialTransactionsErrorModel, FinancialTransactionsModel, FinancialTransactionsResponseModel}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.api.Logger
import play.api.http.Status.OK

import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import play.api.http.Status


@Singleton
class FinancialTransactionsConnector @Inject()(val http: HttpClient,
                                               val config: FrontendAppConfig) extends RawResponseReads {

  lazy val getFinancialTransactionsUrl: String => String = nino => s"${config.ftUrl}/financial-transactions/it/$nino"

  def getFinancialTransactions(nino: String)(implicit headerCarrier: HeaderCarrier):Future[FinancialTransactionsResponseModel] = {

    val url = getFinancialTransactionsUrl(nino)

    Logger.debug(s"[FinancialTransactionsConnector][getFincancialTransactions] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"), implicitly) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[FinancialTransactionsConnector][getFinancialTransactions] - RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[FinancialTransactionsModel].fold(
              invalid => {
                Logger.warn(s"[FinancialTransactionsConnector][getFinancialTransactions] - Json Validation Error. Parsing Financial Transactions Response. Invalid=$invalid")
                FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Financial Transactions response")
              },
              valid => valid
            )
          case _ =>
            Logger.debug(s"[FinancialTransactionsConnector][getFinancialTransactions] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.warn(s"[FinancialTransactionsConnector][getFinancialTransactions] - Response status: [${response.status}] returned from Financial Transactions call")
            FinancialTransactionsErrorModel(response.status, response.body)

        }
    } recover {
      case _ =>
        Logger.warn(s"[FinancialTransactionsConnector][getFinancialTransactions] - Unexpected future failed error")
        FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }

  }



}

