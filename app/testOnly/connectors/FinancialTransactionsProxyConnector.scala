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

package testOnly.connectors

import javax.inject.{Inject, Singleton}

import connectors.RawResponseReads
import testOnly.TestOnlyAppConfig
import testOnly.models.{DataModel, SchemaModel}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class FinancialTransactionsProxyConnector @Inject()(val appConfig: TestOnlyAppConfig,
                                                    val http: HttpClient) extends RawResponseReads {

  def getFinancialData(mtditid: String,
                       onlyOpenItems: Option[String],
                       dateFrom: Option[String],
                       dateTo: Option[String],
                       includeLocks: Option[String],
                       calculateAccruedInterest: Option[String],
                       customerPaymentInfo: Option[String]): Future[HttpResponse] = {

      lazy val url = s"${appConfig.ftUrl}/financial-transactions/it/$mtditid?" +
        onlyOpenItems.fold("")(x => s"&onlyOpenItems=$x") +
        dateFrom.fold("")(x => s"&dateFrom=$x") +
        dateTo.fold("")(x => s"&dateTo=$x") +
        includeLocks.fold("")(x => s"&includeLocks=$x") +
        calculateAccruedInterest.fold("")(x => s"&calculateAccruedInterest=$x") +
        customerPaymentInfo.fold("")(x => s"&customerPaymentInformation=$x")

      http.GET(url)
  }
}
